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

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `TbMsgPackProcessingContextTest` 是 ThingsBoard Application 中验证 `TbMsgPackProcessingContext` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TbMsgPackProcessingContextTest {

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final int TIMEOUT = 10;
    ExecutorService executorService;

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * 功能：验证`High Concurrency Case`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testHighConcurrencyCase() throws InterruptedException {
        //log.warn("preparing the test...");
        int msgCount = 1000;
        int parallelCount = 5;
        executorService = Executors.newFixedThreadPool(parallelCount, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope"));

        ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> messages = new ConcurrentHashMap<>(msgCount);
        for (int i = 0; i < msgCount; i++) {
            messages.put(UUID.randomUUID(), new TbProtoQueueMsg<>(UUID.randomUUID(), null));
        }
        TbRuleEngineSubmitStrategy strategyMock = mock(TbRuleEngineSubmitStrategy.class);
        when(strategyMock.getPendingMap()).thenReturn(messages);

        TbMsgPackProcessingContext context = new TbMsgPackProcessingContext(DataConstants.MAIN_QUEUE_NAME, strategyMock, false);
        for (UUID uuid : messages.keySet()) {
            final CountDownLatch readyLatch = new CountDownLatch(parallelCount);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch finishLatch = new CountDownLatch(parallelCount);
            for (int i = 0; i < parallelCount; i++) {
                //final String taskName = "" + uuid + " " + i;
                executorService.submit(() -> {
                    //log.warn("ready {}", taskName);
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        Assert.fail("failed to await");
                    }
                    //log.warn("go    {}", taskName);

                    context.onSuccess(uuid);

                    finishLatch.countDown();
                });
            }
            assertTrue(readyLatch.await(TIMEOUT, TimeUnit.SECONDS));
            Thread.yield();
            startLatch.countDown(); //run all-at-once submitted tasks
            assertTrue(finishLatch.await(TIMEOUT, TimeUnit.SECONDS));
        }
        assertTrue(context.await(TIMEOUT, TimeUnit.SECONDS));
        verify(strategyMock, times(msgCount)).onSuccess(any(UUID.class));
    }
}
