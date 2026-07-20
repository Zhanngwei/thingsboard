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
 * 1. `MqttPendingSubscription` 是 ThingsBoard Netty MQTT Client 中围绕 MQTT 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
     * 1. `MqttPendingHandler` 是 ThingsBoard Netty MQTT Client 中处理 MQTT 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 它直接协作于事件源、上下文对象和后续处理组件。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
