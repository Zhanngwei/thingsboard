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

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Promise;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `MqttPendingPublish` 是 ThingsBoard Netty MQTT Client 中围绕 MQTT 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
final class MqttPendingPublish {

    /**
     * 消息ID，用于定位对应业务对象。
     */
    private final int messageId;
    private final Promise<Void> future;
    /**
     * 消息载荷，保存消息正文内容。
     */
    private final ByteBuf payload;
    private final MqttPublishMessage message;
    /**
     * QoS 等级，用于区分当前对象的状态或类别。
     */
    private final MqttQoS qos;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final RetransmissionHandler<MqttPublishMessage> publishRetransmissionHandler;
    private final RetransmissionHandler<MqttMessage> pubrelRetransmissionHandler;

    /**
     * 是否满足`sent`条件。
     */
    private boolean sent = false;

    /**
     * 功能：创建 `MqttPendingPublish` 实例，并初始化必要字段。
     * 参数：
     * - `messageId`：消息ID。
     * - `future`：`future` 参数。
     * - `payload`：`payload` 参数。
     * - `message`：待处理消息。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    MqttPendingPublish(int messageId, Promise<Void> future, ByteBuf payload, MqttPublishMessage message, MqttQoS qos, PendingOperation operation) {
        this.messageId = messageId;
        this.future = future;
        this.payload = payload;
        this.message = message;
        this.qos = qos;

        this.publishRetransmissionHandler = new RetransmissionHandler<>(operation);
        this.publishRetransmissionHandler.setOriginalMessage(message);
        this.pubrelRetransmissionHandler = new RetransmissionHandler<>(operation);
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：数值结果。
     */
    int getMessageId() {
        return messageId;
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
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    ByteBuf getPayload() {
        return payload;
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
    MqttPublishMessage getMessage() {
        return message;
    }

    /**
     * 功能：获取QoS 等级。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttQoS getQos() {
        return qos;
    }

    /**
     * 功能：初始化或启动定时器。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * - `sendPacket`：`sendPacket` 参数。
     * 返回：无。
     */
    void startPublishRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.publishRetransmissionHandler.setHandle(((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttPublishMessage(fixedHeader, originalMessage.variableHeader(), this.payload.retain()))));
        this.publishRetransmissionHandler.start(eventLoop);
    }

    /**
     * 功能：处理`on Puback Received`。
     * 参数：无。
     * 返回：无。
     */
    void onPubackReceived() {
        this.publishRetransmissionHandler.stop();
    }

    /**
     * 功能：更新消息。
     * 参数：
     * - `pubrelMessage`：待处理消息。
     * 返回：无。
     */
    void setPubrelMessage(MqttMessage pubrelMessage) {
        this.pubrelRetransmissionHandler.setOriginalMessage(pubrelMessage);
    }

    /**
     * 功能：初始化或启动定时器。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * - `sendPacket`：`sendPacket` 参数。
     * 返回：无。
     */
    void startPubrelRetransmissionTimer(EventLoop eventLoop, Consumer<Object> sendPacket) {
        this.pubrelRetransmissionHandler.setHandle((fixedHeader, originalMessage) ->
                sendPacket.accept(new MqttMessage(fixedHeader, originalMessage.variableHeader())));
        this.pubrelRetransmissionHandler.start(eventLoop);
    }

    /**
     * 功能：处理`on Pubcomp Received`。
     * 参数：无。
     * 返回：无。
     */
    void onPubcompReceived() {
        this.pubrelRetransmissionHandler.stop();
    }

    /**
     * 功能：处理网络通道。
     * 参数：无。
     * 返回：无。
     */
    void onChannelClosed() {
        this.publishRetransmissionHandler.stop();
        this.pubrelRetransmissionHandler.stop();
        if (payload != null) {
            payload.release();
        }
    }
}
