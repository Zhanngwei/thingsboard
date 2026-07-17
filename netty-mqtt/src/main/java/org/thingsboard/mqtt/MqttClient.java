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
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.thingsboard.common.util.ListeningExecutor;

/**
 * 中文说明：
 * 1. `MqttClient` 是 ThingsBoard Netty MQTT Client 中定义 MQTT 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface MqttClient {

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
    Promise<MqttConnectResult> connect(String host);

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
    Promise<MqttConnectResult> connect(String host, int port);

    /**
     *
     * @return boolean value indicating if channel is active
     */
    /**
     * 功能：判断`Connected`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isConnected();

    /**
     * Attempt reconnect to the host that was attempted with {@link #connect(String, int)} method before
     *
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     * @throws IllegalStateException if no previous {@link #connect(String, int)} calls were attempted
     */
    /**
     * 功能：执行 `reconnect` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    Promise<MqttConnectResult> reconnect();

    /**
     * Retrieve the netty {@link EventLoopGroup} we are using
     * @return The netty {@link EventLoopGroup} we use for the connection
     */
    /**
     * 功能：获取事件循环。
     * 参数：无。
     * 返回：处理结果。
     */
    EventLoopGroup getEventLoop();

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
    void setEventLoop(EventLoopGroup eventLoop);

    /**
     * 功能：获取处理器。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    ListeningExecutor getHandlerExecutor();

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic The topic filter to subscribe to
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
    Future<Void> on(String topic, MqttHandler handler);

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
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
    Future<Void> on(String topic, MqttHandler handler, MqttQoS qos);

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic The topic filter to subscribe to
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
    Future<Void> once(String topic, MqttHandler handler);

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscription is only once. If the MqttClient has received 1 message, the subscription will be removed
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
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
    Future<Void> once(String topic, MqttHandler handler, MqttQoS qos);

    /**
     * Remove the subscription for the given topic and handler
     * If you want to unsubscribe from all handlers known for this topic, use {@link #off(String)}
     *
     * @param topic The topic to unsubscribe for
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
    Future<Void> off(String topic, MqttHandler handler);

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
    Future<Void> off(String topic);

    /**
     * Publish a message to the given payload
     * @param topic The topic to publish to
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
    Future<Void> publish(String topic, ByteBuf payload);

    /**
     * Publish a message to the given payload, using the given qos
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
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
    Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos);

    /**
     * Publish a message to the given payload, using optional retain
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param retain true if you want to retain the message on the server, false otherwise
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
    Future<Void> publish(String topic, ByteBuf payload, boolean retain);

    /**
     * Publish a message to the given payload, using the given qos and optional retain
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
     * @param retain true if you want to retain the message on the server, false otherwise
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
    Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain);

    /**
     * Retrieve the MqttClient configuration
     * @return The {@link MqttClientConfig} instance we use
     */
    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    MqttClientConfig getClientConfig();


    /**
     * Construct the MqttClientImpl with additional config.
     * This config can also be changed using the {@link #getClientConfig()} function
     *
     * @param config The config object to use while looking for settings
     * @param defaultHandler The handler for incoming messages that do not match any topic subscriptions
     */
    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：
     * - `config`：配置对象。
     * - `defaultHandler`：处理器对象。
     * - `handlerExecutor`：处理器对象。
     * 返回：处理结果。
     */
    static MqttClient create(MqttClientConfig config, MqttHandler defaultHandler, ListeningExecutor handlerExecutor){
        return new MqttClientImpl(config, defaultHandler, handlerExecutor);
    }

    /**
     * Send disconnect and close channel
     *
     */
    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void disconnect();

    /**
     * Sets the {@see #MqttClientCallback} object for this MqttClient
     * @param callback The callback to be set
     */
    /**
     * 功能：更新回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void setCallback(MqttClientCallback callback);

}
