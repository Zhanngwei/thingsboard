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
 * 1. 类目的：`TbTenantRuleEngineStats` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbTenantRuleEngineStats` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
