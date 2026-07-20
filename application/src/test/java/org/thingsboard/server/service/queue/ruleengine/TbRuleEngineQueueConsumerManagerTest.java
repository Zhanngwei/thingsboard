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

/**
 * 中文说明：
 * 1. `TbRuleEngineQueueConsumerManagerTest` 是 ThingsBoard Application 中验证 `TbRuleEngineQueueConsumerManager` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbRuleEngineQueueConsumerManagerTest {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private ActorSystemContext actorContext;
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Mock
    private StatsFactory statsFactory;
    /**
     * 队列，用于按场景创建或提供目标对象。
     */
    @Mock
    private TbRuleEngineQueueFactory queueFactory;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private RuleEngineStatisticsService statisticsService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Mock
    private TbServiceInfoProvider serviceInfoProvider;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Mock
    private PartitionService partitionService;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Mock
    private TbQueueProducerProvider producerProvider;
    private TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Mock
    private TbQueueAdmin queueAdmin;
    private TbRuleEngineConsumerContext ruleEngineConsumerContext;

    /**
     * 管理器，负责处理对应任务或消息。
     */
    private TbRuleEngineQueueConsumerManager consumerManager;
    private Queue queue;

    /**
     * `consumers`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<TestConsumer> consumers;
    private boolean generateQueueMsgs;
    /**
     * `totalConsumedMsgs` 字段，保存当前对象的对应属性。
     */
    private AtomicInteger totalConsumedMsgs;
    private AtomicInteger totalProcessedMsgs;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
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
            if (generateQueueMsgs) {
                consumer.setUpTestMsg();
            }
            consumers.add(consumer);
            return consumer;
        }).when(queueFactory).createToRuleEngineMsgConsumer(any());

        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queue);
        consumerManager = new TbRuleEngineQueueConsumerManager(ruleEngineConsumerContext, queueKey);
    }

    /**
     * 功能：执行 `afterEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterEach() {
        consumerManager.stop();
        consumerManager.awaitStop();
        ruleEngineConsumerContext.stop();

        if (generateQueueMsgs) {
            await().atMost(10, TimeUnit.SECONDS)
                    .untilAsserted(() -> {
                        log.debug("totalConsumedMsgs = {}, totalProcessedMsgs = {}", totalConsumedMsgs.get(), totalProcessedMsgs.get());
                        assertThat(totalProcessedMsgs.get()).isEqualTo(totalConsumedMsgs.get());
                    });
        }
    }

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
        for (TopicPartitionInfo partition : partitions) {
            TestConsumer consumer = getConsumer(partition);
            verifySubscribedAndLaunched(consumer, Set.of(partition));
        }
    }

    /**
     * 功能：验证消息消费者相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证消息消费者相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPartitionsUpdate_singleConsumer() {
        queue.setConsumerPerPartition(false);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);

        Set<TopicPartitionInfo> partitions = Collections.emptySet();
        consumerManager.update(partitions);
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

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPartitionsUpdate_consumerPerPartition() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);

        consumerManager.update(Collections.emptySet());
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

    /**
     * 功能：验证配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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

    /**
     * 功能：验证`Stop`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testStop() {
        queue.setConsumerPerPartition(true);
        consumerManager.init(queue);
        ruleEngineConsumerContext.setReady(true);
        consumerManager.update(createTpis(1));
        TestConsumer consumer = getConsumer(1);
        verifySubscribedAndLaunched(consumer, 1);
        verify(queueFactory, times(1)).createToRuleEngineMsgConsumer(any());

        consumerManager.stop();
        consumerManager.update(createTpis(1, 2, 3, 4)); // to check that no new tasks after stop are processed
        consumerManager.update(createTpis(5, 6, 7));

        verifyUnsubscribedAndStopped(consumer);
        verifyNoMoreInteractions(queueFactory);
    }

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    /**
     * 功能：验证消息消费者相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
        verify(actorContext, never()).tell(any());
        generateQueueMsgs = false;
    }

    /**
     * 功能：验证`Many Different Updates`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
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
        for (int i = 1; i <= iterations; i++) {
            boolean updateQueueConfig = RandomUtils.nextBoolean();
            boolean updatePartitions = !updateQueueConfig;
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
     * 功能：校验`Subscribed And Launched`。
     * 参数：
     * - `consumer`：`consumer` 参数。
     * - `expectedPartitions`：分区标识或分区信息。
     * 返回：无。
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
     * 功能：校验`Subscribed And Launched`。
     * 参数：
     * - `consumer`：`consumer` 参数。
     * - `expectedPartitions`：分区标识或分区信息。
     * 返回：无。
     */
    private void verifySubscribedAndLaunched(TestConsumer consumer, int... expectedPartitions) {
        verifySubscribedAndLaunched(consumer, createTpis(expectedPartitions));
    }

    /**
     * 功能：校验`Unsubscribed And Stopped`。
     * 参数：
     * - `consumer`：`consumer` 参数。
     * 返回：无。
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
     * 功能：校验`Not Touched`。
     * 参数：
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    private void verifyNotTouched(TestConsumer consumer) {
        verify(consumer, never()).subscribe(any());
        verify(consumer, never()).subscribe();
        verify(consumer, never()).doSubscribe(any());
        verify(consumer, never()).unsubscribe();
        verify(consumer, never()).doUnsubscribe();
    }

    /**
     * 功能：校验消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * 返回：无。
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
     * 功能：获取消息消费者。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * 返回：处理结果。
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
     * 功能：获取消息消费者。
     * 参数：
     * - `partition`：分区标识或分区信息。
     * 返回：处理结果。
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
     * 功能：获取消息消费者。
     * 参数：无。
     * 返回：处理结果。
     */
    private TestConsumer getConsumer() {
        return await().atMost(5, TimeUnit.SECONDS)
                .until(() -> consumers.size() == 1 ? consumers.iterator().next() : null, Objects::nonNull);
    }

    /**
     * 功能：保存或创建`Tpis`。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：匹配的数据集合。
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
     * 1. `TestConsumer` 是 ThingsBoard Application 中围绕 `Consumer` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `AbstractTbQueueConsumerTemplate`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    class TestConsumer extends AbstractTbQueueConsumerTemplate<TbMsg, TbProtoQueueMsg<ToRuleEngineMsg>> {

        /**
         * `topics`列表，用于保存一组待处理对象。
         */
        @Getter
        private List<String> topics;

        /**
         * 是否满足`subscribed`条件。
         */
        private boolean subscribed;
        private boolean pollingStarted;

        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        private TbMsg testMsg;

        /**
         * 功能：创建 `TbRuleEngineQueueConsumerManagerTest` 实例，并初始化必要字段。
         * 参数：
         * - `topic`：主题名称或主题对象。
         * 返回：新创建的对象实例。
         */
        public TestConsumer(String topic) {
            super(topic);
        }

        /**
         * 功能：执行 `doPoll` 对应的处理。
         * 参数：
         * - `durationInMillis`：`durationInMillis` 参数。
         * 返回：匹配的数据集合。
         */
        @SneakyThrows
        @Override
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

        /**
         * 功能：执行 `decode` 对应的处理。
         * 参数：
         * - `tbMsg`：待处理消息。
         * 返回：处理结果。
         */
        @Override
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

        /**
         * 功能：执行 `doSubscribe` 对应的处理。
         * 参数：
         * - `topicNames`：主题名称或主题对象。
         * 返回：无。
         */
        @Override
        protected void doSubscribe(List<String> topicNames) {
            log.debug("doSubscribe({})", topicNames);
            this.topics = topicNames;
            subscribed = true;
        }

        /**
         * 功能：执行 `doCommit` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        @Override
        protected void doCommit() {
            if (!subscribed) {
                throw new IllegalStateException("Cannot commit because not subscribed");
            }
            log.debug("doCommit() totalConsumedMsgs = {}", totalConsumedMsgs.incrementAndGet());
        }

        /**
         * 功能：执行 `unsubscribe` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        @Override
        public void unsubscribe() {
            super.unsubscribe();
            consumers.remove(this);
        }

        /**
         * 功能：执行 `doUnsubscribe` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        @Override
        protected void doUnsubscribe() {
            log.debug("doUnsubscribe()");
            if (!subscribed) {
                throw new IllegalStateException("Already unsubscribed!");
            }
            subscribed = false;
        }

        /**
         * 功能：判断`Long Polling Supported`。
         * 参数：无。
         * 返回：判断结果。
         */
        @Override
        protected boolean isLongPollingSupported() {
            return false;
        }

        /**
         * 功能：获取`Partitions`。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        public Set<TopicPartitionInfo> getPartitions() {
            return partitions;
        }

        /**
         * 功能：更新消息。
         * 参数：无。
         * 返回：无。
         */
        public void setUpTestMsg() {
            testMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, new DeviceId(UUID.randomUUID()), new TbMsgMetaData(), "{}");
        }
    }

}
