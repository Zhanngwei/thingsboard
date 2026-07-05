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
 * 1. 类目的：`TbMsgPackProcessingContextTest` 是ThingsBoard Application 测试模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbMsgPackProcessingContextTest` 在 ThingsBoard Application 测试模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
