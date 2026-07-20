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
package org.thingsboard.server.transport.mqtt;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslHandler;
import org.thingsboard.server.transport.mqtt.limits.IpFilter;
import org.thingsboard.server.transport.mqtt.limits.ProxyIpFilter;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `MqttTransportServerInitializer` 是 ThingsBoard Common Transport 中负责 MQTT 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `ChannelInitializer`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class MqttTransportServerInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final MqttTransportContext context;
    private final boolean sslEnabled;

    /**
     * 功能：创建 `MqttTransportServerInitializer` 实例，并初始化必要字段。
     * 参数：
     * - `context`：处理上下文。
     * - `sslEnabled`：`sslEnabled` 参数。
     * 返回：新创建的对象实例。
     */
    public MqttTransportServerInitializer(MqttTransportContext context, boolean sslEnabled) {
        this.context = context;
        this.sslEnabled = sslEnabled;
    }

    /**
     * 功能：初始化或启动网络通道。
     * 参数：
     * - `ch`：`ch` 参数。
     * 返回：无。
     */
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        SslHandler sslHandler = null;
        if (context.isProxyEnabled()) {
            pipeline.addLast("proxy", new HAProxyMessageDecoder());
            pipeline.addLast("ipFilter", new ProxyIpFilter(context));
        } else {
            pipeline.addLast("ipFilter", new IpFilter(context));
        }
        if (sslEnabled && context.getSslHandlerProvider() != null) {
            sslHandler = context.getSslHandlerProvider().getSslHandler();
            pipeline.addLast(sslHandler);
        }
        pipeline.addLast("decoder", new MqttDecoder(context.getMaxPayloadSize()));
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

        MqttTransportHandler handler = new MqttTransportHandler(context, sslHandler);

        pipeline.addLast(handler);
        ch.closeFuture().addListener(handler);
    }

}
