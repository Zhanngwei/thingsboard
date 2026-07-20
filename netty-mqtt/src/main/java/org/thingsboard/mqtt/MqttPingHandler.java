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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `MqttPingHandler` 是 ThingsBoard Netty MQTT Client 中处理 MQTT 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ChannelInboundHandlerAdapter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
final class MqttPingHandler extends ChannelInboundHandlerAdapter {

    /**
     * MQTT 保活时间，用于控制时间范围或等待时长。
     */
    private final int keepaliveSeconds;

    /**
     * PING 响应超时时间，用于控制时间范围或等待时长。
     */
    private ScheduledFuture<?> pingRespTimeout;

    /**
     * 功能：创建 `MqttPingHandler` 实例，并初始化必要字段。
     * 参数：
     * - `keepaliveSeconds`：`keepaliveSeconds` 参数。
     * 返回：新创建的对象实例。
     */
    MqttPingHandler(int keepaliveSeconds) {
        this.keepaliveSeconds = keepaliveSeconds;
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
        if (!(msg instanceof MqttMessage)) {
            ctx.fireChannelRead(msg);
            return;
        }
        MqttMessage message = (MqttMessage) msg;
        if (message.fixedHeader().messageType() == MqttMessageType.PINGREQ) {
            this.handlePingReq(ctx.channel());
        } else if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            this.handlePingResp(ctx.channel());
        } else {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    /**
     * 功能：执行 `userEventTriggered` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `evt`：`evt` 参数。
     * 返回：无。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case READER_IDLE:
                    log.debug("[{}] No reads were performed for specified period for channel {}", event.state(), ctx.channel().id());
                    this.sendPingReq(ctx.channel());
                    break;
                case WRITER_IDLE:
                    log.debug("[{}] No writes were performed for specified period for channel {}", event.state(), ctx.channel().id());
                    this.sendPingReq(ctx.channel());
                    break;
            }
        }
    }

    /**
     * 功能：发送或提交`Ping Req`。
     * 参数：
     * - `channel`：网络通道。
     * 返回：无。
     */
    private void sendPingReq(Channel channel) {
        log.trace("[{}] Sending ping request", channel.id());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));

        if (this.pingRespTimeout == null) {
            this.pingRespTimeout = channel.eventLoop().schedule(() -> {
                MqttFixedHeader fixedHeader2 = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
                channel.writeAndFlush(new MqttMessage(fixedHeader2)).addListener(ChannelFutureListener.CLOSE);
                //TODO: what do when the connection is closed ?
            }, this.keepaliveSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * 功能：处理`Ping Req`。
     * 参数：
     * - `channel`：网络通道。
     * 返回：无。
     */
    private void handlePingReq(Channel channel) {
        log.trace("[{}] Handling ping request", channel.id());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));
    }

    /**
     * 功能：处理`Ping Resp`。
     * 参数：
     * - `channel`：网络通道。
     * 返回：无。
     */
    private void handlePingResp(Channel channel) {
        log.trace("[{}] Handling ping response", channel.id());
        if (this.pingRespTimeout != null && !this.pingRespTimeout.isCancelled() && !this.pingRespTimeout.isDone()) {
            this.pingRespTimeout.cancel(true);
            this.pingRespTimeout = null;
        }
    }
}
