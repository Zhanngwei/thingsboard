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
package org.thingsboard.server.transport.coap.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.CoapTransportResource;

import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`JsonCoapAdaptor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@Slf4j
public class JsonCoapAdaptor implements CoapTransportAdaptor {

    /**
     * 功能：转换遥测。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `telemetryMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `attributesMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToAttributesProto(new JsonParser().parse(payload));
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException {
        return CoapAdaptorUtils.toGetAttributeRequestMsg(inbound);
    }

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `rpcResponseMsgDescriptor`：响应对象。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException {
        Optional<Integer> requestId = CoapTransportResource.getRequestId(inbound);
        String payload = validatePayload(sessionId, inbound, false);
        JsonObject response = new JsonParser().parse(payload).getAsJsonObject();
        return TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId.orElseThrow(() -> new AdaptorException("Request id is missing!")))
                .setPayload(response.toString()).build();
    }

    /**
     * 功能：转换RPC。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        return JsonConverter.convertToServerRpcRequest(new JsonParser().parse(payload), 0);
    }

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException {
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        String payload = validatePayload(sessionId, inbound, true);
        try {
            return JsonConverter.convertToClaimDeviceProto(deviceId, payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public Response convertToPublish(TransportProtos.AttributeUpdateNotificationMsg msg) throws AdaptorException {
        return getObserveNotification(JsonConverter.toJson(msg));
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * - `rpcRequestDynamicMessageBuilder`：请求对象。
     * 返回：处理结果。
     */
    @Override
    public Response convertToPublish(TransportProtos.ToDeviceRpcRequestMsg msg, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException {
        return getObserveNotification(JsonConverter.toJson(msg, true));
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public Response convertToPublish(TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        JsonElement result = JsonConverter.toJson(msg);
        response.setPayload(result.toString());
        return response;
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException {
        String payload = validatePayload(sessionId, inbound, false);
        try {
            return JsonConverter.convertToProvisionRequestMsg(payload);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public Response convertToPublish(TransportProtos.GetAttributeResponseMsg msg) throws AdaptorException {
        if (msg.getSharedStateMsg()) {
            if (StringUtils.isEmpty(msg.getError())) {
                Response response = new Response(CoAP.ResponseCode.CONTENT);
                TransportProtos.AttributeUpdateNotificationMsg notificationMsg = TransportProtos.AttributeUpdateNotificationMsg.newBuilder().addAllSharedUpdated(msg.getSharedAttributeListList()).build();
                JsonObject result = JsonConverter.toJson(notificationMsg);
                response.setPayload(result.toString());
                return response;
            } else {
                return new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            if (msg.getClientAttributeListCount() == 0 && msg.getSharedAttributeListCount() == 0) {
                return new Response(CoAP.ResponseCode.NOT_FOUND);
            } else {
                Response response = new Response(CoAP.ResponseCode.CONTENT);
                JsonObject result = JsonConverter.toJson(msg);
                response.setPayload(result.toString());
                return response;
            }
        }
    }

    /**
     * 功能：获取通知。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：处理结果。
     */
    private Response getObserveNotification(JsonElement json) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        response.setPayload(json.toString());
        return response;
    }

    /**
     * 功能：校验消息载荷。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `isEmptyPayloadAllowed`：`isEmptyPayloadAllowed` 参数。
     * 返回：判断结果。
     */
    private String validatePayload(UUID sessionId, Request inbound, boolean isEmptyPayloadAllowed) throws AdaptorException {
        String payload = inbound.getPayloadString();
        if (payload == null) {
            log.debug("[{}] Payload is empty!", sessionId);
            if (!isEmptyPayloadAllowed) {
                throw new AdaptorException(new IllegalArgumentException("Payload is empty!"));
            }
        }
        return payload;
    }

    /**
     * 功能：获取内容格式。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getContentFormat() {
        return MediaTypeRegistry.APPLICATION_JSON;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JsonCoapAdaptor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
