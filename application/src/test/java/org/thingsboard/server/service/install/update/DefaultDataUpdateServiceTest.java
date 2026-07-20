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
package org.thingsboard.server.service.install.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;

/**
 * 中文说明：
 * 1. `DefaultDataUpdateServiceTest` 是 ThingsBoard Application 中验证 `DefaultDataUpdateService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ActiveProfiles("install")
@SpringBootTest(classes = DefaultDataUpdateService.class)
class DefaultDataUpdateServiceTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @MockBean
    DefaultDataUpdateService service;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        willCallRealMethod().given(service).convertDeviceProfileAlarmRulesForVersion330(any());
        willCallRealMethod().given(service).convertDeviceProfileForVersion330(any());
    }

    /**
     * 功能：执行 `readFromResource` 对应的处理。
     * 参数：
     * - `resourceName`：名称。
     * 返回：处理结果。
     */
    JsonNode readFromResource(String resourceName) throws IOException {
        return JacksonUtil.OBJECT_MAPPER.readTree(this.getClass().getClassLoader().getResourceAsStream(resourceName));
    }

    /**
     * 功能：转换设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void convertDeviceProfileAlarmRulesForVersion330FirstRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_in.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isTrue();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    /**
     * 功能：转换设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void convertDeviceProfileAlarmRulesForVersion330SecondRun() throws IOException {
        JsonNode spec = readFromResource("update/330/device_profile_001_out.json");
        JsonNode expected = readFromResource("update/330/device_profile_001_out.json");

        assertThat(service.convertDeviceProfileForVersion330(spec.get("profileData"))).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString()); // use IDE feature <Click to see difference>
    }

    /**
     * 功能：转换设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void convertDeviceProfileAlarmRulesForVersion330EmptyJson() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ }");
        JsonNode expected = JacksonUtil.toJsonNode("{ }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    /**
     * 功能：转换设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void convertDeviceProfileAlarmRulesForVersion330AlarmNodeNull() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ \"alarms\" : null }");
        JsonNode expected = JacksonUtil.toJsonNode("{ \"alarms\" : null }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

    /**
     * 功能：转换设备配置。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void convertDeviceProfileAlarmRulesForVersion330NoAlarmNode() throws JsonProcessingException {
        JsonNode spec = JacksonUtil.toJsonNode("{ \"configuration\": { \"type\": \"DEFAULT\" } }");
        JsonNode expected = JacksonUtil.toJsonNode("{ \"configuration\": { \"type\": \"DEFAULT\" } }");

        assertThat(service.convertDeviceProfileForVersion330(spec)).isFalse();
        assertThat(spec.toPrettyString()).isEqualTo(expected.toPrettyString());
    }

}
