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

@Slf4j
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
class TbWebSocketHandlerTest {

    /**
     * 字段说明：
     * 1. 保存 `wsHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    TbWebSocketHandler wsHandler;
    NativeWebSocketSession session;
    /**
     * 字段说明：
     * 1. 保存 `nativeSession` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    Session nativeSession;
    RemoteEndpoint.Async asyncRemote;
    /**
     * 字段说明：
     * 1. 保存 `sessionRef` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    WebSocketSessionRef sessionRef;
    int maxMsgQueuePerSession;
    /**
     * 字段说明：
     * 1. 保存 `sendHandler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    TbWebSocketHandler.SessionMetaData sendHandler;
    ExecutorService executor;

    @BeforeEach
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void setUp() throws IOException {
        maxMsgQueuePerSession = 100;
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
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

    @AfterEach
    /**
     * 方法说明：
     * 1. 职责：执行 `tearDown` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void tearDown() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `sendHandler_sendMsg_parallel_no_race` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `sendHandler_sendMsg_message_order` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `sendHandler_sendMsg_queue_size_exceed` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void sendHandler_sendMsg_queue_size_exceed() {
        willDoNothing().given(asyncRemote).sendText(anyString(), any()); // send text will never call back, so queue will grow each sendMsg
        sendHandler.sendMsg("first message to stay in-flight all the time during this test");
        IntStream.range(0, maxMsgQueuePerSession).parallel().forEach(i -> sendHandler.sendMsg("hello " + i));
        verify(sendHandler, never()).closeSession(any());
        sendHandler.sendMsg("excessive message");
        verify(sendHandler, times(1)).closeSession(eq(new CloseStatus(1008, "Max pending updates limit reached!")));
        verify(asyncRemote, times(1)).sendText(anyString(), any());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `sendHandler_onMsg_allProcessed` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void sendHandler_onMsg_allProcessed() throws Exception {
        Deque<String> msgs = new ConcurrentLinkedDeque<>();
        doAnswer(inv -> msgs.add(inv.getArgument(1))).when(wsHandler).processMsg(any(), any());
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            String msg = String.valueOf(i);
            executor.submit(() -> {
                try {
                    Thread.sleep(new Random().nextInt(50));
                    sendHandler.onMsg(msg);
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
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
