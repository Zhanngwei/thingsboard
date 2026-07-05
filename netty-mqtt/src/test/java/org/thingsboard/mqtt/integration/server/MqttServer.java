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
package org.thingsboard.mqtt.integration.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessageType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 中文说明：
 * 1. 类目的：`MqttServer` 是 ThingsBoard Netty MQTT 测试模块 中的Netty MQTT 集成测试类型，用于通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Test Server。
 */
@Slf4j
public class MqttServer {

    @Getter
    private final List<MqttMessageType> eventsFromClient = new CopyOnWriteArrayList<>();
    /**
     * MQTT 端口，用于描述服务监听或访问地址。
     */
    @Getter
    private final int mqttPort = 8885;

    /**
     * 服务端 Channel，表示当前网络连接使用的通道。
     */
    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    /**
     * 工作线程组，用于支撑当前网络或外部服务交互。
     */
    private EventLoopGroup workerGroup;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void init() throws Exception {
        log.info("Starting MQTT server...");
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new MqttDecoder(65536));
                        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

                        MqttTransportHandler handler = new MqttTransportHandler(eventsFromClient);

                        pipeline.addLast(handler);
                        ch.closeFuture().addListener(handler);
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        serverChannel = b.bind(mqttPort).sync().channel();
        log.info("Mqtt transport started!");
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void shutdown() throws InterruptedException {
        log.info("Stopping MQTT transport!");
        try {
            serverChannel.close().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
        log.info("MQTT transport stopped!");
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttServer` 在 ThingsBoard Netty MQTT 测试模块 中承担Netty MQTT 集成测试类型职责，核心目的是通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
