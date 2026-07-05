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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;

/**
 * 中文说明：
 * 1. 类目的：`TbKafkaSettingsTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@SpringBootTest(classes = TbKafkaSettings.class)
@TestPropertySource(properties = {
        "queue.type=kafka",
        "queue.kafka.bootstrap.servers=localhost:9092",
        "queue.kafka.other-inline=metrics.recording.level:INFO;metrics.sample.window.ms:30000",
})
class TbKafkaSettingsTest {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    TbKafkaSettings settings;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void beforeEach() {
        settings = spy(settings); //SpyBean is not aware on @ConditionalOnProperty, that is why the traditional spy in use
    }

    /**
     * 功能：验证 `givenToProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToProps_whenConfigureSSL_thenVerifyOnce() {
        Properties props = settings.toProps();

        assertThat(props).as("TB_QUEUE_KAFKA_REQUEST_TIMEOUT_MS").containsEntry("request.timeout.ms", 30000);
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("session.timeout.ms", 10000);

        //other-inline
        assertThat(props).as("metrics.recording.level").containsEntry("metrics.recording.level", "INFO");
        assertThat(props).as("TB_QUEUE_KAFKA_SESSION_TIMEOUT_MS").containsEntry("metrics.sample.window.ms", "30000");

        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenToAdminProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToAdminProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toAdminProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenToConsumerProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenToConsumerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toConsumerProps("main");
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }

    /**
     * 功能：验证 `givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenTotoProducerProps_whenConfigureSSL_thenVerifyOnce() {
        settings.toProducerProps();
        Mockito.verify(settings).toProps();
        Mockito.verify(settings).configureSSL(any());
    }


/*
 * 本类总结：
 * 1. 核心职责：`TbKafkaSettingsTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}