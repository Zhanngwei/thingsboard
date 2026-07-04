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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbQueueResponseTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class DefaultTbQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueResponseTemplate<Request, Response> {

    /**
     * 字段说明：
     * 1. 保存 `requestTemplate` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbQueueConsumer<Request> requestTemplate;
    private final TbQueueProducer<Response> responseTemplate;
    /**
     * 字段说明：
     * 1. 保存 `pendingRequests` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final ConcurrentMap<UUID, String> pendingRequests;
    private final ExecutorService loopExecutor;
    /**
     * 字段说明：
     * 1. 保存 `timeoutExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final ScheduledExecutorService timeoutExecutor;
    private final ExecutorService callbackExecutor;
    /**
     * 字段说明：
     * 1. 保存 `stats` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final MessagesStats stats;
    private final int maxPendingRequests;
    /**
     * 字段说明：
     * 1. 保存 `requestTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final long requestTimeout;

    /**
     * 字段说明：
     * 1. 保存 `pollInterval` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final long pollInterval;
    private volatile boolean stopped = false;
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    @Builder
    /**
     * 方法说明：
     * 1. 职责：执行 `DefaultTbQueueResponseTemplate` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DefaultTbQueueResponseTemplate(TbQueueConsumer<Request> requestTemplate,
                                          TbQueueProducer<Response> responseTemplate,
                                          TbQueueHandler<Request, Response> handler,
                                          long pollInterval,
                                          long requestTimeout,
                                          int maxPendingRequests,
                                          ExecutorService executor,
                                          MessagesStats stats) {
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.requestTimeout = requestTimeout;
        this.callbackExecutor = executor;
        this.stats = stats;
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-queue-response-template-timeout-" + requestTemplate.getTopic()));
        this.loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-queue-response-template-loop-" + requestTemplate.getTopic()));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init(TbQueueHandler<Request, Response> handler) {
        this.responseTemplate.init();
        requestTemplate.subscribe();
        loopExecutor.submit(() -> {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            while (!stopped) {
                try {
                    // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
                    while (pendingRequestCount.get() >= maxPendingRequests) {
                        try {
                            Thread.sleep(pollInterval);
                        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                        } catch (InterruptedException e) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e);
                        }
                    }
                    List<Request> requests = requestTemplate.poll(pollInterval);

                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (requests.isEmpty()) {
                        continue;
                    }

                    requests.forEach(request -> {
                        long currentTime = System.currentTimeMillis();
                        long expireTs = bytesToLong(request.getHeaders().get(EXPIRE_TS_HEADER));
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (expireTs >= currentTime) {
                            byte[] requestIdHeader = request.getHeaders().get(REQUEST_ID_HEADER);
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (requestIdHeader == null) {
                                log.error("[{}] Missing requestId in header", request);
                                return;
                            }
                            byte[] responseTopicHeader = request.getHeaders().get(RESPONSE_TOPIC_HEADER);
                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                            if (responseTopicHeader == null) {
                                log.error("[{}] Missing response topic in header", request);
                                return;
                            }
                            UUID requestId = bytesToUuid(requestIdHeader);
                            String responseTopic = bytesToString(responseTopicHeader);
                            try {
                                pendingRequestCount.getAndIncrement();
                                stats.incrementTotal();
                                AsyncCallbackTemplate.withCallbackAndTimeout(handler.handle(request),
                                        response -> {
                                            pendingRequestCount.decrementAndGet();
                                            response.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
                                            responseTemplate.send(TopicPartitionInfo.builder().topic(responseTopic).build(), response, null);
                                            stats.incrementSuccessful();
                                        },
                                        e -> {
                                            pendingRequestCount.decrementAndGet();
                                            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                                            if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                                                log.warn("[{}] Timeout to process the request: {}", requestId, request, e);
                                            } else {
                                                log.trace("[{}] Failed to process the request: {}", requestId, request, e);
                                            }
                                            stats.incrementFailed();
                                        },
                                        requestTimeout,
                                        timeoutExecutor,
                                        callbackExecutor);
                            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                            } catch (Throwable e) {
                                pendingRequestCount.decrementAndGet();
                                log.warn("[{}] Failed to process the request: {}", requestId, request, e);
                                stats.incrementFailed();
                            }
                        }
                    });
                    requestTemplate.commit();
                // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                } catch (Throwable e) {
                    log.warn("Failed to obtain messages from queue.", e);
                    try {
                        Thread.sleep(pollInterval);
                    // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop() {
        stopped = true;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (requestTemplate != null) {
            requestTemplate.unsubscribe();
        }
        if (responseTemplate != null) {
            responseTemplate.stop();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbQueueResponseTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
