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
package org.thingsboard.monitoring.data.notification;

import org.thingsboard.monitoring.data.Latency;

import java.util.Collection;

/**
 * 中文说明：
 * 1. `HighLatencyNotification` 是 ThingsBoard Monitoring 中围绕通知提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `Notification`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class HighLatencyNotification implements Notification {

    /**
     * 延迟数据列表，用于保存一组待处理对象。
     */
    private final Collection<Latency> highLatencies;
    private final int thresholdMs;

    /**
     * 功能：创建 `HighLatencyNotification` 实例，并初始化必要字段。
     * 参数：
     * - `highLatencies`：数据列表。
     * - `thresholdMs`：`thresholdMs` 参数。
     * 返回：新创建的对象实例。
     */
    public HighLatencyNotification(Collection<Latency> highLatencies, int thresholdMs) {
        this.highLatencies = highLatencies;
        this.thresholdMs = thresholdMs;
    }

    /**
     * 功能：获取`Text`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getText() {
        StringBuilder text = new StringBuilder();
        text.append("Some of the latencies are higher than ").append(thresholdMs).append(" ms:\n");
        highLatencies.forEach(latency -> {
            text.append(String.format("[%s] *%s*\n", latency.getKey(), latency.getFormattedValue()));
        });
        return text.toString();
    }

}
