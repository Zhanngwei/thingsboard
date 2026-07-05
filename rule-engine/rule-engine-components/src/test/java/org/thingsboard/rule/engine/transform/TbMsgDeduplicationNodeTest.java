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
 * `TbMsgDeduplicationNodeTest` 测试类，用于验证 `TbMsgDeduplicationNode` 相关行为。
 */
@Slf4j
public class TbMsgDeduplicationNodeTest extends AbstractRuleNodeUpgradeTest {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final ThingsBoardThreadFactory factory = ThingsBoardThreadFactory.forName("de-duplication-node-test");
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(factory);
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private final int deduplicationInterval = 1;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;

    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbMsgDeduplicationNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgDeduplicationNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    private TbNodeConfiguration nodeConfiguration;

    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private CountDownLatch awaitTellSelfLatch;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
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
     * 功能：执行 `invokeTellSelf` 对应的处理。
     * 参数：
     * - `maxNumberOfInvocation`：`maxNumberOfInvocation` 参数。
     * 返回：无。
     */
    private void invokeTellSelf(int maxNumberOfInvocation) {
        invokeTellSelf(maxNumberOfInvocation, false, 0);
    }

    /**
     * 功能：执行 `invokeTellSelf` 对应的处理。
     * 参数：
     * - `maxNumberOfInvocation`：`maxNumberOfInvocation` 参数。
     * - `delayScheduleTimeout`：`delayScheduleTimeout` 参数。
     * - `delayMultiplier`：`delayMultiplier` 参数。
     * 返回：无。
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
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    public void destroy() {
        executorService.shutdown();
        node.destroy();
    }

    /**
     * 功能：验证 `given_100_messages_strategy_first_then_verifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> given_100_messages_strategy_first_then_verifyOutput() {
        return Stream.of(
                Arguments.of((String) null),
                Arguments.of(DataConstants.MAIN_QUEUE_NAME),
                Arguments.of(DataConstants.HP_QUEUE_NAME)
        );
    }

    /**
     * 功能：验证 `given_100_messages_strategy_first_then_verifyOutput` 描述的测试场景。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    public void given_100_messages_strategy_first_then_verifyOutput(String queueName) throws TbNodeException, ExecutionException, InterruptedException {
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
     * 功能：验证 `given_100_messages_strategy_last_then_verifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void given_100_messages_strategy_last_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
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
     * 功能：验证 `given_100_messages_strategy_all_then_verifyOutput` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void given_100_messages_strategy_all_then_verifyOutput() throws TbNodeException, ExecutionException, InterruptedException {
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
     * 功能：验证 `given_100_messages_strategy_all_then_verifyOutput_2_packs` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void given_100_messages_strategy_all_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
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
     * 功能：验证 `given_100_messages_strategy_last_then_verifyOutput_2_packs` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void given_100_messages_strategy_last_then_verifyOutput_2_packs() throws TbNodeException, ExecutionException, InterruptedException {
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
    /**
     * 功能：验证 `givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig` 描述的测试场景。
     * 参数：无。
     * 返回：处理结果。
     */
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
     * 功能：获取时间戳。
     * 参数：
     * - `firstMsgPack`：待处理消息。
     * 返回：处理结果。
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
     * 功能：获取`Tb Msgs`。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `msgCount`：待处理消息。
     * - `currentTimeMillis`：`currentTimeMillis` 参数。
     * - `initTsStep`：`initTsStep` 参数。
     * 返回：匹配的数据集合。
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
     * 功能：保存或创建消息。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `ts`：时间戳。
     * 返回：处理结果。
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
     * 功能：获取数据。
     * 参数：
     * - `msgs`：待处理消息。
     * 返回：文本结果。
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

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected TbNode getTestNode() {
        return node;
    }

}
/*
 * 本类总结：{@code TbMsgDeduplicationNodeTest} 为 {@code TbMsgDeduplicationNode} 的 消息转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
