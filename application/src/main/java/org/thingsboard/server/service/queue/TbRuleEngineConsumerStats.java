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

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 中文说明：
 * 1. `TbRuleEngineConsumerStats` 是 ThingsBoard Application 中围绕统计数据提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbRuleEngineConsumerStats {

    /**
     * `TOTAL_MSGS`常量，用于统一引用固定值。
     */
    public static final String TOTAL_MSGS = "totalMsgs";
    public static final String SUCCESSFUL_MSGS = "successfulMsgs";
    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final String TMP_TIMEOUT = "tmpTimeout";
    public static final String TMP_FAILED = "tmpFailed";
    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final String TIMEOUT_MSGS = "timeoutMsgs";
    public static final String FAILED_MSGS = "failedMsgs";
    /**
     * `SUCCESSFUL_ITERATIONS`常量，用于统一引用固定值。
     */
    public static final String SUCCESSFUL_ITERATIONS = "successfulIterations";
    public static final String FAILED_ITERATIONS = "failedIterations";
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String TENANT_ID_TAG = "tenantId";

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final StatsFactory statsFactory;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final StatsCounter totalMsgCounter;
    private final StatsCounter successMsgCounter;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final StatsCounter tmpTimeoutMsgCounter;
    private final StatsCounter tmpFailedMsgCounter;

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final StatsCounter timeoutMsgCounter;
    private final StatsCounter failedMsgCounter;

    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter successIterationsCounter;
    private final StatsCounter failedIterationsCounter;

    private final List<StatsCounter> counters = new ArrayList<>();
    private final ConcurrentMap<UUID, TbTenantRuleEngineStats> tenantStats = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, Timer> tenantMsgProcessTimers = new ConcurrentHashMap<>();
    private final ConcurrentMap<TenantId, RuleEngineException> tenantExceptions = new ConcurrentHashMap<>();

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private final String queueName;
    private final TenantId tenantId;

    /**
     * 功能：创建 `TbRuleEngineConsumerStats` 实例，并初始化必要字段。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * - `statsFactory`：`statsFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRuleEngineConsumerStats(QueueKey queueKey, StatsFactory statsFactory) {
        this.queueName = queueKey.getQueueName();
        this.tenantId = queueKey.getTenantId();
        this.statsFactory = statsFactory;

        String statsKey = StatsType.RULE_ENGINE.getName() + "." + queueName;
        String tenant = tenantId == null || tenantId.isSysTenantId() ? "system" : tenantId.toString();
        this.totalMsgCounter = statsFactory.createStatsCounter(statsKey, TOTAL_MSGS, TENANT_ID_TAG, tenant);
        this.successMsgCounter = statsFactory.createStatsCounter(statsKey, SUCCESSFUL_MSGS, TENANT_ID_TAG, tenant);
        this.timeoutMsgCounter = statsFactory.createStatsCounter(statsKey, TIMEOUT_MSGS, TENANT_ID_TAG, tenant);
        this.failedMsgCounter = statsFactory.createStatsCounter(statsKey, FAILED_MSGS, TENANT_ID_TAG, tenant);
        this.tmpTimeoutMsgCounter = statsFactory.createStatsCounter(statsKey, TMP_TIMEOUT, TENANT_ID_TAG, tenant);
        this.tmpFailedMsgCounter = statsFactory.createStatsCounter(statsKey, TMP_FAILED, TENANT_ID_TAG, tenant);
        this.successIterationsCounter = statsFactory.createStatsCounter(statsKey, SUCCESSFUL_ITERATIONS, TENANT_ID_TAG, tenant);
        this.failedIterationsCounter = statsFactory.createStatsCounter(statsKey, FAILED_ITERATIONS, TENANT_ID_TAG, tenant);

        counters.add(totalMsgCounter);
        counters.add(successMsgCounter);
        counters.add(timeoutMsgCounter);
        counters.add(failedMsgCounter);

        counters.add(tmpTimeoutMsgCounter);
        counters.add(tmpFailedMsgCounter);
        counters.add(successIterationsCounter);
        counters.add(failedIterationsCounter);
    }

    /**
     * 功能：获取定时器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * 返回：处理结果。
     */
    public Timer getTimer(TenantId tenantId, String status) {
        return tenantMsgProcessTimers.computeIfAbsent(tenantId,
                id -> statsFactory.createTimer(StatsType.RULE_ENGINE.getName() + "." + queueName,
                        "tenantId", tenantId.getId().toString(),
                        "status", status
                ));
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `finalIterationForPack`：`finalIterationForPack` 参数。
     * 返回：无。
     */
    public void log(TbRuleEngineProcessingResult msg, boolean finalIterationForPack) {
        int success = msg.getSuccessMap().size();
        int pending = msg.getPendingMap().size();
        int failed = msg.getFailedMap().size();
        totalMsgCounter.add(success + pending + failed);
        successMsgCounter.add(success);
        msg.getSuccessMap().values().forEach(m -> getTenantStats(m).logSuccess());
        if (finalIterationForPack) {
            if (pending > 0 || failed > 0) {
                timeoutMsgCounter.add(pending);
                failedMsgCounter.add(failed);
                if (pending > 0) {
                    msg.getPendingMap().values().forEach(m -> getTenantStats(m).logTimeout());
                }
                if (failed > 0) {
                    msg.getFailedMap().values().forEach(m -> getTenantStats(m).logFailed());
                }
                failedIterationsCounter.increment();
            } else {
                successIterationsCounter.increment();
            }
        } else {
            failedIterationsCounter.increment();
            tmpTimeoutMsgCounter.add(pending);
            tmpFailedMsgCounter.add(failed);
            if (pending > 0) {
                msg.getPendingMap().values().forEach(m -> getTenantStats(m).logTmpTimeout());
            }
            if (failed > 0) {
                msg.getFailedMap().values().forEach(m -> getTenantStats(m).logTmpFailed());
            }
        }
        msg.getExceptionsMap().forEach(tenantExceptions::putIfAbsent);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `m`：`m` 参数。
     * 返回：处理结果。
     */
    private TbTenantRuleEngineStats getTenantStats(TbProtoQueueMsg<ToRuleEngineMsg> m) {
        ToRuleEngineMsg reMsg = m.getValue();
        return tenantStats.computeIfAbsent(new UUID(reMsg.getTenantIdMSB(), reMsg.getTenantIdLSB()), TbTenantRuleEngineStats::new);
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, TbTenantRuleEngineStats> getTenantStats() {
        return tenantStats;
    }

    /**
     * 功能：获取队列名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getQueueName() {
        return queueName;
    }

    /**
     * 功能：获取租户。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<TenantId, RuleEngineException> getTenantExceptions() {
        return tenantExceptions;
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
            counters.forEach(counter -> {
                stats.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            if (tenantId.isSysTenantId()) {
                log.info("[{}] Stats: {}", queueName, stats);
            } else {
                log.info("[{}][{}] Stats: {}", queueName, tenantId, stats);
            }
        }
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void reset() {
        counters.forEach(StatsCounter::clear);
        tenantStats.clear();
        tenantExceptions.clear();
    }
}
