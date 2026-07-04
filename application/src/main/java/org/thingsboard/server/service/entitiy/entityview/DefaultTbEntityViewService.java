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
package org.thingsboard.server.service.entitiy.entityview;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isBlank;

@Service
@AllArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbEntityViewService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultTbEntityViewService extends AbstractTbEntityService implements TbEntityViewService {

    /**
     * 字段说明：
     * 1. 保存 `entityViewService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final EntityViewService entityViewService;
    private final AttributesService attributesService;
    /**
     * 字段说明：
     * 1. 保存 `tsSubService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TelemetrySubscriptionService tsSubService;
    private final TimeseriesService tsService;

    final Map<TenantId, Map<EntityId, List<EntityView>>> localCache = new ConcurrentHashMap<>();

    @Override
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
    public EntityView save(EntityView entityView, EntityView existingEntityView, User user) throws Exception {
        ActionType actionType = entityView.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = entityView.getTenantId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.saveEntityView(entityView));
            this.updateEntityViewAttributes(tenantId, savedEntityView, existingEntityView, user);
            autoCommit(user, savedEntityView.getId());
            notificationEntityService.logEntityAction(savedEntityView.getTenantId(), savedEntityView.getId(), savedEntityView,
                    null, actionType, user);
            // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
            localCache.computeIfAbsent(savedEntityView.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(savedEntityView.getTenantId(), savedEntityView.getId(),
                    entityView.getId() == null ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
            return savedEntityView;
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            notificationEntityService.logEntityAction(user.getTenantId(), emptyId(EntityType.ENTITY_VIEW), entityView, actionType, user, e);
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateEntityViewAttributes` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void updateEntityViewAttributes(TenantId tenantId, EntityView savedEntityView, EntityView oldEntityView, User user) throws ThingsboardException {
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<ListenableFuture<?>> futures = new ArrayList<>();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (oldEntityView != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (oldEntityView.getKeys() != null && oldEntityView.getKeys().getAttributes() != null) {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.CLIENT_SCOPE, oldEntityView.getKeys().getAttributes().getCs(), user));
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SERVER_SCOPE, oldEntityView.getKeys().getAttributes().getSs(), user));
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                futures.add(deleteAttributesFromEntityView(oldEntityView, DataConstants.SHARED_SCOPE, oldEntityView.getKeys().getAttributes().getSh(), user));
            }
            List<String> tsKeys = oldEntityView.getKeys() != null && oldEntityView.getKeys().getTimeseries() != null ?
                    oldEntityView.getKeys().getTimeseries() : Collections.emptyList();
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            futures.add(deleteLatestFromEntityView(oldEntityView, tsKeys, user));
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (savedEntityView.getKeys() != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (savedEntityView.getKeys().getAttributes() != null) {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.CLIENT_SCOPE, savedEntityView.getKeys().getAttributes().getCs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SERVER_SCOPE, savedEntityView.getKeys().getAttributes().getSs(), user));
                futures.add(copyAttributesFromEntityToEntityView(savedEntityView, DataConstants.SHARED_SCOPE, savedEntityView.getKeys().getAttributes().getSh(), user));
            }
            futures.add(copyLatestFromEntityToEntityView(tenantId, savedEntityView));
        }
        for (ListenableFuture<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to copy attributes to entity view", e);
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `delete` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void delete(EntityView entityView, User user) throws ThingsboardException {
        TenantId tenantId = entityView.getTenantId();
        EntityViewId entityViewId = entityView.getId();
        try {
            entityViewService.deleteEntityView(tenantId, entityViewId);
            notificationEntityService.logEntityAction(tenantId, entityViewId, entityView, entityView.getCustomerId(),
                    ActionType.DELETED, user, entityViewId.toString());

            localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>()).clear();
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, entityViewId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    ActionType.DELETED, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEntityViewToCustomer` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityView assignEntityViewToCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId, entityViewId, customerId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, savedEntityView.getCustomerId(),
                    actionType, user, entityViewId.toString(), customerId.toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), customerId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignEntityViewFromCustomer` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityView unassignEntityViewFromCustomer(TenantId tenantId, EntityViewId entityViewId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromCustomer(tenantId, entityViewId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customer.getId(),
                    actionType, user, savedEntityView.getId().toString(), customer.getId().toString(), customer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEntityViewToPublicCustomer` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityView assignEntityViewToPublicCustomer(TenantId tenantId, EntityViewId entityViewId, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToCustomer(tenantId,
                    entityViewId, publicCustomer.getId()));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, savedEntityView.getCustomerId(),
                    actionType, user, savedEntityView.getId().toString(), publicCustomer.getId().toString(), publicCustomer.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `assignEntityViewToEdge` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityView assignEntityViewToEdge(TenantId tenantId, CustomerId customerId, EntityViewId entityViewId, Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_EDGE;
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.assignEntityViewToEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customerId, actionType,
                    user, savedEntityView.getEntityId().toString(), edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignEntityViewFromEdge` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityView unassignEntityViewFromEdge(TenantId tenantId, CustomerId customerId, EntityView entityView,
                                                 Edge edge, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_EDGE;
        EntityViewId entityViewId = entityView.getId();
        EdgeId edgeId = edge.getId();
        try {
            EntityView savedEntityView = checkNotNull(entityViewService.unassignEntityViewFromEdge(tenantId, entityViewId, edgeId));
            notificationEntityService.logEntityAction(tenantId, entityViewId, savedEntityView, customerId, actionType,
                    user, entityViewId.toString(), edgeId.toString(), edge.getName());
            return savedEntityView;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ENTITY_VIEW),
                    actionType, user, e, entityViewId.toString(), edgeId.toString());
            throw e;
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityViewsByTenantIdAndEntityIdAsync` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<List<EntityView>> findEntityViewsByTenantIdAndEntityIdAsync(TenantId tenantId, EntityId entityId) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(tenantId, (k) -> new ConcurrentReferenceHashMap<>());
        List<EntityView> fromLocalCache = localCacheByTenant.get(entityId);
        if (fromLocalCache != null) {
            return Futures.immediateFuture(fromLocalCache);
        }

        ListenableFuture<List<EntityView>> future = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId);

        return Futures.transform(future, (entityViewList) -> {
            localCacheByTenant.put(entityId, entityViewList);
            return entityViewList;
        }, MoreExecutors.directExecutor());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `onComponentLifecycleMsg` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void onComponentLifecycleMsg(ComponentLifecycleMsg componentLifecycleMsg) {
        Map<EntityId, List<EntityView>> localCacheByTenant = localCache.computeIfAbsent(componentLifecycleMsg.getTenantId(), (k) -> new ConcurrentReferenceHashMap<>());
        EntityViewId entityViewId = new EntityViewId(componentLifecycleMsg.getEntityId().getId());
        deleteOldCacheValue(localCacheByTenant, entityViewId);
        if (componentLifecycleMsg.getEvent() != ComponentLifecycleEvent.DELETED) {
            EntityView entityView = entityViewService.findEntityViewById(componentLifecycleMsg.getTenantId(), entityViewId);
            if (entityView != null) {
                localCacheByTenant.remove(entityView.getEntityId());
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deleteOldCacheValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void deleteOldCacheValue(Map<EntityId, List<EntityView>> localCacheByTenant, EntityViewId entityViewId) {
        for (var entry : localCacheByTenant.entrySet()) {
            EntityView toDelete = null;
            for (EntityView view : entry.getValue()) {
                if (entityViewId.equals(view.getId())) {
                    toDelete = view;
                    break;
                }
            }
            if (toDelete != null) {
                entry.getValue().remove(toDelete);
                break;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `copyAttributesFromEntityToEntityView` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<Void>> copyAttributesFromEntityToEntityView(EntityView entityView, String scope, Collection<String> keys, User user) throws ThingsboardException {
        EntityViewId entityId = entityView.getId();
        if (keys != null && !keys.isEmpty()) {
            ListenableFuture<List<AttributeKvEntry>> getAttrFuture = attributesService.find(entityView.getTenantId(), entityView.getEntityId(), scope, keys);
            return Futures.transform(getAttrFuture, attributeKvEntries -> {
                List<AttributeKvEntry> attributes;
                if (attributeKvEntries != null && !attributeKvEntries.isEmpty()) {
                    attributes =
                            attributeKvEntries.stream()
                                    .filter(attributeKvEntry -> {
                                        long startTime = entityView.getStartTimeMs();
                                        long endTime = entityView.getEndTimeMs();
                                        long lastUpdateTs = attributeKvEntry.getLastUpdateTs();
                                        return startTime == 0 && endTime == 0 ||
                                                (endTime == 0 && startTime < lastUpdateTs) ||
                                                (startTime == 0 && endTime > lastUpdateTs) ||
                                                (startTime < lastUpdateTs && endTime > lastUpdateTs);
                                    }).collect(Collectors.toList());
                    tsSubService.saveAndNotify(entityView.getTenantId(), entityId, scope, attributes, new FutureCallback<Void>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, null);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            try {
                                logAttributesUpdated(entityView.getTenantId(), user, entityId, scope, attributes, t);
                            } catch (ThingsboardException e) {
                                log.error("Failed to log attribute updates", e);
                            }
                        }
                    });
                }
                return null;
            }, MoreExecutors.directExecutor());
        } else {
            return Futures.immediateFuture(null);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `copyLatestFromEntityToEntityView` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<List<Void>> copyLatestFromEntityToEntityView(TenantId tenantId, EntityView entityView) {
        EntityViewId entityId = entityView.getId();
        List<String> keys = entityView.getKeys() != null && entityView.getKeys().getTimeseries() != null ?
                entityView.getKeys().getTimeseries() : Collections.emptyList();
        long startTs = entityView.getStartTimeMs();
        long endTs = entityView.getEndTimeMs() == 0 ? Long.MAX_VALUE : entityView.getEndTimeMs();
        ListenableFuture<List<String>> keysFuture;
        if (keys.isEmpty()) {
            keysFuture = Futures.transform(tsService.findAllLatest(tenantId,
                    entityView.getEntityId()), latest -> latest.stream().map(TsKvEntry::getKey).collect(Collectors.toList()), MoreExecutors.directExecutor());
        } else {
            keysFuture = Futures.immediateFuture(keys);
        }
        ListenableFuture<List<TsKvEntry>> latestFuture = Futures.transformAsync(keysFuture, fetchKeys -> {
            List<ReadTsKvQuery> queries = fetchKeys.stream().filter(key -> !isBlank(key)).map(key -> new BaseReadTsKvQuery(key, startTs, endTs, 1, "DESC")).collect(Collectors.toList());
            if (!queries.isEmpty()) {
                return tsService.findAll(tenantId, entityView.getEntityId(), queries);
            } else {
                return Futures.immediateFuture(null);
            }
        }, MoreExecutors.directExecutor());
        return Futures.transform(latestFuture, latestValues -> {
            if (latestValues != null && !latestValues.isEmpty()) {
                tsSubService.saveLatestAndNotify(entityView.getTenantId(), entityId, latestValues, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                });
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deleteAttributesFromEntityView` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> deleteAttributesFromEntityView(EntityView entityView, String scope, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteAndNotify(entityView.getTenantId(), entityId, scope, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logAttributesDeleted(entityView.getTenantId(), user, entityId, scope, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log attribute delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            resultFuture.set(null);
        }
        return resultFuture;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deleteLatestFromEntityView` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> deleteLatestFromEntityView(EntityView entityView, List<String> keys, User user) {
        EntityViewId entityId = entityView.getId();
        SettableFuture<Void> resultFuture = SettableFuture.create();
        if (keys != null && !keys.isEmpty()) {
            tsSubService.deleteLatest(entityView.getTenantId(), entityId, keys, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(tmp);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, keys, t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        } else {
            tsSubService.deleteAllLatest(entityView.getTenantId(), entityId, new FutureCallback<Collection<String>>() {
                @Override
                public void onSuccess(@Nullable Collection<String> keys) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, new ArrayList<>(keys), null);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    try {
                        logTimeseriesDeleted(entityView.getTenantId(), user, entityId, Collections.emptyList(), t);
                    } catch (ThingsboardException e) {
                        log.error("Failed to log timeseries delete", e);
                    }
                    resultFuture.setException(t);
                }
            });
        }
        return resultFuture;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logAttributesUpdated` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logAttributesUpdated(TenantId tenantId, User user, EntityId entityId, String scope, List<AttributeKvEntry> attributes, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_UPDATED, user, toException(e), scope, attributes);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logAttributesDeleted` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logAttributesDeleted(TenantId tenantId, User user, EntityId entityId, String scope, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.ATTRIBUTES_DELETED, user, toException(e), scope, keys);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logTimeseriesDeleted` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void logTimeseriesDeleted(TenantId tenantId, User user, EntityId entityId, List<String> keys, Throwable e) throws ThingsboardException {
        notificationEntityService.logEntityAction(tenantId, entityId, ActionType.TIMESERIES_DELETED, user, toException(e), keys);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `toException` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbEntityViewService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
