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
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `MqttPendingUnsubscription` 是 ThingsBoard Netty MQTT Client 中围绕 MQTT 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
final class MqttPendingUnsubscription{

    /**
     * 异步结果，表示当前对象的对应属性。
     */
    private final Promise<Void> future;
    private final String topic;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final RetransmissionHandler<MqttUnsubscribeMessage> retransmissionHandler;

    /**
     * 功能：创建 `MqttPendingUnsubscription` 实例，并初始化必要字段。
     * 参数：
     * - `future`：`future` 参数。
     * - `topic`：主题名称或主题对象。
     * - `unsubscribeMessage`：待处理消息。
     * - `operation`：`operation` 参数。
     * 返回：新创建的对象实例。
     */
    MqttPendingUnsubscription(Promise<Void> future, String topic, MqttUnsubscribeMessage unsubscribeMessage, PendingOperation operation) {
        this.future = future;
        this.topic = topic;

        this.retransmissionHandler = new RetransmissionHandler<>(operation);
        this.retransmissionHandler.setOriginalMessage(unsubscribeMessage);
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
     * 功能：初始化或启动定时器。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * - `sendPacket`：`sendPacket` 参数。
     * 返回：无。
     */
    void startRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.retransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttUnsubscribeMessage(fixedHeader, originalMessage.variableHeader(), originalMessage.payload())));
        this.retransmissionHandler.start(eventLoop);
    }

    /**
     * 功能：处理`on Unsuback Received`。
     * 参数：无。
     * 返回：无。
     */
    void onUnsubackReceived(){
        this.retransmissionHandler.stop();
    }

    /**
     * 功能：处理网络通道。
     * 参数：无。
     * 返回：无。
     */
    void onChannelClosed(){
        this.retransmissionHandler.stop();
    }
}
