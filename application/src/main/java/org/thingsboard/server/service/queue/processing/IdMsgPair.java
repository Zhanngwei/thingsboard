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
package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`IdMsgPair` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
public class IdMsgPair<T extends com.google.protobuf.GeneratedMessageV3> {
    /**
     * `uuid`ID，用于定位对应业务对象。
     */
    @Getter
    final UUID uuid;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    final TbProtoQueueMsg<T> msg;

    /**
     * 功能：创建 `IdMsgPair` 实例，并初始化必要字段。
     * 参数：
     * - `uuid`：`uuid`ID。
     * - `msg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public IdMsgPair(UUID uuid, TbProtoQueueMsg<T> msg) {
        this.uuid = uuid;
        this.msg = msg;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`IdMsgPair` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
