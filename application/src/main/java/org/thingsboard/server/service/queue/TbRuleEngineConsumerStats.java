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
 * 1. 类目的：`TbRuleEngineConsumerStats` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbRuleEngineConsumerStats` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
