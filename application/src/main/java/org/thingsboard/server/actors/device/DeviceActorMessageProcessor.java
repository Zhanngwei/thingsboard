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
package org.thingsboard.server.actors.device;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.LinkedHashMapRemoveEldest;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKey;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.data.rpc.ToDeviceRpcRequestBody;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceCredentialsUpdateNotificationMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.common.msg.timeout.DeviceActorServerSideRpcTimeoutMsg;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeUpdateNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceSessionsCacheEntry;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.SessionCloseNotificationProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEvent;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionSubscriptionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SessionType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseStatusMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportUpdateCredentialsProto;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.gen.transport.TransportProtos.UplinkNotificationMsg;
import org.thingsboard.server.service.rpc.RpcSubmitStrategy;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`DeviceActorMessageProcessor` 是ThingsBoard Application 模块中的Actor 通信与消息处理类型，用于管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 生命周期：由 ActorService 创建，随组件初始化、消息投递和停止流程变化。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Actor / Command。
 */
@Slf4j
public class DeviceActorMessageProcessor extends AbstractContextAwareMsgProcessor {

    /**
     * 会话常量，用于统一引用固定值。
     */
    static final String SESSION_TIMEOUT_MESSAGE = "session timeout!";
    final TenantId tenantId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    final DeviceId deviceId;
    final LinkedHashMapRemoveEldest<UUID, SessionInfoMetaData> sessions;
    /**
     * 属性映射关系，用于按键查找对应值。
     */
    private final Map<UUID, SessionInfo> attributeSubscriptions;
    private final Map<UUID, SessionInfo> rpcSubscriptions;
    /**
     * 设备映射关系，用于按键查找对应值。
     */
    private final Map<Integer, ToDeviceRpcRequestMetadata> toDeviceRpcPendingMap;
    private final boolean rpcSequential;
    /**
     * RPC，表示当前对象的对应属性。
     */
    private final RpcSubmitStrategy rpcSubmitStrategy;
    private final ScheduledExecutorService scheduler;

    /**
     * RPC，表示当前对象的对应属性。
     */
    private int rpcSeq = 0;
    private String deviceName;
    /**
     * 设备，用于区分不同处理分支。
     */
    private String deviceType;
    private TbMsgMetaData defaultMetaData;
    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    private EdgeId edgeId;
    private ScheduledFuture<?> awaitRpcResponseFuture;

    DeviceActorMessageProcessor(ActorSystemContext systemContext, TenantId tenantId, DeviceId deviceId) {
        super(systemContext);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.rpcSubmitStrategy = RpcSubmitStrategy.parse(systemContext.getRpcSubmitStrategy());
        this.rpcSequential = !rpcSubmitStrategy.equals(RpcSubmitStrategy.BURST);
        this.attributeSubscriptions = new HashMap<>();
        this.rpcSubscriptions = new HashMap<>();
        this.toDeviceRpcPendingMap = new LinkedHashMap<>();
        this.sessions = new LinkedHashMapRemoveEldest<>(systemContext.getMaxConcurrentSessionsPerDevice(), this::notifyTransportAboutClosedSessionMaxSessionsLimit);
        this.scheduler = systemContext.getScheduler();
        if (initAttributes()) {
            restoreSessions();
        }
    }

