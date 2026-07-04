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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.event.ClusterTopologyChangeEvent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.TelemetrySubscriptionUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@TbCoreComponent
@Service
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbLocalSubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultTbLocalSubscriptionService implements TbLocalSubscriptionService {

    private final ConcurrentMap<String, Map<Integer, TbSubscription<?>>> subscriptionsBySessionId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TbEntityLocalSubsInfo> subscriptionsByEntityId = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TbEntityUpdatesInfo> entityUpdates = new ConcurrentHashMap<>();

    /**
     * 字段说明：
     * 1. 保存 `attrService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final AttributesService attrService;
    private final TimeseriesService tsService;
    /**
     * 字段说明：
     * 1. 保存 `serviceInfoProvider` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;
    /**
     * 字段说明：
     * 1. 保存 `clusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbClusterService clusterService;
    private final SubscriptionManagerService subscriptionManagerService;

    /**
     * 字段说明：
     * 1. 保存 `tsCallBackExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExecutorService tsCallBackExecutor;

    /**
     * 方法说明：
     * 1. 职责：执行 `DefaultTbLocalSubscriptionService` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DefaultTbLocalSubscriptionService(AttributesService attrService, TimeseriesService tsService, TbServiceInfoProvider serviceInfoProvider,
                                             PartitionService partitionService, TbClusterService clusterService,
                                             @Lazy SubscriptionManagerService subscriptionManagerService) {
        this.attrService = attrService;
        this.tsService = tsService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.clusterService = clusterService;
        this.subscriptionManagerService = subscriptionManagerService;
    }

    /**
     * 字段说明：
     * 1. 保存 `serviceId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String serviceId;
    private ExecutorService subscriptionUpdateExecutor;

    private final Lock subsLock = new ReentrantLock();

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
        subscriptionUpdateExecutor = ThingsBoardExecutors.newWorkStealingPool(20, getClass());
        tsCallBackExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("ts-sub-callback"));
        serviceId = serviceInfoProvider.getServiceId();
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
        if (subscriptionUpdateExecutor != null) {
            subscriptionUpdateExecutor.shutdownNow();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    @EventListener(ClusterTopologyChangeEvent.class)
    /**
     * 方法说明：
     * 1. 职责：执行 `onApplicationEvent` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onApplicationEvent(ClusterTopologyChangeEvent event) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (event.getQueueKeys().stream().anyMatch(key -> ServiceType.TB_CORE.equals(key.getType()))) {
            /*
             * If the cluster topology has changed, we need to push all current subscriptions to SubscriptionManagerService again.
             * Otherwise, the SubscriptionManagerService may "forget" those subscriptions in case of restart.
             * Although this is resource consuming operation, it is cheaper than sending ping/pong commands periodically
             * It is also cheaper than caching the subscriptions by entity id and then lookup of those caches every time we have new telemetry in SubscriptionManagerService.
             * Even if we cache locally the list of active subscriptions by entity id, it is still time-consuming operation to get them from cache
             * Since number of subscriptions is usually much less than number of devices that are pushing data.
             */
            subscriptionsByEntityId.values().forEach(sub -> pushSubEventToManagerService(sub.getTenantId(), sub.getEntityId(), sub.toEvent(ComponentLifecycleEvent.UPDATED)));
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onCoreStartupMsg` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg) {
        subscriptionUpdateExecutor.submit(() -> {
            Set<Integer> partitions = new HashSet<>(coreStartupMsg.getPartitionsList());
            AtomicInteger counter = new AtomicInteger();
            subscriptionsByEntityId.values().forEach(sub -> {
                var tpi = partitionService.resolve(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId());
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!tpi.isMyPartition() && partitions.contains(tpi.getPartition().orElse(Integer.MAX_VALUE))) {
                    pushToQueue(sub.getEntityId(), sub.toEvent(ComponentLifecycleEvent.UPDATED), tpi);
                    counter.incrementAndGet();
                }
            });
            log.info("[{}] Pushed {} subscriptions to [{}]", serviceId, counter.get(), coreStartupMsg.getServiceId());
        });
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `addSubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void addSubscription(TbSubscription<?> subscription) {
        TenantId tenantId = subscription.getTenantId();
        EntityId entityId = subscription.getEntityId();
        log.debug("[{}][{}] Register subscription: {}", tenantId, entityId, subscription);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.computeIfAbsent(subscription.getSessionId(), k -> new ConcurrentHashMap<>());
        sessionSubscriptions.put(subscription.getSubscriptionId(), subscription);
        modifySubscription(tenantId, entityId, subscription, true);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onSubEventCallback` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onSubEventCallback(TransportProtos.TbEntitySubEventCallbackProto subEventCallback, TbCallback callback) {
        UUID entityId = new UUID(subEventCallback.getEntityIdMSB(), subEventCallback.getEntityIdLSB());
        onSubEventCallback(entityId, subEventCallback.getSeqNumber(), new TbEntityUpdatesInfo(subEventCallback.getAttributesUpdateTs(), subEventCallback.getTimeSeriesUpdateTs()), callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onSubEventCallback` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onSubEventCallback(EntityId entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback callback) {
        onSubEventCallback(entityId.getId(), seqNumber, entityUpdatesInfo, callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onSubEventCallback` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onSubEventCallback(UUID entityId, int seqNumber, TbEntityUpdatesInfo entityUpdatesInfo, TbCallback callback) {
        log.debug("[{}][{}] Processing sub event callback: {}.", entityId, seqNumber, entityUpdatesInfo);
        entityUpdates.put(entityId, entityUpdatesInfo);
        Set<TbSubscription<?>> pendingSubs = null;
        subsLock.lock();
        try {
            TbEntityLocalSubsInfo entitySubs = subscriptionsByEntityId.get(entityId);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (entitySubs != null) {
                pendingSubs = entitySubs.clearPendingSubscriptions(seqNumber);
            }
        } finally {
            subsLock.unlock();
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (pendingSubs != null) {
            pendingSubs.forEach(this::checkMissedUpdates);
        }
        callback.onSuccess();
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
    public void cancelSubscription(String sessionId, int subscriptionId) {
        log.debug("[{}][{}] Going to remove subscription.", sessionId, subscriptionId);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.get(sessionId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sessionSubscriptions != null) {
            TbSubscription<?> subscription = sessionSubscriptions.remove(subscriptionId);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (subscription != null) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (sessionSubscriptions.isEmpty()) {
                    subscriptionsBySessionId.remove(sessionId);
                }
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, false);
            } else {
                log.debug("[{}][{}] Subscription not found!", sessionId, subscriptionId);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
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
        log.debug("[{}] Going to remove session subscriptions.", sessionId);
        Map<Integer, TbSubscription<?>> sessionSubscriptions = subscriptionsBySessionId.remove(sessionId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (sessionSubscriptions != null) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (TbSubscription<?> subscription : sessionSubscriptions.values()) {
                modifySubscription(subscription.getTenantId(), subscription.getEntityId(), subscription, false);
            }
        } else {
            log.debug("[{}] No session subscriptions found!", sessionId);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onTimeSeriesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onTimeSeriesUpdate(TransportProtos.TbSubUpdateProto proto, TbCallback callback) {
        //TODO: optimize to avoid re-wrapping from TsValueListProto -> List<KV> -> Map<String, List<Object>>. Low priority.
        onTimeSeriesUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onTimeSeriesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> data, TbCallback callback) {
        onTimeSeriesUpdate(entityId.getId(), data, callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onTimeSeriesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onTimeSeriesUpdate(UUID entityId, List<TsKvEntry> data, TbCallback callback) {
        entityUpdates.get(entityId).timeSeriesUpdateTs = System.currentTimeMillis();
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.TIMESERIES.equals(sub.getType()),
                s -> {
                    TbTimeSeriesSubscription sub = (TbTimeSeriesSubscription) s;
                    List<TsKvEntry> updateData = null;
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (sub.isAllKeys()) {
                        updateData = data;
                    } else {
                        for (TsKvEntry kv : data) {
                            if (sub.getKeyStates().containsKey((kv.getKey()))) {
                                if (updateData == null) {
                                    updateData = new ArrayList<>();
                                }
                                updateData.add(kv);
                            }
                        }
                    }
                    if (updateData != null) {
                        TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), updateData);
                        update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                        subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                    }
                }, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributesUpdate(TransportProtos.TbSubUpdateProto proto, TbCallback callback) {
        onAttributesUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), proto.getScope(), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAttributesUpdate(EntityId entityId, String scope, List<TsKvEntry> data, TbCallback callback) {
        onAttributesUpdate(entityId.getId(), scope, data, callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onAttributesUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onAttributesUpdate(UUID entityId, String scope, List<TsKvEntry> data, TbCallback callback) {
        entityUpdates.get(entityId).attributesUpdateTs = System.currentTimeMillis();
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ATTRIBUTES.equals(sub.getType()),
                s -> {
                    TbAttributeSubscription sub = (TbAttributeSubscription) s;
                    if (sub.getScope() == null || TbAttributeSubscriptionScope.ANY_SCOPE.equals(sub.getScope()) || sub.getScope().name().equals(scope)) {
                        List<TsKvEntry> updateData = null;
                        if (sub.isAllKeys()) {
                            updateData = data;
                        } else {
                            for (TsKvEntry kv : data) {
                                if (sub.getKeyStates().containsKey((kv.getKey()))) {
                                    if (updateData == null) {
                                        updateData = new ArrayList<>();
                                    }
                                    updateData.add(kv);
                                }
                            }
                        }
                        if (updateData != null) {
                            TelemetrySubscriptionUpdate update = new TelemetrySubscriptionUpdate(sub.getSubscriptionId(), updateData);
                            update.getLatestValues().forEach((key, value) -> sub.getKeyStates().put(key, value));
                            subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, update));
                        }
                    }
                }, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAlarmUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAlarmUpdate(TransportProtos.TbAlarmSubUpdateProto proto, TbCallback callback) {
        onAlarmUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onAlarmUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onAlarmUpdate(EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback) {
        onAlarmUpdate(entityId.getId(), new AlarmSubscriptionUpdate(alarm, deleted), callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onAlarmUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onAlarmUpdate(UUID entityId, AlarmSubscriptionUpdate update, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.ALARMS.equals(sub.getType()),
                update, callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onNotificationUpdate(TransportProtos.NotificationsSubUpdateProto proto, TbCallback callback) {
        onNotificationUpdate(new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()), TbSubscriptionUtils.fromProto(proto), callback);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onNotificationUpdate(EntityId entityId, NotificationsSubscriptionUpdate update, TbCallback callback) {
        onNotificationUpdate(entityId.getId(), update, callback);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void onNotificationUpdate(UUID entityId, NotificationsSubscriptionUpdate update, TbCallback callback) {
        processSubscriptionData(entityId,
                sub -> TbSubscriptionType.NOTIFICATIONS.equals(sub.getType()) || TbSubscriptionType.NOTIFICATIONS_COUNT.equals(sub.getType()),
                update, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `onNotificationRequestUpdate` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onNotificationRequestUpdate(TenantId tenantId, NotificationRequestUpdate update, TbCallback callback) {
        log.trace("[{}] Received notification request update: {}", tenantId, update);
        NotificationsSubscriptionUpdate theUpdate = new NotificationsSubscriptionUpdate(update);
        subscriptionsByEntityId.values().forEach(subInfo -> {
            if (subInfo.isNf() && tenantId.equals(subInfo.getTenantId()) && EntityType.USER.equals(subInfo.getEntityId().getEntityType())) {
                subInfo.getSubs().forEach(s -> {
                    TbSubscription<NotificationsSubscriptionUpdate> sub = (TbSubscription<NotificationsSubscriptionUpdate>) s;
                    subscriptionUpdateExecutor.submit(() -> sub.getUpdateProcessor().accept(sub, theUpdate));
                });
            }
        });
        callback.onSuccess();
    }

    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `processSubscriptionData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T> void processSubscriptionData(UUID entityId,
                                             Predicate<TbSubscription<?>> filter,
                                             T data,
                                             TbCallback callback) {
        log.trace("[{}] Received subscription data: {}", entityId, data);
        var subs = subscriptionsByEntityId.get(entityId);
        if (subs != null) {
            subs.getSubs().forEach(s -> {
                if (filter.test(s)) {
                    subscriptionUpdateExecutor.submit(() -> {
                        TbSubscription<T> sub = (TbSubscription<T>) s;
                        sub.getUpdateProcessor().accept(sub, data);
                    });
                }
            });
        }
        callback.onSuccess();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processSubscriptionData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void processSubscriptionData(UUID entityId,
                                         Predicate<TbSubscription<?>> filter,
                                         Consumer<TbSubscription<?>> processor,
                                         TbCallback callback) {
        var subs = subscriptionsByEntityId.get(entityId);
        if (subs != null) {
            subs.getSubs().forEach(s -> {
                if (filter.test(s)) {
                    processor.accept(s);
                }
            });
        }
        callback.onSuccess();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `modifySubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void modifySubscription(TenantId tenantId, EntityId entityId, TbSubscription<?> subscription, boolean add) {
        TbSubscription<?> missedUpdatesCandidate = null;
        TbEntitySubEvent event;
        subsLock.lock();
        try {
            TbEntityLocalSubsInfo entitySubs = subscriptionsByEntityId.computeIfAbsent(entityId.getId(), id -> new TbEntityLocalSubsInfo(tenantId, entityId));
            event = add ? entitySubs.add(subscription) : entitySubs.remove(subscription);
            if (entitySubs.isEmpty()) {
                subscriptionsByEntityId.remove(entityId.getId());
                entityUpdates.remove(entityId.getId());
            } else if (add) {
                missedUpdatesCandidate = entitySubs.registerPendingSubscription(subscription, event);
            }
        } finally {
            subsLock.unlock();
        }
        if (event != null) {
            log.trace("[{}][{}][{}] Event: {}", tenantId, entityId, subscription.getSubscriptionId(), event);
            pushSubEventToManagerService(tenantId, entityId, event);
            if (missedUpdatesCandidate != null) {
                checkMissedUpdates(missedUpdatesCandidate);
            }
        } else {
            log.trace("[{}][{}][{}] No changes detected.", tenantId, entityId, subscription.getSubscriptionId());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushSubEventToManagerService` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushSubEventToManagerService(TenantId tenantId, EntityId entityId, TbEntitySubEvent event) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (tpi.isMyPartition()) {
            // Subscription is managed on the same server;
            subscriptionManagerService.onSubEvent(serviceId, event, TbCallback.EMPTY);
        } else {
            pushToQueue(entityId, event, tpi);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushToQueue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushToQueue(EntityId entityId, TbEntitySubEvent event, TopicPartitionInfo tpi) {
        clusterService.pushMsgToCore(tpi, entityId.getId(), TbSubscriptionUtils.toSubEventProto(serviceId, event), null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkMissedUpdates` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void checkMissedUpdates(TbSubscription<?> subscription) {
        log.trace("[{}][{}][{}] Check missed updates for subscription: {}",
                subscription.getTenantId(), subscription.getEntityId(), subscription.getSubscriptionId(), subscription);
        switch (subscription.getType()) {
            case TIMESERIES:
                handleNewTelemetrySubscription((TbTimeSeriesSubscription) subscription);
                break;
            case ATTRIBUTES:
                handleNewAttributeSubscription((TbAttributeSubscription) subscription);
                break;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleNewAttributeSubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleNewAttributeSubscription(TbAttributeSubscription subscription) {
        log.trace("[{}][{}][{}] Processing attribute subscription for entity [{}]",
                subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        var entityUpdateInfo = entityUpdates.get(subscription.getEntityId().getId());
        if (entityUpdateInfo != null && entityUpdateInfo.attributesUpdateTs > 0 && subscription.getQueryTs() > entityUpdateInfo.attributesUpdateTs) {
            log.trace("[{}][{}][{}] No need to check for missed updates [{}]",
                    subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
            return;
        }
        final Map<String, Long> keyStates = subscription.getKeyStates();
        String scope;
        if (subscription.getScope() != null && !TbAttributeSubscriptionScope.ANY_SCOPE.equals(subscription.getScope())) {
            scope = subscription.getScope().name();
        } else {
            scope = DataConstants.CLIENT_SCOPE;
        }
        DonAsynchron.withCallback(attrService.find(subscription.getTenantId(), subscription.getEntityId(), scope, keyStates.keySet()), values -> {
                    List<TsKvEntry> updates = new ArrayList<>();
                    values.forEach(latestEntry -> {
                        if (latestEntry.getLastUpdateTs() > keyStates.get(latestEntry.getKey())) {
                            updates.add(new BasicTsKvEntry(latestEntry.getLastUpdateTs(), latestEntry));
                        }
                    });
                    var missedUpdates = updates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                    if (!missedUpdates.isEmpty()) {
                        onAttributesUpdate(subscription.getEntityId(), scope, missedUpdates, TbCallback.EMPTY);
                    }
                },
                e -> log.error("Failed to fetch missed updates.", e), tsCallBackExecutor);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleNewTelemetrySubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void handleNewTelemetrySubscription(TbTimeSeriesSubscription subscription) {
        log.trace("[{}][{}][{}] Processing telemetry subscription for entity [{}]",
                subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getEntityId());
        var entityUpdateInfo = entityUpdates.get(subscription.getEntityId().getId());
        if (entityUpdateInfo != null && entityUpdateInfo.timeSeriesUpdateTs > 0 && subscription.getQueryTs() > entityUpdateInfo.timeSeriesUpdateTs) {
            log.trace("[{}][{}][{}] No need to check for missed updates. time [{}][{}] diff: {}ms",
                    subscription.getTenantId(), subscription.getSessionId(), subscription.getSubscriptionId(), subscription.getQueryTs(), entityUpdateInfo.timeSeriesUpdateTs, subscription.getQueryTs() - entityUpdateInfo.timeSeriesUpdateTs);
            return;
        }

        long curTs = System.currentTimeMillis();

        if (subscription.isLatestValues()) {
            DonAsynchron.withCallback(tsService.findLatest(subscription.getTenantId(), subscription.getEntityId(), subscription.getKeyStates().keySet()),
                    missedUpdates -> {
                        if (missedUpdates != null && !missedUpdates.isEmpty()) {
                            missedUpdates = missedUpdates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                            if (!missedUpdates.isEmpty()) {
                                onTimeSeriesUpdate(subscription.getEntityId(), missedUpdates, TbCallback.EMPTY);
                            }
                        }
                    },
                    e -> log.error("Failed to fetch missed updates.", e),
                    tsCallBackExecutor);
        } else {
            List<ReadTsKvQuery> queries = new ArrayList<>();
            subscription.getKeyStates().forEach((key, value) -> {
                if (curTs > value) {
                    long startTs = subscription.getStartTime() > 0 ? Math.max(subscription.getStartTime(), value + 1L) : (value + 1L);
                    long endTs = subscription.getEndTime() > 0 ? Math.min(subscription.getEndTime(), curTs) : curTs;
                    queries.add(new BaseReadTsKvQuery(key, startTs, endTs, 0, 1000, Aggregation.NONE));
                }
            });
            if (!queries.isEmpty()) {
                DonAsynchron.withCallback(tsService.findAll(subscription.getTenantId(), subscription.getEntityId(), queries),
                        missedUpdates -> {
                            if (missedUpdates != null && !missedUpdates.isEmpty()) {
                                missedUpdates = missedUpdates.stream().filter(u -> u.getValue() != null).collect(Collectors.toList());
                                if (!missedUpdates.isEmpty()) {
                                    onTimeSeriesUpdate(subscription.getEntityId(), missedUpdates, TbCallback.EMPTY);
                                }
                            }
                        },
                        e -> log.error("Failed to fetch missed updates.", e),
                        tsCallBackExecutor);
            }
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbLocalSubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
