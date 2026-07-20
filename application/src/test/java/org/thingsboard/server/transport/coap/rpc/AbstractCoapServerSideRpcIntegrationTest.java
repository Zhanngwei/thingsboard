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
package org.thingsboard.server.transport.coap.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.AbstractCoapIntegrationTest;
import org.thingsboard.server.transport.coap.CoapTestCallback;
import org.thingsboard.server.transport.coap.CoapTestClient;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `AbstractCoapServerSideRpcIntegrationTest` 是 ThingsBoard Application 中验证 `AbstractCoapServerSideRpcIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractCoapIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractCoapServerSideRpcIntegrationTest extends AbstractCoapIntegrationTest {

    public static final  String RPC_REQUEST_PROTO_SCHEMA = "syntax =\"proto3\";\n" +
            "package rpc;\n" +
            "\n" +
            "message RpcRequestMsg {\n" +
            "  optional string method = 1;\n" +
            "  optional int32 requestId = 2;\n" +
            "  Params params = 3;\n" +
            "\n" +
            "  message Params {\n" +
            "      optional string pin = 1;\n" +
            "      optional int32 value = 2;\n" +
            "   }\n" +
            "}";

    /**
     * 设备常量，用于统一引用固定值。
     */
    protected static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";

    /**
     * RPC常量，用于统一引用固定值。
     */
    protected static final Long asyncContextTimeoutToUseRpcPlugin = 10000L;

    /**
     * 功能：处理RPC。
     * 参数：
     * - `protobuf`：`protobuf` 参数。
     * 返回：无。
     */
    protected void processOneWayRpcTest(boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, true, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await One Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null && 0 == callbackCoap.getObserve());
        validateCurrentStateNotification(callbackCoap);
        int expectedObserveAfterRpcProcessed = callbackCoap.getObserve() + 1;
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        String result = doPostAsync("/api/rpc/oneway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await One Way Rpc setGpio(method, params, value)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null && expectedObserveAfterRpcProcessed == callbackCoap.getObserve());
        validateOneWayStateChangedNotification(callbackCoap, result);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `expectedResponseResult`：响应对象。
     * - `protobuf`：`protobuf` 参数。
     * 返回：无。
     */
    protected void processTwoWayRpcTest(String expectedResponseResult, boolean protobuf) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.RPC);
        CoapTestCallback callbackCoap = new TestCoapCallbackForRPC(client, false, protobuf);

        CoapObserveRelation observeRelation = client.getObserveRelation(callbackCoap);
        String awaitAlias = "await Two Way Rpc (client.getObserveRelation)";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.VALID.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        0 == callbackCoap.getObserve());
        validateCurrentStateNotification(callbackCoap);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String deviceId = savedDevice.getId().getId().toString();
        int expectedObserveCountAfterGpioRequest1 = callbackCoap.getObserve() + 1;
        String actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest1 == callbackCoap.getObserve());
        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        int expectedObserveCountAfterGpioRequest2 = callbackCoap.getObserve() + 1;
        actualResult = doPostAsync("/api/rpc/twoway/" + deviceId, setGpioRequest, String.class, status().isOk());
        awaitAlias = "await Two Way Rpc (setGpio(method, params, value) first";
        await(awaitAlias)
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> CoAP.ResponseCode.CONTENT.equals(callbackCoap.getResponseCode()) &&
                        callbackCoap.getObserve() != null &&
                        expectedObserveCountAfterGpioRequest2 == callbackCoap.getObserve());

        validateTwoWayStateChangedNotification(callbackCoap, expectedResponseResult, actualResult);

        observeRelation.proactiveCancel();
        assertTrue(observeRelation.isCanceled());
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * - `client`：客户端对象。
     * 返回：无。
     */
    protected void processOnLoadResponse(CoapResponse response, CoapTestClient client) {
        JsonNode responseJson = JacksonUtil.fromBytes(response.getPayload());
        int requestId = responseJson.get("id").asInt();
        client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, requestId));
        client.postMethod(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                log.warn("RPC {} command response ack: {}", requestId, response.getCode());
            }

            @Override
            public void onError() {
                log.warn("RPC {} command response ack error, no connect", requestId);
            }
        }, DEVICE_RESPONSE, MediaTypeRegistry.APPLICATION_JSON);
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * - `client`：客户端对象。
     * 返回：无。
     */
    protected void processOnLoadProtoResponse(CoapResponse response, CoapTestClient client) {
        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = getProtoTransportPayloadConfiguration();
        ProtoFileElement rpcRequestProtoFileElement = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());
        DynamicSchema rpcRequestProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcRequestProtoFileElement, ProtoTransportPayloadConfiguration.RPC_REQUEST_PROTO_SCHEMA);

        byte[] requestPayload = response.getPayload();
        DynamicMessage.Builder rpcRequestMsg = rpcRequestProtoSchema.newMessageBuilder("RpcRequestMsg");
        Descriptors.Descriptor rpcRequestMsgDescriptor = rpcRequestMsg.getDescriptorForType();
        try {
            DynamicMessage dynamicMessage = DynamicMessage.parseFrom(rpcRequestMsgDescriptor, requestPayload);
            Descriptors.FieldDescriptor requestIdDescriptor = rpcRequestMsgDescriptor.findFieldByName("requestId");
            int requestId = (int) dynamicMessage.getField(requestIdDescriptor);
            ProtoFileElement rpcResponseProtoSchemaFile = DynamicProtoUtils.getProtoFileElement(protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
            DynamicSchema rpcResponseProtoSchema = DynamicProtoUtils.getDynamicSchema(rpcResponseProtoSchemaFile, ProtoTransportPayloadConfiguration.RPC_RESPONSE_PROTO_SCHEMA);
            DynamicMessage.Builder rpcResponseBuilder = rpcResponseProtoSchema.newMessageBuilder("RpcResponseMsg");
            Descriptors.Descriptor rpcResponseMsgDescriptor = rpcResponseBuilder.getDescriptorForType();
            DynamicMessage rpcResponseMsg = rpcResponseBuilder
                    .setField(rpcResponseMsgDescriptor.findFieldByName("payload"), DEVICE_RESPONSE)
                    .build();
            client.setURI(CoapTestClient.getFeatureTokenUrl(accessToken, FeatureType.RPC, requestId));
            client.postMethod(new CoapHandler() {
                @Override
                public void onLoad(CoapResponse response) {
                    log.warn("RPC {} command response ack: {}", requestId, response.getCode());
                }

                @Override
                public void onError() {
                    log.warn("RPC {} command response ack error, no connect", requestId);
                }
            }, rpcResponseMsg.toByteArray(), MediaTypeRegistry.APPLICATION_JSON);
        } catch (InvalidProtocolBufferException e) {
            log.warn("Command Response Ack Error, Invalid response received: ", e);
        }
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    private ProtoTransportPayloadConfiguration getProtoTransportPayloadConfiguration() {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        assertTrue(transportConfiguration instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
        assertTrue(coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
        TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
        assertTrue(transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration);
        return (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateCurrentStateNotification(CoapTestCallback callback) {
        assertArrayEquals(EMPTY_PAYLOAD, callback.getPayloadBytes());
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `callback`：处理完成后的回调。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    private void validateOneWayStateChangedNotification(CoapTestCallback callback, String result) {
        assertTrue(StringUtils.isEmpty(result));
        assertNotNull(callback.getPayloadBytes());
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `callback`：处理完成后的回调。
     * - `expectedResult`：`expectedResult` 参数。
     * - `actualResult`：`actualResult` 参数。
     * 返回：无。
     */
    private void validateTwoWayStateChangedNotification(CoapTestCallback callback, String expectedResult, String actualResult) {
        assertEquals(expectedResult, actualResult);
        assertNotNull(callback.getPayloadBytes());
    }

    /**
     * 中文说明：
     * 1. `TestCoapCallbackForRPC` 是 ThingsBoard Application 中负责 CoAP 接入或传输适配的类型。
     * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
     * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
     * 4. 直接依赖的类型边界包括 `CoapTestCallback`。
     * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
     * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
     */
    protected class TestCoapCallbackForRPC extends CoapTestCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final CoapTestClient client;
        private final boolean isOneWayRpc;
        /**
         * 是否满足Protobuf条件。
         */
        private final boolean protobuf;

        TestCoapCallbackForRPC(CoapTestClient client, boolean isOneWayRpc, boolean protobuf) {
            this.client = client;
            this.isOneWayRpc = isOneWayRpc;
            this.protobuf = protobuf;
        }

        /**
         * 功能：处理`on Load`。
         * 参数：
         * - `response`：响应对象。
         * 返回：无。
         */
        @Override
        public void onLoad(CoapResponse response) {
            payloadBytes = response.getPayload();
            responseCode = response.getCode();
            observe = response.getOptions().getObserve();
            if (observe != null) {
                if (!isOneWayRpc && observe > 0) {
                    if (!protobuf){
                        processOnLoadResponse(response, client);
                    } else {
                        processOnLoadProtoResponse(response, client);
                    }
                }
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：无。
         * 返回：无。
         */
        @Override
        public void onError() {
            log.warn("Command Response Ack Error, No connect");
        }
    }
}
