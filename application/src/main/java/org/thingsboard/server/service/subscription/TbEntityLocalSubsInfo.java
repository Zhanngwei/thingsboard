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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Information about the local websocket subscriptions.
 */
@Slf4j
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`TbEntityLocalSubsInfo` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class TbEntityLocalSubsInfo {

    @Getter
    /**
     * 字段说明：
     * 1. 保存 `tenantId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TenantId tenantId;
    @Getter
    /**
     * 字段说明：
     * 1. 保存 `entityId` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final EntityId entityId;
    @Getter
    private final Lock lock = new ReentrantLock();
    @Getter
    private final Set<TbSubscription<?>> subs = ConcurrentHashMap.newKeySet();
    private volatile TbSubscriptionsInfo state = new TbSubscriptionsInfo();

    private final Map<Integer, Set<TbSubscription<?>>> pendingSubs = new ConcurrentHashMap<>();
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `pendingTimeSeriesEvent` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int pendingTimeSeriesEvent;
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `pendingTimeSeriesEventTs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long pendingTimeSeriesEventTs;
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `pendingAttributesEvent` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int pendingAttributesEvent;
    @Getter
    @Setter
    /**
     * 字段说明：
     * 1. 保存 `pendingAttributesEventTs` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long pendingAttributesEventTs;

    /**
     * 字段说明：
     * 1. 保存 `seqNumber` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int seqNumber = 0;

    /**
     * 方法说明：
     * 1. 职责：执行 `add` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbEntitySubEvent add(TbSubscription<?> subscription) {
        log.trace("[{}][{}][{}] Adding: {}", tenantId, entityId, subscription.getSubscriptionId(), subscription);
        boolean created = subs.isEmpty();
        subs.add(subscription);
        TbSubscriptionsInfo newState = created ? state : state.copy();
        boolean stateChanged = false;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (subscription.getType()) {
            case NOTIFICATIONS:
            case NOTIFICATIONS_COUNT:
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!newState.notifications) {
                    newState.notifications = true;
                    stateChanged = true;
                }
                break;
            case ALARMS:
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!newState.alarms) {
                    newState.alarms = true;
                    stateChanged = true;
                }
                break;
            case ATTRIBUTES:
                var attrSub = (TbAttributeSubscription) subscription;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!newState.attrAllKeys) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (attrSub.isAllKeys()) {
                        newState.attrAllKeys = true;
                        stateChanged = true;
                    } else {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (newState.attrKeys == null) {
                            newState.attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                            stateChanged = true;
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        } else if (newState.attrKeys.addAll(attrSub.getKeyStates().keySet())) {
                            stateChanged = true;
                        }
                    }
                }
                break;
            case TIMESERIES:
                var tsSub = (TbTimeSeriesSubscription) subscription;
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!newState.tsAllKeys) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (tsSub.isAllKeys()) {
                        newState.tsAllKeys = true;
                        stateChanged = true;
                    } else {
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        if (newState.tsKeys == null) {
                            newState.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                            stateChanged = true;
                        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                        } else if (newState.tsKeys.addAll(tsSub.getKeyStates().keySet())) {
                            stateChanged = true;
                        }
                    }
                }
                break;
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stateChanged) {
            state = newState;
        }
        if (created) {
            return toEvent(ComponentLifecycleEvent.CREATED);
        } else if (stateChanged) {
            return toEvent(ComponentLifecycleEvent.UPDATED);
        } else {
            return null;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `remove` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbEntitySubEvent remove(TbSubscription<?> sub) {
        log.trace("[{}][{}][{}] Removing: {}", tenantId, entityId, sub.getSubscriptionId(), sub);
        if (!subs.remove(sub)) {
            return null;
        }
        if (subs.isEmpty()) {
            return toEvent(ComponentLifecycleEvent.DELETED);
        }
        TbSubscriptionsInfo oldState = state.copy();
        TbSubscriptionsInfo newState = new TbSubscriptionsInfo();
        for (TbSubscription<?> subscription : subs) {
            switch (subscription.getType()) {
                case NOTIFICATIONS:
                case NOTIFICATIONS_COUNT:
                    if (!newState.notifications) {
                        newState.notifications = true;
                    }
                    break;
                case ALARMS:
                    if (!newState.alarms) {
                        newState.alarms = true;
                    }
                    break;
                case ATTRIBUTES:
                    var attrSub = (TbAttributeSubscription) subscription;
                    if (!newState.attrAllKeys && attrSub.isAllKeys()) {
                        newState.attrAllKeys = true;
                        continue;
                    }
                    if (newState.attrKeys == null) {
                        newState.attrKeys = new HashSet<>(attrSub.getKeyStates().keySet());
                    } else {
                        newState.attrKeys.addAll(attrSub.getKeyStates().keySet());
                    }
                    break;
                case TIMESERIES:
                    var tsSub = (TbTimeSeriesSubscription) subscription;
                    if (!newState.tsAllKeys && tsSub.isAllKeys()) {
                        newState.tsAllKeys = true;
                        continue;
                    }
                    if (newState.tsKeys == null) {
                        newState.tsKeys = new HashSet<>(tsSub.getKeyStates().keySet());
                    } else {
                        newState.tsKeys.addAll(tsSub.getKeyStates().keySet());
                    }
                    break;
            }
        }
        if (newState.equals(oldState)) {
            return null;
        } else {
            this.state = newState;
            return toEvent(ComponentLifecycleEvent.UPDATED);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toEvent` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbEntitySubEvent toEvent(ComponentLifecycleEvent type) {
        seqNumber++;
        var result = TbEntitySubEvent.builder().tenantId(tenantId).entityId(entityId).type(type).seqNumber(seqNumber);
        if (!ComponentLifecycleEvent.DELETED.equals(type)) {
            result.info(state.copy(seqNumber));
        }
        return result.build();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `isNf` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isNf() {
        return state.notifications;
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `isEmpty` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public boolean isEmpty() {
        return state.isEmpty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `registerPendingSubscription` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TbSubscription<?> registerPendingSubscription(TbSubscription<?> subscription, TbEntitySubEvent event) {
        if (TbSubscriptionType.ATTRIBUTES.equals(subscription.getType())) {
            if (event != null) {
                log.trace("[{}][{}] Registering new pending attributes subscription event: {} for subscription: {}", tenantId, entityId, event.getSeqNumber(), subscription.getSubscriptionId());
                pendingAttributesEvent = event.getSeqNumber();
                pendingAttributesEventTs = System.currentTimeMillis();
                pendingSubs.computeIfAbsent(pendingAttributesEvent, e -> new HashSet<>()).add(subscription);
            } else if (pendingAttributesEvent > 0) {
                log.trace("[{}][{}] Registering pending attributes subscription {} for event: {} ", tenantId, entityId, subscription.getSubscriptionId(), pendingAttributesEvent);
                pendingSubs.computeIfAbsent(pendingAttributesEvent, e -> new HashSet<>()).add(subscription);
            } else {
                return subscription;
            }
        } else if (subscription instanceof TbTimeSeriesSubscription) {
            if (event != null) {
                log.trace("[{}][{}] Registering new pending time-series subscription event: {} for subscription: {}", tenantId, entityId, event.getSeqNumber(), subscription.getSubscriptionId());
                pendingTimeSeriesEvent = event.getSeqNumber();
                pendingTimeSeriesEventTs = System.currentTimeMillis();
                pendingSubs.computeIfAbsent(pendingTimeSeriesEvent, e -> new HashSet<>()).add(subscription);
            } else if (pendingTimeSeriesEvent > 0) {
                log.trace("[{}][{}] Registering pending time-series subscription {} for event: {} ", tenantId, entityId, subscription.getSubscriptionId(), pendingTimeSeriesEvent);
                pendingSubs.computeIfAbsent(pendingTimeSeriesEvent, e -> new HashSet<>()).add(subscription);
            } else {
                return subscription;
            }
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `clearPendingSubscriptions` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Set<TbSubscription<?>> clearPendingSubscriptions(int seqNumber) {
        if (pendingTimeSeriesEvent == seqNumber) {
            pendingTimeSeriesEvent = 0;
            pendingTimeSeriesEventTs = 0L;
        } else if (pendingAttributesEvent == seqNumber) {
            pendingAttributesEvent = 0;
            pendingAttributesEventTs = 0L;
        }
        return pendingSubs.remove(seqNumber);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbEntityLocalSubsInfo` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
