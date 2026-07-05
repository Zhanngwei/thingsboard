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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelAllObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCreateRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCreateResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDeleteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDeleteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteUpdateRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MWriteResponseCompositeCallback;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcReadResponseCompositeCallback;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcWriteCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromRpcBody;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;

/**
 * 中文说明：
 * 1. 类目的：`DefaultLwM2MRpcRequestHandler` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MRpcRequestHandler implements LwM2MRpcRequestHandler {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TransportService transportService;
    private final LwM2mClientContext clientContext;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final LwM2MTelemetryLogService logService;
    private final LwM2mModelProvider modelProvider;

    /**
     * 功能：处理设备。
     * 参数：
     * - `rpcRequest`：请求对象。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        log.debug("Received params: {}", rpcRequest.getParams());
        try {
            LwM2MOperationType operationType = LwM2MOperationType.fromType(rpcRequest.getMethodName());
            if (operationType == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(), ResponseCode.METHOD_NOT_ALLOWED, "Unsupported operation type: " + rpcRequest.getMethodName());
                return;
            }
            LwM2mClient client = clientContext.getClientBySessionInfo(sessionInfo);

            if (client == null) {
                log.warn("Missing client for session: [{}]", sessionInfo);
                return;
            }

            if (client.getRegistration() == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(), ResponseCode.INTERNAL_SERVER_ERROR, "Registration is empty");
                return;
            }
            UUID rpcId = new UUID(rpcRequest.getRequestIdMSB(), rpcRequest.getRequestIdLSB());

            if (rpcId.equals(client.getLastSentRpcId())) {
                log.debug("[{}]][{}] Rpc has already sent!", client.getEndpoint(), rpcId);
                return;
            }
            try {
                if (operationType.isHasObjectId()) {
                    LwM2MRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), LwM2MRpcRequestHeader.class);
                    String objectId = getIdFromParameters(client, header);
                    switch (operationType) {
                        case READ:
                            sendReadRequest(client, rpcRequest, objectId);
                            break;
                        case OBSERVE:
                            sendObserveRequest(client, rpcRequest, objectId);
                            break;
                        case DISCOVER:
                            sendDiscoverRequest(client, rpcRequest, objectId);
                            break;
                        case EXECUTE:
                            sendExecuteRequest(client, rpcRequest, objectId);
                            break;
                        case WRITE_ATTRIBUTES:
                            sendWriteAttributesRequest(client, rpcRequest, objectId);
                            break;
                        case OBSERVE_CANCEL:
                            sendCancelObserveRequest(client, rpcRequest, objectId);
                            break;
                        case DELETE:
                            sendDeleteRequest(client, rpcRequest, objectId);
                            break;
                        case WRITE_UPDATE:
                            sendWriteUpdateRequest(client, rpcRequest, objectId);
                            break;
                        case WRITE_REPLACE:
                            sendWriteReplaceRequest(client, rpcRequest, objectId);
                            break;
                        case CREATE:
                            sendCreateRequest(client, rpcRequest, objectId);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported operation: " + operationType.name());
                    }
                } else if (operationType.isComposite()) {
                    ContentFormat contentFormatComposite = this.getCompositeContentFormat(client);
                    if (contentFormatComposite != null) {
                        switch (operationType) {
                            case READ_COMPOSITE:
                                sendReadCompositeRequest(client, rpcRequest, contentFormatComposite);
                                break;
                            case WRITE_COMPOSITE:
                                sendWriteCompositeRequest(client, rpcRequest, contentFormatComposite);
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported operation: " + operationType.name());
                        }
                    } else {
                        this.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                                ResponseCode.INTERNAL_SERVER_ERROR, "This device does not support Composite Operation");
                    }
                } else {
                    switch (operationType) {
                        case OBSERVE_CANCEL_ALL:
                            sendCancelAllObserveRequest(client, rpcRequest);
                            break;
                        case OBSERVE_READ_ALL:
                            sendObserveAllRequest(client, rpcRequest);
                            break;
                        case DISCOVER_ALL:
                            sendDiscoverAllRequest(client, rpcRequest);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported operation: " + operationType.name());
                    }
                }
            } catch (IllegalArgumentException e) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(), ResponseCode.BAD_REQUEST, e.getMessage());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to send RPC: [{}]", sessionInfo, rpcRequest, e);
            this.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(), ResponseCode.INTERNAL_SERVER_ERROR, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendReadRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MReadCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcReadResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendReadRequest(client, request, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `contentFormatComposite`：`contentFormatComposite` 参数。
     * 返回：无。
     */
    private void sendReadCompositeRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, ContentFormat contentFormatComposite) {
        String[] versionedIds = getIdsFromParameters(client, requestMsg);
        TbLwM2MReadCompositeRequest request = TbLwM2MReadCompositeRequest.builder().versionedIds(versionedIds).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MReadCompositeCallback(uplinkHandler, logService, client, versionedIds);
        var rpcCallback = new RpcReadResponseCompositeCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendReadCompositeRequest(client, request, rpcCallback, contentFormatComposite);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MObserveCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcReadResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendObserveRequest(client, request, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * 返回：无。
     */
    private void sendObserveAllRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MObserveAllRequest request = TbLwM2MObserveAllRequest.builder().timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendObserveAllRequest(client, request, new RpcLinkSetCallback<>(transportService, client, requestMsg, null));
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * 返回：无。
     */
    private void sendDiscoverAllRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MDiscoverAllRequest request = TbLwM2MDiscoverAllRequest.builder().timeout(clientContext.getRequestTimeout(client)).build();
        downlinkHandler.sendDiscoverAllRequest(client, request, new RpcLinkSetCallback<>(transportService, client, requestMsg, null));
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendDiscoverRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MDiscoverRequest request = TbLwM2MDiscoverRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MDiscoverCallback(logService, client, versionedId);
        var rpcCallback = new RpcDiscoverCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendDiscoverRequest(client, request, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendExecuteRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MExecuteRequest downlink = TbLwM2MExecuteRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MExecuteCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendExecuteRequest(client, downlink, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendWriteAttributesRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteAttributesRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteAttributesRequest.class);
        TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(versionedId)
                .attributes(requestBody.getAttributes())
                .timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MWriteAttributesCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteAttributesRequest(client, request, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendWriteUpdateRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteUpdateRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteUpdateRequest.class);
        TbLwM2MWriteUpdateRequest.TbLwM2MWriteUpdateRequestBuilder builder = TbLwM2MWriteUpdateRequest.builder().versionedId(versionedId);
        builder.value(requestBody.getValue()).timeout(clientContext.getRequestTimeout(client));
        var mainCallback = new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteUpdateRequest(client, builder.build(), rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendCreateRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcCreateRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcCreateRequest.class);
        TbLwM2MCreateRequest.TbLwM2MCreateRequestBuilder builder = TbLwM2MCreateRequest.builder().versionedId(versionedId);
        builder.value(requestBody.getValue()).nodes(requestBody.getNodes()).timeout(clientContext.getRequestTimeout(client));
        var mainCallback = new TbLwM2MCreateResponseCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcCreateResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendCreateRequest(client, builder.build(), rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendWriteReplaceRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteReplaceRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteReplaceRequest.class);
        LwM2mPath path = new LwM2mPath(fromVersionedIdToObjectId(versionedId));
        if (path.isResource()) {
            ResourceModel resourceModel = client.getResourceModel(versionedId, modelProvider);
            if (resourceModel != null && resourceModel.multiple) {
                try {
                    Map<Integer, Object> value = convertMultiResourceValuesFromRpcBody(requestBody.getValue(), resourceModel.type, versionedId);
                    requestBody.setValue(value);
                } catch (Exception e) {
                }
            }
        }
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                .value(requestBody.getValue())
                .timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteReplaceRequest(client, request, rpcCallback);
    }

    /**
     * WriteComposite {"nodes":{"/3/0/14":"+04", "/1/0/2":100, "/5/0/1":"coap://localhost:5685"}}
     * {"result":"CHANGED"}
     * Map<String, Object> nodes = new HashMap<>();
     * nodes.put("/3/0/14", "+02");
     * nodes.put("/1/0/2", 100);
     * nodes.put("/5/0/1", "coap://localhost:5685");
     */
    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `contentFormatComposite`：`contentFormatComposite` 参数。
     * 返回：无。
     */
    private void sendWriteCompositeRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, ContentFormat contentFormatComposite) {
        RpcWriteCompositeRequest rpcWriteCompositeRequest = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteCompositeRequest.class);
        Map<String, Object> validNodes = validateNodes(client, rpcWriteCompositeRequest.getNodes());
        if (validNodes.size() > 0) {
            rpcWriteCompositeRequest.setNodes(validNodes);
            var mainCallback = new TbLwM2MWriteResponseCompositeCallback(uplinkHandler, logService, client, null);
            var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
            downlinkHandler.sendWriteCompositeRequest(client, rpcWriteCompositeRequest, rpcCallback, contentFormatComposite);
        } else {
            throw new IllegalArgumentException(String.format("nodes: %s is not validate value", rpcWriteCompositeRequest.getNodes().toString()));
        }
    }

    /**
     * 功能：校验`Nodes`。
     * 参数：
     * - `client`：客户端对象。
     * - `nodes`：键值映射。
     * 返回：判断结果。
     */
    private Map<String, Object> validateNodes(LwM2mClient client, Map<String, Object> nodes) {
        Map<String, Object> newNodes = new LinkedHashMap<>();
        nodes.forEach((key, value) -> {
            String versionedId;
            try {
                LwM2mPath path = new LwM2mPath(fromVersionedIdToObjectId(key));
                if (path.isResource() || path.isResourceInstance()) {
                    versionedId = key;
                }
                else {
                    throw new IllegalArgumentException(String.format("nodes: %s is not validate value. " +
                            "The WriteComposite operation is only used for SingleResources or/and ResourceInstance.", nodes.toString()));
                }
            } catch (Exception e) {
                versionedId = clientContext.getObjectIdByKeyNameFromProfile(client, key);
            }
            // validate value. Must be only primitive, not JsonObject or JsonArray
            try {
                JsonElement element = JsonUtils.parse(value.toString());
                if (!element.isJsonNull() && !element.isJsonPrimitive()) {
                    throw new IllegalArgumentException(String.format("nodes: %s is not validate value. " +
                            "The WriteComposite operation is only used for SingleResources or/and ResourceInstance.", nodes.toString()));
                }
                else if (versionedId != null) {
                    newNodes.put(fromVersionedIdToObjectId(versionedId), value);
                }
            } catch (JsonSyntaxException jse) {
                if (versionedId != null) {
                    newNodes.put(fromVersionedIdToObjectId(versionedId), value);
                }
            }
        });
        return newNodes;
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendCancelObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MCancelObserveRequest downlink = TbLwM2MCancelObserveRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MCancelObserveCallback(logService, client, versionedId);
        var rpcCallback = new RpcCancelObserveCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendCancelObserveRequest(client, downlink, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：无。
     */
    private void sendDeleteRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MDeleteRequest downlink = TbLwM2MDeleteRequest.builder().versionedId(versionedId).timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MDeleteCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendDeleteRequest(client, downlink, rpcCallback);
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * 返回：无。
     */
    private void sendCancelAllObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MCancelAllRequest downlink = TbLwM2MCancelAllRequest.builder().timeout(clientContext.getRequestTimeout(client)).build();
        var mainCallback = new TbLwM2MCancelAllObserveCallback(logService, client);
        var rpcCallback = new RpcCancelAllObserveCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendCancelAllRequest(client, downlink, rpcCallback);
    }

    /**
     * 功能：获取参数集合。
     * 参数：
     * - `client`：客户端对象。
     * - `header`：`header` 参数。
     * 返回：文本结果。
     */
    private String getIdFromParameters(LwM2mClient client, LwM2MRpcRequestHeader header) {
        String targetId;
        if (StringUtils.isNotEmpty(header.getKey())) {
            targetId = clientContext.getObjectIdByKeyNameFromProfile(client, header.getKey());
        } else if (StringUtils.isNotEmpty(header.getId())) {
            targetId = header.getId();
        } else {
            throw new IllegalArgumentException("Can't find 'key' or 'id' in the requestParams parameters!");
        }
        return targetId;
    }

    /**
     * 功能：获取参数集合。
     * 参数：
     * - `client`：客户端对象。
     * - `rpcRequest`：请求对象。
     * 返回：处理结果。
     */
    private String[] getIdsFromParameters(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        RpcReadCompositeRequest requestParams = JacksonUtil.fromString(rpcRequest.getParams(), RpcReadCompositeRequest.class);
        if (requestParams.getKeys() != null && requestParams.getKeys().length > 0) {
            Set<String> targetIds = ConcurrentHashMap.newKeySet();
            for (String key : requestParams.getKeys()) {
                String targetId = clientContext.getObjectIdByKeyNameFromProfile(client, key);
                if (targetId != null) {
                    targetIds.add(targetId);
                }
            }
            return targetIds.toArray(String[]::new);
        } else if (requestParams.getIds() != null && requestParams.getIds().length > 0) {
            return requestParams.getIds();
        } else {
            throw new IllegalArgumentException("Can't find 'key' or 'id' in the requestParams parameters!");
        }
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `requestId`：请求ID。
     * - `result`：`result` 参数。
     * - `error`：错误信息。
     * 返回：无。
     */
    private void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ResponseCode result, String error) {
        String payload = JacksonUtil.toString(LwM2MRpcResponseBody.builder().result(result.getName()).error(error).build());
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setError(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `toDeviceResponse`：设备信息或设备标识。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceResponse, TransportProtos.SessionInfoProto sessionInfo) {
        log.debug("OnToDeviceRpcResponse: [{}], sessionUUID: [{}]", toDeviceResponse, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
        transportService.process(sessionInfo, toDeviceResponse, null);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `toServerResponse`：响应对象。
     * 返回：无。
     */
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        log.info("[{}] toServerRpcResponse", toServerResponse);
    }

    /**
     * 功能：获取内容格式。
     * 参数：
     * - `client`：客户端对象。
     * 返回：处理结果。
     */
    private ContentFormat getCompositeContentFormat(LwM2mClient client) {
        if (client.getClientSupportContentFormats().contains(ContentFormat.SENML_JSON)) {
            return ContentFormat.SENML_JSON;
        }
        else if (client.getClientSupportContentFormats().contains(ContentFormat.SENML_CBOR)) {
            return ContentFormat.SENML_CBOR;
        }
        else {
            return null;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultLwM2MRpcRequestHandler` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
