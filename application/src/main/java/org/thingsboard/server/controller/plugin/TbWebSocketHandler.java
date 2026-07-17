/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller.plugin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.config.WebSocketConfiguration;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.jwt.JwtAuthenticationProvider;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;
import org.thingsboard.server.service.ws.AuthCmd;
import org.thingsboard.server.service.ws.SessionEvent;
import org.thingsboard.server.service.ws.WebSocketMsgEndpoint;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.WebSocketSessionType;
import org.thingsboard.server.service.ws.WsCommandsWrapper;
import org.thingsboard.server.service.ws.notification.cmd.NotificationCmdsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.TelemetryCmdsWrapper;

import javax.annotation.PostConstruct;
import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.service.ws.DefaultWebSocketService.NUMBER_OF_PING_ATTEMPTS;

/**
 * 中文说明：
 * 1. `TbWebSocketHandler` 是 ThingsBoard Application 中处理 `Tb Web Socket` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TextWebSocketHandler`、`WebSocketMsgEndpoint`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class TbWebSocketHandler extends TextWebSocketHandler implements WebSocketMsgEndpoint {

    private final ConcurrentMap<String, SessionMetaData> internalSessionMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> externalSessionMap = new ConcurrentHashMap<>();

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired @Lazy
    private WebSocketService webSocketService;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    private TbTenantProfileCache tenantProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private RateLimitService rateLimitService;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Autowired
    private JwtAuthenticationProvider authenticationProvider;

    /**
     * 结束时间戳，用于限定查询或统计的终点。
     */
    @Value("${server.ws.send_timeout:5000}")
    private long sendTimeout;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${server.ws.ping_timeout:30000}")
    private long pingTimeout;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Value("${server.ws.max_queue_messages_per_session:1000}")
    private int wsMaxQueueMessagesPerSession;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${server.ws.auth_timeout_ms:10000}")
    private int authTimeoutMs;

    private final ConcurrentMap<String, WebSocketSessionRef> blacklistedSessions = new ConcurrentHashMap<>();

    private final ConcurrentMap<TenantId, Set<String>> tenantSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<CustomerId, Set<String>> customerSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> regularUserSessionsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserId, Set<String>> publicUserSessionsMap = new ConcurrentHashMap<>();

    /**
     * `pendingSessions` 字段，保存当前对象的对应属性。
     */
    private Cache<String, SessionMetaData> pendingSessions;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        pendingSessions = Caffeine.newBuilder()
                .expireAfterWrite(authTimeoutMs, TimeUnit.MILLISECONDS)
                .<String, SessionMetaData>removalListener((sessionId, sessionMd, removalCause) -> {
                    if (removalCause == RemovalCause.EXPIRED && sessionMd != null) {
                        try {
                            close(sessionMd.sessionRef, CloseStatus.POLICY_VIOLATION);
                        } catch (IOException e) {
                            log.warn("IO error", e);
                        }
                    }
                })
                .build();
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `session`：会话对象。
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SessionMetaData sessionMd = getSessionMd(session.getId());
            if (sessionMd == null) {
                log.trace("[{}] Failed to find session", session.getId());
                session.close(CloseStatus.SERVER_ERROR.withReason("Session not found!"));
                return;
            }
            sessionMd.onMsg(message.getPayload());
        } catch (IOException e) {
            log.warn("IO error", e);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `sessionMd`：会话对象。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processMsg(SessionMetaData sessionMd, String msg) throws IOException {
        WebSocketSessionRef sessionRef = sessionMd.sessionRef;
        WsCommandsWrapper cmdsWrapper;
        try {
            switch (sessionRef.getSessionType()) {
                case GENERAL:
                    cmdsWrapper = JacksonUtil.fromString(msg, WsCommandsWrapper.class);
                    break;
                case TELEMETRY:
                    cmdsWrapper = JacksonUtil.fromString(msg, TelemetryCmdsWrapper.class).toCommonCmdsWrapper();
                    break;
                case NOTIFICATIONS:
                    cmdsWrapper = JacksonUtil.fromString(msg, NotificationCmdsWrapper.class).toCommonCmdsWrapper();
                    break;
                default:
                    return;
            }
        } catch (Exception e) {
            log.debug("{} Failed to decode subscription cmd: {}", sessionRef, e.getMessage(), e);
            if (sessionRef.getSecurityCtx() != null) {
                webSocketService.sendError(sessionRef, 1, SubscriptionErrorCode.BAD_REQUEST, "Failed to parse the payload");
            } else {
                close(sessionRef, CloseStatus.BAD_DATA.withReason(e.getMessage()));
            }
            return;
        }

        if (sessionRef.getSecurityCtx() != null) {
            log.trace("{} Processing {}", sessionRef, msg);
            webSocketService.handleCommands(sessionRef, cmdsWrapper);
        } else {
            AuthCmd authCmd = cmdsWrapper.getAuthCmd();
            if (authCmd == null) {
                close(sessionRef, CloseStatus.POLICY_VIOLATION.withReason("Auth cmd is missing"));
                return;
            }
            log.trace("{} Authenticating session", sessionRef);
            SecurityUser securityCtx;
            try {
                securityCtx = authenticationProvider.authenticate(authCmd.getToken());
            } catch (Exception e) {
                close(sessionRef, CloseStatus.BAD_DATA.withReason(e.getMessage()));
                return;
            }
            sessionRef.setSecurityCtx(securityCtx);
            pendingSessions.invalidate(sessionMd.session.getId());
            establishSession(sessionMd.session, sessionRef, sessionMd);

            webSocketService.handleCommands(sessionRef, cmdsWrapper);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `session`：会话对象。
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        try {
            SessionMetaData sessionMd = getSessionMd(session.getId());
            if (sessionMd != null) {
                log.trace("{} Processing pong response {}", sessionMd.sessionRef, message.getPayload());
                sessionMd.processPongMessage(System.currentTimeMillis());
            } else {
                log.trace("[{}] Failed to find session", session.getId());
                session.close(CloseStatus.SERVER_ERROR.withReason("Session not found!"));
            }
        } catch (IOException e) {
            log.warn("IO error", e);
        }
    }

    /**
     * 功能：执行 `afterConnectionEstablished` 对应的处理。
     * 参数：
     * - `session`：会话对象。
     * 返回：无。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        try {
            if (session instanceof NativeWebSocketSession) {
                Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
                if (nativeSession != null) {
                    nativeSession.getAsyncRemote().setSendTimeout(sendTimeout);
                }
            }
            WebSocketSessionRef sessionRef = toRef(session);
            log.debug("[{}][{}] Session opened from address: {}", sessionRef.getSessionId(), session.getId(), session.getRemoteAddress());
            establishSession(session, sessionRef, null);
        } catch (InvalidParameterException e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.BAD_DATA.withReason(e.getMessage()));
        } catch (JwtExpiredTokenException e) {
            log.trace("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason(e.getMessage()));
        } catch (Exception e) {
            log.warn("[{}] Failed to start session", session.getId(), e);
            session.close(CloseStatus.SERVER_ERROR.withReason(e.getMessage()));
        }
    }

    /**
     * 功能：执行 `establishSession` 对应的处理。
     * 参数：
     * - `session`：会话对象。
     * - `sessionRef`：会话对象。
     * - `sessionMd`：会话对象。
     * 返回：无。
     */
    private void establishSession(WebSocketSession session, WebSocketSessionRef sessionRef, SessionMetaData sessionMd) throws IOException {
        if (sessionRef.getSecurityCtx() != null) {
            if (!checkLimits(session, sessionRef)) {
                return;
            }
            int maxMsgQueueSize = Optional.ofNullable(getTenantProfileConfiguration(sessionRef))
                    .map(DefaultTenantProfileConfiguration::getWsMsgQueueLimitPerSession)
                    .filter(profileLimit -> profileLimit > 0 && profileLimit < wsMaxQueueMessagesPerSession)
                    .orElse(wsMaxQueueMessagesPerSession);
            if (sessionMd == null) {
                sessionMd = new SessionMetaData(session, sessionRef);
            }
            sessionMd.setMaxMsgQueueSize(maxMsgQueueSize);

            internalSessionMap.put(session.getId(), sessionMd);
            externalSessionMap.put(sessionRef.getSessionId(), session.getId());
            processInWebSocketService(sessionRef, SessionEvent.onEstablished());
            log.info("[{}][{}][{}][{}] Session established from address: {}", sessionRef.getSecurityCtx().getTenantId(),
                    sessionRef.getSecurityCtx().getId(), sessionRef.getSessionId(), session.getId(), session.getRemoteAddress());
        } else {
            sessionMd = new SessionMetaData(session, sessionRef);
            pendingSessions.put(session.getId(), sessionMd);
            externalSessionMap.put(sessionRef.getSessionId(), session.getId());
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `session`：会话对象。
     * - `tError`：错误信息。
     * 返回：无。
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable tError) throws Exception {
        super.handleTransportError(session, tError);
        SessionMetaData sessionMd = getSessionMd(session.getId());
        if (sessionMd != null) {
            processInWebSocketService(sessionMd.sessionRef, SessionEvent.onError(tError));
        } else {
            log.trace("[{}] Failed to find session", session.getId());
        }
        log.trace("[{}] Session transport error", session.getId(), tError);
    }

    /**
     * 功能：执行 `afterConnectionClosed` 对应的处理。
     * 参数：
     * - `session`：会话对象。
     * - `closeStatus`：`closeStatus` 参数。
     * 返回：无。
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        super.afterConnectionClosed(session, closeStatus);
        SessionMetaData sessionMd = internalSessionMap.remove(session.getId());
        if (sessionMd == null) {
            sessionMd = pendingSessions.asMap().remove(session.getId());
        }
        if (sessionMd != null) {
            externalSessionMap.remove(sessionMd.sessionRef.getSessionId());
            if (sessionMd.sessionRef.getSecurityCtx() != null) {
                cleanupLimits(session, sessionMd.sessionRef);
                processInWebSocketService(sessionMd.sessionRef, SessionEvent.onClosed());
            }
            log.info("{} Session is closed", sessionMd.sessionRef);
        } else {
            log.info("[{}] Session is closed", session.getId());
        }
    }

    /**
     * 功能：处理服务。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `event`：`event` 参数。
     * 返回：无。
     */
    private void processInWebSocketService(WebSocketSessionRef sessionRef, SessionEvent event) {
        if (sessionRef.getSecurityCtx() == null) {
            return;
        }
        try {
            webSocketService.handleSessionEvent(sessionRef, event);
        } catch (BeanCreationNotAllowedException e) {
            log.warn("{} Failed to close session due to possible shutdown state", sessionRef);
        }
    }

    /**
     * 功能：执行 `toRef` 对应的处理。
     * 参数：
     * - `session`：会话对象。
     * 返回：处理结果。
     */
    private WebSocketSessionRef toRef(WebSocketSession session) {
        String path = session.getUri().getPath();
        WebSocketSessionType sessionType;
        if (path.equals(WebSocketConfiguration.WS_API_ENDPOINT)) {
            sessionType = WebSocketSessionType.GENERAL;
        } else {
            String type = StringUtils.substringAfter(path, WebSocketConfiguration.WS_PLUGINS_ENDPOINT);
            sessionType = WebSocketSessionType.forName(type)
                    .orElseThrow(() -> new InvalidParameterException("Unknown session type"));
        }

        SecurityUser securityCtx = null;
        String token = StringUtils.substringAfter(session.getUri().getQuery(), "token=");
        if (StringUtils.isNotEmpty(token)) {
            securityCtx = authenticationProvider.authenticate(token);
        }
        return WebSocketSessionRef.builder()
                .sessionId(UUID.randomUUID().toString())
                .securityCtx(securityCtx)
                .localAddress(session.getLocalAddress())
                .remoteAddress(session.getRemoteAddress())
                .sessionType(sessionType)
                .build();
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `internalSessionId`：会话ID。
     * 返回：处理结果。
     */
    private SessionMetaData getSessionMd(String internalSessionId) {
        SessionMetaData sessionMd = internalSessionMap.get(internalSessionId);
        if (sessionMd == null) {
            sessionMd = pendingSessions.getIfPresent(internalSessionId);
        }
        return sessionMd;
    }

    /**
     * 中文说明：
     * 1. `SessionMetaData` 是 ThingsBoard Application 中承载会话信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 直接依赖的类型边界包括 `SendHandler`。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    class SessionMetaData implements SendHandler {
        /**
         * 会话，保存当前连接或交互过程的会话信息。
         */
        private final WebSocketSession session;
        private final RemoteEndpoint.Async asyncRemote;
        /**
         * 会话，保存当前连接或交互过程的会话信息。
         */
        private final WebSocketSessionRef sessionRef;

        final AtomicBoolean isSending = new AtomicBoolean(false);
        private final Queue<TbWebSocketMsg<?>> outboundMsgQueue = new ConcurrentLinkedQueue<>();
        private final AtomicInteger outboundMsgQueueSize = new AtomicInteger();
        /**
         * 队列，承载当前步骤需要处理的内容。
         */
        @Setter
        private int maxMsgQueueSize = wsMaxQueueMessagesPerSession;

        private final Queue<String> inboundMsgQueue = new ConcurrentLinkedQueue<>();
        private final Lock inboundMsgQueueProcessorLock = new ReentrantLock();

        /**
         * 时间，用于控制时间范围或等待时长。
         */
        private volatile long lastActivityTime;

        SessionMetaData(WebSocketSession session, WebSocketSessionRef sessionRef) {
            super();
            this.session = session;
            Session nativeSession = ((NativeWebSocketSession) session).getNativeSession(Session.class);
            this.asyncRemote = nativeSession.getAsyncRemote();
            this.sessionRef = sessionRef;
            this.lastActivityTime = System.currentTimeMillis();
        }

        /**
         * 功能：发送或提交`Ping`。
         * 参数：
         * - `currentTime`：`currentTime` 参数。
         * 返回：无。
         */
        void sendPing(long currentTime) {
            try {
                long timeSinceLastActivity = currentTime - lastActivityTime;
                if (timeSinceLastActivity >= pingTimeout) {
                    log.warn("{} Closing session due to ping timeout", sessionRef);
                    closeSession(CloseStatus.SESSION_NOT_RELIABLE);
                } else if (timeSinceLastActivity >= pingTimeout / NUMBER_OF_PING_ATTEMPTS) {
                    sendMsg(TbWebSocketPingMsg.INSTANCE);
                }
            } catch (Exception e) {
                log.trace("{} Failed to send ping msg", sessionRef, e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        /**
         * 功能：停止或关闭会话。
         * 参数：
         * - `reason`：`reason` 参数。
         * 返回：无。
         */
        void closeSession(CloseStatus reason) {
            try {
                close(this.sessionRef, reason);
            } catch (IOException ioe) {
                log.trace("{} Session transport error", sessionRef, ioe);
            } finally {
                outboundMsgQueue.clear();
            }
        }

        /**
         * 功能：处理消息。
         * 参数：
         * - `currentTime`：`currentTime` 参数。
         * 返回：无。
         */
        void processPongMessage(long currentTime) {
            lastActivityTime = currentTime;
        }

        /**
         * 功能：发送或提交消息。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        void sendMsg(String msg) {
            sendMsg(new TbWebSocketTextMsg(msg));
        }

        /**
         * 功能：发送或提交消息。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        void sendMsg(TbWebSocketMsg<?> msg) {
            if (outboundMsgQueueSize.get() < maxMsgQueueSize) {
                outboundMsgQueue.add(msg);
                outboundMsgQueueSize.incrementAndGet();
                processNextMsg();
            } else {
                log.info("{} Session closed due to updates queue size exceeded", sessionRef);
                closeSession(CloseStatus.POLICY_VIOLATION.withReason("Max pending updates limit reached!"));
            }
        }

        /**
         * 功能：发送或提交消息。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        private void sendMsgInternal(TbWebSocketMsg<?> msg) {
            try {
                if (TbWebSocketMsgType.TEXT.equals(msg.getType())) {
                    TbWebSocketTextMsg textMsg = (TbWebSocketTextMsg) msg;
                    this.asyncRemote.sendText(textMsg.getMsg(), this);
                    // isSending status will be reset in the onResult method by call back
                } else {
                    TbWebSocketPingMsg pingMsg = (TbWebSocketPingMsg) msg;
                    this.asyncRemote.sendPing(pingMsg.getMsg()); // blocking call
                    isSending.set(false);
                    processNextMsg();
                }
            } catch (Exception e) {
                log.trace("{} Failed to send msg", sessionRef, e);
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
            }
        }

        /**
         * 功能：处理`on Result`。
         * 参数：
         * - `result`：`result` 参数。
         * 返回：无。
         */
        @Override
        public void onResult(SendResult result) {
            if (!result.isOK()) {
                log.trace("{} Failed to send msg", sessionRef, result.getException());
                closeSession(CloseStatus.SESSION_NOT_RELIABLE);
                return;
            }

            isSending.set(false);
            processNextMsg();
        }

        /**
         * 功能：处理消息。
         * 参数：无。
         * 返回：无。
         */
        private void processNextMsg() {
            if (outboundMsgQueue.isEmpty() || !isSending.compareAndSet(false, true)) {
                return;
            }
            TbWebSocketMsg<?> msg = outboundMsgQueue.poll();
            if (msg != null) {
                outboundMsgQueueSize.decrementAndGet();
                sendMsgInternal(msg);
            } else {
                isSending.set(false);
            }
        }

        /**
         * 功能：处理消息。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        public void onMsg(String msg) throws IOException {
            inboundMsgQueue.add(msg);
            tryProcessInboundMsgs();
        }

        /**
         * 功能：执行 `tryProcessInboundMsgs` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        void tryProcessInboundMsgs() throws IOException {
            while (!inboundMsgQueue.isEmpty()) {
                if (inboundMsgQueueProcessorLock.tryLock()) {
                    try {
                        String msg;
                        while ((msg = inboundMsgQueue.poll()) != null) {
                            processMsg(this, msg);
                        }
                    } finally {
                        inboundMsgQueueProcessorLock.unlock();
                    }
                } else {
                    return;
                }
            }
        }
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `subscriptionId`：订阅ID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void send(WebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException {
        log.debug("{} Sending {}", sessionRef, msg);
        String externalId = sessionRef.getSessionId();
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                TenantId tenantId = sessionRef.getSecurityCtx().getTenantId();
                if (!rateLimitService.checkRateLimit(LimitedApi.WS_UPDATES_PER_SESSION, tenantId, (Object) sessionRef.getSessionId())) {
                    if (blacklistedSessions.putIfAbsent(externalId, sessionRef) == null) {
                        log.info("{} Failed to process session update. Max session updates limit reached", sessionRef);
                        sessionMd.sendMsg("{\"subscriptionId\":" + subscriptionId + ", \"errorCode\":" + ThingsboardErrorCode.TOO_MANY_UPDATES.getErrorCode() + ", \"errorMsg\":\"Too many updates!\"}");
                    }
                    return;
                } else {
                    log.debug("{} Session is no longer blacklisted.", sessionRef);
                    blacklistedSessions.remove(externalId);
                }
                sessionMd.sendMsg(msg);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    /**
     * 功能：发送或提交`Ping`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `currentTime`：`currentTime` 参数。
     * 返回：无。
     */
    @Override
    public void sendPing(WebSocketSessionRef sessionRef, long currentTime) throws IOException {
        String externalId = sessionRef.getSessionId();
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = internalSessionMap.get(internalId);
            if (sessionMd != null) {
                sessionMd.sendPing(currentTime);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `reason`：`reason` 参数。
     * 返回：无。
     */
    @Override
    public void close(WebSocketSessionRef sessionRef, CloseStatus reason) throws IOException {
        String externalId = sessionRef.getSessionId();
        log.debug("{} Processing close request", sessionRef.toString());
        String internalId = externalSessionMap.get(externalId);
        if (internalId != null) {
            SessionMetaData sessionMd = getSessionMd(internalId);
            if (sessionMd != null) {
                sessionMd.session.close(reason);
            } else {
                log.warn("[{}][{}] Failed to find session by internal id", externalId, internalId);
            }
        } else {
            log.warn("[{}] Failed to find session by external id", externalId);
        }
    }

    /**
     * 功能：校验`Limits`。
     * 参数：
     * - `session`：会话对象。
     * - `sessionRef`：会话对象。
     * 返回：判断结果。
     */
    private boolean checkLimits(WebSocketSession session, WebSocketSessionRef sessionRef) throws IOException {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) {
            return true;
        }
        boolean limitAllowed;
        String sessionId = session.getId();
        if (tenantProfileConfiguration.getMaxWsSessionsPerTenant() > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                limitAllowed = tenantSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerTenant();
                if (limitAllowed) {
                    tenantSessions.add(sessionId);
                }
            }
            if (!limitAllowed) {
                log.info("{} Failed to start session. Max tenant sessions limit reached", sessionRef.toString());
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Max tenant sessions limit reached!"));
                return false;
            }
        }

        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (tenantProfileConfiguration.getMaxWsSessionsPerCustomer() > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    limitAllowed = customerSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerCustomer();
                    if (limitAllowed) {
                        customerSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max customer sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max customer sessions limit reached"));
                    return false;
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerRegularUser() > 0
                    && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    limitAllowed = regularUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerRegularUser();
                    if (limitAllowed) {
                        regularUserSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max regular user sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max regular user sessions limit reached"));
                    return false;
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerPublicUser() > 0
                    && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    limitAllowed = publicUserSessions.size() < tenantProfileConfiguration.getMaxWsSessionsPerPublicUser();
                    if (limitAllowed) {
                        publicUserSessions.add(sessionId);
                    }
                }
                if (!limitAllowed) {
                    log.info("{} Failed to start session. Max public user sessions limit reached", sessionRef.toString());
                    session.close(CloseStatus.POLICY_VIOLATION.withReason("Max public user sessions limit reached"));
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 功能：删除或清理`Limits`。
     * 参数：
     * - `session`：会话对象。
     * - `sessionRef`：会话对象。
     * 返回：无。
     */
    private void cleanupLimits(WebSocketSession session, WebSocketSessionRef sessionRef) {
        var tenantProfileConfiguration = getTenantProfileConfiguration(sessionRef);
        if (tenantProfileConfiguration == null) return;

        String sessionId = session.getId();
        rateLimitService.cleanUp(LimitedApi.WS_UPDATES_PER_SESSION, sessionRef.getSessionId());
        blacklistedSessions.remove(sessionRef.getSessionId());
        if (tenantProfileConfiguration.getMaxWsSessionsPerTenant() > 0) {
            Set<String> tenantSessions = tenantSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getTenantId(), id -> ConcurrentHashMap.newKeySet());
            synchronized (tenantSessions) {
                tenantSessions.remove(sessionId);
            }
        }
        if (sessionRef.getSecurityCtx().isCustomerUser()) {
            if (tenantProfileConfiguration.getMaxWsSessionsPerCustomer() > 0) {
                Set<String> customerSessions = customerSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getCustomerId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (customerSessions) {
                    customerSessions.remove(sessionId);
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerRegularUser() > 0 && UserPrincipal.Type.USER_NAME.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> regularUserSessions = regularUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (regularUserSessions) {
                    regularUserSessions.remove(sessionId);
                }
            }
            if (tenantProfileConfiguration.getMaxWsSessionsPerPublicUser() > 0 && UserPrincipal.Type.PUBLIC_ID.equals(sessionRef.getSecurityCtx().getUserPrincipal().getType())) {
                Set<String> publicUserSessions = publicUserSessionsMap.computeIfAbsent(sessionRef.getSecurityCtx().getId(), id -> ConcurrentHashMap.newKeySet());
                synchronized (publicUserSessions) {
                    publicUserSessions.remove(sessionId);
                }
            }
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `sessionRef`：会话对象。
     * 返回：处理结果。
     */
    private DefaultTenantProfileConfiguration getTenantProfileConfiguration(WebSocketSessionRef sessionRef) {
        return Optional.ofNullable(tenantProfileCache.get(sessionRef.getSecurityCtx().getTenantId()))
                .map(TenantProfile::getDefaultProfileConfiguration).orElse(null);
    }

}
