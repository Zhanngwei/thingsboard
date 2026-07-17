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
package org.thingsboard.server.transport.mqtt.sparkplug.attributes;

import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.dao.service.DaoSqlTest;

/**
 * Created by nickAS21 on 12.01.23
 */
/**
 * 中文说明：
 * 1. `MqttV5ClientSparkplugBAttributesTest` 是 ThingsBoard Application 中验证 `MqttV5ClientSparkplugBAttributes` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractMqttV5ClientSparkplugAttributesTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class MqttV5ClientSparkplugBAttributesTest extends AbstractMqttV5ClientSparkplugAttributesTest {

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
    public void afterTest () throws MqttException {
        if (client.isConnected()) {
            client.disconnect();        }
    }

    /**
     * 功能：验证客户端相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMDReBirth() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMDReBirth();
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk();
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk();
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk();
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk();
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttribute() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttribute();
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttributes_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        processClientDeviceWithCorrectAccessTokenPublishWithBirth_SharedAttributes_LongType_IfMetricFailedTypeCheck_SendValueOk();
    }

}
