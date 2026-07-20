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
package org.thingsboard.server.transport.mqtt.mqttv5.rpc;

import com.nimbusds.jose.util.StandardCharset;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.mqtt.mqttv5.AbstractMqttV5Test;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestCallback;
import org.thingsboard.server.transport.mqtt.mqttv5.MqttV5TestClient;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;

/**
 * 中文说明：
 * 1. `AbstractMqttV5RpcTest` 是 ThingsBoard Application 中验证 `AbstractMqttV5Rpc` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5Test`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public abstract class AbstractMqttV5RpcTest extends AbstractMqttV5Test {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String DEVICE_RESPONSE = "{\"value1\":\"A\",\"value2\":\"B\"}";


    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    protected void processOneWayRpcTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        MqttV5TestCallback callback = new MqttV5TestCallback(DEVICE_RPC_REQUESTS_SUB_TOPIC.replace("+", "0"));
        client.setCallback(callback);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_MOST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.RPC, 1);

        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"23\",\"value\": 1}}";
        String result = doPostAsync("/api/rpc/oneway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        assertTrue(StringUtils.isEmpty(result));
        callback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals(MqttQoS.AT_MOST_ONCE.value(), callback.getQoS());
        client.disconnect();
    }

    /**
     * 功能：处理RPC。
     * 参数：无。
     * 返回：无。
     */
    protected void processJsonTwoWayRpcTest() throws Exception {
        MqttV5TestClient client = new MqttV5TestClient();
        client.connectAndWait(accessToken);
        client.subscribeAndWait(DEVICE_RPC_REQUESTS_SUB_TOPIC, MqttQoS.AT_LEAST_ONCE);
        awaitForDeviceActorToReceiveSubscription(savedDevice.getId(), FeatureType.RPC, 1);
        MqttV5TestRpcCallback callback = new MqttV5TestRpcCallback(client, DEVICE_RPC_REQUESTS_SUB_TOPIC.replace("+", "0"));
        client.setCallback(callback);
        String setGpioRequest = "{\"method\":\"setGpio\",\"params\":{\"pin\": \"26\",\"value\": 1}}";
        String actualRpcResponse = doPostAsync("/api/rpc/twoway/" + savedDevice.getId(), setGpioRequest, String.class, status().isOk());
        callback.getSubscribeLatch().await(3, TimeUnit.SECONDS);
        assertEquals(JacksonUtil.toJsonNode(setGpioRequest), JacksonUtil.fromBytes(callback.getPayloadBytes()));
        assertEquals("{\"value1\":\"A\",\"value2\":\"B\"}", actualRpcResponse);
        client.disconnect();
    }

    /**
     * 中文说明：
     * 1. `MqttV5TestRpcCallback` 是 ThingsBoard Application 中处理 MQTT 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `MqttV5TestCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    protected class MqttV5TestRpcCallback extends MqttV5TestCallback {

        /**
         * 客户端，用于发起外部调用或协议交互。
         */
        private final MqttV5TestClient client;

        /**
         * 功能：创建 `AbstractMqttV5RpcTest` 实例，并初始化必要字段。
         * 参数：
         * - `client`：客户端对象。
         * - `awaitSubTopic`：主题名称或主题对象。
         * 返回：新创建的对象实例。
         */
        public MqttV5TestRpcCallback(MqttV5TestClient client, String awaitSubTopic) {
            super(awaitSubTopic);
            this.client = client;
        }

        /**
         * 功能：执行 `messageArrivedOnAwaitSubTopic` 对应的处理。
         * 参数：
         * - `requestTopic`：请求对象。
         * - `mqttMessage`：待处理消息。
         * 返回：无。
         */
        @Override
        protected void messageArrivedOnAwaitSubTopic(String requestTopic, MqttMessage mqttMessage) {
            log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
            if (awaitSubTopic.equals(requestTopic)) {
                qoS = mqttMessage.getQos();
                payloadBytes = mqttMessage.getPayload();
                String responseTopic = requestTopic.replace("request", "response");
                try {
                    client.publish(responseTopic, DEVICE_RESPONSE.getBytes(StandardCharset.UTF_8));
                } catch (MqttException e) {
                    log.warn("Failed to publish response on topic: {} due to: ", responseTopic, e);
                }
                subscribeLatch.countDown();
            }
        }
    }

}
