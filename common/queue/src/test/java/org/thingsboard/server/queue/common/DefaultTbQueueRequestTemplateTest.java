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
package org.thingsboard.server.queue.common;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.longThat;

/**
 * 中文说明：
 * 1. `DefaultTbQueueRequestTemplateTest` 是 ThingsBoard Common Queue 中验证 `DefaultTbQueueRequestTemplate` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DefaultTbQueueRequestTemplateTest {

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Mock
    TbQueueAdmin queueAdmin;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Mock
    TbQueueProducer<TbQueueMsg> requestTemplate;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Mock
    TbQueueConsumer<TbQueueMsg> responseTemplate;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Mock
    ExecutorService executorMock;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    ExecutorService executor;
    String topic = "js-responses-tb-node-0";
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    long maxRequestTimeout = 10;
    long maxPendingRequests = 32;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    long pollInterval = 5;

    /**
     * `inst` 字段，保存当前对象的对应属性。
     */
    DefaultTbQueueRequestTemplate<TbQueueMsg, TbQueueMsg> inst;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {
        willReturn(topic).given(responseTemplate).getTopic();
        inst = spy(new DefaultTbQueueRequestTemplate<>(
                queueAdmin, requestTemplate, responseTemplate,
                maxRequestTimeout, maxPendingRequests, pollInterval, executorMock));

    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() throws Exception {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：验证 `givenInstance_whenVerifyInitialParameters_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenInstance_whenVerifyInitialParameters_thenOK() {
        assertThat(inst.maxPendingRequests, equalTo(maxPendingRequests));
        assertThat(inst.maxRequestTimeoutNs, equalTo(TimeUnit.MILLISECONDS.toNanos(maxRequestTimeout)));
        assertThat(inst.pollInterval, equalTo(pollInterval));
        assertThat(inst.executor, is(executorMock));
        assertThat(inst.stopped, is(false));
        assertThat(inst.internalExecutor, is(false));
    }

    /**
     * 功能：验证 `givenExternalExecutor_whenInitStop_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenExternalExecutor_whenInitStop_thenOK() {
        inst.init();
        assertThat(inst.nextCleanupNs, equalTo(0L));
        verify(queueAdmin, times(1)).createTopicIfNotExists(topic);
        verify(requestTemplate, times(1)).init();
        verify(responseTemplate, times(1)).subscribe();
        verify(executorMock, times(1)).submit(any(Runnable.class));

        inst.stop();
        assertThat(inst.stopped, is(true));
        verify(responseTemplate, times(1)).unsubscribe();
        verify(requestTemplate, times(1)).stop();
        verify(executorMock, never()).shutdownNow();
    }

    /**
     * 功能：验证 `givenMainLoop_whenLoopFewTimes_thenVerifyInvocationCount` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMainLoop_whenLoopFewTimes_thenVerifyInvocationCount() throws InterruptedException {
        executor = inst.createExecutor();
        CountDownLatch latch = new CountDownLatch(5);
        willDoNothing().given(inst).sleep(anyLong());
        willAnswer(invocation -> {
            if (latch.getCount() == 1) {
                inst.stop(); //stop the loop in natural way
            }
            if (latch.getCount() == 3 || latch.getCount() == 4) {
                latch.countDown();
                throw new RuntimeException("test catch block");
            }
            latch.countDown();
            return null;
        }).given(inst).fetchAndProcessResponses();

        executor.submit(inst::mainLoop);
        latch.await(10, TimeUnit.SECONDS);

        verify(inst, times(5)).fetchAndProcessResponses();
        verify(inst, times(2)).sleep(longThat(lessThan(TimeUnit.MILLISECONDS.toNanos(inst.pollInterval))));
    }

    /**
     * 功能：验证 `givenMessages_whenSend_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMessages_whenSend_thenOK() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any());
        inst.init();
        final int msgCount = 10;
        for (int i = 0; i < msgCount; i++) {
            inst.send(getRequestMsgMock());
        }
        assertThat(inst.pendingRequests.mappingCount(), equalTo((long) msgCount));
        verify(inst, times(msgCount)).sendToRequestTemplate(any(), any(), any(), any());
    }

    /**
     * 功能：验证 `givenMessagesOverMaxPendingRequests_whenSend_thenImmediateFailedFutureForTheOfRequests` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMessagesOverMaxPendingRequests_whenSend_thenImmediateFailedFutureForTheOfRequests() {
        willDoNothing().given(inst).sendToRequestTemplate(any(), any(), any(), any());
        inst.init();
        int msgOverflowCount = 10;
        for (int i = 0; i < inst.maxPendingRequests; i++) {
            assertThat(inst.send(getRequestMsgMock()).isDone(), is(false)); //SettableFuture future - pending only
        }
        for (int i = 0; i < msgOverflowCount; i++) {
            assertThat("max pending requests overflow", inst.send(getRequestMsgMock()).isDone(), is(true)); //overflow, immediate failed future
        }
        assertThat(inst.pendingRequests.mappingCount(), equalTo(inst.maxPendingRequests));
        verify(inst, times((int) inst.maxPendingRequests)).sendToRequestTemplate(any(), any(), any(), any());
    }

    /**
     * 功能：验证 `givenNothing_whenSendAndFetchAndProcessResponsesWithTimeout_thenFail` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void givenNothing_whenSendAndFetchAndProcessResponsesWithTimeout_thenFail() {
        //given
        AtomicLong currentTime = new AtomicLong();
        willAnswer(x -> {
            log.info("currentTime={}", currentTime.get());
            return currentTime.get();
        }).given(inst).getCurrentClockNs();
        inst.init();
        inst.setupNextCleanup();
        willReturn(Collections.emptyList()).given(inst).doPoll();

        //when
        long stepNs = TimeUnit.MILLISECONDS.toNanos(1);
        for (long i = 0; i <= inst.maxRequestTimeoutNs * 2; i = i + stepNs) {
            currentTime.addAndGet(stepNs);
            assertThat(inst.send(getRequestMsgMock()).isDone(), is(false)); //SettableFuture future - pending only
            if (i % (inst.maxRequestTimeoutNs * 3 / 2) == 0) {
                inst.fetchAndProcessResponses();
            }
        }

        //then
        ArgumentCaptor<DefaultTbQueueRequestTemplate.ResponseMetaData> argumentCaptorResp = ArgumentCaptor.forClass(DefaultTbQueueRequestTemplate.ResponseMetaData.class);
        ArgumentCaptor<UUID> argumentCaptorUUID = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<Long> argumentCaptorLong = ArgumentCaptor.forClass(Long.class);
        verify(inst, atLeastOnce()).setTimeoutException(argumentCaptorUUID.capture(), argumentCaptorResp.capture(), argumentCaptorLong.capture());

        List<DefaultTbQueueRequestTemplate.ResponseMetaData> responseMetaDataList = argumentCaptorResp.getAllValues();
        List<Long> tickTsList = argumentCaptorLong.getAllValues();
        for (int i = 0; i < responseMetaDataList.size(); i++) {
            assertThat("tickTs >= calculatedExpTime", tickTsList.get(i), greaterThanOrEqualTo(responseMetaDataList.get(i).getSubmitTime() + responseMetaDataList.get(i).getTimeout()));
        }
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    TbQueueMsg getRequestMsgMock() {
        return mock(TbQueueMsg.class, RETURNS_DEEP_STUBS);
    }
}
