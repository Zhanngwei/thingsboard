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
package org.thingsboard.server.transport.mqtt.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.script.ScriptException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 中文说明：
 * 1. `MqttTopicFilterFactoryTest` 是 ThingsBoard Common Transport 中验证 `MqttTopicFilterFactory` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(MockitoJUnitRunner.class)
public class MqttTopicFilterFactoryTest {

    /**
     * `TEST_STR_1`常量，用于统一引用固定值。
     */
    private static String TEST_STR_1 = "Sensor/Temperature/House/48";
    private static String TEST_STR_2 = "Sensor/Temperature";
    /**
     * `TEST_STR_3`常量，用于统一引用固定值。
     */
    private static String TEST_STR_3 = "Sensor/Temperature2/House/48";
    private static String TEST_STR_4 = "/Sensor/Temperature2/House/48";
    /**
     * `TEST_STR_5`常量，用于统一引用固定值。
     */
    private static String TEST_STR_5 = "Sensor/ Temperature";
    private static String TEST_STR_6 = "/";

    /**
     * 功能：执行 `metadataCanBeUpdated` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void metadataCanBeUpdated() throws ScriptException {
        MqttTopicFilter filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature/House/+");
        assertTrue(filter.filter(TEST_STR_1));
        assertFalse(filter.filter(TEST_STR_2));

        filter = MqttTopicFilterFactory.toFilter("Sensor/+/House/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertFalse(filter.filter(TEST_STR_2));

        filter = MqttTopicFilterFactory.toFilter("Sensor/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertTrue(filter.filter(TEST_STR_3));

        filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature/#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertFalse(filter.filter(TEST_STR_3));

        filter = MqttTopicFilterFactory.toFilter("#");
        assertTrue(filter.filter(TEST_STR_1));
        assertTrue(filter.filter(TEST_STR_2));
        assertTrue(filter.filter(TEST_STR_3));
        assertTrue(filter.filter(TEST_STR_4));
        assertTrue(filter.filter(TEST_STR_5));
        assertTrue(filter.filter(TEST_STR_6));

        filter = MqttTopicFilterFactory.toFilter("Sensor/Temperature#");
        assertFalse(filter.filter(TEST_STR_2));
    }

}
