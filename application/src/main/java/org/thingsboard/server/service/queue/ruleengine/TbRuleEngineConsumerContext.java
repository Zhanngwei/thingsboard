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
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 中文说明：
 * 1. 类目的：`TbRuleEngineConsumerContext` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Component
@TbRuleEngineComponent
@Slf4j
@Data
public class TbRuleEngineConsumerContext {

    /**
     * 轮询间隔，用于控制时间范围或等待时长。
     */
    @Value("${queue.rule-engine.poll-interval}")
    private long pollDuration;
    /**
     * 版本包处理超时时间，用于控制时间范围或等待时长。
     */
    @Value("${queue.rule-engine.pack-processing-timeout}")
    private long packProcessingTimeout;
    /**
     * 是否启用`stats`。
     */
    @Value("${queue.rule-engine.stats.enabled:true}")
    private boolean statsEnabled;
    /**
     * 是否启用`prometheus stats`。
     */
    @Value("${queue.rule-engine.prometheus-stats.enabled:false}")
    private boolean prometheusStatsEnabled;
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Value("${queue.rule-engine.topic-deletion-delay:15}")
    private int topicDeletionDelayInSec;
    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${queue.rule-engine.management-thread-pool-size:12}")
    private int mgmtThreadPoolSize;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final ActorSystemContext actorContext;
    private final StatsFactory statsFactory;
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final TbRuleEngineSubmitStrategyFactory submitStrategyFactory;
    private final TbRuleEngineProcessingStrategyFactory processingStrategyFactory;
    /**
     * 队列，用于按场景创建或提供目标对象。
     */
    private final TbRuleEngineQueueFactory queueFactory;
    private final RuleEngineStatisticsService statisticsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private final TbQueueProducerProvider producerProvider;
    private final TbQueueAdmin queueAdmin;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ExecutorService consumersExecutor;
    private ExecutorService mgmtExecutor;
    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    private ScheduledExecutorService scheduler;

    /**
     * 当前对象是否已准备就绪。
     */
    private volatile boolean isReady = false;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    void init() {
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer"));
        this.mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(mgmtThreadPoolSize, "tb-rule-engine-mgmt");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-rule-engine-consumer-scheduler"));
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        scheduler.shutdownNow();
        consumersExecutor.shutdownNow();
        mgmtExecutor.shutdownNow();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbRuleEngineConsumerContext` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
