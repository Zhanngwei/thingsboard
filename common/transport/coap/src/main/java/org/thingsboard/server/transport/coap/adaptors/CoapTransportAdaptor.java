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

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `CoapTransportAdaptor` 是 ThingsBoard Common Transport 中定义 CoAP 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface CoapTransportAdaptor {

    /**
     * 功能：转换遥测。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `telemetryMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, Request inbound, Descriptors.Descriptor telemetryMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `attributesMsgDescriptor`：待处理消息。
     * 返回：处理结果。
     */
    TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, Request inbound, Descriptors.Descriptor attributesMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    TransportProtos.GetAttributeRequestMsg convertToGetAttributes(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `rpcResponseMsgDescriptor`：响应对象。
     * 返回：处理结果。
     */
    TransportProtos.ToDeviceRpcResponseMsg convertToDeviceRpcResponse(UUID sessionId, Request inbound, Descriptors.Descriptor rpcResponseMsgDescriptor) throws AdaptorException;

    /**
     * 功能：转换RPC。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    TransportProtos.ToServerRpcRequestMsg convertToServerRpcRequest(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：转换设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    TransportProtos.ClaimDeviceMsg convertToClaimDevice(UUID sessionId, Request inbound, TransportProtos.SessionInfoProto sessionInfo) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `responseMsg`：响应对象。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.GetAttributeResponseMsg responseMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `notificationMsg`：待处理消息。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.AttributeUpdateNotificationMsg notificationMsg) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `rpcRequest`：请求对象。
     * - `rpcRequestDynamicMessageBuilder`：请求对象。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.ToDeviceRpcRequestMsg rpcRequest, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) throws AdaptorException;

    /**
     * 功能：转换`To Publish`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    Response convertToPublish(TransportProtos.ToServerRpcResponseMsg msg) throws AdaptorException;

    /**
     * 功能：转换消息。
     * 参数：
     * - `sessionId`：会话ID。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    ProvisionDeviceRequestMsg convertToProvisionRequestMsg(UUID sessionId, Request inbound) throws AdaptorException;

    /**
     * 功能：获取内容格式。
     * 参数：无。
     * 返回：数值结果。
     */
    int getContentFormat();

}
