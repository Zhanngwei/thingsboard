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
package org.thingsboard.server.service.query;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.KvUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.ComplexFilterPredicate;
import org.thingsboard.server.common.data.query.DynamicValue;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.FilterPredicateType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.query.SimpleKeyFilterPredicate;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.subscription.TbAttributeSubscriptionScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@Slf4j
@TbCoreComponent
/**
 * 中文说明：
 * 1. 类目的：`DefaultEntityQueryService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultEntityQueryService implements EntityQueryService {

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

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `dbCallbackExecutor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `timeseriesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesService timeseriesService;

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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `countEntitiesByQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query) {
        return entityService.countEntitiesByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntityDataByQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (query.getKeyFilters() != null) {
            resolveDynamicValuesInPredicates(
                    query.getKeyFilters().stream()
                            .map(KeyFilter::getPredicate)
                            .collect(Collectors.toList()),
                    securityUser
            );
        }
        return entityService.findEntityDataByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `resolveDynamicValuesInPredicates` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void resolveDynamicValuesInPredicates(List<KeyFilterPredicate> predicates, SecurityUser user) {
        predicates.forEach(predicate -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (predicate.getType() == FilterPredicateType.COMPLEX) {
                resolveDynamicValuesInPredicates(
                        ((ComplexFilterPredicate) predicate).getPredicates(),
                        user
                );
            } else {
                setResolvedValue(user, (SimpleKeyFilterPredicate<?>) predicate);
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setResolvedValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setResolvedValue(SecurityUser user, SimpleKeyFilterPredicate<?> predicate) {
        DynamicValue<?> dynamicValue = predicate.getValue().getDynamicValue();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (dynamicValue != null && dynamicValue.getResolvedValue() == null) {
            resolveDynamicValue(dynamicValue, user, predicate.getType());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `resolveDynamicValue` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <T> void resolveDynamicValue(DynamicValue<T> dynamicValue, SecurityUser user, FilterPredicateType predicateType) {
        EntityId entityId;
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (dynamicValue.getSourceType()) {
            case CURRENT_TENANT:
                entityId = user.getTenantId();
                break;
            case CURRENT_CUSTOMER:
                entityId = user.getCustomerId();
                break;
            case CURRENT_USER:
                entityId = user.getId();
                break;
            default:
                throw new RuntimeException("Not supported operation for source type: {" + dynamicValue.getSourceType() + "}");
        }

        try {
            Optional<AttributeKvEntry> valueOpt = attributesService.find(user.getTenantId(), entityId,
                    TbAttributeSubscriptionScope.SERVER_SCOPE.name(), dynamicValue.getSourceAttribute()).get();

            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (valueOpt.isPresent()) {
                AttributeKvEntry entry = valueOpt.get();
                Object resolved = null;
                // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
                switch (predicateType) {
                    case STRING:
                        resolved = KvUtil.getStringValue(entry);
                        break;
                    case NUMERIC:
                        resolved = KvUtil.getDoubleValue(entry);
                        break;
                    case BOOLEAN:
                        resolved = KvUtil.getBoolValue(entry);
                        break;
                    case COMPLEX:
                        break;
                }

                dynamicValue.setResolvedValue((T) resolved);
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findAlarmDataByQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query) {
        EntityDataQuery entityDataQuery = this.buildEntityDataQuery(query);
        PageData<EntityData> entities = entityService.findEntityDataByQuery(securityUser.getTenantId(),
                securityUser.getCustomerId(), entityDataQuery);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entities.getTotalElements() > 0) {
            LinkedHashMap<EntityId, EntityData> entitiesMap = new LinkedHashMap<>();
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (EntityData entityData : entities.getData()) {
                entitiesMap.put(entityData.getEntityId(), entityData);
            }
            PageData<AlarmData> alarms = alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(), query, entitiesMap.keySet());
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (AlarmData alarmData : alarms.getData()) {
                EntityId entityId = alarmData.getEntityId();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (entityId != null) {
                    EntityData entityData = entitiesMap.get(entityId);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (entityData != null) {
                        alarmData.getLatest().putAll(entityData.getLatest());
                    }
                }
            }
            return alarms;
        } else {
            return new PageData<>();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `countAlarmsByQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public long countAlarmsByQuery(SecurityUser securityUser, AlarmCountQuery query) {
        return alarmService.countAlarmsByQuery(securityUser.getTenantId(), securityUser.getCustomerId(), query);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildEntityDataQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EntityDataQuery buildEntityDataQuery(AlarmDataQuery query) {
        EntityDataSortOrder sortOrder = query.getPageLink().getSortOrder();
        EntityDataSortOrder entitiesSortOrder;
        if (sortOrder == null || sortOrder.getKey().getType().equals(EntityKeyType.ALARM_FIELD)) {
            entitiesSortOrder = new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, ModelConstants.CREATED_TIME_PROPERTY));
        } else {
            entitiesSortOrder = sortOrder;
        }
        EntityDataPageLink edpl = new EntityDataPageLink(maxEntitiesPerAlarmSubscription, 0, null, entitiesSortOrder);
        return new EntityDataQuery(query.getEntityFilter(), edpl, query.getEntityFields(), query.getLatestValues(), query.getKeyFilters());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getKeysByQuery` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                         boolean isTimeseries, boolean isAttributes, String attributesScope) {
        final DeferredResult<ResponseEntity> response = new DeferredResult<>();
        if (!isAttributes && !isTimeseries) {
            replyWithEmptyResponse(response);
            return response;
        }

        List<EntityId> ids = this.findEntityDataByQuery(securityUser, query).getData().stream()
                .map(EntityData::getEntityId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            replyWithEmptyResponse(response);
            return response;
        }

        Set<EntityType> types = ids.stream().map(EntityId::getEntityType).collect(Collectors.toSet());
        final ListenableFuture<List<String>> timeseriesKeysFuture;
        final ListenableFuture<List<String>> attributesKeysFuture;

        if (isTimeseries) {
            timeseriesKeysFuture = dbCallbackExecutor.submit(() -> timeseriesService.findAllKeysByEntityIds(tenantId, ids));
        } else {
            timeseriesKeysFuture = null;
        }

        if (isAttributes) {
            Map<EntityType, List<EntityId>> typesMap = ids.stream().collect(Collectors.groupingBy(EntityId::getEntityType));
            List<ListenableFuture<List<String>>> futures = new ArrayList<>(typesMap.size());
            typesMap.forEach((type, entityIds) -> futures.add(dbCallbackExecutor.submit(() -> attributesService.findAllKeysByEntityIds(tenantId, type, entityIds, attributesScope))));
            attributesKeysFuture = Futures.transform(Futures.allAsList(futures), lists -> {
                if (CollectionUtils.isEmpty(lists)) {
                    return Collections.emptyList();
                }
                return lists.stream().flatMap(List::stream).distinct().sorted().collect(Collectors.toList());
            }, dbCallbackExecutor);
        } else {
            attributesKeysFuture = null;
        }

        if (isTimeseries && isAttributes) {
            Futures.whenAllComplete(timeseriesKeysFuture, attributesKeysFuture).run(() -> {
                try {
                    replyWithResponse(response, types, timeseriesKeysFuture.get(), attributesKeysFuture.get());
                } catch (Exception e) {
                    log.error("Failed to fetch timeseries and attributes keys!", e);
                    AccessValidator.handleError(e, response, HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }, dbCallbackExecutor);
        } else if (isTimeseries) {
            addCallback(timeseriesKeysFuture, keys -> replyWithResponse(response, types, keys, null),
                    error -> {
                        log.error("Failed to fetch timeseries keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        } else {
            addCallback(attributesKeysFuture, keys -> replyWithResponse(response, types, null, keys),
                    error -> {
                        log.error("Failed to fetch attributes keys!", error);
                        AccessValidator.handleError(error, response, HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }
        return response;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `replyWithResponse` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void replyWithResponse(DeferredResult<ResponseEntity> response, Set<EntityType> types, List<String> timeseriesKeys, List<String> attributesKeys) {
        ObjectNode json = JacksonUtil.newObjectNode();
        addItemsToArrayNode(json.putArray("entityTypes"), types);
        addItemsToArrayNode(json.putArray("timeseries"), timeseriesKeys);
        addItemsToArrayNode(json.putArray("attribute"), attributesKeys);
        response.setResult(new ResponseEntity<>(json, HttpStatus.OK));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `replyWithEmptyResponse` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void replyWithEmptyResponse(DeferredResult<ResponseEntity> response) {
        replyWithResponse(response, Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addItemsToArrayNode` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addItemsToArrayNode(ArrayNode arrayNode, Collection<?> collection) {
        if (!CollectionUtils.isEmpty(collection)) {
            collection.forEach(item -> arrayNode.add(item.toString()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addCallback` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addCallback(ListenableFuture<List<String>> future, Consumer<List<String>> success, Consumer<Throwable> error) {
        Futures.addCallback(future, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> keys) {
                success.accept(keys);
            }

            @Override
            public void onFailure(Throwable t) {
                error.accept(t);
            }
        }, dbCallbackExecutor);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEntityQueryService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
