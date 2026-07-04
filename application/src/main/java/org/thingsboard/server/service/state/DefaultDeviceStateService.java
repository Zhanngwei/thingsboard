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
package org.thingsboard.server.service.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.DbTypeInfoComponent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

/**
 * Created by ashvayka on 01.05.18.
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`DefaultDeviceStateService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultDeviceStateService extends AbstractPartitionBasedService<DeviceId> implements DeviceStateService {

    /**
     * 字段说明：
     * 1. 保存 `ACTIVITY_STATE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String ACTIVITY_STATE = "active";
    public static final String LAST_CONNECT_TIME = "lastConnectTime";
    /**
     * 字段说明：
     * 1. 保存 `LAST_DISCONNECT_TIME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String LAST_DISCONNECT_TIME = "lastDisconnectTime";
    public static final String LAST_ACTIVITY_TIME = "lastActivityTime";
    /**
     * 字段说明：
     * 1. 保存 `INACTIVITY_ALARM_TIME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String INACTIVITY_ALARM_TIME = "inactivityAlarmTime";
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    private static final List<EntityKey> PERSISTENT_TELEMETRY_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.TIME_SERIES, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_DISCONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT));

    private static final List<EntityKey> PERSISTENT_ATTRIBUTE_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_DISCONNECT_TIME));

    public static final List<String> PERSISTENT_ATTRIBUTES = Arrays.asList(ACTIVITY_STATE, LAST_CONNECT_TIME,
            LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT);
    private static final List<EntityKey> PERSISTENT_ENTITY_FIELDS = Arrays.asList(
            new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "type"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "label"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

    /**
     * 字段说明：
     * 1. 保存 `deviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DeviceService deviceService;
    private final AttributesService attributesService;
    /**
     * 字段说明：
     * 1. 保存 `tsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final PartitionService partitionService;
    private final EntityQueryRepository entityQueryRepository;
    /**
     * 字段说明：
     * 1. 保存 `dbTypeInfoComponent` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DbTypeInfoComponent dbTypeInfoComponent;
    private final TbApiUsageReportClient apiUsageReportClient;
    /**
     * 字段说明：
     * 1. 保存 `notificationRuleProcessor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final NotificationRuleProcessor notificationRuleProcessor;
    @Autowired @Lazy
    /**
     * 字段说明：
     * 1. 保存 `tsSubService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TelemetrySubscriptionService tsSubService;

    @Value("${state.defaultInactivityTimeoutInSec}")
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `defaultInactivityTimeoutInSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long defaultInactivityTimeoutInSec;

    @Value("#{${state.defaultInactivityTimeoutInSec} * 1000}")
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `defaultInactivityTimeoutMs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long defaultInactivityTimeoutMs;

    @Value("${state.defaultStateCheckIntervalInSec}")
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `defaultStateCheckIntervalInSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int defaultStateCheckIntervalInSec;

    @Value("${usage.stats.devices.report_interval:60}")
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `defaultActivityStatsIntervalInSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int defaultActivityStatsIntervalInSec;

    @Value("${state.persistToTelemetry:false}")
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `persistToTelemetry` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean persistToTelemetry;

    @Value("${state.initFetchPackSize:50000}")
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `initFetchPackSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int initFetchPackSize;

    @Value("${state.telemetryTtl:0}")
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `telemetryTtl` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int telemetryTtl;

    /**
     * 字段说明：
     * 1. 保存 `deviceStateExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ListeningExecutorService deviceStateExecutor;
    private ListeningExecutorService deviceStateCallbackExecutor;

    final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    @PostConstruct
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        super.init();
        deviceStateExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state"));
        deviceStateCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state-callback"));
        scheduledExecutor.scheduleWithFixedDelay(this::checkStates, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
        scheduledExecutor.scheduleWithFixedDelay(this::reportActivityStats, defaultActivityStatsIntervalInSec, defaultActivityStatsIntervalInSec, TimeUnit.SECONDS);
    }

    @PreDestroy
    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop() {
        super.stop();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceStateExecutor != null) {
            deviceStateExecutor.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceStateCallbackExecutor != null) {
            deviceStateCallbackExecutor.shutdownNow();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getServiceName` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getServiceName() {
        return "Device State";
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getSchedulerExecutorName` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getSchedulerExecutorName() {
        return "device-state-scheduled";
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceConnect` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long lastConnectTime) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lastConnectTime < 0) {
            log.trace("[{}][{}] On device connect: received negative last connect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastConnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastConnectTime = stateData.getState().getLastConnectTime();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lastConnectTime <= currentLastConnectTime) {
            log.trace("[{}][{}] On device connect: received outdated last connect ts [{}]. Skipping this event. Current last connect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastConnectTime, currentLastConnectTime);
            return;
        }
        log.trace("[{}][{}] On device connect: processing connect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastConnectTime);
        stateData.getState().setLastConnectTime(lastConnectTime);
        save(deviceId, LAST_CONNECT_TIME, lastConnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.CONNECT_EVENT);
        checkAndUpdateState(deviceId, stateData);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceActivity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivity) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        log.trace("[{}] on Device Activity [{}], lastReportedActivity [{}]", tenantId.getId(), deviceId.getId(), lastReportedActivity);
        final DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lastReportedActivity > 0 && lastReportedActivity > stateData.getState().getLastActivityTime()) {
            updateActivityState(deviceId, stateData, lastReportedActivity);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateActivityState` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void updateActivityState(DeviceId deviceId, DeviceStateData stateData, long lastReportedActivity) {
        log.trace("updateActivityState - fetched state {} for device {}, lastReportedActivity {}", stateData, deviceId, lastReportedActivity);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stateData != null) {
            save(deviceId, LAST_ACTIVITY_TIME, lastReportedActivity);
            DeviceState state = stateData.getState();
            state.setLastActivityTime(lastReportedActivity);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!state.isActive()) {
                state.setActive(true);
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (lastReportedActivity <= state.getLastInactivityAlarmTime()) {
                    state.setLastInactivityAlarmTime(0);
                    save(deviceId, INACTIVITY_ALARM_TIME, 0);
                }
                onDeviceActivityStatusChange(deviceId, true, stateData);
            }
        } else {
            log.debug("updateActivityState - fetched state IS NULL for device {}, lastReportedActivity {}", deviceId, lastReportedActivity);
            cleanupEntity(deviceId);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceDisconnect` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long lastDisconnectTime) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (lastDisconnectTime < 0) {
            log.trace("[{}][{}] On device disconnect: received negative last disconnect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastDisconnectTime = stateData.getState().getLastDisconnectTime();
        if (lastDisconnectTime <= currentLastDisconnectTime) {
            log.trace("[{}][{}] On device disconnect: received outdated last disconnect ts [{}]. Skipping this event. Current last disconnect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime, currentLastDisconnectTime);
            return;
        }
        log.trace("[{}][{}] On device disconnect: processing disconnect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastDisconnectTime);
        stateData.getState().setLastDisconnectTime(lastDisconnectTime);
        save(deviceId, LAST_DISCONNECT_TIME, lastDisconnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.DISCONNECT_EVENT);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceInactivityTimeoutUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (inactivityTimeout <= 0L) {
            inactivityTimeout = defaultInactivityTimeoutMs;
        }
        log.trace("[{}] on Device Activity Timeout Update device id {} inactivityTimeout {}", tenantId.getId(), deviceId.getId(), inactivityTimeout);
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        stateData.getState().setInactivityTimeout(inactivityTimeout);
        checkAndUpdateState(deviceId, stateData);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceInactivity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long lastInactivityTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastInactivityTime < 0) {
            log.trace("[{}][{}] On device inactivity: received negative last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastInactivityAlarmTime = stateData.getState().getLastInactivityAlarmTime();
        if (lastInactivityTime <= currentLastInactivityAlarmTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less than current last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastInactivityAlarmTime);
            return;
        }
        long currentLastActivityTime = stateData.getState().getLastActivityTime();
        if (lastInactivityTime <= currentLastActivityTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less or equal to current last activity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastActivityTime);
            return;
        }
        log.trace("[{}][{}] On device inactivity: processing inactivity event with ts [{}].", tenantId.getId(), deviceId.getId(), lastInactivityTime);
        reportInactivity(lastInactivityTime, deviceId, stateData);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onQueueMsg` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB()));
            if (proto.getDeleted()) {
                onDeviceDeleted(tenantId, deviceId);
                callback.onSuccess();
            } else {
                Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
                if (device != null) {
                    if (proto.getAdded()) {
                        Futures.addCallback(fetchDeviceState(device), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable DeviceStateData state) {
                                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, device.getId());
                                if (addDeviceUsingState(tpi, state)) {
                                    save(deviceId, ACTIVITY_STATE, false);
                                    callback.onSuccess();
                                } else {
                                    log.debug("[{}][{}] Device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                                            , tenantId, deviceId, tpi.getFullTopicName());
                                    callback.onFailure(new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!"));
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to register device to the state service", t);
                                callback.onFailure(t);
                            }
                        }, deviceStateCallbackExecutor);
                    } else if (proto.getUpdated()) {
                        DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
                        TbMsgMetaData md = new TbMsgMetaData();
                        md.putValue("deviceName", device.getName());
                        md.putValue("deviceLabel", device.getLabel());
                        md.putValue("deviceType", device.getType());
                        stateData.setMetaData(md);
                        callback.onSuccess();
                    }
                } else {
                    //Device was probably deleted while message was in queue;
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            log.trace("Failed to process queue msg: [{}]", proto, e);
            callback.onFailure(e);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAddedPartitions` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
        PageDataIterable<DeviceIdInfo> deviceIdInfos = new PageDataIterable<>(deviceService::findDeviceIdInfos, initFetchPackSize);
        Map<TopicPartitionInfo, List<DeviceIdInfo>> tpiDeviceMap = new HashMap<>();

        for (DeviceIdInfo idInfo : deviceIdInfos) {
            TopicPartitionInfo tpi;
            try {
                tpi = partitionService.resolve(ServiceType.TB_CORE, idInfo.getTenantId(), idInfo.getDeviceId());
            } catch (Exception e) {
                log.warn("Failed to resolve partition for device with id [{}], tenant id [{}], customer id [{}]. Reason: {}",
                        idInfo.getDeviceId(), idInfo.getTenantId(), idInfo.getCustomerId(), e.getMessage());
                continue;
            }
            if (addedPartitions.contains(tpi) && !deviceStates.containsKey(idInfo.getDeviceId())) {
                tpiDeviceMap.computeIfAbsent(tpi, tmp -> new ArrayList<>()).add(idInfo);
            }
        }

        for (var entry : tpiDeviceMap.entrySet()) {
            AtomicInteger counter = new AtomicInteger(0);
            // hard-coded limit of 1000 is due to the Entity Data Query limitations and should not be changed.
            for (List<DeviceIdInfo> partition : Lists.partition(entry.getValue(), 1000)) {
                log.info("[{}] Submit task for device states: {}", entry.getKey(), partition.size());
                DevicePackFutureHolder devicePackFutureHolder = new DevicePackFutureHolder();
                var devicePackFuture = deviceStateExecutor.submit(() -> {
                    try {
                        List<DeviceStateData> states;
                        if (persistToTelemetry && !dbTypeInfoComponent.isLatestTsDaoStoredToSql()) {
                            states = fetchDeviceStateDataUsingSeparateRequests(partition);
                        } else {
                            states = fetchDeviceStateDataUsingEntityDataQuery(partition);
                        }
                        if (devicePackFutureHolder.future == null || !devicePackFutureHolder.future.isCancelled()) {
                            for (var state : states) {
                                if (!addDeviceUsingState(entry.getKey(), state)) {
                                    return;
                                }
                                checkAndUpdateState(state.getDeviceId(), state);
                            }
                            log.info("[{}] Initialized {} out of {} device states", entry.getKey().getPartition().orElse(0), counter.addAndGet(states.size()), entry.getValue().size());
                        }
                    } catch (Throwable t) {
                        log.error("Unexpected exception while device pack fetching", t);
                        throw t;
                    }
                });
                devicePackFutureHolder.future = devicePackFuture;
                result.computeIfAbsent(entry.getKey(), tmp -> new ArrayList<>()).add(devicePackFuture);
            }
        }
        return result;
    }

    /**
     * 中文说明：
     * 1. 类目的：`DevicePackFutureHolder` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private static class DevicePackFutureHolder {
        /**
         * 字段说明：
         * 1. 保存 `future` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private volatile ListenableFuture<?> future;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkAndUpdateState` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void checkAndUpdateState(@Nonnull DeviceId deviceId, @Nonnull DeviceStateData state) {
        var deviceState = state.getState();
        if (deviceState.isActive()) {
            updateInactivityStateIfExpired(getCurrentTimeMillis(), deviceId, state);
        } else {
            //trying to fix activity state
            if (isActive(getCurrentTimeMillis(), deviceState)) {
                updateActivityState(deviceId, state, deviceState.getLastActivityTime());
                if (deviceState.getLastInactivityAlarmTime() != 0L && deviceState.getLastInactivityAlarmTime() >= deviceState.getLastActivityTime()) {
                    deviceState.setLastInactivityAlarmTime(0L);
                    save(deviceId, INACTIVITY_ALARM_TIME, 0L);
                }
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addDeviceUsingState` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean addDeviceUsingState(TopicPartitionInfo tpi, DeviceStateData state) {
        Set<DeviceId> deviceIds = partitionedEntities.get(tpi);
        if (deviceIds != null) {
            deviceIds.add(state.getDeviceId());
            deviceStates.putIfAbsent(state.getDeviceId(), state);
            return true;
        } else {
            log.debug("[{}] Device belongs to external partition {}", state.getDeviceId(), tpi.getFullTopicName());
            return false;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkStates` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void checkStates() {
        try {
            final long ts = getCurrentTimeMillis();
            partitionedEntities.forEach((tpi, deviceIds) -> {
                log.debug("Calculating state updates. tpi {} for {} devices", tpi.getFullTopicName(), deviceIds.size());
                Set<DeviceId> idsFromRemovedTenant = new HashSet<>();
                for (DeviceId deviceId : deviceIds) {
                    DeviceStateData stateData;
                    try {
                        stateData = getOrFetchDeviceStateData(deviceId);
                    } catch (Exception e) {
                        log.error("[{}] Failed to get or fetch device state data", deviceId, e);
                        continue;
                    }
                    try {
                        updateInactivityStateIfExpired(ts, deviceId, stateData);
                    } catch (Exception e) {
                        if (e instanceof TenantNotFoundException) {
                            idsFromRemovedTenant.add(deviceId);
                        } else {
                            log.warn("[{}] Failed to update inactivity state [{}]", deviceId, e.getMessage());
                        }
                    }
                }
                deviceIds.removeAll(idsFromRemovedTenant);
            });
        } catch (Throwable t) {
            log.warn("Failed to check devices states", t);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportActivityStats` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void reportActivityStats() {
        try {
            Map<TenantId, Pair<AtomicInteger, AtomicInteger>> stats = new HashMap<>();
            for (DeviceStateData stateData : deviceStates.values()) {
                Pair<AtomicInteger, AtomicInteger> tenantDevicesActivity = stats.computeIfAbsent(stateData.getTenantId(),
                        tenantId -> Pair.of(new AtomicInteger(), new AtomicInteger()));
                if (stateData.getState().isActive()) {
                    tenantDevicesActivity.getLeft().incrementAndGet();
                } else {
                    tenantDevicesActivity.getRight().incrementAndGet();
                }
            }

            stats.forEach((tenantId, tenantDevicesActivity) -> {
                int active = tenantDevicesActivity.getLeft().get();
                int inactive = tenantDevicesActivity.getRight().get();
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.ACTIVE_DEVICES, active);
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.INACTIVE_DEVICES, inactive);
                if (active > 0) {
                    log.debug("[{}] Active devices: {}, inactive devices: {}", tenantId, active, inactive);
                }
            });
        } catch (Throwable t) {
            log.warn("Failed to report activity states", t);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateInactivityStateIfExpired` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void updateInactivityStateIfExpired(long ts, DeviceId deviceId, DeviceStateData stateData) {
        log.trace("Processing state {} for device {}", stateData, deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            if (!isActive(ts, state)
                    && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() <= state.getLastActivityTime())
                    && stateData.getDeviceCreationTime() + state.getInactivityTimeout() <= ts) {
                if (partitionService.resolve(ServiceType.TB_CORE, stateData.getTenantId(), deviceId).isMyPartition()) {
                    reportInactivity(ts, deviceId, stateData);
                } else {
                    cleanupEntity(deviceId);
                }
            }
        } else {
            log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
            cleanupEntity(deviceId);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportInactivity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void reportInactivity(long ts, DeviceId deviceId, DeviceStateData stateData) {
        DeviceState state = stateData.getState();
        state.setActive(false);
        state.setLastInactivityAlarmTime(ts);
        save(deviceId, INACTIVITY_ALARM_TIME, ts);
        onDeviceActivityStatusChange(deviceId, false, stateData);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isActive` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    boolean isActive(long ts, DeviceState state) {
        return ts < state.getLastActivityTime() + state.getInactivityTimeout();
    }

    @Nonnull
    /**
     * 方法说明：
     * 1. 职责：执行 `getOrFetchDeviceStateData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        return deviceStates.computeIfAbsent(deviceId, this::fetchDeviceStateDataUsingSeparateRequests);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fetchDeviceStateDataUsingSeparateRequests` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    DeviceStateData fetchDeviceStateDataUsingSeparateRequests(final DeviceId deviceId) {
        final Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
        if (device == null) {
            log.warn("[{}] Failed to fetch device by Id!", deviceId);
            throw new RuntimeException("Failed to fetch device by id [" + deviceId + "]!");
        }
        try {
            return fetchDeviceState(device).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch device state!", deviceId, e);
            throw new RuntimeException("Failed to fetch device state for device [" + deviceId + "]");
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceActivityStatusChange` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onDeviceActivityStatusChange(DeviceId deviceId, boolean active, DeviceStateData stateData) {
        save(deviceId, ACTIVITY_STATE, active);
        pushRuleEngineMessage(stateData, active ? TbMsgType.ACTIVITY_EVENT : TbMsgType.INACTIVITY_EVENT);
        TbMsgMetaData metaData = stateData.getMetaData();
        notificationRuleProcessor.process(DeviceActivityTrigger.builder()
                .tenantId(stateData.getTenantId()).customerId(stateData.getCustomerId())
                .deviceId(deviceId).active(active)
                .deviceName(metaData.getValue("deviceName"))
                .deviceType(metaData.getValue("deviceType"))
                .deviceLabel(metaData.getValue("deviceLabel"))
                .build());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanDeviceStateIfBelongsToExternalPartition` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    boolean cleanDeviceStateIfBelongsToExternalPartition(TenantId tenantId, final DeviceId deviceId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        boolean cleanup = !partitionedEntities.containsKey(tpi);
        if (cleanup) {
            cleanupEntity(deviceId);
            log.debug("[{}][{}] device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , tenantId, deviceId, tpi.getFullTopicName());
        }
        return cleanup;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onDeviceDeleted` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        cleanupEntity(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedEntities.get(tpi);
        if (deviceIdSet != null) {
            deviceIdSet.remove(deviceId);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `cleanupEntityOnPartitionRemoval` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void cleanupEntityOnPartitionRemoval(DeviceId deviceId) {
        cleanupEntity(deviceId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanupEntity` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void cleanupEntity(DeviceId deviceId) {
        deviceStates.remove(deviceId);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `fetchDeviceState` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<DeviceStateData> fetchDeviceState(Device device) {
        ListenableFuture<DeviceStateData> future;
        if (persistToTelemetry) {
            ListenableFuture<List<TsKvEntry>> tsData = tsService.findLatest(TenantId.SYS_TENANT_ID, device.getId(), PERSISTENT_ATTRIBUTES);
            future = Futures.transform(tsData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        } else {
            ListenableFuture<List<AttributeKvEntry>> attrData = attributesService.find(TenantId.SYS_TENANT_ID, device.getId(), SERVER_SCOPE, PERSISTENT_ATTRIBUTES);
            future = Futures.transform(attrData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        }
        return transformInactivityTimeout(future);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `transformInactivityTimeout` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<DeviceStateData> transformInactivityTimeout(ListenableFuture<DeviceStateData> future) {
        return Futures.transformAsync(future, deviceStateData -> {
            if (!persistToTelemetry || deviceStateData.getState().getInactivityTimeout() != defaultInactivityTimeoutMs) {
                return future; //fail fast
            }
            var attributesFuture = attributesService.find(TenantId.SYS_TENANT_ID, deviceStateData.getDeviceId(), SERVER_SCOPE, INACTIVITY_TIMEOUT);
            return Futures.transform(attributesFuture, attributes -> {
                attributes.flatMap(KvEntry::getLongValue).ifPresent((inactivityTimeout) -> {
                    if (inactivityTimeout > 0) {
                        deviceStateData.getState().setInactivityTimeout(inactivityTimeout);
                    }
                });
                return deviceStateData;
            }, MoreExecutors.directExecutor());
        }, deviceStateCallbackExecutor);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractDeviceStateData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T extends KvEntry> Function<List<T>, DeviceStateData> extractDeviceStateData(Device device) {
        return new Function<>() {
            @Nonnull
            @Override
            public DeviceStateData apply(@Nullable List<T> data) {
                try {
                    long lastActivityTime = getEntryValue(data, LAST_ACTIVITY_TIME, 0L);
                    long inactivityAlarmTime = getEntryValue(data, INACTIVITY_ALARM_TIME, 0L);
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
                    // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
                    final boolean active = getEntryValue(data, ACTIVITY_STATE, false);
                    DeviceState deviceState = DeviceState.builder()
                            .active(active)
                            .lastConnectTime(getEntryValue(data, LAST_CONNECT_TIME, 0L))
                            .lastDisconnectTime(getEntryValue(data, LAST_DISCONNECT_TIME, 0L))
                            .lastActivityTime(lastActivityTime)
                            .lastInactivityAlarmTime(inactivityAlarmTime)
                            .inactivityTimeout(inactivityTimeout)
                            .build();
                    TbMsgMetaData md = new TbMsgMetaData();
                    md.putValue("deviceName", device.getName());
                    md.putValue("deviceLabel", device.getLabel());
                    md.putValue("deviceType", device.getType());
                    DeviceStateData deviceStateData = DeviceStateData.builder()
                            .customerId(device.getCustomerId())
                            .tenantId(device.getTenantId())
                            .deviceId(device.getId())
                            .deviceCreationTime(device.getCreatedTime())
                            .metaData(md)
                            .state(deviceState).build();
                    log.debug("[{}] Fetched device state from the DB {}", device.getId(), deviceStateData);
                    return deviceStateData;
                } catch (Exception e) {
                    log.warn("[{}] Failed to fetch device state data", device.getId(), e);
                    throw new RuntimeException("Failed to fetch device state data for device [" + device.getId() + "]", e);
                }
            }
        };
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fetchDeviceStateDataUsingSeparateRequests` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<DeviceStateData> fetchDeviceStateDataUsingSeparateRequests(List<DeviceIdInfo> deviceIds) {
        List<Device> devices = deviceService.findDevicesByIds(deviceIds.stream().map(DeviceIdInfo::getDeviceId).collect(Collectors.toList()));
        List<ListenableFuture<DeviceStateData>> deviceStateFutures = new ArrayList<>();
        for (Device device : devices) {
            deviceStateFutures.add(fetchDeviceState(device));
        }
        try {
            List<DeviceStateData> result = Futures.successfulAsList(deviceStateFutures).get(5, TimeUnit.MINUTES);
            boolean success = true;
            for (int i = 0; i < result.size(); i++) {
                success = false;
                if (result.get(i) == null) {
                    DeviceIdInfo deviceIdInfo = deviceIds.get(i);
                    log.warn("[{}][{}] Failed to initialized device state due to:", deviceIdInfo.getTenantId(), deviceIdInfo.getDeviceId());
                }
            }
            return success ? result : result.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String deviceIdsStr = deviceIds.stream()
                    .map(DeviceIdInfo::getDeviceId)
                    .map(UUIDBased::getId)
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
            log.warn("Failed to initialized device state futures for ids [{}] due to:", deviceIdsStr, e);
            throw new RuntimeException("Failed to initialized device state futures for ids [" + deviceIdsStr + "]!", e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fetchDeviceStateDataUsingEntityDataQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<DeviceStateData> fetchDeviceStateDataUsingEntityDataQuery(List<DeviceIdInfo> deviceIds) {
        EntityListFilter ef = new EntityListFilter();
        ef.setEntityType(EntityType.DEVICE);
        ef.setEntityList(deviceIds.stream().map(DeviceIdInfo::getDeviceId).map(DeviceId::getId).map(UUID::toString).collect(Collectors.toList()));

        EntityDataQuery query = new EntityDataQuery(ef,
                new EntityDataPageLink(deviceIds.size(), 0, null, null),
                PERSISTENT_ENTITY_FIELDS,
                persistToTelemetry ? PERSISTENT_TELEMETRY_KEYS : PERSISTENT_ATTRIBUTE_KEYS, Collections.emptyList());
        PageData<EntityData> queryResult = entityQueryRepository.findEntityDataByQueryInternal(query);

        Map<EntityId, DeviceIdInfo> deviceIdInfos = deviceIds.stream().collect(Collectors.toMap(DeviceIdInfo::getDeviceId, java.util.function.Function.identity()));

        return queryResult.getData().stream().map(ed -> toDeviceStateData(ed, deviceIdInfos.get(ed.getEntityId()))).collect(Collectors.toList());

    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toDeviceStateData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    DeviceStateData toDeviceStateData(EntityData ed, DeviceIdInfo deviceIdInfo) {
        long lastActivityTime = getEntryValue(ed, getKeyType(), LAST_ACTIVITY_TIME, 0L);
        long inactivityAlarmTime = getEntryValue(ed, getKeyType(), INACTIVITY_ALARM_TIME, 0L);
        long inactivityTimeout = getEntryValue(ed, getKeyType(), INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        if (persistToTelemetry && inactivityTimeout == defaultInactivityTimeoutMs) {
            log.trace("[{}] default value for inactivity timeout fetched {}, going to fetch inactivity timeout from attributes",
                    deviceIdInfo.getDeviceId(), inactivityTimeout);
            inactivityTimeout = getEntryValue(ed, EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        }
        // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
        final boolean active = getEntryValue(ed, getKeyType(), ACTIVITY_STATE, false);
        DeviceState deviceState = DeviceState.builder()
                .active(active)
                .lastConnectTime(getEntryValue(ed, getKeyType(), LAST_CONNECT_TIME, 0L))
                .lastDisconnectTime(getEntryValue(ed, getKeyType(), LAST_DISCONNECT_TIME, 0L))
                .lastActivityTime(lastActivityTime)
                .lastInactivityAlarmTime(inactivityAlarmTime)
                .inactivityTimeout(inactivityTimeout)
                .build();
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("deviceName", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "name", ""));
        md.putValue("deviceLabel", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "label", ""));
        md.putValue("deviceType", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "type", ""));
        return DeviceStateData.builder()
                .customerId(deviceIdInfo.getCustomerId())
                .tenantId(deviceIdInfo.getTenantId())
                .deviceId(deviceIdInfo.getDeviceId())
                .deviceCreationTime(getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "createdTime", 0L))
                .metaData(md)
                .state(deviceState).build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getKeyType` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EntityKeyType getKeyType() {
        return persistToTelemetry ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, String defaultValue) {
        return getEntryValue(ed, keyType, keyName, s -> s, defaultValue);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private long getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, long defaultValue) {
        return getEntryValue(ed, keyType, keyName, Long::parseLong, defaultValue);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, boolean defaultValue) {
        return getEntryValue(ed, keyType, keyName, Boolean::parseBoolean, defaultValue);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T> T getEntryValue(EntityData ed, EntityKeyType entityKeyType, String attributeName, Function<String, T> converter, T defaultValue) {
        if (ed != null && ed.getLatest() != null) {
            var map = ed.getLatest().get(entityKeyType);
            if (map != null) {
                var value = map.get(attributeName);
                if (value != null && !StringUtils.isEmpty(value.getValue())) {
                    try {
                        return converter.apply(value.getValue());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private long getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, long defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getLongValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getEntryValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, boolean defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getBooleanValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushRuleEngineMessage` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushRuleEngineMessage(DeviceStateData stateData, TbMsgType msgType) {
        DeviceState state = stateData.getState();
        try {
            String data;
            if (msgType.equals(TbMsgType.CONNECT_EVENT)) {
                ObjectNode stateNode = JacksonUtil.convertValue(state, ObjectNode.class);
                stateNode.remove(ACTIVITY_STATE);
                data = JacksonUtil.toString(stateNode);
            } else {
                data = JacksonUtil.toString(state);
            }
            TbMsgMetaData md = stateData.getMetaData().copy();
            if (!persistToTelemetry) {
                md.putValue(SCOPE, SERVER_SCOPE);
            }
            TbMsg tbMsg = TbMsg.newMsg(msgType, stateData.getDeviceId(), stateData.getCustomerId(), md, TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(stateData.getTenantId(), stateData.getDeviceId(), tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push inactivity alarm: {}", stateData.getDeviceId(), state, e);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void save(DeviceId deviceId, String key, long value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(getCurrentTimeMillis(), new LongDataEntry(key, value))),
                    telemetryTtl, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `save` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(getCurrentTimeMillis(), new BooleanDataEntry(key, value))),
                    telemetryTtl, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurrentTimeMillis` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 中文说明：
     * 1. 类目的：`TelemetrySaveCallback` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        /**
         * 字段说明：
         * 1. 保存 `deviceId` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final DeviceId deviceId;
        private final String key;
        /**
         * 字段说明：
         * 1. 保存 `value` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onSuccess` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        @Override
        /**
         * 方法说明：
         * 1. 职责：执行 `onFailure` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultDeviceStateService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
