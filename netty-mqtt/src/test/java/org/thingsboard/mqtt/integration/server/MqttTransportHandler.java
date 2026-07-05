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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

import static io.netty.handler.codec.mqtt.MqttMessageType.CONNACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.DISCONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGREQ;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBACK;
import static io.netty.handler.codec.mqtt.MqttMessageType.PUBLISH;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

/**
 * 中文说明：
 * 1. 类目的：`MqttTransportHandler` 是 ThingsBoard Netty MQTT 测试模块 中的Netty MQTT 集成测试类型，用于通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Integration Test / Test Server。
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>> {

    /**
     * 客户端列表，用于保存一组待处理对象。
     */
    private final List<MqttMessageType> eventsFromClient;
    private final UUID sessionId;

    /**
     * 功能：创建 `MqttTransportHandler` 实例，并初始化必要字段。
     * 参数：
     * - `eventsFromClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    MqttTransportHandler(List<MqttMessageType> eventsFromClient) {
        this.sessionId = UUID.randomUUID();
        this.eventsFromClient = eventsFromClient;
    }

    /**
     * 功能：执行 `channelRead` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        try {
            if (msg instanceof MqttMessage) {
                MqttMessage message = (MqttMessage) msg;
                if (message.decoderResult().isSuccess()) {
                    processMqttMsg(ctx, message);
                } else {
                    log.error("[{}] Message decoding failed: {}", sessionId, message.decoderResult().cause().getMessage());
                    ctx.close();
                }
            } else {
                log.debug("[{}] Received non mqtt message: {}", sessionId, msg.getClass().getSimpleName());
                ctx.close();
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.fixedHeader() == null) {
            ctx.close();
            return;
        }
        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                eventsFromClient.add(CONNECT);
                processConnect(ctx, (MqttConnectMessage) msg);
                break;
            case DISCONNECT:
                eventsFromClient.add(DISCONNECT);
                ctx.close();
                break;
            case PUBLISH:
                // QoS 0 and 1 supported only here
                eventsFromClient.add(PUBLISH);
                MqttPublishMessage mqttPubMsg = (MqttPublishMessage) msg;
                ack(ctx, mqttPubMsg.variableHeader().packetId());
                break;
            case PINGREQ:
                // We will not handle PINGREQ and will not send any PINGRESP to simulate the MQTT server is down
                eventsFromClient.add(PINGREQ);
                break;
            default:
                break;
        }
    }

    /**
     * 功能：处理`Connect`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        String userName = msg.payload().userName();
        String clientId = msg.payload().clientIdentifier();

        log.warn("[{}][{}] Processing connect msg for client: {}!", sessionId, userName, clientId);
        ctx.writeAndFlush(createMqttConnAckMsg(msg));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private MqttConnAckMessage createMqttConnAckMsg(MqttConnectMessage msg) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(CONNACK, false, AT_MOST_ONCE, false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader =
                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, !msg.variableHeader().isCleanSession());
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    private void ack(ChannelHandlerContext ctx, int msgId) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(msgId));
        }
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `requestId`：请求ID。
     * 返回：处理结果。
     */
    public static MqttPubAckMessage createMqttPubAckMsg(int requestId) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMsgIdVariableHeader =
                MqttMessageIdVariableHeader.from(requestId);
        return new MqttPubAckMessage(mqttFixedHeader, mqttMsgIdVariableHeader);
    }

    /**
     * 功能：执行 `operationComplete` 对应的处理。
     * 参数：
     * - `future`：`future` 参数。
     * 返回：无。
     */
    @Override
    public void operationComplete(Future<? super Void> future) {
        log.trace("[{}] Channel closed!", sessionId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttTransportHandler` 在 ThingsBoard Netty MQTT 测试模块 中承担Netty MQTT 集成测试类型职责，核心目的是通过内置 MQTT server 验证客户端连接、心跳、发布订阅和异常关闭行为。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
