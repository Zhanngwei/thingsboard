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
package org.thingsboard.server.service.queue;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `TbTenantRuleEngineStats` 是 ThingsBoard Application 中围绕租户提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
@Data
public class TbTenantRuleEngineStats {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final UUID tenantId;

    private final AtomicInteger totalMsgCounter = new AtomicInteger(0);
    private final AtomicInteger successMsgCounter = new AtomicInteger(0);
    private final AtomicInteger tmpTimeoutMsgCounter = new AtomicInteger(0);
    private final AtomicInteger tmpFailedMsgCounter = new AtomicInteger(0);

    private final AtomicInteger timeoutMsgCounter = new AtomicInteger(0);
    private final AtomicInteger failedMsgCounter = new AtomicInteger(0);

    private final Map<String, AtomicInteger> counters = new HashMap<>();

    /**
     * 功能：创建 `TbTenantRuleEngineStats` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    public TbTenantRuleEngineStats(UUID tenantId) {
        this.tenantId = tenantId;
        counters.put(TbRuleEngineConsumerStats.TOTAL_MSGS, totalMsgCounter);
        counters.put(TbRuleEngineConsumerStats.SUCCESSFUL_MSGS, successMsgCounter);
        counters.put(TbRuleEngineConsumerStats.TIMEOUT_MSGS, timeoutMsgCounter);
        counters.put(TbRuleEngineConsumerStats.FAILED_MSGS, failedMsgCounter);

        counters.put(TbRuleEngineConsumerStats.TMP_TIMEOUT, tmpTimeoutMsgCounter);
        counters.put(TbRuleEngineConsumerStats.TMP_FAILED, tmpFailedMsgCounter);
    }

    /**
     * 功能：执行 `logSuccess` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logSuccess() {
        totalMsgCounter.incrementAndGet();
        successMsgCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `logFailed` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logFailed() {
        totalMsgCounter.incrementAndGet();
        failedMsgCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `logTimeout` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logTimeout() {
        totalMsgCounter.incrementAndGet();
        timeoutMsgCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `logTmpFailed` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logTmpFailed() {
        totalMsgCounter.incrementAndGet();
        tmpFailedMsgCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `logTmpTimeout` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void logTmpTimeout() {
        totalMsgCounter.incrementAndGet();
        tmpTimeoutMsgCounter.incrementAndGet();
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void printStats() {
        int total = totalMsgCounter.get();
        if (total > 0) {
            StringBuilder stats = new StringBuilder();
            counters.forEach((label, value) -> {
                stats.append(label).append(" = [").append(value.get()).append("]");
            });
            log.info("[{}] Stats: {}", tenantId, stats);
        }
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void reset() {
        counters.values().forEach(counter -> counter.set(0));
    }
}