    /**
     * 功能：初始化或启动`Attributes`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean initAttributes() {
        Device device = systemContext.getDeviceService().findDeviceById(tenantId, deviceId);
        if (device != null) {
            this.deviceName = device.getName();
            this.deviceType = device.getType();
            this.defaultMetaData = new TbMsgMetaData();
            this.defaultMetaData.putValue("deviceName", deviceName);
            this.defaultMetaData.putValue("deviceType", deviceType);
            if (systemContext.isEdgesEnabled()) {
                this.edgeId = findRelatedEdgeId();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：无。
     * 返回：处理结果。
     */
    private EdgeId findRelatedEdgeId() {
        List<EntityRelation> result =
                systemContext.getRelationService().findByToAndType(tenantId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        if (result != null && result.size() > 0) {
            EntityRelation relationToEdge = result.get(0);
            if (relationToEdge.getFrom() != null && relationToEdge.getFrom().getId() != null) {
                log.trace("[{}][{}] found edge [{}] for device", tenantId, deviceId, relationToEdge.getFrom().getId());
                return new EdgeId(relationToEdge.getFrom().getId());
            } else {
                log.trace("[{}][{}] edge relation is empty {}", tenantId, deviceId, relationToEdge);
            }
        } else {
            log.trace("[{}][{}] device doesn't have any related edge", tenantId, deviceId);
        }
        return null;
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `context`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processRpcRequest(TbActorCtx context, ToDeviceRpcRequestActorMsg msg) {
        ToDeviceRpcRequest request = msg.getMsg();
        UUID rpcId = request.getId();
        log.debug("[{}][{}] Received RPC request to process ...", deviceId, rpcId);
        ToDeviceRpcRequestMsg rpcRequest = createToDeviceRpcRequestMsg(request);

        long timeout = request.getExpirationTime() - System.currentTimeMillis();
        boolean persisted = request.isPersisted();

        if (timeout <= 0) {
            log.debug("[{}][{}] Ignoring message due to exp time reached, {}", deviceId, rpcId, request.getExpirationTime());
            if (persisted) {
                createRpc(request, RpcStatus.EXPIRED);
            }
            return;
        } else if (persisted) {
            createRpc(request, RpcStatus.QUEUED);
        }

        boolean sent = false;
        int requestId = rpcRequest.getRequestId();
        if (systemContext.isEdgesEnabled() && edgeId != null) {
            log.debug("[{}][{}] device is related to edge: [{}]. Saving RPC request: [{}][{}] to edge queue", tenantId, deviceId, edgeId.getId(), rpcId, requestId);
            try {
                if (systemContext.getEdgeService().isEdgeActiveAsync(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE).get()) {
                    saveRpcRequestToEdgeQueue(request, requestId).get();
                } else {
                    log.error("[{}][{}][{}] Failed to save RPC request to edge queue {}. The Edge is currently offline or unreachable", tenantId, deviceId, edgeId.getId(), request);
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("[{}][{}][{}] Failed to save RPC request to edge queue {}", tenantId, deviceId, edgeId.getId(), request, e);
            }
        } else if (isSendNewRpcAvailable()) {
            sent = rpcSubscriptions.size() > 0;
            Set<UUID> syncSessionSet = new HashSet<>();
            rpcSubscriptions.forEach((sessionId, sessionInfo) -> {
                log.debug("[{}][{}][{}][{}] send RPC request to transport ...", deviceId, sessionId, rpcId, requestId);
                sendToTransport(rpcRequest, sessionId, sessionInfo.getNodeId());
                if (SessionType.SYNC == sessionInfo.getType()) {
                    syncSessionSet.add(sessionId);
                }
            });
            log.trace("Rpc syncSessionSet [{}] subscription after sent [{}]", syncSessionSet, rpcSubscriptions);
            syncSessionSet.forEach(rpcSubscriptions::remove);
        }

        if (persisted) {
            ObjectNode response = JacksonUtil.newObjectNode();
            response.put("rpcId", rpcId.toString());
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, JacksonUtil.toString(response), null));
        }

        if (!persisted && request.isOneway() && sent) {
            log.debug("[{}] RPC command response sent [{}][{}]!", deviceId, rpcId, requestId);
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, null, null));
        } else {
            registerPendingRpcRequest(context, msg, sent, rpcRequest, timeout);
        }
        String rpcSent = sent ? "sent!" : "NOT sent!";
        log.debug("[{}][{}][{}] RPC request is {}", deviceId, rpcId, requestId, rpcSent);
    }

    /**
     * 功能：判断RPC。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isSendNewRpcAvailable() {
        switch (rpcSubmitStrategy) {
            case SEQUENTIAL_ON_ACK_FROM_DEVICE:
                return toDeviceRpcPendingMap.values().stream().filter(md -> !md.isDelivered()).findAny().isEmpty();
            case SEQUENTIAL_ON_RESPONSE_FROM_DEVICE:
                return toDeviceRpcPendingMap.values().stream().filter(ToDeviceRpcRequestMetadata::isDelivered).findAny().isEmpty();
            default:
                return true;
        }
    }

    /**
     * 功能：保存或创建RPC。
     * 参数：
     * - `request`：请求对象。
     * - `status`：`status` 参数。
     * 返回：无。
     */
    private void createRpc(ToDeviceRpcRequest request, RpcStatus status) {
        Rpc rpc = new Rpc(new RpcId(request.getId()));
        rpc.setCreatedTime(System.currentTimeMillis());
        rpc.setTenantId(tenantId);
        rpc.setDeviceId(deviceId);
        rpc.setExpirationTime(request.getExpirationTime());
        rpc.setRequest(JacksonUtil.valueToTree(request));
        rpc.setStatus(status);
        rpc.setAdditionalInfo(JacksonUtil.toJsonNode(request.getAdditionalInfo()));
        systemContext.getTbRpcService().save(tenantId, rpc);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `request`：请求对象。
     * 返回：处理结果。
     */
    private ToDeviceRpcRequestMsg createToDeviceRpcRequestMsg(ToDeviceRpcRequest request) {
        ToDeviceRpcRequestBody body = request.getBody();
        return ToDeviceRpcRequestMsg.newBuilder()
                .setRequestId(rpcSeq++)
                .setMethodName(body.getMethod())
                .setParams(body.getParams())
                .setExpirationTime(request.getExpirationTime())
                .setRequestIdMSB(request.getId().getMostSignificantBits())
                .setRequestIdLSB(request.getId().getLeastSignificantBits())
                .setOneway(request.isOneway())
                .setPersisted(request.isPersisted())
                .build();
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `responseMsg`：响应对象。
     * 返回：无。
     */
    void processRpcResponsesFromEdge(FromDeviceRpcResponseActorMsg responseMsg) {
        log.debug("[{}] Processing RPC command response from edge session", deviceId);
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(responseMsg.getRequestId());
        boolean success = requestMd != null;
        if (success) {
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(responseMsg.getMsg());
        } else {
            log.debug("[{}] RPC command response [{}] is stale!", deviceId, responseMsg.getRequestId());
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processRemoveRpc(RemoveRpcActorMsg msg) {
        UUID rpcId = msg.getRequestId();
        log.debug("[{}][{}] Received remove RPC request ...", deviceId, rpcId);
        Map.Entry<Integer, ToDeviceRpcRequestMetadata> entry = null;
        for (Map.Entry<Integer, ToDeviceRpcRequestMetadata> e : toDeviceRpcPendingMap.entrySet()) {
            if (e.getValue().getMsg().getMsg().getId().equals(rpcId)) {
                entry = e;
                break;
            }
        }

        if (entry != null) {
            Integer requestId = entry.getKey();
            if (entry.getValue().isDelivered()) {
                toDeviceRpcPendingMap.remove(requestId);
                if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                    clearAwaitRpcResponseScheduler();
                    sendNextPendingRequest(rpcId, requestId, "Removed pending RPC!");
                }
            } else {
                Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> firstRpc = getFirstRpc();
                if (firstRpc.isPresent() && requestId.equals(firstRpc.get().getKey())) {
                    toDeviceRpcPendingMap.remove(requestId);
                    sendNextPendingRequest(rpcId, requestId, "Removed pending RPC!");
                } else {
                    toDeviceRpcPendingMap.remove(requestId);
                }
            }
        }
    }

    /**
     * 功能：保存或创建RPC。
     * 参数：
     * - `context`：处理上下文。
     * - `msg`：待处理消息。
     * - `sent`：`sent` 参数。
     * - `rpcRequest`：请求对象。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void registerPendingRpcRequest(TbActorCtx context, ToDeviceRpcRequestActorMsg msg, boolean sent, ToDeviceRpcRequestMsg rpcRequest, long timeout) {
        int requestId = rpcRequest.getRequestId();
        UUID rpcId = new UUID(rpcRequest.getRequestIdMSB(), rpcRequest.getRequestIdLSB());
        log.debug("[{}][{}][{}] Registering pending RPC request...", deviceId, rpcId, requestId);
        toDeviceRpcPendingMap.put(requestId, new ToDeviceRpcRequestMetadata(msg, sent));
        DeviceActorServerSideRpcTimeoutMsg timeoutMsg = new DeviceActorServerSideRpcTimeoutMsg(requestId, timeout);
        scheduleMsgWithDelay(context, timeoutMsg, timeoutMsg.getTimeout());
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processServerSideRpcTimeout(DeviceActorServerSideRpcTimeoutMsg msg) {
        Integer requestId = msg.getId();
        var requestMd = toDeviceRpcPendingMap.remove(requestId);
        if (requestMd != null) {
            var toDeviceRpcRequest = requestMd.getMsg().getMsg();
            UUID rpcId = toDeviceRpcRequest.getId();
            log.debug("[{}][{}][{}] RPC request timeout detected!", deviceId, rpcId, requestId);
            if (toDeviceRpcRequest.isPersisted()) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), RpcStatus.EXPIRED, null);
            }
            systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId,
                    null, requestMd.isSent() ? RpcError.TIMEOUT : RpcError.NO_ACTIVE_CONNECTION));
            if (!requestMd.isDelivered()) {
                sendNextPendingRequest(rpcId, requestId, "Pending RPC timeout detected!");
                return;
            }
            if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                clearAwaitRpcResponseScheduler();
                sendNextPendingRequest(rpcId, requestId, "Pending RPC timeout detected!");
            }
        }
    }

    /**
     * 功能：发送或提交`Pending Requests`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `nodeId`：节点实例ID。
     * 返回：无。
     */
    private void sendPendingRequests(UUID sessionId, String nodeId) {
        SessionType sessionType = getSessionType(sessionId);
        if (!toDeviceRpcPendingMap.isEmpty()) {
            log.debug("[{}] Pushing {} pending RPC messages to session: [{}]", deviceId, sessionId, toDeviceRpcPendingMap.size());
            if (sessionType == SessionType.SYNC) {
                log.debug("[{}] Cleanup sync RPC session [{}]", deviceId, sessionId);
                rpcSubscriptions.remove(sessionId);
            }
        } else {
            log.debug("[{}] No pending RPC messages for session: [{}]", deviceId, sessionId);
        }
        Set<Integer> sentOneWayIds = new HashSet<>();

        if (rpcSequential) {
            getFirstRpc().ifPresent(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        } else if (sessionType == SessionType.ASYNC) {
            toDeviceRpcPendingMap.entrySet().forEach(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        } else {
            toDeviceRpcPendingMap.entrySet().stream().findFirst().ifPresent(processPendingRpc(sessionId, nodeId, sentOneWayIds));
        }

        sentOneWayIds.stream().filter(id -> !toDeviceRpcPendingMap.get(id).getMsg().getMsg().isPersisted()).forEach(toDeviceRpcPendingMap::remove);
    }

    /**
     * 功能：获取RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    private Optional<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> getFirstRpc() {
        if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
            return toDeviceRpcPendingMap.entrySet().stream()
                    .findFirst().filter(entry -> {
                        var md = entry.getValue();
                        if (md.isDelivered()) {
                            if (awaitRpcResponseFuture == null || awaitRpcResponseFuture.isCancelled()) {
                                var toDeviceRpcRequest = md.getMsg().getMsg();
                                awaitRpcResponseFuture = scheduleAwaitRpcResponseFuture(toDeviceRpcRequest.getId(), entry.getKey());
                            }
                            return false;
                        }
                        return true;
                    });
        }
        return toDeviceRpcPendingMap.entrySet().stream().filter(e -> !e.getValue().isDelivered()).findFirst();
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `rpcId`：RPCID。
     * - `requestId`：请求ID。
     * - `logMessage`：待处理消息。
     * 返回：无。
     */
    private void sendNextPendingRequest(UUID rpcId, int requestId, String logMessage) {
        log.debug("[{}][{}][{}] {} Going to send next pending request ...", deviceId, rpcId, requestId, logMessage);
        if (rpcSequential) {
            rpcSubscriptions.forEach((id, s) -> sendPendingRequests(id, s.getNodeId()));
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `sessionId`：会话ID。
     * - `nodeId`：节点实例ID。
     * - `sentOneWayIds`：`sentOneWayIds` 参数。
     * 返回：处理结果。
     */
    private Consumer<Map.Entry<Integer, ToDeviceRpcRequestMetadata>> processPendingRpc(UUID sessionId, String nodeId, Set<Integer> sentOneWayIds) {
        return entry -> {
            ToDeviceRpcRequest request = entry.getValue().getMsg().getMsg();
            ToDeviceRpcRequestBody body = request.getBody();
            Integer requestId = entry.getKey();
            UUID rpcId = request.getId();
            if (request.isOneway() && !rpcSequential) {
                sentOneWayIds.add(requestId);
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(new FromDeviceRpcResponse(rpcId, null, null));
            }
            ToDeviceRpcRequestMsg rpcRequest = ToDeviceRpcRequestMsg.newBuilder()
                    .setRequestId(requestId)
                    .setMethodName(body.getMethod())
                    .setParams(body.getParams())
                    .setExpirationTime(request.getExpirationTime())
                    .setRequestIdMSB(rpcId.getMostSignificantBits())
                    .setRequestIdLSB(rpcId.getLeastSignificantBits())
                    .setOneway(request.isOneway())
                    .setPersisted(request.isPersisted())
                    .build();
            log.debug("[{}][{}][{}][{}] Send pending RPC request to transport ...", deviceId, sessionId, rpcId, requestId);
            sendToTransport(rpcRequest, sessionId, nodeId);
        };
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `wrapper`：`wrapper` 参数。
     * 返回：无。
     */
    void process(TransportToDeviceActorMsgWrapper wrapper) {
        TransportToDeviceActorMsg msg = wrapper.getMsg();
        TbCallback callback = wrapper.getCallback();
        var sessionInfo = msg.getSessionInfo();

        if (msg.hasSessionEvent()) {
            processSessionStateMsgs(sessionInfo, msg.getSessionEvent());
        }
        if (msg.hasSubscribeToAttributes()) {
            processSubscriptionCommands(sessionInfo, msg.getSubscribeToAttributes());
        }
        if (msg.hasSubscribeToRPC()) {
            processSubscriptionCommands(sessionInfo, msg.getSubscribeToRPC());
        }
        if (msg.hasSendPendingRPC()) {
            sendPendingRequests(getSessionId(sessionInfo), sessionInfo.getNodeId());
        }
        if (msg.hasGetAttributes()) {
            handleGetAttributesRequest(sessionInfo, msg.getGetAttributes());
        }
        if (msg.hasToDeviceRPCCallResponse()) {
            processRpcResponses(sessionInfo, msg.getToDeviceRPCCallResponse());
        }
        if (msg.hasSubscriptionInfo()) {
            handleSessionActivity(sessionInfo, msg.getSubscriptionInfo());
        }
        if (msg.hasClaimDevice()) {
            handleClaimDeviceMsg(msg.getClaimDevice());
        }
        if (msg.hasRpcResponseStatusMsg()) {
            processRpcResponseStatus(sessionInfo, msg.getRpcResponseStatusMsg());
        }
        if (msg.hasUplinkNotificationMsg()) {
            processUplinkNotificationMsg(sessionInfo, msg.getUplinkNotificationMsg());
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `uplinkNotificationMsg`：待处理消息。
     * 返回：无。
     */
    private void processUplinkNotificationMsg(SessionInfoProto sessionInfo, UplinkNotificationMsg uplinkNotificationMsg) {
        String nodeId = sessionInfo.getNodeId();
        sessions.entrySet().stream()
                .filter(kv -> kv.getValue().getSessionInfo().getNodeId().equals(nodeId) && (kv.getValue().isSubscribedToAttributes() || kv.getValue().isSubscribedToRPC()))
                .forEach(kv -> {
                    ToTransportMsg msg = ToTransportMsg.newBuilder()
                            .setSessionIdMSB(kv.getKey().getMostSignificantBits())
                            .setSessionIdLSB(kv.getKey().getLeastSignificantBits())
                            .setUplinkNotificationMsg(uplinkNotificationMsg)
                            .build();
                    systemContext.getTbCoreToTransportService().process(kv.getValue().getSessionInfo().getNodeId(), msg);
                });
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void handleClaimDeviceMsg(ClaimDeviceMsg msg) {
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        systemContext.getClaimDevicesService().registerClaimingInfo(tenantId, deviceId, msg.getSecretKey(), msg.getDurationMs());
    }

    /**
     * 功能：上报会话。
     * 参数：无。
     * 返回：无。
     */
    private void reportSessionOpen() {
        systemContext.getDeviceStateService().onDeviceConnect(tenantId, deviceId);
    }

    /**
     * 功能：上报会话。
     * 参数：无。
     * 返回：无。
     */
    private void reportSessionClose() {
        systemContext.getDeviceStateService().onDeviceDisconnect(tenantId, deviceId);
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleGetAttributesRequest(SessionInfoProto sessionInfo, GetAttributeRequestMsg request) {
        int requestId = request.getRequestId();
        if (request.getOnlyShared()) {
            Futures.addCallback(findAllAttributesByScope(DataConstants.SHARED_SCOPE), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<AttributeKvEntry> result) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setRequestId(requestId)
                            .setSharedStateMsg(true)
                            .addAllSharedAttributeList(toTsKvProtos(result))
                            .setIsMultipleAttributesRequest(request.getSharedAttributeNamesCount() > 1)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }

                @Override
                public void onFailure(Throwable t) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setError(t.getMessage())
                            .setSharedStateMsg(true)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }
            }, MoreExecutors.directExecutor());
        } else {
            Futures.addCallback(getAttributesKvEntries(request), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<List<AttributeKvEntry>> result) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setRequestId(requestId)
                            .addAllClientAttributeList(toTsKvProtos(result.get(0)))
                            .addAllSharedAttributeList(toTsKvProtos(result.get(1)))
                            .setIsMultipleAttributesRequest(
                                    request.getSharedAttributeNamesCount() + request.getClientAttributeNamesCount() > 1)
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }

                @Override
                public void onFailure(Throwable t) {
                    GetAttributeResponseMsg responseMsg = GetAttributeResponseMsg.newBuilder()
                            .setError(t.getMessage())
                            .build();
                    sendToTransport(responseMsg, sessionInfo);
                }
            }, MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：获取`Attributes Kv Entries`。
     * 参数：
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<List<AttributeKvEntry>>> getAttributesKvEntries(GetAttributeRequestMsg request) {
        ListenableFuture<List<AttributeKvEntry>> clientAttributesFuture;
        ListenableFuture<List<AttributeKvEntry>> sharedAttributesFuture;
        if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAllAttributesByScope(DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAllAttributesByScope(DataConstants.SHARED_SCOPE);
        } else if (!CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else if (CollectionUtils.isEmpty(request.getClientAttributeNamesList()) && !CollectionUtils.isEmpty(request.getSharedAttributeNamesList())) {
            clientAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            sharedAttributesFuture = findAttributesByScope(toSet(request.getSharedAttributeNamesList()), DataConstants.SHARED_SCOPE);
        } else {
            sharedAttributesFuture = Futures.immediateFuture(Collections.emptyList());
            clientAttributesFuture = findAttributesByScope(toSet(request.getClientAttributeNamesList()), DataConstants.CLIENT_SCOPE);
        }
        return Futures.allAsList(Arrays.asList(clientAttributesFuture, sharedAttributesFuture));
    }

    /**
     * 功能：获取`All Attributes By Scope`。
     * 参数：
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<AttributeKvEntry>> findAllAttributesByScope(String scope) {
        return systemContext.getAttributesService().findAll(tenantId, deviceId, scope);
    }

    /**
     * 功能：获取`Attributes By Scope`。
     * 参数：
     * - `attributesSet`：`attributesSet` 参数。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<AttributeKvEntry>> findAttributesByScope(Set<String> attributesSet, String scope) {
        return systemContext.getAttributesService().find(tenantId, deviceId, scope, attributesSet);
    }

    /**
     * 功能：执行 `toSet` 对应的处理。
     * 参数：
     * - `strings`：数据列表。
     * 返回：匹配的数据集合。
     */
    private Set<String> toSet(List<String> strings) {
        return new HashSet<>(strings);
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `sessionId`：会话ID。
     * 返回：处理结果。
     */
    private SessionType getSessionType(UUID sessionId) {
        return sessions.containsKey(sessionId) ? SessionType.ASYNC : SessionType.SYNC;
    }

    /**
     * 功能：处理`Attributes Update`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processAttributesUpdate(DeviceAttributesEventNotificationMsg msg) {
        if (attributeSubscriptions.size() > 0) {
            boolean hasNotificationData = false;
            AttributeUpdateNotificationMsg.Builder notification = AttributeUpdateNotificationMsg.newBuilder();
            if (msg.isDeleted()) {
                List<String> sharedKeys = msg.getDeletedKeys().stream()
                        .filter(key -> DataConstants.SHARED_SCOPE.equals(key.getScope()))
                        .map(AttributeKey::getAttributeKey)
                        .collect(Collectors.toList());
                if (!sharedKeys.isEmpty()) {
                    notification.addAllSharedDeleted(sharedKeys);
                    hasNotificationData = true;
                }
            } else {
                if (DataConstants.SHARED_SCOPE.equals(msg.getScope())) {
                    List<AttributeKvEntry> attributes = new ArrayList<>(msg.getValues());
                    if (attributes.size() > 0) {
                        List<TsKvProto> sharedUpdated = msg.getValues().stream().map(this::toTsKvProto)
                                .collect(Collectors.toList());
                        if (!sharedUpdated.isEmpty()) {
                            notification.addAllSharedUpdated(sharedUpdated);
                            hasNotificationData = true;
                        }
                    } else {
                        log.debug("[{}] No public shared side attributes changed!", deviceId);
                    }
                }
            }
            if (hasNotificationData) {
                AttributeUpdateNotificationMsg finalNotification = notification.build();
                attributeSubscriptions.forEach((key, value) -> sendToTransport(finalNotification, key, value.getNodeId()));
            }
        } else {
            log.debug("[{}] No registered attributes subscriptions to process!", deviceId);
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `responseMsg`：响应对象。
     * 返回：无。
     */
    private void processRpcResponses(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg responseMsg) {
        UUID sessionId = getSessionId(sessionInfo);
        log.debug("[{}][{}] Processing RPC command response: {}", deviceId, sessionId, responseMsg);
        int requestId = responseMsg.getRequestId();
        ToDeviceRpcRequestMetadata requestMd = toDeviceRpcPendingMap.remove(requestId);
        boolean success = requestMd != null;
        if (success) {
            ToDeviceRpcRequest toDeviceRequestMsg = requestMd.getMsg().getMsg();
            UUID rpcId = toDeviceRequestMsg.getId();
            boolean delivered = requestMd.isDelivered();
            boolean hasError = StringUtils.isNotEmpty(responseMsg.getError());
            try {
                String payload = hasError ? responseMsg.getError() : responseMsg.getPayload();
                systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(
                        new FromDeviceRpcResponse(rpcId, payload, null));
                if (toDeviceRequestMsg.isPersisted()) {
                    RpcStatus status = hasError ? RpcStatus.FAILED : RpcStatus.SUCCESSFUL;
                    JsonNode response;
                    try {
                        response = JacksonUtil.toJsonNode(payload);
                    } catch (IllegalArgumentException e) {
                        response = JacksonUtil.newObjectNode().put("error", payload);
                    }
                    systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), status, response);
                }
            } finally {
                if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                    clearAwaitRpcResponseScheduler();
                    String errorResponse = hasError ? "error response" : "response";
                    String rpcState = delivered ? "" : "undelivered ";
                    sendNextPendingRequest(rpcId, requestId, String.format("Received %s for %sRPC!", errorResponse, rpcState));
                } else if (!delivered) {
                    String errorResponse = hasError ? "error response" : "response";
                    sendNextPendingRequest(rpcId, requestId, String.format("Received %s for undelivered RPC!", errorResponse));
                }
            }
        } else {
            log.debug("[{}][{}][{}] RPC command response is stale!", deviceId, sessionId, requestId);
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `responseMsg`：响应对象。
     * 返回：无。
     */
    private void processRpcResponseStatus(SessionInfoProto sessionInfo, ToDeviceRpcResponseStatusMsg responseMsg) {
        UUID rpcId = new UUID(responseMsg.getRequestIdMSB(), responseMsg.getRequestIdLSB());
        RpcStatus status = RpcStatus.valueOf(responseMsg.getStatus());
        UUID sessionId = getSessionId(sessionInfo);
        int requestId = responseMsg.getRequestId();
        log.debug("[{}][{}][{}][{}] Processing RPC command response status: [{}]", deviceId, sessionId, rpcId, requestId, status);
        ToDeviceRpcRequestMetadata md = toDeviceRpcPendingMap.get(requestId);
        if (md != null) {
            var toDeviceRpcRequest = md.getMsg().getMsg();
            boolean persisted = toDeviceRpcRequest.isPersisted();
            boolean oneWayRpc = toDeviceRpcRequest.isOneway();
            JsonNode response = null;
            if (status.equals(RpcStatus.DELIVERED)) {
                if (oneWayRpc) {
                    toDeviceRpcPendingMap.remove(requestId);
                    if (rpcSequential) {
                        var fromDeviceRpcResponse = new FromDeviceRpcResponse(rpcId, null, null);
                        systemContext.getTbCoreDeviceRpcService().processRpcResponseFromDeviceActor(fromDeviceRpcResponse);
                    }
                } else {
                    md.setDelivered(true);
                    if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)) {
                        awaitRpcResponseFuture = scheduleAwaitRpcResponseFuture(rpcId, requestId);
                    }
                }
            } else if (status.equals(RpcStatus.TIMEOUT)) {
                Integer maxRpcRetries = toDeviceRpcRequest.getRetries();
                maxRpcRetries = maxRpcRetries == null ?
                        systemContext.getMaxRpcRetries() : Math.min(maxRpcRetries, systemContext.getMaxRpcRetries());
                if (maxRpcRetries <= md.getRetries()) {
                    toDeviceRpcPendingMap.remove(requestId);
                    status = RpcStatus.FAILED;
                    response = JacksonUtil.newObjectNode().put("error", "There was a Timeout and all retry " +
                            "attempts have been exhausted. Retry attempts set: " + maxRpcRetries);
                } else {
                    md.setRetries(md.getRetries() + 1);
                }
            }

            if (persisted) {
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), status, response);
            }
            if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE)
                    && status.equals(RpcStatus.DELIVERED) && !oneWayRpc) {
                return;
            }
            if (!status.equals(RpcStatus.SENT)) {
                sendNextPendingRequest(rpcId, requestId, String.format("RPC was %s!", status.name().toLowerCase()));
            }
        } else {
            log.warn("[{}][{}][{}][{}] RPC has already been removed from pending map.", deviceId, sessionId, rpcId, requestId);
        }
    }

    /**
     * 功能：处理订阅。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `subscribeCmd`：`subscribeCmd` 参数。
     * 返回：无。
     */
    private void processSubscriptionCommands(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling attributes subscription for session: [{}]", deviceId, sessionId);
            attributeSubscriptions.remove(sessionId);
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToAttributes(true);
            log.debug("[{}] Registering attributes subscription for session: [{}]", deviceId, sessionId);
            attributeSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            dumpSessions();
        }
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    private UUID getSessionId(SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    /**
     * 功能：处理订阅。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `subscribeCmd`：`subscribeCmd` 参数。
     * 返回：无。
     */
    private void processSubscriptionCommands(SessionInfoProto sessionInfo, SubscribeToRPCMsg subscribeCmd) {
        UUID sessionId = getSessionId(sessionInfo);
        if (subscribeCmd.getUnsubscribe()) {
            log.debug("[{}] Canceling RPC subscription for session: [{}]", deviceId, sessionId);
            rpcSubscriptions.remove(sessionId);
            clearAwaitRpcResponseScheduler();
        } else {
            SessionInfoMetaData sessionMD = sessions.get(sessionId);
            if (sessionMD == null) {
                sessionMD = new SessionInfoMetaData(new SessionInfo(subscribeCmd.getSessionType(), sessionInfo.getNodeId()));
            }
            sessionMD.setSubscribedToRPC(true);
            rpcSubscriptions.put(sessionId, sessionMD.getSessionInfo());
            log.debug("[{}] Registered RPC subscription for session: [{}] Going to check for pending requests ...", deviceId, sessionId);
            sendPendingRequests(sessionId, sessionInfo.getNodeId());
            dumpSessions();
        }
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void processSessionStateMsgs(SessionInfoProto sessionInfo, SessionEventMsg msg) {
        UUID sessionId = getSessionId(sessionInfo);
        Objects.requireNonNull(sessionId);
        if (msg.getEvent() == SessionEvent.OPEN) {
            if (sessions.containsKey(sessionId)) {
                log.debug("[{}][{}] Received duplicate session open event.", deviceId, sessionId);
                return;
            }
            log.debug("[{}] Processing new session: [{}] Current sessions size: {}", deviceId, sessionId, sessions.size());

            sessions.put(sessionId, new SessionInfoMetaData(new SessionInfo(SessionType.ASYNC, sessionInfo.getNodeId())));
            if (sessions.size() == 1) {
                reportSessionOpen();
            }
            systemContext.getDeviceStateService().onDeviceActivity(tenantId, deviceId, System.currentTimeMillis());
            dumpSessions();
        } else if (msg.getEvent() == SessionEvent.CLOSED) {
            log.debug("[{}][{}] Canceling subscriptions for closed session.", deviceId, sessionId);
            sessions.remove(sessionId);
            attributeSubscriptions.remove(sessionId);
            rpcSubscriptions.remove(sessionId);
            clearAwaitRpcResponseScheduler();
            if (sessions.isEmpty()) {
                reportSessionClose();
            }
            dumpSessions();
        }
    }

    /**
     * 功能：执行 `scheduleAwaitRpcResponseFuture` 对应的处理。
     * 参数：
     * - `rpcId`：RPCID。
     * - `requestId`：请求ID。
     * 返回：异步处理结果。
     */
    private ScheduledFuture<?> scheduleAwaitRpcResponseFuture(UUID rpcId, int requestId) {
        return scheduler.schedule(() -> {
            var md = toDeviceRpcPendingMap.remove(requestId);
            if (md == null) {
                return;
            }
            sendNextPendingRequest(rpcId, requestId, "RPC was removed from pending map due to await timeout on response from device!");
            var toDeviceRpcRequest = md.getMsg().getMsg();
            if (toDeviceRpcRequest.isPersisted()) {
                var responseAwaitTimeout = JacksonUtil.newObjectNode().put("error", "There was a timeout awaiting for RPC response from device.");
                systemContext.getTbRpcService().save(tenantId, new RpcId(rpcId), RpcStatus.FAILED, responseAwaitTimeout);
            }
        }, systemContext.getRpcResponseTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：删除或清理RPC。
     * 参数：无。
     * 返回：无。
     */
    private void clearAwaitRpcResponseScheduler() {
        if (rpcSubmitStrategy.equals(RpcSubmitStrategy.SEQUENTIAL_ON_RESPONSE_FROM_DEVICE) && awaitRpcResponseFuture != null) {
            awaitRpcResponseFuture.cancel(true);
        }
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `sessionInfoProto`：会话对象。
     * - `subscriptionInfo`：`subscriptionInfo` 参数。
     * 返回：无。
     */
    private void handleSessionActivity(SessionInfoProto sessionInfoProto, SubscriptionInfoProto subscriptionInfo) {
        UUID sessionId = getSessionId(sessionInfoProto);
        Objects.requireNonNull(sessionId);

        SessionInfoMetaData sessionMD = sessions.get(sessionId);
        if (sessionMD != null) {
            sessionMD.setLastActivityTime(subscriptionInfo.getLastActivityTime());
            sessionMD.setSubscribedToAttributes(subscriptionInfo.getAttributeSubscription());
            sessionMD.setSubscribedToRPC(subscriptionInfo.getRpcSubscription());
            if (subscriptionInfo.getAttributeSubscription()) {
                attributeSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
            }
            if (subscriptionInfo.getRpcSubscription()) {
                rpcSubscriptions.putIfAbsent(sessionId, sessionMD.getSessionInfo());
            }
        }
        systemContext.getDeviceStateService().onDeviceActivity(tenantId, deviceId, subscriptionInfo.getLastActivityTime());
        if (sessionMD != null) {
            dumpSessions();
        }
    }

    /**
     * 功能：处理凭据。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processCredentialsUpdate(TbActorMsg msg) {
        if (((DeviceCredentialsUpdateNotificationMsg) msg).getDeviceCredentials().getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
            sessions.forEach((k, v) -> {
                notifyTransportAboutDeviceCredentialsUpdate(k, v, ((DeviceCredentialsUpdateNotificationMsg) msg).getDeviceCredentials());
            });
        } else {
            sessions.forEach((sessionId, sessionMd) -> notifyTransportAboutClosedSession(sessionId, sessionMd, "device credentials updated!"));
            attributeSubscriptions.clear();
            rpcSubscriptions.clear();
            dumpSessions();

        }
    }

    /**
     * 功能：通知会话。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionMd`：会话对象。
     * 返回：无。
     */
    private void notifyTransportAboutClosedSessionMaxSessionsLimit(UUID sessionId, SessionInfoMetaData sessionMd) {
        log.debug("remove eldest session (max concurrent sessions limit reached per device) sessionId: [{}] sessionMd: [{}]", sessionId, sessionMd);
        notifyTransportAboutClosedSession(sessionId, sessionMd, "max concurrent sessions limit reached per device!");
    }

    /**
     * 功能：通知会话。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionMd`：会话对象。
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void notifyTransportAboutClosedSession(UUID sessionId, SessionInfoMetaData sessionMd, String message) {
        SessionCloseNotificationProto sessionCloseNotificationProto = SessionCloseNotificationProto
                .newBuilder()
                .setMessage(message).build();
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setSessionCloseNotification(sessionCloseNotificationProto)
                .build();
        systemContext.getTbCoreToTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    /**
     * 功能：通知设备凭据。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionMd`：会话对象。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：无。
     */
    void notifyTransportAboutDeviceCredentialsUpdate(UUID sessionId, SessionInfoMetaData sessionMd, DeviceCredentials deviceCredentials) {
        ToTransportUpdateCredentialsProto.Builder notification = ToTransportUpdateCredentialsProto.newBuilder();
        notification.addCredentialsId(deviceCredentials.getCredentialsId());
        notification.addCredentialsValue(deviceCredentials.getCredentialsValue());
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToTransportUpdateCredentialsNotification(notification).build();
        systemContext.getTbCoreToTransportService().process(sessionMd.getSessionInfo().getNodeId(), msg);
    }

    /**
     * 功能：处理名称。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processNameOrTypeUpdate(DeviceNameOrTypeUpdateMsg msg) {
        this.deviceName = msg.getDeviceName();
        this.deviceType = msg.getDeviceType();
        this.defaultMetaData = new TbMsgMetaData();
        this.defaultMetaData.putValue("deviceName", deviceName);
        this.defaultMetaData.putValue("deviceType", deviceType);
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processEdgeUpdate(DeviceEdgeUpdateMsg msg) {
        log.trace("[{}] Processing edge update {}", deviceId, msg);
        this.edgeId = msg.getEdgeId();
    }

    /**
     * 功能：发送或提交传输层。
     * 参数：
     * - `responseMsg`：响应对象。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    private void sendToTransport(GetAttributeResponseMsg responseMsg, SessionInfoProto sessionInfo) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionInfo.getSessionIdMSB())
                .setSessionIdLSB(sessionInfo.getSessionIdLSB())
                .setGetAttributesResponse(responseMsg).build();
        systemContext.getTbCoreToTransportService().process(sessionInfo.getNodeId(), msg);
    }

    /**
     * 功能：发送或提交传输层。
     * 参数：
     * - `notificationMsg`：待处理消息。
     * - `sessionId`：会话ID。
     * - `nodeId`：节点实例ID。
     * 返回：无。
     */
    private void sendToTransport(AttributeUpdateNotificationMsg notificationMsg, UUID sessionId, String nodeId) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setAttributeUpdateNotification(notificationMsg).build();
        systemContext.getTbCoreToTransportService().process(nodeId, msg);
    }

    /**
     * 功能：发送或提交传输层。
     * 参数：
     * - `rpcMsg`：待处理消息。
     * - `sessionId`：会话ID。
     * - `nodeId`：节点实例ID。
     * 返回：无。
     */
    private void sendToTransport(ToDeviceRpcRequestMsg rpcMsg, UUID sessionId, String nodeId) {
        ToTransportMsg msg = ToTransportMsg.newBuilder()
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setToDeviceRequest(rpcMsg).build();
        systemContext.getTbCoreToTransportService().process(nodeId, msg);
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `msg`：待处理消息。
     * - `requestId`：请求ID。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> saveRpcRequestToEdgeQueue(ToDeviceRpcRequest msg, Integer requestId) {
        ObjectNode body = JacksonUtil.newObjectNode();
        body.put("requestId", requestId);
        body.put("requestUUID", msg.getId().toString());
        body.put("oneway", msg.isOneway());
        body.put("expirationTime", msg.getExpirationTime());
        body.put("method", msg.getBody().getMethod());
        body.put("params", msg.getBody().getParams());
        body.put("persisted", msg.isPersisted());
        body.put("retries", msg.getRetries());
        body.put("additionalInfo", msg.getAdditionalInfo());

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, EdgeEventType.DEVICE, EdgeEventActionType.RPC_CALL, deviceId, body);

        return Futures.transform(systemContext.getEdgeEventService().saveAsync(edgeEvent), unused -> {
            systemContext.getClusterService().onEdgeEventUpdate(tenantId, edgeId);
            return null;
        }, systemContext.getDbCallbackExecutor());
    }

    /**
     * 功能：执行 `toTsKvProtos` 对应的处理。
     * 参数：
     * - `result`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<TsKvProto> toTsKvProtos(@Nullable List<AttributeKvEntry> result) {
        List<TsKvProto> clientAttributes;
        if (result == null || result.isEmpty()) {
            clientAttributes = Collections.emptyList();
        } else {
            clientAttributes = new ArrayList<>(result.size());
            for (AttributeKvEntry attrEntry : result) {
                clientAttributes.add(toTsKvProto(attrEntry));
            }
        }
        return clientAttributes;
    }

    /**
     * 功能：执行 `toTsKvProto` 对应的处理。
     * 参数：
     * - `attrEntry`：`attrEntry` 参数。
     * 返回：处理结果。
     */
    private TsKvProto toTsKvProto(AttributeKvEntry attrEntry) {
        return TsKvProto.newBuilder().setTs(attrEntry.getLastUpdateTs())
                .setKv(toKeyValueProto(attrEntry)).build();
    }

    /**
     * 功能：执行 `toKeyValueProto` 对应的处理。
     * 参数：
     * - `kvEntry`：`kvEntry` 参数。
     * 返回：处理结果。
     */
    private KeyValueProto toKeyValueProto(KvEntry kvEntry) {
        KeyValueProto.Builder builder = KeyValueProto.newBuilder();
        builder.setKey(kvEntry.getKey());
        switch (kvEntry.getDataType()) {
            case BOOLEAN:
                builder.setType(KeyValueType.BOOLEAN_V);
                builder.setBoolV(kvEntry.getBooleanValue().get());
                break;
            case DOUBLE:
                builder.setType(KeyValueType.DOUBLE_V);
                builder.setDoubleV(kvEntry.getDoubleValue().get());
                break;
            case LONG:
                builder.setType(KeyValueType.LONG_V);
                builder.setLongV(kvEntry.getLongValue().get());
                break;
            case STRING:
                builder.setType(KeyValueType.STRING_V);
                builder.setStringV(kvEntry.getStrValue().get());
                break;
            case JSON:
                builder.setType(KeyValueType.JSON_V);
                builder.setJsonV(kvEntry.getJsonValue().get());
                break;
        }
        return builder.build();
    }

    /**
     * 功能：执行 `restoreSessions` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void restoreSessions() {
        if (systemContext.isLocalCacheType()) {
            return;
        }
        log.debug("[{}] Restoring sessions from cache", deviceId);
        DeviceSessionsCacheEntry sessionsDump;
        try {
            sessionsDump = systemContext.getDeviceSessionCacheService().get(deviceId);
        } catch (Exception e) {
            log.warn("[{}] Failed to decode device sessions from cache", deviceId);
            return;
        }
        if (sessionsDump.getSessionsCount() == 0) {
            log.debug("[{}] No session information found", deviceId);
            return;
        }
        // TODO: Take latest max allowed sessions size from cache
        for (SessionSubscriptionInfoProto sessionSubscriptionInfoProto : sessionsDump.getSessionsList()) {
            SessionInfoProto sessionInfoProto = sessionSubscriptionInfoProto.getSessionInfo();
            UUID sessionId = getSessionId(sessionInfoProto);
            SessionInfo sessionInfo = new SessionInfo(SessionType.ASYNC, sessionInfoProto.getNodeId());
            SubscriptionInfoProto subInfo = sessionSubscriptionInfoProto.getSubscriptionInfo();
            SessionInfoMetaData sessionMD = new SessionInfoMetaData(sessionInfo, subInfo.getLastActivityTime());
            sessions.put(sessionId, sessionMD);
            if (subInfo.getAttributeSubscription()) {
                attributeSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToAttributes(true);
            }
            if (subInfo.getRpcSubscription()) {
                rpcSubscriptions.put(sessionId, sessionInfo);
                sessionMD.setSubscribedToRPC(true);
            }
            log.debug("[{}] Restored session: {}", deviceId, sessionMD);
        }
        log.debug("[{}] Restored sessions: {}, RPC subscriptions: {}, attribute subscriptions: {}", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
    }

    /**
     * 功能：执行 `dumpSessions` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void dumpSessions() {
        if (systemContext.isLocalCacheType()) {
            return;
        }
        log.debug("[{}] Dumping sessions: {}, RPC subscriptions: {}, attribute subscriptions: {} to cache", deviceId, sessions.size(), rpcSubscriptions.size(), attributeSubscriptions.size());
        List<SessionSubscriptionInfoProto> sessionsList = new ArrayList<>(sessions.size());
        sessions.forEach((uuid, sessionMD) -> {
            if (sessionMD.getSessionInfo().getType() == SessionType.SYNC) {
                return;
            }
            SessionInfo sessionInfo = sessionMD.getSessionInfo();
            SubscriptionInfoProto subscriptionInfoProto = SubscriptionInfoProto.newBuilder()
                    .setLastActivityTime(sessionMD.getLastActivityTime())
                    .setAttributeSubscription(sessionMD.isSubscribedToAttributes())
                    .setRpcSubscription(sessionMD.isSubscribedToRPC()).build();
            SessionInfoProto sessionInfoProto = SessionInfoProto.newBuilder()
                    .setSessionIdMSB(uuid.getMostSignificantBits())
                    .setSessionIdLSB(uuid.getLeastSignificantBits())
                    .setNodeId(sessionInfo.getNodeId()).build();
            sessionsList.add(SessionSubscriptionInfoProto.newBuilder()
                    .setSessionInfo(sessionInfoProto)
                    .setSubscriptionInfo(subscriptionInfoProto).build());
            log.debug("[{}] Dumping session: {}", deviceId, sessionMD);
        });
        systemContext.getDeviceSessionCacheService()
                .put(deviceId, DeviceSessionsCacheEntry.newBuilder()
                        .addAllSessions(sessionsList).build());
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    void init(TbActorCtx ctx) {
        PageLink pageLink = new PageLink(1024, 0, null, new SortOrder("createdTime"));
        PageData<Rpc> pageData;
        do {
            pageData = systemContext.getTbRpcService().findAllByDeviceIdAndStatus(tenantId, deviceId, RpcStatus.QUEUED, pageLink);
            pageData.getData().forEach(rpc -> {
                ToDeviceRpcRequest msg = JacksonUtil.convertValue(rpc.getRequest(), ToDeviceRpcRequest.class);
                long timeout = rpc.getExpirationTime() - System.currentTimeMillis();
                if (timeout <= 0) {
                    rpc.setStatus(RpcStatus.EXPIRED);
                    systemContext.getTbRpcService().save(tenantId, rpc);
                } else {
                    registerPendingRpcRequest(ctx, new ToDeviceRpcRequestActorMsg(systemContext.getServiceId(), msg), false, createToDeviceRpcRequestMsg(msg), timeout);
                }
            });
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    /**
     * 功能：校验超时时间。
     * 参数：无。
     * 返回：无。
     */
    void checkSessionsTimeout() {
        final long expTime = System.currentTimeMillis() - systemContext.getSessionInactivityTimeout();
        List<UUID> expiredIds = null;

        for (Map.Entry<UUID, SessionInfoMetaData> kv : sessions.entrySet()) { //entry set are cached for stable sessions
            if (kv.getValue().getLastActivityTime() < expTime) {
                final UUID id = kv.getKey();
                if (expiredIds == null) {
                    expiredIds = new ArrayList<>(1); //most of the expired sessions is a single event
                }
                expiredIds.add(id);
            }
        }

        if (expiredIds != null) {
            int removed = 0;
            for (UUID id : expiredIds) {
                final SessionInfoMetaData session = sessions.remove(id);
                rpcSubscriptions.remove(id);
                attributeSubscriptions.remove(id);
                if (session != null) {
                    removed++;
                    notifyTransportAboutClosedSession(id, session, SESSION_TIMEOUT_MESSAGE);
                }
            }
            if (removed != 0) {
                dumpSessions();
            }
        }

    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceActorMessageProcessor` 在 ThingsBoard Application 模块 中承担Actor 通信与消息处理类型职责，核心目的是管理租户、设备、规则链或规则节点的异步消息路由。
 * 2. 核心流程：接收 Actor 消息后定位处理器，执行业务逻辑并通过 tell 或回调继续路由。
 * 3. 关键依赖：主要依赖或协作对象包括ActorSystemContext、ActorRef、队列服务、Rule Engine 节点和 DAO 服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
