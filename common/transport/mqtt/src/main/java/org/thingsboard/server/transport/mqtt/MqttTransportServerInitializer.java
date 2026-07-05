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
 * 1. 类目的：`MqttTransportServerInitializer` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`MqttTransportServerInitializer` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
