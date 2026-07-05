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
package org.thingsboard.server.transport.mqtt.mqttv3;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 中文说明：
 * 1. 类目的：`MqttTestSubscribeOnTopicCallback` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class MqttTestSubscribeOnTopicCallback extends MqttTestCallback {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    protected final String awaitSubTopic;

    /**
     * 功能：创建 `MqttTestSubscribeOnTopicCallback` 实例，并初始化必要字段。
     * 参数：
     * - `awaitSubTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public MqttTestSubscribeOnTopicCallback(String awaitSubTopic) {
        super();
        this.awaitSubTopic = awaitSubTopic;
    }

    /**
     * 功能：执行 `messageArrived` 对应的处理。
     * 参数：
     * - `requestTopic`：请求对象。
     * - `mqttMessage`：待处理消息。
     * 返回：无。
     */
    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            messageArrivedQoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTestSubscribeOnTopicCallback` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
