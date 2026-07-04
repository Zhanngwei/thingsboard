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
package org.thingsboard.server.service.subscription;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.ComparisonTsValue;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggKey;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AggTimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.GetTsCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.TimeSeriesCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
@TbCoreComponent
@Service
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbEntityDataSubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultTbEntityDataSubscriptionService implements TbEntityDataSubscriptionService {

    /**
     * 字段说明：
     * 1. 保存 `DEFAULT_LIMIT` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int DEFAULT_LIMIT = 100;
    private final Map<String, Map<Integer, TbAbstractSubCtx>> subscriptionsBySessionId = new ConcurrentHashMap<>();

    @Autowired @Lazy
    /**
     * 字段说明：
     * 1. 保存 `wsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private WebSocketService wsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EntityService entityService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `alarmService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AlarmService alarmService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AttributesService attributesService;

    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `localSubscriptionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbLocalSubscriptionService localSubscriptionService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesService tsService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `serviceInfoProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbServiceInfoProvider serviceInfoProvider;

    @Autowired
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `dbCallbackExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DbCallbackExecutorService dbCallbackExecutor;

    /**
     * 字段说明：
     * 1. 保存 `scheduler` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ScheduledExecutorService scheduler;

    @Value("${database.ts.type}")
    /**
     * 字段说明：
     * 1. 保存 `databaseTsType` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String databaseTsType;
    @Value("${server.ws.dynamic_page_link.refresh_interval:6}")
    /**
     * 字段说明：
     * 1. 保存 `dynamicPageLinkRefreshInterval` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long dynamicPageLinkRefreshInterval;
    @Value("${server.ws.dynamic_page_link.refresh_pool_size:1}")
    /**
     * 字段说明：
     * 1. 保存 `dynamicPageLinkRefreshPoolSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int dynamicPageLinkRefreshPoolSize;
    @Value("${server.ws.max_entities_per_data_subscription:1000}")
    /**
     * 字段说明：
     * 1. 保存 `maxEntitiesPerDataSubscription` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxEntitiesPerDataSubscription;
    @Value("${server.ws.max_entities_per_alarm_subscription:1000}")
    /**
     * 字段说明：
     * 1. 保存 `maxEntitiesPerAlarmSubscription` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxEntitiesPerAlarmSubscription;
    @Value("${server.ws.dynamic_page_link.max_alarm_queries_per_refresh_interval:10}")
    /**
     * 字段说明：
     * 1. 保存 `maxAlarmQueriesPerRefreshInterval` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxAlarmQueriesPerRefreshInterval;
    @Value("${ui.dashboard.max_datapoints_limit:50000}")
    /**
     * 字段说明：
     * 1. 保存 `maxDatapointLimit` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxDatapointLimit;

    /**
     * 字段说明：
     * 1. 保存 `wsCallBackExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExecutorService wsCallBackExecutor;
    private boolean tsInSqlDB;
    /**
     * 字段说明：
     * 1. 保存 `serviceId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String serviceId;
    private SubscriptionServiceStatistics stats = new SubscriptionServiceStatistics();

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `initExecutor` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        wsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ws-entity-sub-callback"));
        tsInSqlDB = databaseTsType.equalsIgnoreCase("sql") || databaseTsType.equalsIgnoreCase("timescale");
        ThreadFactory tbThreadFactory = ThingsBoardThreadFactory.forName("ws-entity-sub-scheduler");
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (dynamicPageLinkRefreshPoolSize == 1) {
            scheduler = Executors.newSingleThreadScheduledExecutor(tbThreadFactory);
        } else {
            scheduler = Executors.newScheduledThreadPool(dynamicPageLinkRefreshPoolSize, tbThreadFactory);
        }
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `shutdownExecutor` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shutdownExecutor() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (wsCallBackExecutor != null) {
            wsCallBackExecutor.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleCmd(WebSocketSessionRef session, EntityDataCmd cmd) {
        TbEntityDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ctx != null) {
            log.debug("[{}][{}] Updating existing subscriptions using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (cmd.hasAnyCmd()) {
                ctx.clearEntitySubscriptions();
            }
        } else {
            log.debug("[{}][{}] Creating new subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setCurrentCmd(cmd);

        // Fetch entity list using entity data query
        if (cmd.getQuery() != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ctx.getQuery() == null) {
                log.debug("[{}][{}] Initializing data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            } else {
                log.debug("[{}][{}] Updating data using query: {}", session.getSessionId(), cmd.getCmdId(), cmd.getQuery());
            }
            ctx.setAndResolveQuery(cmd.getQuery());
            EntityDataQuery query = ctx.getQuery();
            //Step 1. Update existing query with the contents of LatestValueCmd
            if (cmd.getLatestCmd() != null) {
                cmd.getLatestCmd().getKeys().forEach(key -> {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!query.getLatestValues().contains(key)) {
                        query.getLatestValues().add(key);
                    }
                });
            }
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            ctx.cancelTasks();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (ctx.getQuery().getPageLink().isDynamic()) {
                //TODO: validate number of dynamic page links against rate limits. Ignore dynamic flag if limit is reached.
                TbEntityDataSubCtx finalCtx = ctx;
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshDynamicQuery(finalCtx),
                        dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }

        try {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            List<ListenableFuture<?>> cmdFutures = new ArrayList<>();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (cmd.getAggHistoryCmd() != null) {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                cmdFutures.add(handleAggHistoryCmd(ctx, cmd.getAggHistoryCmd()));
            }
            if (cmd.getAggTsCmd() != null) {
                cmdFutures.add(handleAggTsCmd(ctx, cmd.getAggTsCmd()));
            }
            if (cmd.getHistoryCmd() != null) {
                cmdFutures.add(handleHistoryCmd(ctx, cmd.getHistoryCmd()));
            }
            if (cmdFutures.isEmpty()) {
                handleRegularCommands(ctx, cmd);
            } else {
                TbEntityDataSubCtx finalCtx = ctx;
                Futures.addCallback(Futures.allAsList(cmdFutures), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable List<Object> result) {
                        handleRegularCommands(finalCtx, cmd);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}][{}] Failed to process command", finalCtx.getSessionId(), finalCtx.getCmdId());
                    }
                }, wsCallBackExecutor);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleRegularCommands` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleRegularCommands(TbEntityDataSubCtx ctx, EntityDataCmd cmd) {
        try {
            if (cmd.getLatestCmd() != null || cmd.getTsCmd() != null) {
                if (cmd.getLatestCmd() != null) {
                    handleLatestCmd(ctx, cmd.getLatestCmd());
                }
                if (cmd.getTsCmd() != null) {
                    handleTimeSeriesCmd(ctx, cmd.getTsCmd());
                }
            } else {
                checkAndSendInitialData(ctx);
            }
        } catch (RuntimeException e) {
            handleWsCmdRuntimeException(ctx.getSessionId(), e, cmd);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkAndSendInitialData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void checkAndSendInitialData(@Nullable TbEntityDataSubCtx theCtx) {
        if (!theCtx.isInitialDataSent()) {
            EntityDataUpdate update = new EntityDataUpdate(theCtx.getCmdId(), theCtx.getData(), null, theCtx.getMaxEntitiesPerDataSubscription());
            theCtx.sendWsMsg(update);
            theCtx.setInitialDataSent(true);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAggHistoryCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleAggHistoryCmd(TbEntityDataSubCtx ctx, AggHistoryCmd cmd) {
        ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (AggKey key : cmd.getKeys()) {
            if (key.getPreviousValueOnly() == null || !key.getPreviousValueOnly()) {
                var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getEndTs(), cmd.getEndTs() - cmd.getStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
            }
            if (key.getPreviousStartTs() != null && key.getPreviousEndTs() != null && key.getPreviousEndTs() >= key.getPreviousStartTs()) {
                var query = new BaseReadTsKvQuery(key.getKey(), key.getPreviousStartTs(), key.getPreviousEndTs(), key.getPreviousEndTs() - key.getPreviousStartTs(), 1, key.getAgg());
                queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, true));
            }
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getEndTs(), false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAggTsCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleAggTsCmd(TbEntityDataSubCtx ctx, AggTimeSeriesCmd cmd) {
        ConcurrentMap<Integer, ReadTsKvQueryInfo> queries = new ConcurrentHashMap<>();
        for (AggKey key : cmd.getKeys()) {
            var query = new BaseReadTsKvQuery(key.getKey(), cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), cmd.getTimeWindow(), 1, key.getAgg());
            queries.put(query.getId(), new ReadTsKvQueryInfo(key, query, false));
        }
        return handleAggCmd(ctx, cmd.getKeys(), queries, cmd.getStartTs(), cmd.getStartTs() + cmd.getTimeWindow(), true);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleAggCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleAggCmd(TbEntityDataSubCtx ctx, List<AggKey> keys, ConcurrentMap<Integer, ReadTsKvQueryInfo> queries,
                                                              long startTs, long endTs, boolean subscribe) {
        Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        List<ReadTsKvQuery> queryList = queries.values().stream().map(ReadTsKvQueryInfo::getQuery).collect(Collectors.toList());
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), queryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (ReadTsKvQueryResult queryResult : queryResults) {
                            ReadTsKvQueryInfo queryInfo = queries.get(queryResult.getQueryId());
                            ComparisonTsValue comparisonTsValue = entityData.getAggLatest().computeIfAbsent(queryInfo.getKey().getId(), agg -> new ComparisonTsValue());
                            if (queryInfo.isPrevious()) {
                                comparisonTsValue.setPrevious(queryResult.toTsValue(queryInfo.getQuery()));
                            } else {
                                comparisonTsValue.setCurrent(queryResult.toTsValue(queryInfo.getQuery()));
                                lastTsMap.put(queryInfo.getQuery().getKey(), queryResult.getLastEntryTs());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        entityData.getAggLatest().putIfAbsent(key.getId(), new ComparisonTsValue(TsValue.EMPTY, TsValue.EMPTY));
                    });
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, startTs, endTs, true);
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleWsCmdRuntimeException` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleWsCmdRuntimeException(String sessionId, RuntimeException e, EntityDataCmd cmd) {
        log.debug("[{}] Failed to process ws cmd: {}", sessionId, cmd, e);
        wsService.close(sessionId, CloseStatus.SERVICE_RESTARTED);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleCmd(WebSocketSessionRef session, EntityCountCmd cmd) {
        TbEntityCountSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getRegularQueryInvocationCnt().incrementAndGet();
            stats.getRegularQueryTimeSpent().addAndGet(end - start);
            TbEntityCountSubCtx finalCtx = ctx;
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> refreshDynamicQuery(finalCtx),
                    dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
            finalCtx.setRefreshTask(task);
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleCmd(WebSocketSessionRef session, AlarmDataCmd cmd) {
        TbAlarmDataSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            log.debug("[{}][{}] Creating new alarm subscription using: {}", session.getSessionId(), cmd.getCmdId(), cmd);
            ctx = createSubCtx(session, cmd);
        }
        ctx.setAndResolveQuery(cmd.getQuery());
        AlarmDataQuery adq = ctx.getQuery();
        long start = System.currentTimeMillis();
        ctx.fetchData();
        long end = System.currentTimeMillis();
        stats.getRegularQueryInvocationCnt().incrementAndGet();
        stats.getRegularQueryTimeSpent().addAndGet(end - start);
        List<EntityData> entities = ctx.getEntitiesData();
        ctx.cancelTasks();
        ctx.clearEntitySubscriptions();
        if (entities.isEmpty()) {
            AlarmDataUpdate update = new AlarmDataUpdate(cmd.getCmdId(), new PageData<>(), null, 0, 0);
            ctx.sendWsMsg(update);
        } else {
            ctx.fetchAlarms();
            ctx.createLatestValuesSubscriptions(cmd.getQuery().getLatestValues());
            if (adq.getPageLink().getTimeWindow() > 0) {
                TbAlarmDataSubCtx finalCtx = ctx;
                ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                        () -> refreshAlarmQuery(finalCtx), dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
                finalCtx.setRefreshTask(task);
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void handleCmd(WebSocketSessionRef session, AlarmCountCmd cmd) {
        TbAlarmCountSubCtx ctx = getSubCtx(session.getSessionId(), cmd.getCmdId());
        if (ctx == null) {
            ctx = createSubCtx(session, cmd);
            long start = System.currentTimeMillis();
            ctx.fetchData();
            long end = System.currentTimeMillis();
            stats.getAlarmQueryInvocationCnt().incrementAndGet();
            stats.getAlarmQueryTimeSpent().addAndGet(end - start);
            TbAlarmCountSubCtx finalCtx = ctx;
            ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(
                    () -> refreshDynamicQuery(finalCtx),
                    dynamicPageLinkRefreshInterval, dynamicPageLinkRefreshInterval, TimeUnit.SECONDS);
            finalCtx.setRefreshTask(task);
        } else {
            log.debug("[{}][{}] Received duplicate command: {}", session.getSessionId(), cmd.getCmdId(), cmd);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean validate(TbAbstractSubCtx<?> finalCtx) {
        if (finalCtx.isStopped()) {
            log.warn("[{}][{}][{}] Received validation task for already stopped context.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        var cmdMap = subscriptionsBySessionId.get(finalCtx.getSessionId());
        if (cmdMap == null) {
            log.warn("[{}][{}][{}] Received validation task for already removed session.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        } else if (!cmdMap.containsKey(finalCtx.getCmdId())) {
            log.warn("[{}][{}][{}] Received validation task for unregistered cmdId.", finalCtx.getTenantId(), finalCtx.getSessionId(), finalCtx.getCmdId());
            return false;
        }
        return true;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `refreshDynamicQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void refreshDynamicQuery(TbAbstractSubCtx<?> finalCtx) {
        try {
            if (validate(finalCtx)) {
                long start = System.currentTimeMillis();
                finalCtx.update();
                long end = System.currentTimeMillis();
                log.trace("[{}][{}] Executing query: {}", finalCtx.getSessionId(), finalCtx.getCmdId(), finalCtx.getQuery());
                stats.getDynamicQueryInvocationCnt().incrementAndGet();
                stats.getDynamicQueryTimeSpent().addAndGet(end - start);
            } else {
                finalCtx.stop();
            }
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to refresh query", finalCtx.getSessionId(), finalCtx.getCmdId(), e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `refreshAlarmQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void refreshAlarmQuery(TbAlarmDataSubCtx finalCtx) {
        if (validate(finalCtx)) {
            finalCtx.checkAndResetInvocationCounter();
        } else {
            finalCtx.stop();
        }
    }

    @Scheduled(fixedDelayString = "${server.ws.dynamic_page_link.stats:10000}")
    /**
     * 方法说明：
     * 1. 职责：执行 `printStats` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void printStats() {
        int alarmQueryInvocationCntValue = stats.getAlarmQueryInvocationCnt().getAndSet(0);
        long alarmQueryInvocationTimeValue = stats.getAlarmQueryTimeSpent().getAndSet(0);
        int regularQueryInvocationCntValue = stats.getRegularQueryInvocationCnt().getAndSet(0);
        long regularQueryInvocationTimeValue = stats.getRegularQueryTimeSpent().getAndSet(0);
        int dynamicQueryInvocationCntValue = stats.getDynamicQueryInvocationCnt().getAndSet(0);
        long dynamicQueryInvocationTimeValue = stats.getDynamicQueryTimeSpent().getAndSet(0);
        long dynamicQueryCnt = subscriptionsBySessionId.values().stream().mapToLong(m -> m.values().stream().filter(TbAbstractSubCtx::isDynamic).count()).sum();
        if (regularQueryInvocationCntValue > 0 || dynamicQueryInvocationCntValue > 0 || dynamicQueryCnt > 0 || alarmQueryInvocationCntValue > 0) {
            log.info("Stats: regularQueryInvocationCnt = [{}], regularQueryInvocationTime = [{}], " +
                            "dynamicQueryCnt = [{}] dynamicQueryInvocationCnt = [{}], dynamicQueryInvocationTime = [{}], " +
                            "alarmQueryInvocationCnt = [{}], alarmQueryInvocationTime = [{}]",
                    regularQueryInvocationCntValue, regularQueryInvocationTimeValue,
                    dynamicQueryCnt, dynamicQueryInvocationCntValue, dynamicQueryInvocationTimeValue,
                    alarmQueryInvocationCntValue, alarmQueryInvocationTimeValue);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSubCtx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbEntityDataSubCtx createSubCtx(WebSocketSessionRef sessionRef, EntityDataCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbEntityDataSubCtx ctx = new TbEntityDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, sessionRef, cmd.getCmdId(), maxEntitiesPerDataSubscription);
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSubCtx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbEntityCountSubCtx createSubCtx(WebSocketSessionRef sessionRef, EntityCountCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbEntityCountSubCtx ctx = new TbEntityCountSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, sessionRef, cmd.getCmdId());
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `createSubCtx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbAlarmDataSubCtx createSubCtx(WebSocketSessionRef sessionRef, AlarmDataCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbAlarmDataSubCtx ctx = new TbAlarmDataSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, alarmService, sessionRef, cmd.getCmdId(), maxEntitiesPerAlarmSubscription,
                maxAlarmQueriesPerRefreshInterval);
        ctx.setAndResolveQuery(cmd.getQuery());
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSubCtx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbAlarmCountSubCtx createSubCtx(WebSocketSessionRef sessionRef, AlarmCountCmd cmd) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.computeIfAbsent(sessionRef.getSessionId(), k -> new HashMap<>());
        TbAlarmCountSubCtx ctx = new TbAlarmCountSubCtx(serviceId, wsService, entityService, localSubscriptionService,
                attributesService, stats, alarmService, sessionRef, cmd.getCmdId());
        if (cmd.getQuery() != null) {
            ctx.setAndResolveQuery(cmd.getQuery());
        }
        sessionSubs.put(cmd.getCmdId(), ctx);
        return ctx;
    }

    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `getSubCtx` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T extends TbAbstractSubCtx> T getSubCtx(String sessionId, int cmdId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(sessionId);
        if (sessionSubs != null) {
            return (T) sessionSubs.get(cmdId);
        } else {
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleTimeSeriesCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleTimeSeriesCmd(TbEntityDataSubCtx ctx, TimeSeriesCmd cmd) {
        log.debug("[{}][{}] Fetching time-series data for last {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getTimeWindow(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, true);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `handleHistoryCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleHistoryCmd(TbEntityDataSubCtx ctx, EntityHistoryCmd cmd) {
        log.debug("[{}][{}] Fetching history data for start {} and end {} ms for keys: ({})", ctx.getSessionId(), ctx.getCmdId(), cmd.getStartTs(), cmd.getEndTs(), cmd.getKeys());
        return handleGetTsCmd(ctx, cmd, false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleGetTsCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<TbEntityDataSubCtx> handleGetTsCmd(TbEntityDataSubCtx ctx, GetTsCmd cmd, boolean subscribe) {
        Map<Integer, String> queriesKeys = new ConcurrentHashMap<>();

        List<String> keys = cmd.getKeys();
        List<ReadTsKvQuery> finalTsKvQueryList;
        List<ReadTsKvQuery> tsKvQueryList = keys.stream().map(key -> {
            var query = new BaseReadTsKvQuery(key, cmd.getStartTs(), cmd.getEndTs(), cmd.toAggregationParams(), getLimit(cmd.getLimit()));
            queriesKeys.put(query.getId(), query.getKey());
            return query;
        }).collect(Collectors.toList());
        if (cmd.isFetchLatestPreviousPoint()) {
            finalTsKvQueryList = new ArrayList<>(tsKvQueryList);
            finalTsKvQueryList.addAll(keys.stream().map(key -> {
                        var query = new BaseReadTsKvQuery(key, cmd.getStartTs() - TimeUnit.DAYS.toMillis(365), cmd.getStartTs(), cmd.toAggregationParams(), 1);
                        queriesKeys.put(query.getId(), query.getKey());
                        return query;
                    }
            ).collect(Collectors.toList()));
        } else {
            finalTsKvQueryList = tsKvQueryList;
        }
        Map<EntityData, ListenableFuture<List<ReadTsKvQueryResult>>> fetchResultMap = new HashMap<>();
        List<EntityData> entityDataList = ctx.getData().getData();
        entityDataList.forEach(entityData -> fetchResultMap.put(entityData,
                tsService.findAllByQueries(ctx.getTenantId(), entityData.getEntityId(), finalTsKvQueryList)));
        return Futures.transform(Futures.allAsList(fetchResultMap.values()), f -> {
            // Map that holds last ts for each key for each entity.
            Map<EntityData, Map<String, Long>> lastTsEntityMap = new HashMap<>();
            fetchResultMap.forEach((entityData, future) -> {
                try {
                    Map<String, Long> lastTsMap = new HashMap<>();
                    lastTsEntityMap.put(entityData, lastTsMap);

                    List<ReadTsKvQueryResult> queryResults = future.get();
                    if (queryResults != null) {
                        for (ReadTsKvQueryResult queryResult : queryResults) {
                            String queryKey = queriesKeys.get(queryResult.getQueryId());
                            if (queryKey != null) {
                                entityData.getTimeseries().merge(queryKey, queryResult.toTsValues(), ArrayUtils::addAll);
                                lastTsMap.merge(queryKey, queryResult.getLastEntryTs(), Math::max);
                            } else {
                                log.warn("ReadTsKvQueryResult for {} {} has queryId not matching the initial query",
                                        entityData.getEntityId().getEntityType(), entityData.getEntityId());
                            }
                        }
                    }
                    // Populate with empty values if no data found.
                    keys.forEach(key -> {
                        if (!entityData.getTimeseries().containsKey(key)) {
                            entityData.getTimeseries().put(key, new TsValue[0]);
                        }
                    });

                    if (cmd.isFetchLatestPreviousPoint()) {
                        entityData.getTimeseries().values().forEach(dataArray -> Arrays.sort(dataArray, (o1, o2) -> Long.compare(o2.getTs(), o1.getTs())));
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.warn("[{}][{}][{}] Failed to fetch historical data", ctx.getSessionId(), ctx.getCmdId(), entityData.getEntityId(), e);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to fetch historical data!"));
                }
            });
            ctx.getWsLock().lock();
            try {
                EntityDataUpdate update;
                if (!ctx.isInitialDataSent()) {
                    update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                    ctx.setInitialDataSent(true);
                } else {
                    update = new EntityDataUpdate(ctx.getCmdId(), null, entityDataList, ctx.getMaxEntitiesPerDataSubscription());
                }
                if (subscribe) {
                    ctx.createTimeSeriesSubscriptions(lastTsEntityMap, cmd.getStartTs(), cmd.getEndTs());
                }
                ctx.sendWsMsg(update);
                entityDataList.forEach(EntityData::clearTsAndAggData);
            } finally {
                ctx.getWsLock().unlock();
            }
            return ctx;
        }, wsCallBackExecutor);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleLatestCmd` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleLatestCmd(TbEntityDataSubCtx ctx, LatestValueCmd latestCmd) {
        log.trace("[{}][{}] Going to process latest command: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
        //Fetch the latest values for telemetry keys in case they are not copied from NoSQL to SQL DB in hybrid mode.
        if (!tsInSqlDB) {
            log.trace("[{}][{}] Going to fetch missing latest values: {}", ctx.getSessionId(), ctx.getCmdId(), latestCmd);
            List<String> allTsKeys = latestCmd.getKeys().stream()
                    .filter(key -> key.getType().equals(EntityKeyType.TIME_SERIES))
                    .map(EntityKey::getKey).collect(Collectors.toList());

            Map<EntityData, ListenableFuture<Map<String, TsValue>>> missingTelemetryFutures = new HashMap<>();
            for (EntityData entityData : ctx.getData().getData()) {
                Map<EntityKeyType, Map<String, TsValue>> latestEntityData = entityData.getLatest();
                Map<String, TsValue> tsEntityData = latestEntityData.get(EntityKeyType.TIME_SERIES);
                Set<String> missingTsKeys = new LinkedHashSet<>(allTsKeys);
                if (tsEntityData != null) {
                    missingTsKeys.removeAll(tsEntityData.keySet());
                } else {
                    tsEntityData = new HashMap<>();
                    latestEntityData.put(EntityKeyType.TIME_SERIES, tsEntityData);
                }

                ListenableFuture<List<TsKvEntry>> missingTsData = tsService.findLatest(ctx.getTenantId(), entityData.getEntityId(), missingTsKeys);
                missingTelemetryFutures.put(entityData, Futures.transform(missingTsData, this::toTsValue, MoreExecutors.directExecutor()));
            }
            Futures.addCallback(Futures.allAsList(missingTelemetryFutures.values()), new FutureCallback<>() {
                @Override
                public void onSuccess(@Nullable List<Map<String, TsValue>> result) {
                    missingTelemetryFutures.forEach((key, value) -> {
                        try {
                            key.getLatest().get(EntityKeyType.TIME_SERIES).putAll(value.get());
                        } catch (InterruptedException | ExecutionException e) {
                            log.warn("[{}][{}] Failed to lookup latest telemetry: {}:{}", ctx.getSessionId(), ctx.getCmdId(), key.getEntityId(), allTsKeys, e);
                        }
                    });
                    EntityDataUpdate update;
                    ctx.getWsLock().lock();
                    try {
                        ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                        if (!ctx.isInitialDataSent()) {
                            update = new EntityDataUpdate(ctx.getCmdId(), ctx.getData(), null, ctx.getMaxEntitiesPerDataSubscription());
                            ctx.setInitialDataSent(true);
                        } else {
                            update = new EntityDataUpdate(ctx.getCmdId(), null, ctx.getData().getData(), ctx.getMaxEntitiesPerDataSubscription());
                        }
                        ctx.sendWsMsg(update);
                    } finally {
                        ctx.getWsLock().unlock();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    log.warn("[{}][{}] Failed to process websocket command: {}:{}", ctx.getSessionId(), ctx.getCmdId(), ctx.getQuery(), latestCmd, t);
                    ctx.sendWsMsg(new EntityDataUpdate(ctx.getCmdId(), SubscriptionErrorCode.INTERNAL_ERROR.getCode(), "Failed to process websocket command!"));
                }
            }, wsCallBackExecutor);
        } else {
            ctx.getWsLock().lock();
            try {
                ctx.createLatestValuesSubscriptions(latestCmd.getKeys());
                checkAndSendInitialData(ctx);
            } finally {
                ctx.getWsLock().unlock();
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toTsValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Map<String, TsValue> toTsValue(List<TsKvEntry> data) {
        return data.stream().collect(Collectors.toMap(TsKvEntry::getKey, value -> new TsValue(value.getTs(), value.getValueAsString())));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `cancelSubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void cancelSubscription(String sessionId, UnsubscribeCmd cmd) {
        cleanupAndCancel(getSubCtx(sessionId, cmd.getCmdId()));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanupAndCancel` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void cleanupAndCancel(TbAbstractSubCtx ctx) {
        if (ctx != null) {
            ctx.stop();
            if (ctx.getSessionId() != null) {
                Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.get(ctx.getSessionId());
                if (sessionSubs != null) {
                    sessionSubs.remove(ctx.getCmdId());
                }
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `cancelAllSessionSubscriptions` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void cancelAllSessionSubscriptions(String sessionId) {
        Map<Integer, TbAbstractSubCtx> sessionSubs = subscriptionsBySessionId.remove(sessionId);
        if (sessionSubs != null) {
            sessionSubs.values().forEach(this::cleanupAndCancel);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getLimit` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private int getLimit(int limit) {
        return limit == 0 ? DEFAULT_LIMIT : limit;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbEntityDataSubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
