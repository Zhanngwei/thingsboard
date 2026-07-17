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
package org.thingsboard.server.transport.mqtt.sparkplug.rpc;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * 中文说明：
 * 1. `MqttV5RpcSparkplugTest` 是 ThingsBoard Application 中验证 `MqttV5RpcSparkplug` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5RpcSparkplugTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
@Slf4j
public class MqttV5RpcSparkplugTest  extends AbstractMqttV5RpcSparkplugTest {

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        beforeSparkplugTest();
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws MqttException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_Success();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublish_TwoWayRpc_Success();
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS();
    }

    /**
     * 功能：验证RPC相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InBirthNotHaveMetric_BAD_REQUEST_PARAMS() throws Exception {
        processClientNodeWithCorrectAccessTokenPublish_TwoWayRpc_InvalidTypeMessage_INVALID_ARGUMENTS();
    }

}
