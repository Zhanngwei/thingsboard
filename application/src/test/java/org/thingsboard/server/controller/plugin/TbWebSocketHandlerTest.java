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
package org.thingsboard.server.controller.plugin;

import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.adapter.NativeWebSocketSession;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.service.ws.WebSocketSessionRef;

import javax.websocket.RemoteEndpoint;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. 类目的：`TbWebSocketHandlerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@Slf4j
class TbWebSocketHandlerTest {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    TbWebSocketHandler wsHandler;
    NativeWebSocketSession session;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    Session nativeSession;
    RemoteEndpoint.Async asyncRemote;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    WebSocketSessionRef sessionRef;
    int maxMsgQueuePerSession;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    TbWebSocketHandler.SessionMetaData sendHandler;
    ExecutorService executor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() throws IOException {
        maxMsgQueuePerSession = 100;
        executor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        wsHandler = spy(new TbWebSocketHandler());
        willDoNothing().given(wsHandler).close(any(), any());
        session = mock(NativeWebSocketSession.class);
        nativeSession = mock(Session.class);
        willReturn(nativeSession).given(session).getNativeSession(Session.class);
        asyncRemote = mock(RemoteEndpoint.Async.class);
        willReturn(asyncRemote).given(nativeSession).getAsyncRemote();
        sessionRef = mock(WebSocketSessionRef.class, Mockito.RETURNS_DEEP_STUBS); //prevent NPE on logs
        TbWebSocketHandler.SessionMetaData sessionMd = wsHandler.new SessionMetaData(session, sessionRef);
        sessionMd.setMaxMsgQueueSize(maxMsgQueuePerSession);
        sendHandler = spy(sessionMd);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：发送或提交消息。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void sendHandler_sendMsg_parallel_no_race() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession * 2);
        AtomicInteger sendersCount = new AtomicInteger();
        willAnswer(invocation -> {
            assertThat(sendersCount.incrementAndGet()).as("no race").isEqualTo(1);
            String text = invocation.getArgument(0);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                sendersCount.decrementAndGet();
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //first batch
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        Awaitility.await("first batch processed").atMost(30, TimeUnit.SECONDS).until(() -> finishLatch.getCount() == maxMsgQueuePerSession);
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
        //second batch - to test pause between big msg batches
        IntStream.range(100, 100 + maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession * 2)).onResult(any());
        assertThat(sendHandler.isSending.get()).as("sendHandler not is in sending state").isFalse();
    }

    /**
     * 功能：发送或提交消息。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void sendHandler_sendMsg_message_order() throws InterruptedException {
        CountDownLatch finishLatch = new CountDownLatch(maxMsgQueuePerSession);
        Collection<String> outputs = new ConcurrentLinkedQueue<>();
        willAnswer(invocation -> {
            String text = invocation.getArgument(0);
            outputs.add(text);
            SendHandler onResultHandler = invocation.getArgument(1);
            SendResult sendResult = new SendResult();
            executor.submit(() -> {
                onResultHandler.onResult(sendResult);
                finishLatch.countDown();
            });
            return null;
        }).given(asyncRemote).sendText(anyString(), any());

        List<String> inputs = IntStream.range(0, maxMsgQueuePerSession).mapToObj(i -> "msg " + i).collect(Collectors.toList());
        inputs.forEach(s -> sendHandler.sendMsg(s));

        assertThat(finishLatch.await(30, TimeUnit.SECONDS)).as("all callbacks fired").isTrue();
        assertThat(outputs).as("inputs exactly the same as outputs").containsExactlyElementsOf(inputs);

        verify(sendHandler, never()).closeSession(any());
        verify(sendHandler, times(maxMsgQueuePerSession)).onResult(any());
    }

    /**
     * 功能：发送或提交队列。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void sendHandler_sendMsg_queue_size_exceed() {
        willDoNothing().given(asyncRemote).sendText(anyString(), any()); // send text will never call back, so queue will grow each sendMsg
        sendHandler.sendMsg("first message to stay in-flight all the time during this test");
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        verify(sendHandler, never()).closeSession(any());
        sendHandler.sendMsg("excessive message");
        verify(sendHandler, times(1)).closeSession(eq(new CloseStatus(1008, "Max pending updates limit reached!")));
        verify(asyncRemote, times(1)).sendText(anyString(), any());
    }

    /**
     * 功能：发送或提交消息。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void sendHandler_onMsg_allProcessed() throws Exception {
        Deque<String> msgs = new ConcurrentLinkedDeque<>();
        doAnswer(inv -> msgs.add(inv.getArgument(1))).when(wsHandler).processMsg(any(), any());
        for (int i = 0; i < 100; i++) {
            String msg = String.valueOf(i);
            executor.submit(() -> {
                try {
                    Thread.sleep(new Random().nextInt(50));
                    sendHandler.onMsg(msg);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(msgs).map(Integer::parseInt).doesNotHaveDuplicates().hasSize(100);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbWebSocketHandlerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
