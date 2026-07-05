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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. 类目的：`TbMsgPackProcessingContext` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public class TbMsgPackProcessingContext {

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    private final String queueName;
    private final TbRuleEngineSubmitStrategy submitStrategy;
    /**
     * 是否满足超时时间条件。
     */
    private final boolean skipTimeoutMsgsPossible;
    /**
     * 是否启用`profiler`。
     */
    @Getter
    private final boolean profilerEnabled;
    private final AtomicInteger pendingCount;
    private final CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
    /**
     * `pendingMap`映射关系，用于按键查找对应值。
     */
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingMap;
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> successMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> failedMap = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentMap<TenantId, RuleEngineException> exceptionsMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, RuleNodeInfo> lastRuleNodeMap = new ConcurrentHashMap<>();

    /**
     * 是否满足`canceled`条件。
     */
    private volatile boolean canceled = false;

    /**
     * 功能：创建 `TbMsgPackProcessingContext` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `submitStrategy`：`submitStrategy` 参数。
     * - `skipTimeoutMsgsPossible`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public TbMsgPackProcessingContext(String queueName, TbRuleEngineSubmitStrategy submitStrategy, boolean skipTimeoutMsgsPossible) {
        this.queueName = queueName;
        this.submitStrategy = submitStrategy;
        this.skipTimeoutMsgsPossible = skipTimeoutMsgsPossible;
        this.profilerEnabled = log.isDebugEnabled();
        this.pendingMap = submitStrategy.getPendingMap();
        this.pendingCount = new AtomicInteger(pendingMap.size());
    }

    /**
     * 功能：执行 `await` 对应的处理。
     * 参数：
     * - `packProcessingTimeout`：`packProcessingTimeout` 参数。
     * - `milliseconds`：`milliseconds` 参数。
     * 返回：判断结果。
     */
    public boolean await(long packProcessingTimeout, TimeUnit milliseconds) throws InterruptedException {
        boolean success = processingTimeoutLatch.await(packProcessingTimeout, milliseconds);
        if (!success && profilerEnabled) {
            msgProfilerMap.values().forEach(this::onTimeout);
        }
        return success;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    public void onSuccess(UUID id) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            successMap.put(id, msg);
            submitStrategy.onSuccess(id);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    public void onFailure(TenantId tenantId, UUID id, RuleEngineException e) {
        TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg;
        boolean empty = false;
        msg = pendingMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            failedMap.put(id, msg);
            exceptionsMap.putIfAbsent(tenantId, e);
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    private final ConcurrentHashMap<UUID, TbMsgProfilerInfo> msgProfilerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TbRuleNodeProfilerInfo> ruleNodeProfilerMap = new ConcurrentHashMap<>();

    /**
     * 功能：处理`on Processing Start`。
     * 参数：
     * - `id`：`id`ID。
     * - `ruleNodeInfo`：`ruleNodeInfo` 参数。
     * 返回：无。
     */
    public void onProcessingStart(UUID id, RuleNodeInfo ruleNodeInfo) {
        lastRuleNodeMap.put(id, ruleNodeInfo);
        if (profilerEnabled) {
            msgProfilerMap.computeIfAbsent(id, TbMsgProfilerInfo::new).onStart(ruleNodeInfo.getRuleNodeId());
            ruleNodeProfilerMap.putIfAbsent(ruleNodeInfo.getRuleNodeId().getId(), new TbRuleNodeProfilerInfo(ruleNodeInfo));
        }
    }

    /**
     * 功能：处理`on Processing End`。
     * 参数：
     * - `id`：`id`ID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    public void onProcessingEnd(UUID id, RuleNodeId ruleNodeId) {
        if (profilerEnabled) {
            long processingTime = msgProfilerMap.computeIfAbsent(id, TbMsgProfilerInfo::new).onEnd(ruleNodeId);
            if (processingTime > 0) {
                ruleNodeProfilerMap.computeIfAbsent(ruleNodeId.getId(), TbRuleNodeProfilerInfo::new).record(processingTime);
            }
        }
    }

    /**
     * 功能：处理超时时间。
     * 参数：
     * - `profilerInfo`：`profilerInfo` 参数。
     * 返回：无。
     */
    public void onTimeout(TbMsgProfilerInfo profilerInfo) {
        Map.Entry<UUID, Long> ruleNodeInfo = profilerInfo.onTimeout();
        if (ruleNodeInfo != null) {
            ruleNodeProfilerMap.computeIfAbsent(ruleNodeInfo.getKey(), TbRuleNodeProfilerInfo::new).record(ruleNodeInfo.getValue());
        }
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    public RuleNodeInfo getLastVisitedRuleNode(UUID id) {
        return lastRuleNodeMap.get(id);
    }

    /**
     * 功能：执行 `printProfilerStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void printProfilerStats() {
        if (profilerEnabled) {
            log.debug("Top Rule Nodes by max execution time:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingLong(TbRuleNodeProfilerInfo::getMaxExecutionTime).reversed()).limit(5)
                    .forEach(info -> log.debug("[{}][{}] max execution time: {}. {}", queueName, info.getRuleNodeId(), info.getMaxExecutionTime(), info.getLabel()));

            log.info("Top Rule Nodes by avg execution time:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingDouble(TbRuleNodeProfilerInfo::getAvgExecutionTime).reversed()).limit(5)
                    .forEach(info -> log.info("[{}][{}] avg execution time: {}. {}", queueName, info.getRuleNodeId(), info.getAvgExecutionTime(), info.getLabel()));

            log.info("Top Rule Nodes by execution count:");
            ruleNodeProfilerMap.values().stream()
                    .sorted(Comparator.comparingInt(TbRuleNodeProfilerInfo::getExecutionCount).reversed()).limit(5)
                    .forEach(info -> log.info("[{}][{}] execution count: {}. {}", queueName, info.getRuleNodeId(), info.getExecutionCount(), info.getLabel()));
        }
    }

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void cleanup() {
        canceled = true;
        pendingMap.clear();
        successMap.clear();
        failedMap.clear();
    }

    /**
     * 功能：判断`Canceled`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isCanceled() {
        return skipTimeoutMsgsPossible && canceled;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgPackProcessingContext` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
