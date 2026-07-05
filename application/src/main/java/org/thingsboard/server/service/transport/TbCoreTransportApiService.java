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
package org.thingsboard.server.service.transport;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;

/**
 * Created by ashvayka on 05.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`TbCoreTransportApiService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@TbCoreComponent
public class TbCoreTransportApiService {
    /**
     * 队列，用于按场景创建或提供目标对象。
     */
    private final TbCoreQueueFactory tbCoreQueueFactory;
    private final TransportApiService transportApiService;
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final StatsFactory statsFactory;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.transport_api.max_pending_requests:10000}")
    private int maxPendingRequests;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.transport_api.max_requests_timeout:10000}")
    private long requestTimeout;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Value("${queue.transport_api.request_poll_interval:25}")
    private int responsePollDuration;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    @Value("${queue.transport_api.max_callback_threads:100}")
    private int maxCallbackThreads;

    /**
     * 回调，负责处理对应任务或消息。
     */
    private ExecutorService transportCallbackExecutor;
    private TbQueueResponseTemplate<TbProtoQueueMsg<TransportApiRequestMsg>,
            /**
             * 传输层，表示当前对象的对应属性。
             */
            TbProtoQueueMsg<TransportApiResponseMsg>> transportApiTemplate;

    /**
     * 功能：创建 `TbCoreTransportApiService` 实例，并初始化必要字段。
     * 参数：
     * - `tbCoreQueueFactory`：队列名称或队列对象。
     * - `transportApiService`：服务对象。
     * - `statsFactory`：`statsFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbCoreTransportApiService(TbCoreQueueFactory tbCoreQueueFactory, TransportApiService transportApiService, StatsFactory statsFactory) {
        this.tbCoreQueueFactory = tbCoreQueueFactory;
        this.transportApiService = transportApiService;
        this.statsFactory = statsFactory;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.transportCallbackExecutor = ThingsBoardExecutors.newWorkStealingPool(maxCallbackThreads, getClass());
        TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> producer = tbCoreQueueFactory.createTransportApiResponseProducer();
        TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> consumer = tbCoreQueueFactory.createTransportApiRequestConsumer();

        String key = StatsType.TRANSPORT.getName();
        MessagesStats queueStats = statsFactory.createMessagesStats(key);

        DefaultTbQueueResponseTemplate.DefaultTbQueueResponseTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> builder = DefaultTbQueueResponseTemplate.builder();
        builder.requestTemplate(consumer);
        builder.responseTemplate(producer);
        builder.maxPendingRequests(maxPendingRequests);
        builder.requestTimeout(requestTimeout);
        builder.pollInterval(responsePollDuration);
        builder.executor(transportCallbackExecutor);
        builder.handler(transportApiService);
        builder.stats(queueStats);
        transportApiTemplate = builder.build();
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `applicationReadyEvent`：`applicationReadyEvent` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        log.info("Received application ready event. Starting polling for events.");
        transportApiTemplate.init(transportApiService);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (transportApiTemplate != null) {
            transportApiTemplate.stop();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbCoreTransportApiService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
