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
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.deduplication.DeduplicationStrategy;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNode;
import org.thingsboard.rule.engine.deduplication.TbMsgDeduplicationNodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 测试目标：验证 {@code TbMsgDeduplicationNodeTest} 覆盖的 消息转换节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMsgDeduplicationNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
public class TbMsgDeduplicationNodeTest extends AbstractRuleNodeUpgradeTest {

    /** 可变 fixture 字段：{@code ctx} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctx;

    /** 固定 fixture 字段：{@code factory} 保存 {@code ThingsBoardThreadFactory} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("de-duplication-node-test");
    /** 固定 fixture 字段：{@code executorService} 保存 {@code ScheduledExecutorService} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);
    /** 固定 fixture 字段：{@code deduplicationInterval} 保存 {@code int} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
    private final int deduplicationInterval = 1;

    /** 可变 fixture 字段：{@code tenantId} 保存 {@code TenantId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TenantId tenantId;

    /** 可变 fixture 字段：{@code node} 保存 {@code TbMsgDeduplicationNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgDeduplicationNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbMsgDeduplicationNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgDeduplicationNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbNodeConfiguration nodeConfiguration;

    /** 可变 fixture 字段：{@code awaitTellSelfLatch} 保存 {@code CountDownLatch} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private CountDownLatch awaitTellSelfLatch;

    /**
     * 生命周期方法：{@code init} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void init() throws TbNodeException {
        ctx = mock(TbContext.class);

        tenantId = TenantId.fromUUID(UUID.randomUUID());
        RuleNodeId ruleNodeId = new RuleNodeId(UUID.randomUUID());

        when(ctx.getSelfId()).thenReturn(ruleNodeId);
        when(ctx.getTenantId()).thenReturn(tenantId);

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsgType type = (TbMsgType) (invocationOnMock.getArguments())[1];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[2];
            TbMsgMetaData metaData = (TbMsgMetaData) (invocationOnMock.getArguments())[3];
            String data = (String) (invocationOnMock.getArguments())[4];
            return TbMsg.newMsg(type, originator, metaData.copy(), data);
        }).when(ctx).newMsg(isNull(), eq(TbMsgType.DEDUPLICATION_TIMEOUT_SELF_MSG), nullable(EntityId.class), any(TbMsgMetaData.class), any(String.class));
        node = spy(new TbMsgDeduplicationNode());
        config = new TbMsgDeduplicationNodeConfiguration().defaultConfiguration();
    }

    /**
     * 辅助方法：{@code invokeTellSelf} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void invokeTellSelf(int maxNumberOfInvocation) {
        invokeTellSelf(maxNumberOfInvocation, false, 0);
    }

    /**
     * 辅助方法：{@code invokeTellSelf} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void invokeTellSelf(int maxNumberOfInvocation, boolean delayScheduleTimeout, int delayMultiplier) {
        AtomicLong scheduleTimeout = new AtomicLong(deduplicationInterval);
        AtomicInteger scheduleCount = new AtomicInteger(0);
        doAnswer((Answer<Void>) invocationOnMock -> {
            scheduleCount.getAndIncrement();
            if (scheduleCount.get() <= maxNumberOfInvocation) {
                TbMsg msg = (TbMsg) (invocationOnMock.getArguments())[0];
                executorService.schedule(() -> {
                    try {
                        node.onMsg(ctx, msg);
                        awaitTellSelfLatch.countDown();
                    } catch (ExecutionException | InterruptedException | TbNodeException e) {
                        log.error("Failed to execute tellSelf method call due to: ", e);
                    }
                }, scheduleTimeout.get(), TimeUnit.SECONDS);
                if (delayScheduleTimeout) {
                    scheduleTimeout.set(scheduleTimeout.get() * delayMultiplier);
                }
            }

            return null;
        }).when(ctx).tellSelf(ArgumentMatchers.any(TbMsg.class), ArgumentMatchers.anyLong());
    }

    /**
     * 生命周期方法：{@code destroy} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    public void destroy() {
        executorService.shutdown();
        node.destroy();
    }

    /** 参数源方法：{@code given_100_messages_strategy_first_then_verifyOutput} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> given_100_messages_strategy_first_then_verifyOutput() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(DataConstants.MAIN_QUEUE_NAME),
                Arguments.of(DataConstants.HP_QUEUE_NAME)
        );
    }

    /**
     * 测试方法：覆盖 {@code given_100_messages_strategy_first_then_verifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource
    public void given_100_messages_strategy_first_then_verifyOutput(String queueName) throws TbNodeException, ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        when(ctx.getQueueName()).thenReturn(queueName);

        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        TbMsg msgToReject = createMsg(deviceId, inputMsgs.get(inputMsgs.size() - 1).getMetaDataTs() + 2);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        TbMsg firstMsg = inputMsgs.get(0);
        TbMsg actualMsg = newMsgCaptor.getValue();
        // msg ids should be different because we create new msg before enqueueForTellNext
        Assertions.assertNotEquals(firstMsg.getId(), actualMsg.getId());
        Assertions.assertEquals(firstMsg.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(firstMsg.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(firstMsg.getData(), actualMsg.getData());
        Assertions.assertEquals(firstMsg.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(firstMsg.getType(), actualMsg.getType());

        if (queueName == null) {
            Assertions.assertEquals(firstMsg.getQueueName(), actualMsg.getQueueName());
        } else {
            Assertions.assertEquals(ctx.getQueueName(), actualMsg.getQueueName());
        }
    }

    /**
     * 测试方法：覆盖 {@code given_100_messages_strategy_last_then_verifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void given_100_messages_strategy_last_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        config.setStrategy(DeduplicationStrategy.LAST);
        config.setInterval(deduplicationInterval);
        config.setMaxPendingMsgs(msgCount);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        TbMsg msgWithLatestTs = getMsgWithLatestTs(inputMsgs);

        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        TbMsg msgToReject = createMsg(deviceId, inputMsgs.get(inputMsgs.size() - 1).getMetaDataTs() + 2);
        node.onMsg(ctx, msgToReject);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(ctx, times(1)).tellFailure(eq(msgToReject), any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation + 1)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        TbMsg actualMsg = newMsgCaptor.getValue();
        // msg ids should be different because we create new msg before enqueueForTellNext
        Assertions.assertNotEquals(msgWithLatestTs.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTs.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTs.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTs.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTs.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTs.getType(), actualMsg.getType());
    }

    /**
     * 测试方法：覆盖 {@code given_100_messages_strategy_all_then_verifyOutput} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void given_100_messages_strategy_all_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation);

        when(ctx.getQueueName()).thenReturn(DataConstants.HP_QUEUE_NAME);
        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(TbMsgType.POST_ATTRIBUTES_REQUEST.name());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> inputMsgs = getTbMsgs(deviceId, msgCount, currentTimeMillis, 500);
        for (TbMsg msg : inputMsgs) {
            node.onMsg(ctx, msg);
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(1)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        Assertions.assertEquals(1, newMsgCaptor.getAllValues().size());
        TbMsg outMessage = newMsgCaptor.getAllValues().get(0);
        Assertions.assertEquals(getMergedData(inputMsgs), outMessage.getData());
        Assertions.assertEquals(deviceId, outMessage.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), outMessage.getType());
        Assertions.assertEquals(DataConstants.HP_QUEUE_NAME, outMessage.getQueueName());
    }

    /**
     * 测试方法：覆盖 {@code given_100_messages_strategy_all_then_verifyOutput_2_packs} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void given_100_messages_strategy_all_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        when(ctx.getQueueName()).thenReturn(DataConstants.HP_QUEUE_NAME);
        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.ALL);
        config.setOutMsgType(TbMsgType.POST_ATTRIBUTES_REQUEST.name());
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> firstMsgPack = getTbMsgs(deviceId, msgCount / 2, currentTimeMillis, 500);
        for (TbMsg msg : firstMsgPack) {
            node.onMsg(ctx, msg);
        }
        long firstPackDeduplicationPackEndTs = firstMsgPack.get(0).getMetaDataTs() + TimeUnit.SECONDS.toMillis(deduplicationInterval);

        List<TbMsg> secondMsgPack = getTbMsgs(deviceId, msgCount / 2, firstPackDeduplicationPackEndTs, 500);
        for (TbMsg msg : secondMsgPack) {
            node.onMsg(ctx, msg);
        }

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());

        TbMsg firstMsg = resultMsgs.get(0);
        Assertions.assertEquals(getMergedData(firstMsgPack), firstMsg.getData());
        Assertions.assertEquals(deviceId, firstMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), firstMsg.getType());
        Assertions.assertEquals(DataConstants.HP_QUEUE_NAME, firstMsg.getQueueName());

        TbMsg secondMsg = resultMsgs.get(1);
        Assertions.assertEquals(getMergedData(secondMsgPack), secondMsg.getData());
        Assertions.assertEquals(deviceId, secondMsg.getOriginator());
        Assertions.assertEquals(config.getOutMsgType(), secondMsg.getType());
        Assertions.assertEquals(DataConstants.HP_QUEUE_NAME, secondMsg.getQueueName());
    }

    /**
     * 测试方法：覆盖 {@code given_100_messages_strategy_last_then_verifyOutput_2_packs} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void given_100_messages_strategy_last_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        int wantedNumberOfTellSelfInvocation = 2;
        int msgCount = 100;
        awaitTellSelfLatch = new CountDownLatch(wantedNumberOfTellSelfInvocation);
        invokeTellSelf(wantedNumberOfTellSelfInvocation, true, 3);

        config.setInterval(deduplicationInterval);
        config.setStrategy(DeduplicationStrategy.LAST);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctx, nodeConfiguration);

        DeviceId deviceId = new DeviceId(UUID.randomUUID());
        long currentTimeMillis = System.currentTimeMillis();

        List<TbMsg> firstMsgPack = getTbMsgs(deviceId, msgCount / 2, currentTimeMillis, 500);
        for (TbMsg msg : firstMsgPack) {
            node.onMsg(ctx, msg);
        }
        long firstPackDeduplicationPackEndTs = firstMsgPack.get(0).getMetaDataTs() + TimeUnit.SECONDS.toMillis(deduplicationInterval);
        TbMsg msgWithLatestTsInFirstPack = getMsgWithLatestTs(firstMsgPack);

        List<TbMsg> secondMsgPack = getTbMsgs(deviceId, msgCount / 2, firstPackDeduplicationPackEndTs, 500);
        for (TbMsg msg : secondMsgPack) {
            node.onMsg(ctx, msg);
        }
        TbMsg msgWithLatestTsInSecondPack = getMsgWithLatestTs(secondMsgPack);

        awaitTellSelfLatch.await();

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> failureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctx, times(msgCount)).ack(any());
        verify(node, times(msgCount + wantedNumberOfTellSelfInvocation)).onMsg(eq(ctx), any());
        verify(ctx, times(2)).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), successCaptor.capture(), failureCaptor.capture());

        List<TbMsg> resultMsgs = newMsgCaptor.getAllValues();
        Assertions.assertEquals(2, resultMsgs.size());

        // verify that newMsg is called but content of messages is the same as in the last msg for the first pack.
        TbMsg actualMsg = resultMsgs.get(0);
        Assertions.assertNotEquals(msgWithLatestTsInFirstPack.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTsInFirstPack.getType(), actualMsg.getType());

        // verify that newMsg is called but content of messages is the same as in the last msg for the second pack.
        actualMsg = resultMsgs.get(1);
        Assertions.assertNotEquals(msgWithLatestTsInSecondPack.getId(), actualMsg.getId());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getOriginator(), actualMsg.getOriginator());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getCustomerId(), actualMsg.getCustomerId());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getData(), actualMsg.getData());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getMetaData(), actualMsg.getMetaData());
        Assertions.assertEquals(msgWithLatestTsInSecondPack.getType(), actualMsg.getType());
    }

    // Rule nodes upgrade
    /** 参数源方法：{@code givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3, \"queueName\":null}",
                        true,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3, \"queueName\":\"Main\"}",
                        true,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3}",
                        false,
                        "{\"interval\":60,\"strategy\":\"FIRST\",\"outMsgType\":null,\"maxPendingMsgs\":100,\"maxRetries\":3}")
        );
    }

    /**
     * 辅助方法：{@code getMsgWithLatestTs} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg getMsgWithLatestTs(List<TbMsg> firstMsgPack) {
        int indexOfLastMsgInArray = firstMsgPack.size() - 1;
        int indexToSetMaxTs = new Random().nextInt(indexOfLastMsgInArray) + 1;
        TbMsg currentMaxTsMsg = firstMsgPack.get(indexOfLastMsgInArray);
        TbMsg newLastMsgOfArray = firstMsgPack.get(indexToSetMaxTs);
        firstMsgPack.set(indexOfLastMsgInArray, newLastMsgOfArray);
        firstMsgPack.set(indexToSetMaxTs, currentMaxTsMsg);
        return currentMaxTsMsg;
    }

    /**
     * 辅助方法：{@code getTbMsgs} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private List<TbMsg> getTbMsgs(DeviceId deviceId, int msgCount, long currentTimeMillis, int initTsStep) {
        List<TbMsg> inputMsgs = new ArrayList<>();
        var ts = currentTimeMillis + initTsStep;
        for (int i = 0; i < msgCount; i++) {
            inputMsgs.add(createMsg(deviceId, ts));
            ts += 2;
        }
        return inputMsgs;
    }

    /**
     * 辅助方法：{@code createMsg} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbMsg createMsg(DeviceId deviceId, long ts) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        dataNode.put("deviceId", deviceId.getId().toString());
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("ts", String.valueOf(ts));
        return TbMsg.newMsg(
                DataConstants.MAIN_QUEUE_NAME,
                TbMsgType.POST_TELEMETRY_REQUEST,
                deviceId,
                metaData,
                JacksonUtil.toString(dataNode));
    }

    /**
     * 辅助方法：{@code getMergedData} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private String getMergedData(List<TbMsg> msgs) {
        ArrayNode mergedData = JacksonUtil.newArrayNode();
        msgs.forEach(msg -> {
            ObjectNode msgNode = JacksonUtil.newObjectNode();
            msgNode.set("msg", JacksonUtil.toJsonNode(msg.getData()));
            msgNode.set("metadata", JacksonUtil.valueToTree(msg.getMetaData().getData()));
            mergedData.add(msgNode);
        });
        return JacksonUtil.toString(mergedData);
    }

    /** 实现方法：{@code getTestNode} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
/*
 * 本类总结：{@code TbMsgDeduplicationNodeTest} 为 {@code TbMsgDeduplicationNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
