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
package org.thingsboard.server.transport.mqtt.mqttv3.client;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Assert;
import org.thingsboard.server.transport.mqtt.AbstractMqttIntegrationTest;
import org.thingsboard.server.transport.mqtt.mqttv3.MqttTestClient;

/**
 * 中文说明：
 * 1. `AbstractMqttClientConnectionTest` 是 ThingsBoard Application 中验证 `AbstractMqttClientConnection` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public abstract class AbstractMqttClientConnectionTest extends AbstractMqttIntegrationTest {

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithCorrectAccessTokenTest() throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        Assert.assertTrue(client.isConnected());
        client.disconnect();
    }

    /**
     * 功能：处理客户端。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithWrongAccessTokenTest() throws Exception {
        MqttTestClient client = new MqttTestClient();
        try {
            client.connectAndWait("wrongAccessToken");
        } catch (MqttException e) {
            Assert.assertEquals(MqttException.REASON_CODE_NOT_AUTHORIZED, e.getReasonCode());
        }
    }

    /**
     * 功能：处理用户名。
     * 参数：无。
     * 返回：无。
     */
    protected void processClientWithWrongClientIdAndEmptyUsernamePasswordTest() throws Exception {
        MqttTestClient client = new MqttTestClient("unknownClientId");
        try {
            client.connectAndWait();
        } catch (MqttException e) {
            Assert.assertEquals(MqttException.REASON_CODE_INVALID_CLIENT_ID, e.getReasonCode());
        }
    }

}
