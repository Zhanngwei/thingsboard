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

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategyFactory;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategyFactory;
import org.thingsboard.server.service.stats.RuleEngineStatisticsService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
/**
 * 中文说明：
 * 1. 类目的：`TbRuleEngineQueueConsumerManagerTest` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
public class TbRuleEngineQueueConsumerManagerTest {

    @Mock
    /**
     * 字段说明：
     * 1. 保存 `actorContext` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ActorSystemContext actorContext;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `statsFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private StatsFactory statsFactory;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `queueFactory` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbRuleEngineQueueFactory queueFactory;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `statisticsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RuleEngineStatisticsService statisticsService;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `serviceInfoProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbServiceInfoProvider serviceInfoProvider;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private PartitionService partitionService;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `producerProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;
    @Mock
    /**
     * 字段说明：
     * 1. 保存 `queueAdmin` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbQueueAdmin queueAdmin;
    private TbRuleEngineConsumerContext ruleEngineConsumerContext;

    /**
     * 字段说明：
     * 1. 保存 `consumerManager` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbRuleEngineQueueConsumerManager consumerManager;
    private Queue queue;

    /**
     * 字段说明：
     * 1. 保存 `consumers` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Set<TestConsumer> consumers;
    private boolean generateQueueMsgs;
    /**
     * 字段说明：
     * 1. 保存 `totalConsumedMsgs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AtomicInteger totalConsumedMsgs;
    private AtomicInteger totalProcessedMsgs;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeEach` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void beforeEach() {
        ruleEngineConsumerContext = new TbRuleEngineConsumerContext(
                actorContext, statsFactory, spy(new TbRuleEngineSubmitStrategyFactory()),
                spy(new TbRuleEngineProcessingStrategyFactory()), queueFactory, statisticsService,
                serviceInfoProvider, partitionService, producerProvider, queueAdmin
        );
        consumers = ConcurrentHashMap.newKeySet();
        generateQueueMsgs = true;
        totalConsumedMsgs = new AtomicInteger();
        totalProcessedMsgs = new AtomicInteger();
        doAnswer(inv -> {
            QueueToRuleEngineMsg msg = inv.getArgument(0);
            msg.getMsg().getCallback().onSuccess();
            totalProcessedMsgs.incrementAndGet();
            log.trace("totalProcessedMsgs = {}", totalProcessedMsgs);
            return null;
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        }).when(actorContext).tell(any());
        ruleEngineMsgProducer = mock(TbQueueProducer.class);
        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(ruleEngineMsgProducer);
        ruleEngineConsumerContext.setMgmtThreadPoolSize(2);
        ruleEngineConsumerContext.setTopicDeletionDelayInSec(5);
        ruleEngineConsumerContext.init();
        ruleEngineConsumerContext.setReady(false);

        queue = new Queue();
        queue.setName("Test");
        queue.setTenantId(TenantId.SYS_TENANT_ID);
        queue.setId(new QueueId(UUID.randomUUID()));
        queue.setTopic("tb_test");
        queue.setPartitions(10);
        queue.setConsumerPerPartition(true);
        queue.setPollInterval(250);
        queue.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(200);
        queue.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES_AND_TIMED_OUT);
        processingStrategy.setRetries(0);
        queue.setProcessingStrategy(processingStrategy);

        doAnswer(i -> {
            TestConsumer consumer = spy(new TestConsumer(queue.getTopic()));
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (generateQueueMsgs) {
                consumer.setUpTestMsg();
            }
            consumers.add(consumer);
            return consumer;
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        }).when(queueFactory).createToRuleEngineMsgConsumer(any());

        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
        consumerManager = new TbRuleEngineQueueConsumerManager(ruleEngineConsumerContext, queueKey);
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterEach` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterEach() {
        consumerManager.stop();
        consumerManager.awaitStop();
        ruleEngineConsumerContext.stop();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (generateQueueMsgs) {
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        log.debug("totalConsumedMsgs = {}, totalProcessedMsgs = {}", totalConsumedMsgs.get(), totalProcessedMsgs.get());
                        assertThat(totalProcessedMsgs.get()).isEqualTo(totalConsumedMsgs.get());
                    });
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testInit_consumerPerPartition` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testInit_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);

        Set<TopicPartitionInfo> partitions = createTpis(2, 3, 4);
        consumerManager.update(partitions);
        partitions = createTpis(3, 4, 5);
        consumerManager.update(partitions);
        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        // simulated multiple partition change events before consumer is ready; only latest partitions should be processed
        verifyNoInteractions(queueFactory);

        ruleEngineConsumerContext.setReady(true);
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 3);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TopicPartitionInfo partition : partitions) {
            TestConsumer consumer = getConsumer(partition);
            verifySubscribedAndLaunched(consumer, Set.of(partition));
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testInit_singleConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testInit_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);

        Set<TopicPartitionInfo> partitions = createTpis(2, 3, 4);
        consumerManager.update(partitions);
        partitions = createTpis(3, 4, 5);
        consumerManager.update(partitions);
        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);

        verifyNoInteractions(queueFactory);

        ruleEngineConsumerContext.setReady(true);
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 1);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPartitionsUpdate_singleConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPartitionsUpdate_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);

        Set<TopicPartitionInfo> partitions = Collections.emptySet();
        consumerManager.update(partitions);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        verify(queueFactory, after(1000).never()).createToRuleEngineMsgConsumer(any());

        partitions = createTpis(1);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = createTpis(4, 5, 6);
        consumerManager.update(partitions);
        verifySubscribedAndLaunched(consumer, partitions);

        partitions = Collections.emptySet();
        consumerManager.update(partitions);
        verifyUnsubscribedAndStopped(consumer);

        partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testPartitionsUpdate_consumerPerPartition` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testPartitionsUpdate_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);

        consumerManager.update(Collections.emptySet());
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        verify(queueFactory, after(1000).never()).createToRuleEngineMsgConsumer(any());

        consumerManager.update(createTpis(1));
        TestConsumer consumer1 = getConsumer(1);
        verifySubscribedAndLaunched(consumer1, 1);

        consumerManager.update(createTpis(1, 2, 3));
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);
        verifyNotTouched(consumer1);

        consumerManager.update(createTpis(3, 4, 5));
        TestConsumer consumer4 = getConsumer(4);
        TestConsumer consumer5 = getConsumer(5);
        verifySubscribedAndLaunched(consumer4, 4);
        verifySubscribedAndLaunched(consumer5, 5);
        verifyUnsubscribedAndStopped(consumer1);
        verifyUnsubscribedAndStopped(consumer2);
        verifyNotTouched(consumer3);

        consumerManager.update(Collections.emptySet());
        verifyUnsubscribedAndStopped(consumer3);
        verifyUnsubscribedAndStopped(consumer4);
        verifyUnsubscribedAndStopped(consumer5);

        consumerManager.update(createTpis(1, 2, 3));
        consumer1 = getConsumer(1);
        consumer2 = getConsumer(2);
        consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConfigUpdate_singleConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testConfigUpdate_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setPollInterval(queue.getPollInterval() / 2);
        newConfig.setPartitions(queue.getPartitions() / 2);
        newConfig.setPackProcessingTimeout(queue.getPackProcessingTimeout() * 2);
        newConfig.getSubmitStrategy().setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        newConfig.getProcessingStrategy().setType(ProcessingStrategyType.RETRY_ALL);
        consumerManager.update(newConfig);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(consumer, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(ruleEngineConsumerContext.getSubmitStrategyFactory(), atLeastOnce()).newInstance(any(), eq(newConfig.getSubmitStrategy()));
                    verify(ruleEngineConsumerContext.getProcessingStrategyFactory(), atLeastOnce()).newInstance(any(), eq(newConfig.getProcessingStrategy()));
                });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConfigUpdate_consumerPerPartition` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testConfigUpdate_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setPollInterval(queue.getPollInterval() / 2);
        newConfig.setPartitions(queue.getPartitions() / 2);
        newConfig.setPackProcessingTimeout(queue.getPackProcessingTimeout() * 2);
        newConfig.getSubmitStrategy().setType(SubmitStrategyType.SEQUENTIAL_BY_ORIGINATOR);
        newConfig.getProcessingStrategy().setType(ProcessingStrategyType.RETRY_ALL);
        consumerManager.update(newConfig);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(consumer1, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(consumer2, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                    verify(consumer3, atLeastOnce()).poll(eq((long) newConfig.getPollInterval()));
                });
        verifyNotTouched(consumer1);
        verifyNotTouched(consumer2);
        verifyNotTouched(consumer3);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConfigUpdate_fromSingleToConsumerPerPartition` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testConfigUpdate_fromSingleToConsumerPerPartition() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setConsumerPerPartition(true);
        consumerManager.update(newConfig);

        verifyUnsubscribedAndStopped(consumer);
        verifySubscribedAndLaunched(getConsumer(1), 1);
        verifySubscribedAndLaunched(getConsumer(2), 2);
        verifySubscribedAndLaunched(getConsumer(3), 3);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testConfigUpdate_fromConsumerPerPartitionToSingle` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testConfigUpdate_fromConsumerPerPartitionToSingle() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2, 3);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        TestConsumer consumer3 = getConsumer(3);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifySubscribedAndLaunched(consumer3, 3);

        Queue newConfig = JacksonUtil.clone(queue);
        newConfig.setConsumerPerPartition(false);
        consumerManager.update(newConfig);

        verifyUnsubscribedAndStopped(consumer1);
        verifyUnsubscribedAndStopped(consumer2);
        verifyUnsubscribedAndStopped(consumer3);
        verifySubscribedAndLaunched(getConsumer(), partitions);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testStop` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testStop() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        consumerManager.update(createTpis(1));
        TestConsumer consumer = getConsumer(1);
        verifySubscribedAndLaunched(consumer, 1);
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        verify(queueFactory, times(1)).createToRuleEngineMsgConsumer(any());

        consumerManager.stop();
        consumerManager.update(createTpis(1, 2, 3, 4)); // to check that no new tasks after stop are processed
        consumerManager.update(createTpis(5, 6, 7));

        verifyUnsubscribedAndStopped(consumer);
        verifyNoMoreInteractions(queueFactory);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDelete_consumerPerPartition` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDelete_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2);
        consumerManager.update(partitions);
        TestConsumer consumer1 = getConsumer(1);
        TestConsumer consumer2 = getConsumer(2);
        verifySubscribedAndLaunched(consumer1, 1);
        verifySubscribedAndLaunched(consumer2, 2);
        verifyMsgProcessed(consumer1.testMsg);
        verifyMsgProcessed(consumer2.testMsg);

        consumerManager.delete(true);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(ruleEngineMsgProducer).send(any(), any(), any());
                });
        clearInvocations(actorContext);
        verify(consumer1, never()).unsubscribe();
        verify(consumer2, never()).unsubscribe();
        int msgCount = totalConsumedMsgs.get();

        await().atLeast(2, TimeUnit.SECONDS) // based on topicDeletionDelayInSec(5) = 5 - ( 3 seconds the code may execute starting consumerManager.delete() call)
                .atMost(7, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    partitions.stream()
                            .map(TopicPartitionInfo::getFullTopicName)
                            .forEach(topic -> {
                                verify(queueAdmin).deleteTopic(eq(topic));
                            });
                });
        verify(consumer1).unsubscribe();
        verify(consumer2).unsubscribe();

        int totalMovedMsgs = totalConsumedMsgs.get() - msgCount;
        assertThat(totalMovedMsgs).isNotZero();
        verify(ruleEngineMsgProducer, atLeast(totalMovedMsgs)).send(any(), any(), any());
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDelete_singleConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDelete_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        Set<TopicPartitionInfo> partitions = createTpis(1, 2);
        consumerManager.update(partitions);
        TestConsumer consumer = getConsumer();
        verifySubscribedAndLaunched(consumer, partitions);
        verifyMsgProcessed(consumer.testMsg);

        consumerManager.delete(true);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(ruleEngineMsgProducer).send(any(), any(), any());
                });
        clearInvocations(actorContext);
        verify(consumer, never()).unsubscribe();
        int msgCount = totalConsumedMsgs.get();

        await().atLeast(2, TimeUnit.SECONDS) // based on topicDeletionDelayInSec(5) = 5 - ( 3 seconds the code may execute starting consumerManager.delete() call)
                .atMost(7, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    partitions.stream()
                            .map(TopicPartitionInfo::getFullTopicName)
                            .forEach(topic -> {
                                verify(queueAdmin).deleteTopic(eq(topic));
                            });
                });
        verify(consumer).unsubscribe();

        int movedMsgs = totalConsumedMsgs.get() - msgCount;
        assertThat(movedMsgs).isNotZero();
        verify(ruleEngineMsgProducer, atLeast(movedMsgs)).send(any(), any(), any());
        // 通过 Actor 消息投递切换到目标处理器，线程安全依赖 Actor 邮箱串行化。
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testManyDifferentUpdates` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testManyDifferentUpdates() throws Exception {
        queue.setConsumerPerPartition(RandomUtils.nextBoolean());
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);

        Supplier<Queue> queueConfigUpdater = () -> {
            Queue oldConfig = consumerManager.getQueue();
            Queue newConfig = JacksonUtil.clone(oldConfig);
            newConfig.setConsumerPerPartition(RandomUtils.nextBoolean());
            newConfig.setPollInterval(RandomUtils.nextInt(100, 501));
            newConfig.setPartitions(RandomUtils.nextInt(1, 10));
            newConfig.setPackProcessingTimeout(RandomUtils.nextLong(100, 5001));
            newConfig.getSubmitStrategy().setType(SubmitStrategyType.values()[RandomUtils.nextInt(0, SubmitStrategyType.values().length)]);
            newConfig.getProcessingStrategy().setType(ProcessingStrategyType.values()[RandomUtils.nextInt(0, ProcessingStrategyType.values().length)]);
            log.info("Generated new config: consumerPerPartition={}, pollInterval={}, processingStrategy={}",
                    newConfig.isConsumerPerPartition(), newConfig.getPollInterval(), newConfig.getProcessingStrategy().getType());
            return newConfig;
        };
        Supplier<Set<TopicPartitionInfo>> partitionsUpdater = () -> {
            int partitionsCount = RandomUtils.nextInt(0, 20);
            int[] partitions = IntStream.generate(() -> RandomUtils.nextInt(0, 20))
                    .distinct().limit(partitionsCount)
                    .sorted().toArray();
            log.info("Generated new partitions: {}", Arrays.toString(partitions));
            return createTpis(partitions);
        };

        int iterations = 100;
        Queue latestConfig = queue;
        Set<TopicPartitionInfo> latestPartitions = Collections.emptySet();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i <= iterations; i++) {
            boolean updateQueueConfig = RandomUtils.nextBoolean();
            boolean updatePartitions = !updateQueueConfig;
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (updateQueueConfig) {
                latestConfig = queueConfigUpdater.get();
                consumerManager.update(latestConfig);
            }
            if (updatePartitions) {
                latestPartitions = partitionsUpdater.get();
                consumerManager.update(latestPartitions);
            }
            Thread.sleep(RandomUtils.nextLong(0, 200));
        }
        if (latestPartitions.isEmpty()) {
            do {
                latestPartitions = partitionsUpdater.get();
            } while (latestPartitions.isEmpty());
            consumerManager.update(latestPartitions);
        }

        Queue expectedConfig = latestConfig;
        Set<TopicPartitionInfo> expectedPartitions = latestPartitions;
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(consumerManager.getQueue()).isEqualTo(expectedConfig);
                    assertThat(consumerManager.getPartitions()).isEqualTo(expectedPartitions);
                });

        if (expectedConfig.isConsumerPerPartition()) {
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                for (TopicPartitionInfo partition : expectedPartitions) {
                    if (consumers.stream().noneMatch(consumer -> consumer.subscribed &&
                            consumer.pollingStarted && Set.of(partition).equals(consumer.getPartitions()))) {
                        return false;
                    }
                }
                return consumers.size() == expectedPartitions.size();
            });
        } else {
            await().atMost(5, TimeUnit.SECONDS).until(() -> {
                return consumers.size() == 1 && consumers.stream()
                        .anyMatch(consumer -> consumer.subscribed && consumer.pollingStarted &&
                                expectedPartitions.equals(consumer.getPartitions()));
            });
        }
        Mockito.reset(ruleEngineConsumerContext.getSubmitStrategyFactory());
        Mockito.reset(ruleEngineConsumerContext.getProcessingStrategyFactory());
        consumers.forEach(Mockito::clearInvocations);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            for (TestConsumer consumer : consumers) {
                verify(consumer, atLeastOnce().description("consumer " + consumer.topics)).poll(expectedConfig.getPollInterval());
            }
            verify(ruleEngineConsumerContext.getSubmitStrategyFactory(), atLeastOnce()).newInstance(any(), eq(expectedConfig.getSubmitStrategy()));
            verify(ruleEngineConsumerContext.getProcessingStrategyFactory(), atLeastOnce()).newInstance(any(), eq(expectedConfig.getProcessingStrategy()));
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifySubscribedAndLaunched` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifySubscribedAndLaunched(TestConsumer consumer, Set<TopicPartitionInfo> expectedPartitions) {
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> consumer.subscribed && consumer.getPartitions().equals(expectedPartitions) && consumer.pollingStarted);
        verify(consumer, times(1)).subscribe(any());
        verify(consumer).subscribe(eq(expectedPartitions));
        verify(consumer).doSubscribe(argThat(topics -> topics.containsAll(expectedPartitions.stream()
                .map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList()))));
        verify(consumer, atLeastOnce()).poll(eq((long) queue.getPollInterval()));
        verify(consumer, atLeastOnce()).doPoll(eq((long) queue.getPollInterval()));
        verify(consumer, never()).unsubscribe();
        Mockito.reset(consumer);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifySubscribedAndLaunched` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifySubscribedAndLaunched(TestConsumer consumer, int... expectedPartitions) {
        verifySubscribedAndLaunched(consumer, createTpis(expectedPartitions));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifyUnsubscribedAndStopped` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifyUnsubscribedAndStopped(TestConsumer consumer) {
        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> !consumer.subscribed && !consumer.topics.isEmpty());
        verify(consumer, never()).subscribe(any());
        verify(consumer, never()).doSubscribe(any());
        assertThat(consumers).doesNotContain(consumer);
        Mockito.reset(consumer);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifyNotTouched` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifyNotTouched(TestConsumer consumer) {
        verify(consumer, never()).subscribe(any());
        verify(consumer, never()).subscribe();
        verify(consumer, never()).doSubscribe(any());
        verify(consumer, never()).unsubscribe();
        verify(consumer, never()).doUnsubscribe();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifyMsgProcessed` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifyMsgProcessed(TbMsg tbMsg) {
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(actorContext, atLeastOnce()).tell(argThat(msg -> {
                return ((QueueToRuleEngineMsg) msg).getMsg().getId().equals(tbMsg.getId());
            }));
        });
    }

    // for consumer-per-partition
    /**
     * 方法说明：
     * 1. 职责：执行 `getConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TestConsumer getConsumer(TopicPartitionInfo tpi) {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.stream()
                        .filter(consumer -> consumer.getPartitions() != null &&
                                consumer.getPartitions().size() == 1 &&
                                consumer.getPartitions().contains(tpi))
                        .findFirst().orElse(null), Objects::nonNull);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TestConsumer getConsumer(int partition) {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.stream()
                        .filter(consumer -> consumer.getPartitions() != null &&
                                consumer.getPartitions().size() == 1 &&
                                consumer.getPartitions().stream()
                                        .anyMatch(tpi -> tpi.getPartition().get().equals(partition)))
                        .findFirst().orElse(null), Objects::nonNull);
    }

    // for single consumer
    /**
     * 方法说明：
     * 1. 职责：执行 `getConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TestConsumer getConsumer() {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 1 ? consumers.iterator().next() : null, Objects::nonNull);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTpis` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Set<TopicPartitionInfo> createTpis(int... partitions) {
        return Arrays.stream(partitions)
                .mapToObj(n -> TopicPartitionInfo.builder()
                        .tenantId(queue.getTenantId())
                        .topic(queue.getTopic())
                        .partition(n)
                        .myPartition(true)
                        .build())
                .collect(Collectors.toSet());
    }


    /**
     * 中文说明：
     * 1. 类目的：`TestConsumer` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
     * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
     */
    class TestConsumer extends AbstractTbQueueConsumerTemplate<TbMsg, TbProtoQueueMsg<ToRuleEngineMsg>> {

        @Getter
        /**
         * 字段说明：
         * 1. 保存 `topics` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private List<String> topics;

        /**
         * 字段说明：
         * 1. 保存 `subscribed` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private boolean subscribed;
        private boolean pollingStarted;

        /**
         * 字段说明：
         * 1. 保存 `testMsg` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private TbMsg testMsg;

        /**
         * 方法说明：
         * 1. 职责：执行 `TestConsumer` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public TestConsumer(String topic) {
            super(topic);
        }

        @SneakyThrows
        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `doPoll` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected List<TbMsg> doPoll(long durationInMillis) {
            log.debug("doPoll({} ms)", durationInMillis);
            if (!subscribed) {
                throw new IllegalStateException("Cannot poll because not subscribed");
            }
            pollingStarted = true;
            if (testMsg != null && RandomUtils.nextBoolean()) {
                Thread.sleep(100);
                return List.of(testMsg);
            }
            return Collections.emptyList();
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `decode` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected TbProtoQueueMsg<ToRuleEngineMsg> decode(TbMsg tbMsg) throws IOException {
            log.debug("decode()");
            UUID tenantId = UUID.randomUUID();
            return new TbProtoQueueMsg<>(UUID.randomUUID(), ToRuleEngineMsg.newBuilder()
                    .setTenantIdMSB(tenantId.getMostSignificantBits())
                    .setTenantIdLSB(tenantId.getLeastSignificantBits())
                    .addRelationTypes("Success")
                    .setTbMsg(TbMsg.toByteString(tbMsg))
                    .build());
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `doSubscribe` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected void doSubscribe(List<String> topicNames) {
            log.debug("doSubscribe({})", topicNames);
            this.topics = topicNames;
            subscribed = true;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `doCommit` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected void doCommit() {
            if (!subscribed) {
                throw new IllegalStateException("Cannot commit because not subscribed");
            }
            log.debug("doCommit() totalConsumedMsgs = {}", totalConsumedMsgs.incrementAndGet());
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `unsubscribe` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void unsubscribe() {
            super.unsubscribe();
            consumers.remove(this);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `doUnsubscribe` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected void doUnsubscribe() {
            log.debug("doUnsubscribe()");
            if (!subscribed) {
                throw new IllegalStateException("Already unsubscribed!");
            }
            subscribed = false;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `isLongPollingSupported` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        protected boolean isLongPollingSupported() {
            return false;
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `getPartitions` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public Set<TopicPartitionInfo> getPartitions() {
            return partitions;
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `setUpTestMsg` 对应的队列服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void setUpTestMsg() {
            testMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, new DeviceId(UUID.randomUUID()), new TbMsgMetaData(), "{}");
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbRuleEngineQueueConsumerManagerTest` 在 ThingsBoard Application 测试模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
