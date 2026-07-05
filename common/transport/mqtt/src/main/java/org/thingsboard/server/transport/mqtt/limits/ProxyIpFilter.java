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
package org.thingsboard.server.transport.mqtt.limits;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportService;

import java.net.InetSocketAddress;

/**
 * 中文说明：
 * 1. 类目的：`ProxyIpFilter` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class ProxyIpFilter extends ChannelInboundHandlerAdapter {


    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private MqttTransportContext context;

    /**
     * 功能：创建 `ProxyIpFilter` 实例，并初始化必要字段。
     * 参数：
     * - `context`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public ProxyIpFilter(MqttTransportContext context) {
        this.context = context;
    }

    /**
     * 功能：执行 `channelRead` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.trace("[{}] Received msg: {}", ctx.channel().id(), msg);
        if (msg instanceof HAProxyMessage) {
            HAProxyMessage proxyMsg = (HAProxyMessage) msg;
            if (proxyMsg.sourceAddress() != null && proxyMsg.sourcePort() > 0) {
                InetSocketAddress address = new InetSocketAddress(proxyMsg.sourceAddress(), proxyMsg.sourcePort());
                if (!context.checkAddress(address)) {
                    closeChannel(ctx);
                } else {
                    log.trace("[{}] Setting address: {}", ctx.channel().id(), address);
                    ctx.channel().attr(MqttTransportService.ADDRESS).set(address);
                    // We no longer need this channel in the pipeline. Similar to HAProxyMessageDecoder
                    ctx.pipeline().remove(this);
                }
            } else {
                log.trace("Received local health-check connection message: {}", proxyMsg);
                closeChannel(ctx);
            }
        }
    }

    /**
     * 功能：停止或关闭网络通道。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void closeChannel(ChannelHandlerContext ctx) {
        while (ctx.pipeline().last() != this) {
            ChannelHandler handler = ctx.pipeline().removeLast();
            if (handler instanceof ChannelInboundHandlerAdapter) {
                try {
                    ((ChannelInboundHandlerAdapter) handler).channelUnregistered(ctx);
                } catch (Exception e) {
                    log.error("Failed to unregister channel: [{}]", ctx, e);
                }
            }

        }
        ctx.pipeline().remove(this);
        ctx.close();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ProxyIpFilter` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
