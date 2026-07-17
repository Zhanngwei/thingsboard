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

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. `RetransmissionHandler` 是 ThingsBoard Netty MQTT Client 中处理 `Retransmission` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `MqttMessage`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@RequiredArgsConstructor
final class RetransmissionHandler<T extends MqttMessage> {

    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped;
    private final PendingOperation pendingOperation;
    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    private ScheduledFuture<?> timer;
    private int timeout = 10;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private BiConsumer<MqttFixedHeader, T> handler;
    private T originalMessage;

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * 返回：无。
     */
    void start(EventLoop eventLoop) {
        if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
        }
        if (this.handler == null) {
            throw new NullPointerException("handler");
        }
        this.timeout = 10;
        this.startTimer(eventLoop);
    }

    /**
     * 功能：初始化或启动定时器。
     * 参数：
     * - `eventLoop`：`eventLoop` 参数。
     * 返回：无。
     */
    private void startTimer(EventLoop eventLoop) {
        if (stopped || pendingOperation.isCanceled()) {
            return;
        }
        this.timer = eventLoop.schedule(() -> {
            if (stopped || pendingOperation.isCanceled()) {
                return;
            }
            this.timeout += 5;
            boolean isDup = this.originalMessage.fixedHeader().isDup();
            if (this.originalMessage.fixedHeader().messageType() == MqttMessageType.PUBLISH && this.originalMessage.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE) {
                isDup = true;
            }
            MqttFixedHeader fixedHeader = new MqttFixedHeader(this.originalMessage.fixedHeader().messageType(), isDup, this.originalMessage.fixedHeader().qosLevel(), this.originalMessage.fixedHeader().isRetain(), this.originalMessage.fixedHeader().remainingLength());
            handler.accept(fixedHeader, originalMessage);
            startTimer(eventLoop);
        }, timeout, TimeUnit.SECONDS);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void stop() {
        stopped = true;
        if (this.timer != null) {
            this.timer.cancel(true);
        }
    }

    /**
     * 功能：更新`Handle`。
     * 参数：
     * - `runnable`：`runnable` 参数。
     * 返回：无。
     */
    void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
        this.handler = runnable;
    }

    /**
     * 功能：更新消息。
     * 参数：
     * - `originalMessage`：待处理消息。
     * 返回：无。
     */
    void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
    }
}
