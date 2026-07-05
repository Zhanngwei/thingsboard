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
package org.thingsboard.mqtt;

/**
 * Created by Valerii Sosliuk on 12/26/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`ChannelClosedException` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 State Machine / Command / Callback。
 */
public class ChannelClosedException extends RuntimeException {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 6266638352424706909L;

    /**
     * 功能：创建 `ChannelClosedException` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ChannelClosedException() {
    }

    /**
     * 功能：创建 `ChannelClosedException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public ChannelClosedException(String message) {
        super(message);
    }

    /**
     * 功能：创建 `ChannelClosedException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
     */
    public ChannelClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 功能：创建 `ChannelClosedException` 实例，并初始化必要字段。
     * 参数：
     * - `cause`：`cause` 参数。
     * 返回：新创建的对象实例。
     */
    public ChannelClosedException(Throwable cause) {
        super(cause);
    }

    /**
     * 功能：创建 `ChannelClosedException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `cause`：`cause` 参数。
     * - `enableSuppression`：`enableSuppression` 参数。
     * - `writableStackTrace`：`writableStackTrace` 参数。
     * 返回：新创建的对象实例。
     */
    public ChannelClosedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ChannelClosedException` 在 ThingsBoard Netty MQTT 模块 中承担Netty MQTT 客户端协议类型职责，核心目的是封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
