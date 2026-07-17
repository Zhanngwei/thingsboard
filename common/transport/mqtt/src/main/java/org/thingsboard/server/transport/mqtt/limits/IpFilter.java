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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ipfilter.AbstractRemoteAddressFilter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportService;

import java.net.InetSocketAddress;

/**
 * 中文说明：
 * 1. `IpFilter` 是 ThingsBoard Common Transport 中处理 `Ip Filter` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractRemoteAddressFilter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class IpFilter extends AbstractRemoteAddressFilter<InetSocketAddress> {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private MqttTransportContext context;

    /**
     * 功能：创建 `IpFilter` 实例，并初始化必要字段。
     * 参数：
     * - `context`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public IpFilter(MqttTransportContext context) {
        this.context = context;
    }

    /**
     * 功能：执行 `accept` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `remoteAddress`：`remoteAddress` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean accept(ChannelHandlerContext ctx, InetSocketAddress remoteAddress) throws Exception {
        log.trace("[{}] Received msg: {}", ctx.channel().id(), remoteAddress);
        if(context.checkAddress(remoteAddress)){
            log.trace("[{}] Setting address: {}", ctx.channel().id(), remoteAddress);
            ctx.channel().attr(MqttTransportService.ADDRESS).set(remoteAddress);
            return true;
        } else {
            return false;
        }
    }
}
