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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Data;

import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`AggHistoryCmd` 是ThingsBoard Application 模块中的WebSocket 服务类型，用于维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 生命周期：随 WebSocket 建连创建订阅，断连或取消订阅时释放。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Observer / Session。
 */
@Data
public class AggHistoryCmd {

    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    private List<AggKey> keys;
    private long startTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long endTs;

}

/*
 * 本类总结：
 * 1. 核心职责：`AggHistoryCmd` 在 ThingsBoard Application 模块 中承担WebSocket 服务类型职责，核心目的是维护仪表盘、遥测、属性或告警订阅的 WebSocket 会话。
 * 2. 核心流程：接收订阅请求后注册监听，数据变化时推送到客户端。
 * 3. 关键依赖：主要依赖或协作对象包括WebSocketSession、SubscriptionService、TelemetryService、缓存和安全上下文。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
