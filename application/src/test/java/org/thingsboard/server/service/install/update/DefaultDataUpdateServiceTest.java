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
 * 1. 类目的：`DefaultDataUpdateServiceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultDataUpdateServiceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
