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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ListeningExecutor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an MqttClientImpl connected to a single MQTT server. Will try to keep the connection going at all times
 */
/**
 * 中文说明：
 * 1. 类目的：`MqttClientImpl` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 State Machine / Command / Callback。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
final class MqttClientImpl implements MqttClient {

    private final Set<String> serverSubscriptions = new HashSet<>();
    private final ConcurrentMap<Integer, MqttPendingUnsubscription> pendingServerUnsubscribes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, MqttIncomingQos2Publish> qos2PendingIncomingPublishes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, MqttPendingPublish> pendingPublishes = new ConcurrentHashMap<>();
    private final HashMultimap<String, MqttSubscription> subscriptions = HashMultimap.create();
    private final ConcurrentMap<Integer, MqttPendingSubscription> pendingSubscriptions = new ConcurrentHashMap<>();
    private final Set<String> pendingSubscribeTopics = new HashSet<>();
    private final HashMultimap<MqttHandler, MqttSubscription> handlerToSubscription = HashMultimap.create();
    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    /**
     * 配置，用于发起外部调用或协议交互。
     */
    private final MqttClientConfig clientConfig;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final MqttHandler defaultHandler;

    /**
     * 事件循环，用于支撑当前网络或外部服务交互。
     */
    private EventLoopGroup eventLoop;

    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    private volatile Channel channel;

    /**
     * 当前连接是否已经断开。
     */
    private volatile boolean disconnected = false;
    private volatile boolean reconnect = false;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    private String host;
    private int port;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    private MqttClientCallback callback;

    /**
     * 处理器列表，用于保存一组待处理对象。
     */
    private final ListeningExecutor handlerExecutor;

    /**
     * Construct the MqttClientImpl with default config
     */
    /**
     * 功能：创建 `MqttClientImpl` 实例，并初始化必要字段。
     * 参数：
     * - `defaultHandler`：处理器对象。
     * - `handlerExecutor`：处理器对象。
     * 返回：新创建的对象实例。
     */
    public MqttClientImpl(MqttHandler defaultHandler, ListeningExecutor handlerExecutor) {
        this(new MqttClientConfig(), defaultHandler, handlerExecutor);
    }

    /**
     * Construct the MqttClientImpl with additional config.
     * This config can also be changed using the {@link #getClientConfig()} function
     *
     * @param clientConfig The config object to use while looking for settings
     */
    /**
     * 功能：创建 `MqttClientImpl` 实例，并初始化必要字段。
     * 参数：
     * - `clientConfig`：客户端对象。
     * - `defaultHandler`：处理器对象。
     * - `handlerExecutor`：处理器对象。
     * 返回：新创建的对象实例。
     */
    public MqttClientImpl(MqttClientConfig clientConfig, MqttHandler defaultHandler, ListeningExecutor handlerExecutor) {
        this.clientConfig = clientConfig;
        this.defaultHandler = defaultHandler;
        this.handlerExecutor = handlerExecutor;
    }

    /**
     * Connect to the specified hostname/ip. By default uses port 1883.
     * If you want to change the port number, see {@link #connect(String, int)}
     *
     * @param host The ip address or host to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `host`：`host` 参数。
     * 返回：处理结果。
     */
    @Override
    public Promise<MqttConnectResult> connect(String host) {
        return connect(host, 1883);
    }

    /**
     * Connect to the specified hostname/ip using the specified port
     *
     * @param host The ip address or host to connect to
     * @param port The tcp port to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * 返回：处理结果。
     */
    @Override
    public Promise<MqttConnectResult> connect(String host, int port) {
        return connect(host, port, false);
    }

    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `reconnect`：`reconnect` 参数。
     * 返回：处理结果。
     */
    private Promise<MqttConnectResult> connect(String host, int port, boolean reconnect) {
        log.trace("[{}] Connecting to server, isReconnect - {}", channel != null ? channel.id() : "UNKNOWN", reconnect);
        if (this.eventLoop == null) {
            this.eventLoop = new NioEventLoopGroup();
        }
        this.host = host;
        this.port = port;
        Promise<MqttConnectResult> connectFuture = new DefaultPromise<>(this.eventLoop.next());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.eventLoop);
        bootstrap.channel(clientConfig.getChannelClass());
        bootstrap.remoteAddress(host, port);
        bootstrap.handler(new MqttChannelInitializer(connectFuture, host, port, clientConfig.getSslContext()));
        ChannelFuture future = bootstrap.connect();

        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                MqttClientImpl.this.channel = f.channel();
                log.debug("[{}][{}] Connected successfully {}!", host, port, this.channel.id());
                MqttClientImpl.this.channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                    if (isConnected()) {
                        return;
                    }
                    log.debug("[{}][{}] Channel is closed {}!", host, port, this.channel.id());
                    ChannelClosedException e = new ChannelClosedException("Channel is closed!");
                    if (callback != null) {
                        callback.connectionLost(e);
                    }
                    pendingSubscriptions.forEach((id, mqttPendingSubscription) -> mqttPendingSubscription.onChannelClosed());
                    pendingSubscriptions.clear();
                    serverSubscriptions.clear();
                    subscriptions.clear();
                    pendingServerUnsubscribes.forEach((id, mqttPendingServerUnsubscribes) -> mqttPendingServerUnsubscribes.onChannelClosed());
                    pendingServerUnsubscribes.clear();
                    qos2PendingIncomingPublishes.clear();
                    pendingPublishes.forEach((id, mqttPendingPublish) -> mqttPendingPublish.onChannelClosed());
                    pendingPublishes.clear();
                    pendingSubscribeTopics.clear();
                    handlerToSubscription.clear();
                    scheduleConnectIfRequired(host, port, true);
                });
            } else {
                log.debug("[{}][{}] Connect failed, trying reconnect!", host, port);
                scheduleConnectIfRequired(host, port, reconnect);
            }
        });
        return connectFuture;
    }

    /**
     * 功能：执行 `scheduleConnectIfRequired` 对应的处理。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `reconnect`：`reconnect` 参数。
     * 返回：无。
     */
    private void scheduleConnectIfRequired(String host, int port, boolean reconnect) {
        log.trace("[{}] Scheduling connect to server, isReconnect - {}", channel != null ? channel.id() : "UNKNOWN", reconnect);
        if (clientConfig.isReconnect() && !disconnected) {
            if (reconnect) {
                this.reconnect = true;
            }
            eventLoop.schedule((Runnable) () -> connect(host, port, reconnect), clientConfig.getReconnectDelay(), TimeUnit.SECONDS);
        }
    }

    /**
     * 功能：判断`Connected`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isConnected() {
        return !disconnected && channel != null && channel.isActive();
    }

    /**
     * 功能：执行 `reconnect` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Promise<MqttConnectResult> reconnect() {
        log.trace("[{}] Reconnecting to server, isReconnect - {}", channel != null ? channel.id() : "UNKNOWN", reconnect);
        if (host == null) {
            throw new IllegalStateException("Cannot reconnect. Call connect() first");
        }
        return connect(host, port);
    }

    /**
     * Retrieve the netty {@link EventLoopGroup} we are using
     *
     * @return The netty {@link EventLoopGroup} we use for the connection
     */
    /**
     * 功能：获取事件循环。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventLoopGroup getEventLoop() {
        return eventLoop;
    }

    /**
     * By default we use the netty {@link NioEventLoopGroup}.
     * If you change the EventLoopGroup to another type, make sure to change the {@link Channel} class using {@link MqttClientConfig#setChannelClass(Class)}
     * If you want to force the MqttClient to use another {@link EventLoopGroup}, call this function before calling {@link #connect(String, int)}
     *
     * @param eventLoop The new eventloop to use
     */
    /**
     * 功能：更新事件循环。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * 返回：无。
     */
    @Override
    public void setEventLoop(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }

    /**
     * 功能：获取处理器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListeningExecutor getHandlerExecutor() {
        return this.handlerExecutor;
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    /**
     * 功能：执行 `on` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> on(String topic, MqttHandler handler) {
        return on(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos     The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    /**
     * 功能：执行 `on` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * - `qos`：`qos` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, false, qos);
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    /**
     * 功能：执行 `once` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> once(String topic, MqttHandler handler) {
        return once(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic   The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos     The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    /**
     * 功能：执行 `once` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * - `qos`：`qos` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscription(topic, handler, true, qos);
    }

    /**
     * Remove the subscription for the given topic and handler
     * If you want to unsubscribe from all handlers known for this topic, use {@link #off(String)}
     *
     * @param topic   The topic to unsubscribe for
     * @param handler The handler to unsubscribe
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    /**
     * 功能：执行 `off` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> off(String topic, MqttHandler handler) {
        log.trace("[{}] Unsubscribing from {}", channel != null ? channel.id() : "UNKNOWN", topic);
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        for (MqttSubscription subscription : this.handlerToSubscription.get(handler)) {
            this.subscriptions.remove(topic, subscription);
        }
        this.handlerToSubscription.removeAll(handler);
        this.checkSubscriptions(topic, future);
        return future;
    }

    /**
     * Remove all subscriptions for the given topic.
     * If you want to specify which handler to unsubscribe, use {@link #off(String, MqttHandler)}
     *
     * @param topic The topic to unsubscribe for
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    /**
     * 功能：执行 `off` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> off(String topic) {
        log.trace("[{}] Unsubscribing from {}", channel != null ? channel.id() : "UNKNOWN", topic);
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        ImmutableSet<MqttSubscription> subscriptions = ImmutableSet.copyOf(this.subscriptions.get(topic));
        for (MqttSubscription subscription : subscriptions) {
            for (MqttSubscription handSub : this.handlerToSubscription.get(subscription.getHandler())) {
                this.subscriptions.remove(topic, handSub);
            }
            this.handlerToSubscription.remove(subscription.getHandler(), subscription);
        }
        this.checkSubscriptions(topic, future);
        return future;
    }

    /**
     * Publish a message to the given payload
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, false);
    }

    /**
     * Publish a message to the given payload, using the given qos
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param qos     The qos to use while publishing
     * @return A future which will be completed when the message is delivered to the server
     */
    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * - `qos`：`qos` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
        return publish(topic, payload, qos, false);
    }

    /**
     * Publish a message to the given payload, using optional retain
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param retain  true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * - `retain`：`retain` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
    }

    /**
     * Publish a message to the given payload, using the given qos and optional retain
     *
     * @param topic   The topic to publish to
     * @param payload The payload to send
     * @param qos     The qos to use while publishing
     * @param retain  true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is delivered to the server
     */
    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `payload`：`payload` 参数。
     * - `qos`：`qos` 参数。
     * - `retain`：`retain` 参数。
     * 返回：异步处理结果。
     */
    @Override
    public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain) {
        log.trace("[{}] Publishing message to {}", channel != null ? channel.id() : "UNKNOWN", topic);
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retain, 0);
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, getNewMessageId().messageId());
        MqttPublishMessage message = new MqttPublishMessage(fixedHeader, variableHeader, payload);
        MqttPendingPublish pendingPublish = new MqttPendingPublish(variableHeader.packetId(), future,
                payload.retain(), message, qos, () -> !pendingPublishes.containsKey(variableHeader.packetId()));
        this.pendingPublishes.put(pendingPublish.getMessageId(), pendingPublish);
        ChannelFuture channelFuture = this.sendAndFlushPacket(message);

        if (channelFuture != null) {
            channelFuture.addListener(result -> {
                pendingPublish.setSent(true);
                if (result.cause() != null) {
                    pendingPublishes.remove(pendingPublish.getMessageId());
                    future.setFailure(result.cause());
                } else {
                    if (pendingPublish.isSent() && pendingPublish.getQos() == MqttQoS.AT_MOST_ONCE) {
                        pendingPublishes.remove(pendingPublish.getMessageId());
                        pendingPublish.getFuture().setSuccess(null); //We don't get an ACK for QOS 0
                    } else if (pendingPublish.isSent()) {
                        pendingPublish.startPublishRetransmissionTimer(eventLoop.next(), MqttClientImpl.this::sendAndFlushPacket);
                    } else {
                        pendingPublishes.remove(pendingPublish.getMessageId());
                    }
                }
            });
        } else {
            pendingPublishes.remove(pendingPublish.getMessageId());
        }
        return future;
    }

    /**
     * Retrieve the MqttClient configuration
     *
     * @return The {@link MqttClientConfig} instance we use
     */
    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public MqttClientConfig getClientConfig() {
        return clientConfig;
    }

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void disconnect() {
        log.trace("[{}] Disconnecting from server", channel != null ? channel.id() : "UNKNOWN");
        disconnected = true;
        if (this.channel != null) {
            MqttMessage message = new MqttMessage(new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0));
            this.sendAndFlushPacket(message).addListener(future1 -> channel.close());
        }
    }

    /**
     * 功能：更新回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void setCallback(MqttClientCallback callback) {
        this.callback = callback;
    }


    ///////////////////////////////////////////// PRIVATE API /////////////////////////////////////////////

    /**
     * 功能：判断`Reconnect`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isReconnect() {
        return reconnect;
    }

    /**
     * 功能：处理`on Successful Reconnect`。
     * 参数：无。
     * 返回：无。
     */
    public void onSuccessfulReconnect() {
        if (callback != null) {
            callback.onSuccessfulReconnect();
        }
    }


    /**
     * 功能：发送或提交`And Flush Packet`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：异步处理结果。
     */
    ChannelFuture sendAndFlushPacket(Object message) {
        if (this.channel == null) {
            return null;
        }
        if (this.channel.isActive()) {
            log.trace("[{}] Sending message {}", channel != null ? channel.id() : "UNKNOWN", message);
            return this.channel.writeAndFlush(message);
        }
        return this.channel.newFailedFuture(new ChannelClosedException("Channel is closed!"));
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    private MqttMessageIdVariableHeader getNewMessageId() {
        int messageId;
        synchronized (this.nextMessageId) {
            this.nextMessageId.compareAndSet(0xffff, 1);
            messageId = this.nextMessageId.getAndIncrement();
        }
        return MqttMessageIdVariableHeader.from(messageId);
    }

    /**
     * 功能：保存或创建订阅。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * - `once`：`once` 参数。
     * - `qos`：`qos` 参数。
     * 返回：异步处理结果。
     */
    private Future<Void> createSubscription(String topic, MqttHandler handler, boolean once, MqttQoS qos) {
        log.trace("[{}] Creating subscription to {}", channel != null ? channel.id() : "UNKNOWN", topic);
        if (this.pendingSubscribeTopics.contains(topic)) {
            Optional<Map.Entry<Integer, MqttPendingSubscription>> subscriptionEntry = this.pendingSubscriptions.entrySet().stream().filter((e) -> e.getValue().getTopic().equals(topic)).findAny();
            if (subscriptionEntry.isPresent()) {
                subscriptionEntry.get().getValue().addHandler(handler, once);
                return subscriptionEntry.get().getValue().getFuture();
            }
        }
        if (this.serverSubscriptions.contains(topic)) {
            MqttSubscription subscription = new MqttSubscription(topic, handler, once);
            this.subscriptions.put(topic, subscription);
            this.handlerToSubscription.put(handler, subscription);
            return this.channel.newSucceededFuture();
        }

        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
        MqttMessageIdVariableHeader variableHeader = getNewMessageId();
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

        final MqttPendingSubscription pendingSubscription = new MqttPendingSubscription(future, topic, message,
                () -> !pendingSubscriptions.containsKey(variableHeader.messageId()));
        pendingSubscription.addHandler(handler, once);
        this.pendingSubscriptions.put(variableHeader.messageId(), pendingSubscription);
        this.pendingSubscribeTopics.add(topic);
        pendingSubscription.setSent(this.sendAndFlushPacket(message) != null); //If not sent, we will send it when the connection is opened

        pendingSubscription.startRetransmitTimer(this.eventLoop.next(), this::sendAndFlushPacket);

        return future;
    }

    /**
     * 功能：校验`Subscriptions`。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `promise`：`promise` 参数。
     * 返回：无。
     */
    private void checkSubscriptions(String topic, Promise<Void> promise) {
        if (!(this.subscriptions.containsKey(topic) && this.subscriptions.get(topic).size() != 0) && this.serverSubscriptions.contains(topic)) {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = getNewMessageId();
            MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topic));
            MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

            MqttPendingUnsubscription pendingUnsubscription = new MqttPendingUnsubscription(promise, topic, message,
                    () -> !pendingServerUnsubscribes.containsKey(variableHeader.messageId()));
            this.pendingServerUnsubscribes.put(variableHeader.messageId(), pendingUnsubscription);
            pendingUnsubscription.startRetransmissionTimer(this.eventLoop.next(), this::sendAndFlushPacket);

            this.sendAndFlushPacket(message);
        } else {
            promise.setSuccess(null);
        }
    }

    /**
     * 功能：获取`Pending Subscriptions`。
     * 参数：无。
     * 返回：处理结果。
     */
    ConcurrentMap<Integer, MqttPendingSubscription> getPendingSubscriptions() {
        return pendingSubscriptions;
    }

    /**
     * 功能：获取`Subscriptions`。
     * 参数：无。
     * 返回：处理结果。
     */
    HashMultimap<String, MqttSubscription> getSubscriptions() {
        return subscriptions;
    }

    /**
     * 功能：获取`Pending Subscribe Topics`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    Set<String> getPendingSubscribeTopics() {
        return pendingSubscribeTopics;
    }

    /**
     * 功能：获取订阅。
     * 参数：无。
     * 返回：处理结果。
     */
    HashMultimap<MqttHandler, MqttSubscription> getHandlerToSubscription() {
        return handlerToSubscription;
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    Set<String> getServerSubscriptions() {
        return serverSubscriptions;
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：处理结果。
     */
    ConcurrentMap<Integer, MqttPendingUnsubscription> getPendingServerUnsubscribes() {
        return pendingServerUnsubscribes;
    }

    /**
     * 功能：获取`Pending Publishes`。
     * 参数：无。
     * 返回：处理结果。
     */
    ConcurrentMap<Integer, MqttPendingPublish> getPendingPublishes() {
        return pendingPublishes;
    }

    /**
     * 功能：获取`Qos2 Pending Incoming Publishes`。
     * 参数：无。
     * 返回：处理结果。
     */
    ConcurrentMap<Integer, MqttIncomingQos2Publish> getQos2PendingIncomingPublishes() {
        return qos2PendingIncomingPublishes;
    }

    /**
     * 中文说明：
     * 1. 类目的：`MqttChannelInitializer` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
     * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
     * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 State Machine / Command / Callback。
     */
    private class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

        /**
         * 异步结果，表示当前对象的对应属性。
         */
        private final Promise<MqttConnectResult> connectFuture;
        private final String host;
        /**
         * 端口号，用于描述服务监听或访问地址。
         */
        private final int port;
        private final SslContext sslContext;


        /**
         * 功能：创建 `MqttClientImpl` 实例，并初始化必要字段。
         * 参数：
         * - `connectFuture`：`connectFuture` 参数。
         * - `host`：`host` 参数。
         * - `port`：`port` 参数。
         * - `sslContext`：处理上下文。
         * 返回：新创建的对象实例。
         */
        public MqttChannelInitializer(Promise<MqttConnectResult> connectFuture, String host, int port, SslContext sslContext) {
            this.connectFuture = connectFuture;
            this.host = host;
            this.port = port;
            this.sslContext = sslContext;
        }

        /**
         * 功能：初始化或启动网络通道。
         * 参数：
         * - `ch`：`ch` 参数。
         * 返回：无。
         */
        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            if (sslContext != null) {
                ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), host, port));
            }

            ch.pipeline().addLast("mqttDecoder", new MqttDecoder(clientConfig.getMaxBytesInMessage()));
            ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(MqttClientImpl.this.clientConfig.getTimeoutSeconds(), MqttClientImpl.this.clientConfig.getTimeoutSeconds(), 0));
            ch.pipeline().addLast("mqttPingHandler", new MqttPingHandler(MqttClientImpl.this.clientConfig.getTimeoutSeconds()));
            ch.pipeline().addLast("mqttHandler", new MqttChannelHandler(MqttClientImpl.this, connectFuture));
        }
    }

    /**
     * 功能：获取处理器。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttHandler getDefaultHandler() {
        return defaultHandler;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MqttClientImpl` 在 ThingsBoard Netty MQTT 模块 中承担Netty MQTT 客户端协议类型职责，核心目的是封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
