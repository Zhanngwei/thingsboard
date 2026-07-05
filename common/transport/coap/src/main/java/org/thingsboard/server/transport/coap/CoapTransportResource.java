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
package org.thingsboard.server.transport.coap;

import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.thingsboard.server.coapserver.CoapServerService;
import org.thingsboard.server.coapserver.TbCoapDtlsSessionInfo;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;
import org.thingsboard.server.transport.coap.callback.CoapNoOpCallback;
import org.thingsboard.server.transport.coap.callback.CoapOkCallback;
import org.thingsboard.server.transport.coap.callback.GetAttributesSyncSessionCallback;
import org.thingsboard.server.transport.coap.callback.ToServerRpcSyncSessionCallback;
import org.thingsboard.server.transport.coap.client.CoapClientContext;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.californium.elements.DtlsEndpointContext.KEY_SESSION_ID;

/**
 * 中文说明：
 * 1. 类目的：`CoapTransportResource` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class CoapTransportResource extends AbstractCoapTransportResource {
    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final int ACCESS_TOKEN_POSITION = 3;
    private static final int FEATURE_TYPE_POSITION = 4;
    /**
     * 请求常量，用于统一引用固定值。
     */
    private static final int REQUEST_ID_POSITION = 5;

    /**
     * 证书常量，用于统一引用固定值。
     */
    private static final int FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST = 3;
    private static final int REQUEST_ID_POSITION_CERTIFICATE_REQUEST = 4;

    /**
     * `dtlsSessionsMap`映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> dtlsSessionsMap;
    private final long timeout;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private final long piggybackTimeout;
    private final CoapClientContext clients;

    /**
     * 功能：创建 `CoapTransportResource` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `coapServerService`：服务对象。
     * - `name`：名称。
     * 返回：新创建的对象实例。
     */
    public CoapTransportResource(CoapTransportContext ctx, CoapServerService coapServerService, String name) {
        super(ctx, name);
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
        this.dtlsSessionsMap = coapServerService.getDtlsSessionsMap();
        this.timeout = coapServerService.getTimeout();
        this.piggybackTimeout = coapServerService.getPiggybackTimeout();
        this.clients = ctx.getClientContext();
        long sessionReportTimeout = ctx.getSessionReportTimeout();
        ctx.getScheduler().scheduleAtFixedRate(clients::reportActivity, new Random().nextInt((int) sessionReportTimeout), sessionReportTimeout, TimeUnit.MILLISECONDS);
    }

    /*
     * Overwritten method from CoapResource to be able to manage our own observe notification counters.
     */
    /**
     * 功能：校验关系。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void checkObserveRelation(Exchange exchange, Response response) {
        String token = getTokenFromRequest(exchange.getRequest());
        final ObserveRelation relation = exchange.getRelation();
        if (relation == null || relation.isCanceled()) {
            return; // because request did not try to establish a relation
        }
        if (response.getCode().isSuccess()) {
            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            }
            AtomicInteger state = clients.getNotificationCounterByToken(token);
            if (state != null) {
                response.getOptions().setObserve(state.getAndIncrement());
            } else {
                response.getOptions().removeObserve();
            }
        } // ObserveLayer takes care of the else case
    }

    /**
     * 功能：处理`Handle Get`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandleGet(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else if (featureType.get() == FeatureType.TELEMETRY) {
            log.trace("Can't fetch/subscribe to timeseries updates");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else if (exchange.getRequestOptions().hasObserve()) {
            processExchangeGetRequest(exchange, featureType.get());
        } else if (featureType.get() == FeatureType.ATTRIBUTES) {
            processRequest(exchange, CoapSessionMsgType.GET_ATTRIBUTES_REQUEST);
        } else {
            log.trace("Invalid feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `featureType`：类型。
     * 返回：无。
     */
    private void processExchangeGetRequest(CoapExchange exchange, FeatureType featureType) {
        boolean unsubscribe = exchange.getRequestOptions().getObserve() == 1;
        CoapSessionMsgType coapSessionMsgType;
        if (featureType == FeatureType.RPC) {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_RPC_COMMANDS_REQUEST : CoapSessionMsgType.SUBSCRIBE_RPC_COMMANDS_REQUEST;
        } else {
            coapSessionMsgType = unsubscribe ? CoapSessionMsgType.UNSUBSCRIBE_ATTRIBUTES_REQUEST : CoapSessionMsgType.SUBSCRIBE_ATTRIBUTES_REQUEST;
        }
        processRequest(exchange, coapSessionMsgType);
    }

    /**
     * 功能：处理`Handle Post`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    protected void processHandlePost(CoapExchange exchange) {
        Optional<FeatureType> featureType = getFeatureType(exchange.advanced().getRequest());
        if (featureType.isEmpty()) {
            log.trace("Missing feature type parameter");
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        } else {
            switch (featureType.get()) {
                case ATTRIBUTES:
                    processRequest(exchange, CoapSessionMsgType.POST_ATTRIBUTES_REQUEST);
                    break;
                case TELEMETRY:
                    processRequest(exchange, CoapSessionMsgType.POST_TELEMETRY_REQUEST);
                    break;
                case RPC:
                    Optional<Integer> requestId = getRequestId(exchange.advanced().getRequest());
                    if (requestId.isPresent()) {
                        processRequest(exchange, CoapSessionMsgType.TO_DEVICE_RPC_RESPONSE);
                    } else {
                        processRequest(exchange, CoapSessionMsgType.TO_SERVER_RPC_REQUEST);
                    }
                    break;
                case CLAIM:
                    processRequest(exchange, CoapSessionMsgType.CLAIM_REQUEST);
                    break;
                case PROVISION:
                    processProvision(exchange);
                    break;
            }
        }
    }

    /**
     * 功能：处理`Provision`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    private void processProvision(CoapExchange exchange) {
        deferAccept(exchange);
        try {
            UUID sessionId = UUID.randomUUID();
            log.trace("[{}] Processing provision publish msg [{}]!", sessionId, exchange.advanced().getRequest());
            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg;
            TransportPayloadType payloadType;
            try {
                provisionRequestMsg = transportContext.getJsonCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                payloadType = TransportPayloadType.JSON;
            } catch (Exception e) {
                if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                    provisionRequestMsg = transportContext.getProtoCoapAdaptor().convertToProvisionRequestMsg(sessionId, exchange.advanced().getRequest());
                    payloadType = TransportPayloadType.PROTOBUF;
                } else {
                    throw new AdaptorException(e);
                }
            }
            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(exchange, payloadType));
        } catch (AdaptorException e) {
            log.trace("Failed to decode message: ", e);
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `type`：类型。
     * 返回：无。
     */
    private void processRequest(CoapExchange exchange, CoapSessionMsgType type) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        deferAccept(exchange);
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();

        var dtlsSessionId = request.getSourceContext().get(KEY_SESSION_ID);
        if (dtlsSessionsMap != null && dtlsSessionId != null && !dtlsSessionId.isEmpty()) {
            TbCoapDtlsSessionInfo tbCoapDtlsSessionInfo = dtlsSessionsMap
                    .computeIfPresent(request.getSourceContext().getPeerAddress(), (dtlsSessionIdStr, dtlsSessionInfo) -> {
                        dtlsSessionInfo.setLastActivityTime(System.currentTimeMillis());
                        return dtlsSessionInfo;
                    });
            if (tbCoapDtlsSessionInfo != null) {
                processRequest(exchange, type, request, tbCoapDtlsSessionInfo.getMsg(), tbCoapDtlsSessionInfo.getDeviceProfile());
            } else {
                processAccessTokenRequest(exchange, type, request);
            }
        } else {
            processAccessTokenRequest(exchange, type, request);
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `type`：类型。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void processAccessTokenRequest(CoapExchange exchange, CoapSessionMsgType type, Request request) {
        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(exchange, (deviceCredentials, deviceProfile) -> processRequest(exchange, type, request, deviceCredentials, deviceProfile)));
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `type`：类型。
     * - `request`：请求对象。
     * - `deviceCredentials`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void processRequest(CoapExchange exchange, CoapSessionMsgType type, Request request, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) {
        TbCoapClientState clientState = null;
        try {
            clientState = clients.getOrCreateClient(type, deviceCredentials, deviceProfile);
            clients.awake(clientState);
            switch (type) {
                case POST_ATTRIBUTES_REQUEST:
                    handlePostAttributesRequest(clientState, exchange, request);
                    break;
                case POST_TELEMETRY_REQUEST:
                    handlePostTelemetryRequest(clientState, exchange, request);
                    break;
                case CLAIM_REQUEST:
                    handleClaimRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_ATTRIBUTES_REQUEST:
                    handleAttributeUnsubscribeRequest(clientState, exchange, request);
                    break;
                case SUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcSubscribeRequest(clientState, exchange, request);
                    break;
                case UNSUBSCRIBE_RPC_COMMANDS_REQUEST:
                    handleRpcUnsubscribeRequest(clientState, exchange, request);
                    break;
                case TO_DEVICE_RPC_RESPONSE:
                    handleToDeviceRpcResponse(clientState, exchange, request);
                    break;
                case TO_SERVER_RPC_REQUEST:
                    handleToServerRpcRequest(clientState, exchange, request);
                    break;
                case GET_ATTRIBUTES_REQUEST:
                    handleGetAttributesRequest(clientState, exchange, request);
                    break;
            }
        } catch (AdaptorException e) {
            if (clientState != null) {
                log.trace("[{}] Failed to decode message: ", clientState.getDeviceId(), e);
            }
            exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handlePostAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostAttributes(sessionId, request,
                clientState.getConfiguration().getAttributesMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handlePostTelemetryRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo, clientState.getAdaptor().convertToPostTelemetry(sessionId, request,
                clientState.getConfiguration().getTelemetryMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleClaimRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToClaimDevice(sessionId, request, sessionInfo),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleAttributeSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String attrSubToken = getTokenFromRequest(request);
        if (!clients.registerAttributeObservation(clientState, attrSubToken, exchange)) {
            log.warn("[{}] Received duplicate attribute subscribe request for token: {}", clientState.getDeviceId(), attrSubToken);
        }
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleAttributeUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterAttributeObservation(clientState, getTokenFromRequest(request), exchange);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleRpcUnsubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        clients.deregisterRpcObservation(clientState, getTokenFromRequest(request), exchange);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleToDeviceRpcResponse(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto session = clientState.getSession();
        if (session == null) {
            session = clients.getNewSyncSession(clientState);
        }
        UUID sessionId = toSessionId(session);
        transportService.process(session,
                clientState.getAdaptor().convertToDeviceRpcResponse(sessionId, request, clientState.getConfiguration().getRpcResponseMsgDescriptor()),
                new CoapOkCallback(exchange, CoAP.ResponseCode.CREATED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleRpcSubscribeRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) {
        String rpcSubToken = getTokenFromRequest(request);
        if (!clients.registerRpcObservation(clientState, rpcSubToken, exchange)) {
            log.warn("[{}] Received duplicate rpc subscribe request.", rpcSubToken);
        }
    }

    /**
     * 功能：处理请求。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleGetAttributesRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new GetAttributesSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToGetAttributes(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `clientState`：客户端对象。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：无。
     */
    private void handleToServerRpcRequest(TbCoapClientState clientState, CoapExchange exchange, Request request) throws AdaptorException {
        TransportProtos.SessionInfoProto sessionInfo = clients.getNewSyncSession(clientState);
        UUID sessionId = toSessionId(sessionInfo);
        transportService.registerSyncSession(sessionInfo, new ToServerRpcSyncSessionCallback(clientState, exchange, request), timeout);
        transportService.process(sessionInfo,
                clientState.getAdaptor().convertToServerRpcRequest(sessionId, request),
                new CoapNoOpCallback(exchange));
    }

    /**
     * Send an empty ACK if we are unable to send the full response within the timeout.
     * If the full response is transmitted before the timeout this will not do anything.
     * If this is triggered the full response will be sent in a separate CON/NON message.
     * Essentially this allows the use of piggybacked responses.
     */
    /**
     * 功能：执行 `deferAccept` 对应的处理。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    private void deferAccept(CoapExchange exchange) {
        if (piggybackTimeout > 0) {
            transportContext.getScheduler().schedule(exchange::accept, piggybackTimeout, TimeUnit.MILLISECONDS);
        } else {
            exchange.accept();
        }
    }

    /**
     * 功能：执行 `toSessionId` 对应的处理。
     * 参数：
     * - `sessionInfoProto`：会话对象。
     * 返回：处理结果。
     */
    private UUID toSessionId(TransportProtos.SessionInfoProto sessionInfoProto) {
        return new UUID(sessionInfoProto.getSessionIdMSB(), sessionInfoProto.getSessionIdLSB());
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    private String getTokenFromRequest(Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    /**
     * 功能：解析凭据。
     * 参数：
     * - `request`：请求对象。
     * 返回：可能存在的结果。
     */
    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() > ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `request`：请求对象。
     * 返回：可能存在的结果。
     */
    protected Optional<FeatureType> getFeatureType(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            int size = uriPath.size();
            if (size >= FEATURE_TYPE_POSITION) {
                if (size == FEATURE_TYPE_POSITION && StringUtils.isNumeric(uriPath.get(size - 1))) {
                    return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 2).toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION - 1).toUpperCase()));
            } else if (size == FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST) {
                if (uriPath.contains(DataConstants.PROVISION)) {
                    return Optional.of(FeatureType.valueOf(DataConstants.PROVISION.toUpperCase()));
                }
                return Optional.of(FeatureType.valueOf(uriPath.get(FEATURE_TYPE_POSITION_CERTIFICATE_REQUEST - 1).toUpperCase()));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `request`：请求对象。
     * 返回：可能存在的结果。
     */
    public static Optional<Integer> getRequestId(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            if (uriPath.size() >= REQUEST_ID_POSITION) {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION - 1)));
            } else {
                return Optional.of(Integer.valueOf(uriPath.get(REQUEST_ID_POSITION_CERTIFICATE_REQUEST - 1)));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    /**
     * 功能：获取`Child`。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    /**
     * 中文说明：
     * 1. 类目的：`DeviceProvisionCallback` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private static class DeviceProvisionCallback implements TransportServiceCallback<TransportProtos.ProvisionDeviceResponseMsg> {
        /**
         * `exchange` 字段，保存当前对象的对应属性。
         */
        private final CoapExchange exchange;
        private final TransportPayloadType payloadType;

        DeviceProvisionCallback(CoapExchange exchange, TransportPayloadType payloadType) {
            this.exchange = exchange;
            this.payloadType = payloadType;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg msg) {
            CoAP.ResponseCode responseCode = CoAP.ResponseCode.CREATED;
            if (!msg.getStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                responseCode = CoAP.ResponseCode.BAD_REQUEST;
            }
            if (payloadType.equals(TransportPayloadType.JSON)) {
                exchange.respond(responseCode, JsonConverter.toJson(msg).toString());
            } else {
                exchange.respond(responseCode, msg.toByteArray());
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`CoapResourceObserver` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    public class CoapResourceObserver implements ResourceObserver {

        /**
         * 功能：执行 `changedName` 对应的处理。
         * 参数：
         * - `old`：`old` 参数。
         * 返回：无。
         */
        @Override
        public void changedName(String old) {
        }

        /**
         * 功能：执行 `changedPath` 对应的处理。
         * 参数：
         * - `old`：`old` 参数。
         * 返回：无。
         */
        @Override
        public void changedPath(String old) {
        }

        /**
         * 功能：执行 `addedChild` 对应的处理。
         * 参数：
         * - `child`：`child` 参数。
         * 返回：无。
         */
        @Override
        public void addedChild(Resource child) {
        }

        /**
         * 功能：执行 `removedChild` 对应的处理。
         * 参数：
         * - `child`：`child` 参数。
         * 返回：无。
         */
        @Override
        public void removedChild(Resource child) {
        }

        /**
         * 功能：执行 `addedObserveRelation` 对应的处理。
         * 参数：
         * - `relation`：`relation` 参数。
         * 返回：无。
         */
        @Override
        public void addedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.registerObserveRelation(token, relation);
            log.trace("Added Observe relation for token: {}", token);
        }

        /**
         * 功能：执行 `removedObserveRelation` 对应的处理。
         * 参数：
         * - `relation`：`relation` 参数。
         * 返回：无。
         */
        @Override
        public void removedObserveRelation(ObserveRelation relation) {
            Request request = relation.getExchange().getRequest();
            String token = getTokenFromRequest(request);
            clients.deregisterObserveRelation(token);
            log.trace("Relation removed for token: {}", token);
        }
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`CoapTransportResource` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
