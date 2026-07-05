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

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.concurrent.Promise;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. 类目的：`MqttPendingSubscription` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 State Machine / Command / Callback。
 */
final class MqttPendingSubscription {

    /**
     * 异步结果，表示当前对象的对应属性。
     */
    private final Promise<Void> future;
    private final String topic;
    private final Set<MqttPendingHandler> handlers = new HashSet<>();
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final MqttSubscribeMessage subscribeMessage;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final RetransmissionHandler<MqttSubscribeMessage> retransmissionHandler;

    /**
     * 是否满足`sent`条件。
     */
    private boolean sent = false;

    /**
     * 功能：创建 `MqttPendingSubscription` 实例，并初始化必要字段。
     * 参数：
     * - `future`：`future` 参数。
     * - `topic`：主题名称或主题对象。
     * - `message`：待处理消息。
     * - `operation`：`operation` 参数。
     * 返回：新创建的对象实例。
     */
    MqttPendingSubscription(Promise<Void> future, String topic, MqttSubscribeMessage message, PendingOperation operation) {
        this.future = future;
        this.topic = topic;
        this.subscribeMessage = message;

        this.retransmissionHandler = new RetransmissionHandler<>(operation);
        this.retransmissionHandler.setOriginalMessage(message);
    }

    /**
     * 功能：获取异步结果。
     * 参数：无。
     * 返回：处理结果。
     */
    Promise<Void> getFuture() {
        return future;
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    String getTopic() {
        return topic;
    }

    /**
     * 功能：判断`Sent`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isSent() {
        return sent;
    }

    /**
     * 功能：更新`Sent`。
     * 参数：
     * - `sent`：`sent` 参数。
     * 返回：无。
     */
    void setSent(boolean sent) {
        this.sent = sent;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttSubscribeMessage getSubscribeMessage() {
        return subscribeMessage;
    }

    /**
     * 功能：保存或创建处理器。
     * 参数：
     * - `handler`：处理器对象。
     * - `once`：`once` 参数。
     * 返回：无。
     */
    void addHandler(MqttHandler handler, boolean once) {
        this.handlers.add(new MqttPendingHandler(handler, once));
    }

    /**
     * 功能：获取`Handlers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    Set<MqttPendingHandler> getHandlers() {
        return handlers;
    }

    /**
     * 功能：初始化或启动定时器。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * - `sendPacket`：`sendPacket` 参数。
     * 返回：无。
     */
    void startRetransmitTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        if (this.sent) { //If the packet is sent, we can start the retransmit timer
            this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                    sendPacket.accept(new MqttSubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
            this.retransmissionHandler.start(eventLoop);
        }
    }

    /**
     * 功能：处理`on Suback Received`。
     * 参数：无。
     * 返回：无。
     */
    void onSubackReceived() {
        this.retransmissionHandler.stop();
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttPendingHandler` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
     * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
     * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 State Machine / Command / Callback。
     */
    final class MqttPendingHandler {
        /**
         * 处理器，负责处理对应任务或消息。
         */
        private final MqttHandler handler;
        private final boolean once;

        /**
         * 功能：创建 `MqttPendingSubscription` 实例，并初始化必要字段。
         * 参数：
         * - `handler`：处理器对象。
         * - `once`：`once` 参数。
         * 返回：新创建的对象实例。
         */
        MqttPendingHandler(MqttHandler handler, boolean once) {
            this.handler = handler;
            this.once = once;
        }

        /**
         * 功能：获取处理器。
         * 参数：无。
         * 返回：处理结果。
         */
        MqttHandler getHandler() {
            return handler;
        }

        /**
         * 功能：判断`Once`。
         * 参数：无。
         * 返回：判断结果。
         */
        boolean isOnce() {
            return once;
        }
    }

    /**
     * 功能：处理网络通道。
     * 参数：无。
     * 返回：无。
     */
    void onChannelClosed() {
        this.retransmissionHandler.stop();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttPendingSubscription` 在 ThingsBoard Netty MQTT 模块 中承担Netty MQTT 客户端协议类型职责，核心目的是封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
