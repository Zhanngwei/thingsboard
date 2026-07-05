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
package org.thingsboard.server.queue.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.queue.TbQueueMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * 中文说明：
 * 1. 类目的：`TbKafkaProducerTemplateTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
class TbKafkaProducerTemplateTest {

    /**
     * 消息生产者，承载当前流程需要传递的内容。
     */
    TbKafkaProducerTemplate<TbQueueMsg> producerTemplate;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        producerTemplate = mock(TbKafkaProducerTemplate.class);
        willCallRealMethod().given(producerTemplate).addAnalyticHeaders(any());
        willReturn("tb-core-to-core-notifications-tb-core-3").given(producerTemplate).getClientId();
    }

    /**
     * 功能：验证`Add Analytic Headers`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testAddAnalyticHeaders() {
        List<Header> headers = new ArrayList<>();
        producerTemplate.addAnalyticHeaders(headers);
        assertThat(headers).isNotEmpty();
        headers.forEach(r -> log.info("RecordHeader key [{}] value [{}]", r.key(), new String(r.value(), StandardCharsets.UTF_8)));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbKafkaProducerTemplateTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
