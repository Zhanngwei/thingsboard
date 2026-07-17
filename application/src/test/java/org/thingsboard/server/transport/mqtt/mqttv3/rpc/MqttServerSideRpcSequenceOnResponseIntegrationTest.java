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

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.mqtt.MqttTestConfigProperties;

/**
 * 中文说明：
 * 1. `MqttServerSideRpcSequenceOnResponseIntegrationTest` 是 ThingsBoard Application 中验证 `MqttServerSideRpcSequenceOnResponseIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttServerSideRpcIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "actors.rpc.submit_strategy=SEQUENTIAL_ON_RESPONSE_FROM_DEVICE",
})
public class MqttServerSideRpcSequenceOnResponseIntegrationTest extends AbstractMqttServerSideRpcIntegrationTest {

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
                .build();
        processBeforeTest(configProperties);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttOneWayRpcQoSAtMostOnce() throws Exception {
        processSequenceOneWayRpcTest(MqttQoS.AT_MOST_ONCE);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttOneWayRpcQoSAtLeastOnce() throws Exception {
        processSequenceOneWayRpcTest(MqttQoS.AT_LEAST_ONCE);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtMostOnce() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_MOST_ONCE);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtLeastOnce() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_LEAST_ONCE);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtMostOnceWithManualAcksEnabled() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_MOST_ONCE, true);
    }

    /**
     * 功能：验证QoS 等级相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequenceServerMqttTwoWayRpcQoSAtLeastOnceWithoutManualAcksEnabled() throws Exception {
        processSequenceTwoWayRpcTest(MqttQoS.AT_LEAST_ONCE, true);
    }


}
