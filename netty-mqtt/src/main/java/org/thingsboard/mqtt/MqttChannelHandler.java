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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 中文说明：
 * 1. `MqttChannelHandler` 是 ThingsBoard Netty MQTT Client 中处理 MQTT 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `SimpleChannelInboundHandler`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
final class MqttChannelHandler extends SimpleChannelInboundHandler<MqttMessage> {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final MqttClientImpl client;
    private final Promise<MqttConnectResult> connectFuture;

    /**
     * 功能：创建 `MqttChannelHandler` 实例，并初始化必要字段。
     * 参数：
     * - `client`：客户端对象。
     * - `connectFuture`：`connectFuture` 参数。
     * 返回：新创建的对象实例。
     */
    MqttChannelHandler(MqttClientImpl client, Promise<MqttConnectResult> connectFuture) {
        this.client = client;
        this.connectFuture = connectFuture;
    }

    /**
     * 功能：执行 `channelRead0` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
        if (msg.decoderResult().isSuccess()) {
            switch (msg.fixedHeader().messageType()) {
                case CONNACK:
                    handleConack(ctx.channel(), (MqttConnAckMessage) msg);
                    break;
                case SUBACK:
                    handleSubAck((MqttSubAckMessage) msg);
                    break;
                case PUBLISH:
                    handlePublish(ctx.channel(), (MqttPublishMessage) msg);
                    break;
                case UNSUBACK:
                    handleUnsuback((MqttUnsubAckMessage) msg);
                    break;
                case PUBACK:
                    handlePuback((MqttPubAckMessage) msg);
                    break;
                case PUBREC:
                    handlePubrec(ctx.channel(), msg);
                    break;
                case PUBREL:
                    handlePubrel(ctx.channel(), msg);
                    break;
                case PUBCOMP:
                    handlePubcomp(msg);
                    break;
            }
        } else {
            log.error("[{}] Message decoding failed: {}", client.getClientConfig().getClientId(), msg.decoderResult().cause().getMessage());
            ctx.close();
        }
    }

    /**
     * 功能：执行 `channelActive` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
                this.client.getClientConfig().getProtocolVersion().protocolName(),  // Protocol Name
                this.client.getClientConfig().getProtocolVersion().protocolLevel(), // Protocol Level
                this.client.getClientConfig().getUsername() != null,                // Has Username
                this.client.getClientConfig().getPassword() != null,                // Has Password
                this.client.getClientConfig().getLastWill() != null                 // Will Retain
                        && this.client.getClientConfig().getLastWill().isRetain(),
                this.client.getClientConfig().getLastWill() != null                 // Will QOS
                        ? this.client.getClientConfig().getLastWill().getQos().value()
                        : 0,
                this.client.getClientConfig().getLastWill() != null,                // Has Will
                this.client.getClientConfig().isCleanSession(),                     // Clean Session
                this.client.getClientConfig().getTimeoutSeconds()                   // Timeout
        );
        MqttConnectPayload payload = new MqttConnectPayload(
                this.client.getClientConfig().getClientId(),
                this.client.getClientConfig().getLastWill() != null ? this.client.getClientConfig().getLastWill().getTopic() : null,
                this.client.getClientConfig().getLastWill() != null ? this.client.getClientConfig().getLastWill().getMessage().getBytes(CharsetUtil.UTF_8) : null,
                this.client.getClientConfig().getUsername(),
                this.client.getClientConfig().getPassword() != null ? this.client.getClientConfig().getPassword().getBytes(CharsetUtil.UTF_8) : null
        );
        ctx.channel().writeAndFlush(new MqttConnectMessage(fixedHeader, variableHeader, payload));
    }

    /**
     * 功能：执行 `channelInactive` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    /**
     * 功能：执行 `invokeHandlersForIncomingPublish` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> invokeHandlersForIncomingPublish(MqttPublishMessage message) {
        var future = Futures.immediateVoidFuture();
        var handlerInvoked = new AtomicBoolean();
        try {
            for (MqttSubscription subscription : ImmutableSet.copyOf(this.client.getSubscriptions().values())) {
                if (subscription.matches(message.variableHeader().topicName())) {
                    future = Futures.transform(future, x -> {
                        if (subscription.isOnce() && subscription.isCalled()) {
                            return null;
                        }
                        message.payload().markReaderIndex();
                        subscription.setCalled(true);
                        subscription.getHandler().onMessage(message.variableHeader().topicName(), message.payload());
                        if (subscription.isOnce()) {
                            this.client.off(subscription.getTopic(), subscription.getHandler());
                        }
                        message.payload().resetReaderIndex();
                        handlerInvoked.set(true);
                        return null;
                    }, client.getHandlerExecutor());
                }
            }
            future = Futures.transform(future, x -> {
                if (!handlerInvoked.get() && client.getDefaultHandler() != null) {
                    client.getDefaultHandler().onMessage(message.variableHeader().topicName(), message.payload());
                }
                return null;
            }, client.getHandlerExecutor());
        } finally {
            Futures.addCallback(future, new FutureCallback<>() {
                @Override
                public void onSuccess(Void result) {
                    message.payload().release();
                }

                @Override
                public void onFailure(Throwable t) {
                    message.payload().release();
                }
            }, MoreExecutors.directExecutor());
        }
        return future;
    }

    /**
     * 功能：处理`Conack`。
     * 参数：
     * - `channel`：网络通道。
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handleConack(Channel channel, MqttConnAckMessage message) {
        switch (message.variableHeader().connectReturnCode()) {
            case CONNECTION_ACCEPTED:
                this.connectFuture.setSuccess(new MqttConnectResult(true, MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));

                this.client.getPendingSubscriptions().entrySet().stream().filter((e) -> !e.getValue().isSent()).forEach((e) -> {
                    channel.write(e.getValue().getSubscribeMessage());
                    e.getValue().setSent(true);
                });

                this.client.getPendingPublishes().forEach((id, publish) -> {
                    if (publish.isSent()) return;
                    channel.write(publish.getMessage());
                    publish.setSent(true);
                    if (publish.getQos() == MqttQoS.AT_MOST_ONCE) {
                        publish.getFuture().setSuccess(null); //We don't get an ACK for QOS 0
                        this.client.getPendingPublishes().remove(publish.getMessageId());
                    }
                });
                channel.flush();
                if (this.client.isReconnect()) {
                    this.client.onSuccessfulReconnect();
                }
                break;

            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                this.connectFuture.setSuccess(new MqttConnectResult(false, message.variableHeader().connectReturnCode(), channel.closeFuture()));
                channel.close();
                // Don't start reconnect logic here
                break;
        }
    }

    /**
     * 功能：处理`Sub Ack`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handleSubAck(MqttSubAckMessage message) {
        MqttPendingSubscription pendingSubscription = this.client.getPendingSubscriptions().remove(message.variableHeader().messageId());
        if (pendingSubscription == null) {
            return;
        }
        pendingSubscription.onSubackReceived();
        for (MqttPendingSubscription.MqttPendingHandler handler : pendingSubscription.getHandlers()) {
            MqttSubscription subscription = new MqttSubscription(pendingSubscription.getTopic(), handler.getHandler(), handler.isOnce());
            this.client.getSubscriptions().put(pendingSubscription.getTopic(), subscription);
            this.client.getHandlerToSubscription().put(handler.getHandler(), subscription);
        }
        this.client.getPendingSubscribeTopics().remove(pendingSubscription.getTopic());

        this.client.getServerSubscriptions().add(pendingSubscription.getTopic());

        if (!pendingSubscription.getFuture().isDone()) {
            pendingSubscription.getFuture().setSuccess(null);
        }
    }

    /**
     * 功能：处理`Publish`。
     * 参数：
     * - `channel`：网络通道。
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handlePublish(Channel channel, MqttPublishMessage message) {
        switch (message.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                invokeHandlersForIncomingPublish(message);
                break;

            case AT_LEAST_ONCE:
                var future = invokeHandlersForIncomingPublish(message);
                if (message.variableHeader().packetId() != -1) {
                    future.addListener(() -> {
                        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                        channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, variableHeader));
                    }, MoreExecutors.directExecutor());
                }
                break;

            case EXACTLY_ONCE:
                if (message.variableHeader().packetId() != -1) {
                    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(message.variableHeader().packetId());
                    MqttMessage pubrecMessage = new MqttMessage(fixedHeader, variableHeader);

                    MqttIncomingQos2Publish incomingQos2Publish = new MqttIncomingQos2Publish(message);
                    this.client.getQos2PendingIncomingPublishes().put(message.variableHeader().packetId(), incomingQos2Publish);

                    channel.writeAndFlush(pubrecMessage);
                }
                break;
        }
    }

    /**
     * 功能：处理`Unsuback`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handleUnsuback(MqttUnsubAckMessage message) {
        MqttPendingUnsubscription unsubscription = this.client.getPendingServerUnsubscribes().get(message.variableHeader().messageId());
        if (unsubscription == null) {
            return;
        }
        unsubscription.onUnsubackReceived();
        this.client.getServerSubscriptions().remove(unsubscription.getTopic());
        unsubscription.getFuture().setSuccess(null);
        this.client.getPendingServerUnsubscribes().remove(message.variableHeader().messageId());
    }

    /**
     * 功能：处理`Puback`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handlePuback(MqttPubAckMessage message) {
        MqttPendingPublish pendingPublish = this.client.getPendingPublishes().get(message.variableHeader().messageId());
        if (pendingPublish == null) {
            return;
        }
        pendingPublish.getFuture().setSuccess(null);
        pendingPublish.onPubackReceived();
        this.client.getPendingPublishes().remove(message.variableHeader().messageId());
        pendingPublish.getPayload().release();
    }

    /**
     * 功能：处理`Pubrec`。
     * 参数：
     * - `channel`：网络通道。
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handlePubrec(Channel channel, MqttMessage message) {
        MqttPendingPublish pendingPublish = this.client.getPendingPublishes().get(((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
        pendingPublish.onPubackReceived();

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttMessage pubrelMessage = new MqttMessage(fixedHeader, variableHeader);
        channel.writeAndFlush(pubrelMessage);

        pendingPublish.setPubrelMessage(pubrelMessage);
        pendingPublish.startPubrelRetransmissionTimer(this.client.getEventLoop().next(), this.client::sendAndFlushPacket);
    }

    /**
     * 功能：处理`Pubrel`。
     * 参数：
     * - `channel`：网络通道。
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handlePubrel(Channel channel, MqttMessage message) {
        var future = Futures.immediateVoidFuture();
        if (this.client.getQos2PendingIncomingPublishes().containsKey(((MqttMessageIdVariableHeader) message.variableHeader()).messageId())) {
            MqttIncomingQos2Publish incomingQos2Publish = this.client.getQos2PendingIncomingPublishes().get(((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
            future = invokeHandlersForIncomingPublish(incomingQos2Publish.getIncomingPublish());
            future = Futures.transform(future, x -> {
                this.client.getQos2PendingIncomingPublishes().remove(incomingQos2Publish.getIncomingPublish().variableHeader().packetId());
                return null;
                }, MoreExecutors.directExecutor());
        }
        future.addListener(() -> {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) message.variableHeader()).messageId());
            channel.writeAndFlush(new MqttMessage(fixedHeader, variableHeader));
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理`Pubcomp`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    private void handlePubcomp(MqttMessage message) {
        MqttMessageIdVariableHeader variableHeader = (MqttMessageIdVariableHeader) message.variableHeader();
        MqttPendingPublish pendingPublish = this.client.getPendingPublishes().get(variableHeader.messageId());
        pendingPublish.getFuture().setSuccess(null);
        this.client.getPendingPublishes().remove(variableHeader.messageId());
        pendingPublish.getPayload().release();
        pendingPublish.onPubcompReceived();
    }

    /**
     * 功能：执行 `exceptionCaught` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            if (cause instanceof IOException) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] IOException: ", client.getClientConfig().getOwnerId(), cause);
                } else {
                    log.info("[{}] IOException: {}", client.getClientConfig().getOwnerId(), cause.getMessage());
                }
            } else {
                log.warn("[{}] exceptionCaught", client.getClientConfig().getOwnerId(), cause);
            }
        } finally {
            ReferenceCountUtil.release(cause);
        }
    }
}
