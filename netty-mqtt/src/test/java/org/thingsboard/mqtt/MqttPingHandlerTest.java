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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.handler.timeout.IdleStateEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`MqttPingHandlerTest` 是 ThingsBoard Netty MQTT 测试模块 中的Netty MQTT 集成测试类型，用于通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Test Server。
 */
class MqttPingHandlerTest {

    /**
     * `KEEP_ALIVE_SECONDS`常量，用于统一引用固定值。
     */
    static final int KEEP_ALIVE_SECONDS = 0;
    static final int PROCESS_SEND_DISCONNECT_MSG_TIME_MS = 500;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    MqttPingHandler mqttPingHandler;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        mqttPingHandler = new MqttPingHandler(KEEP_ALIVE_SECONDS);
    }

    /**
     * 功能：验证 `givenChannelReaderIdleState_whenNoPingResponse_thenDisconnectClient` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenChannelReaderIdleState_whenNoPingResponse_thenDisconnectClient() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        when(ctx.channel()).thenReturn(channel);
        when(channel.eventLoop()).thenReturn(new DefaultEventLoop());
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);

        mqttPingHandler.userEventTriggered(ctx, IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT);
        verify(
                channelFuture,
                after(TimeUnit.SECONDS.toMillis(KEEP_ALIVE_SECONDS) + PROCESS_SEND_DISCONNECT_MSG_TIME_MS)
        ).addListener(eq(ChannelFutureListener.CLOSE));
    }

/*
 * 本类总结：
 * 1. 核心职责：`MqttPingHandlerTest` 在 ThingsBoard Netty MQTT 测试模块 中承担Netty MQTT 集成测试类型职责，核心目的是通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}