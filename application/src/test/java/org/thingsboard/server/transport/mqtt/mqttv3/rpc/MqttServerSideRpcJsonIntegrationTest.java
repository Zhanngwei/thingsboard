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
package org.thingsboard.server.transport.mqtt.mqttv3.rpc;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC;
import static org.thingsboard.server.common.data.device.profile.MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC;

/**
 * 中文说明：
 * 1. `MqttServerSideRpcJsonIntegrationTest` 是 ThingsBoard Application 中验证 `MqttServerSideRpcJsonIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttServerSideRpcIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
public class MqttServerSideRpcJsonIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("RPC test device")
                .gatewayName("RPC test gateway")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttOneWayRpc() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttOneWayRpcOnShortTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttOneWayRpcOnShortJsonTopic() throws Exception {
        processOneWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttTwoWayRpc() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_TOPIC);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttTwoWayRpcOnShortTopic() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC);
    }

    /**
     * 功能：验证主题相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testServerMqttTwoWayRpcOnShortJsonTopic() throws Exception {
        processJsonTwoWayRpcTest(DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC);
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayServerMqttOneWayRpc() throws Exception {
        processJsonOneWayRpcTestGateway("Gateway Device OneWay RPC Json");
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGatewayServerMqttTwoWayRpc() throws Exception {
        processJsonTwoWayRpcTestGateway("Gateway Device TwoWay RPC Json");
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processJsonOneWayRpcTestGateway(String deviceName) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(gatewayAccessToken);
        String payload = "{\"device\": \"" + deviceName + "\", \"type\": \"" + TransportPayloadType.JSON.name() + "\"}";
        byte[] payloadBytes = payload.getBytes();
        validateOneWayRpcGatewayResponse(deviceName, client, payloadBytes);
        client.disconnect();
    }

}
