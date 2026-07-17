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
 * 1. `ProxyIpFilter` 是 ThingsBoard Common Transport 中处理 `Proxy Ip Filter` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ChannelInboundHandlerAdapter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
