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
package org.thingsboard.server.service.sync.ie.importing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Objects;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`BaseEntityImportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
public abstract class BaseEntityImportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityImportService<I, E, D> {

    @Autowired
    @Lazy
    /**
     * 字段说明：
     * 1. 保存 `exportableEntitiesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ExportableEntitiesService exportableEntitiesService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `relationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RelationService relationService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `relationDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private RelationDao relationDao;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsSubService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TelemetrySubscriptionService tsSubService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityActionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected EntityActionService entityActionService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `clusterService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TbClusterService clusterService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityNotificationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TbNotificationEntityService entityNotificationService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `importEntity` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public EntityImportResult<E> importEntity(EntitiesImportCtx ctx, D exportData) throws ThingsboardException {
        EntityImportResult<E> importResult = new EntityImportResult<>();
        ctx.setCurrentImportResult(importResult);
        importResult.setEntityType(getEntityType());
        IdProvider idProvider = new IdProvider(ctx, importResult);

        E entity = exportData.getEntity();
        entity.setExternalId(entity.getId());

        E existingEntity = findExistingEntity(ctx, entity, idProvider);
        importResult.setOldEntity(existingEntity);

        setOwner(ctx.getTenantId(), entity, idProvider);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (existingEntity == null) {
            entity.setId(null);
        } else {
            entity.setId(existingEntity.getId());
            entity.setCreatedTime(existingEntity.getCreatedTime());
        }

        E prepared = prepare(ctx, entity, existingEntity, exportData, idProvider);

        boolean saveOrUpdate = existingEntity == null || compare(ctx, exportData, prepared, existingEntity);

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (saveOrUpdate) {
            E savedEntity = saveOrUpdate(ctx, prepared, exportData, idProvider);
            boolean created = existingEntity == null;
            importResult.setCreated(created);
            importResult.setUpdated(!created);
            importResult.setSavedEntity(savedEntity);
            ctx.putInternalId(exportData.getExternalId(), savedEntity.getId());
        } else {
            importResult.setSavedEntity(existingEntity);
            ctx.putInternalId(exportData.getExternalId(), existingEntity.getId());
            importResult.setUpdatedRelatedEntities(updateRelatedEntitiesIfUnmodified(ctx, prepared, exportData, idProvider));
        }

        processAfterSaved(ctx, importResult, exportData, idProvider);

        return importResult;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `updateRelatedEntitiesIfUnmodified` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean updateRelatedEntitiesIfUnmodified(EntitiesImportCtx ctx, E prepared, D exportData, IdProvider idProvider) {
        return false;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityType` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public abstract EntityType getEntityType();

    /**
     * 方法说明：
     * 1. 职责：执行 `setOwner` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract void setOwner(TenantId tenantId, E entity, IdProvider idProvider);

    /**
     * 方法说明：
     * 1. 职责：执行 `prepare` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract E prepare(EntitiesImportCtx ctx, E entity, E oldEntity, D exportData, IdProvider idProvider);

    /**
     * 方法说明：
     * 1. 职责：执行 `compare` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean compare(EntitiesImportCtx ctx, D exportData, E prepared, E existing) {
        var newCopy = deepCopy(prepared);
        var existingCopy = deepCopy(existing);
        cleanupForComparison(newCopy);
        cleanupForComparison(existingCopy);
        var result = !newCopy.equals(existingCopy);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (result) {
            log.debug("[{}] Found update.", prepared.getId());
            log.debug("[{}] From: {}", prepared.getId(), newCopy);
            log.debug("[{}] To: {}", prepared.getId(), existingCopy);
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deepCopy` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract E deepCopy(E e);

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanupForComparison` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void cleanupForComparison(E e) {
        e.setTenantId(null);
        e.setCreatedTime(0);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveOrUpdate` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected abstract E saveOrUpdate(EntitiesImportCtx ctx, E entity, D exportData, IdProvider idProvider);


    /**
     * 方法说明：
     * 1. 职责：执行 `processAfterSaved` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void processAfterSaved(EntitiesImportCtx ctx, EntityImportResult<E> importResult, D exportData, IdProvider idProvider) throws ThingsboardException {
        E savedEntity = importResult.getSavedEntity();
        E oldEntity = importResult.getOldEntity();

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (importResult.isCreated() || importResult.isUpdated()) {
            importResult.addSendEventsCallback(() -> onEntitySaved(ctx.getUser(), savedEntity, oldEntity));
        }

        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ctx.isUpdateRelations() && exportData.getRelations() != null) {
            importRelations(ctx, exportData.getRelations(), importResult, idProvider);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ctx.isSaveAttributes() && exportData.getAttributes() != null) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (exportData.getAttributes().values().stream().anyMatch(d -> !d.isEmpty())) {
                importResult.setUpdatedRelatedEntities(true);
            }
            importAttributes(ctx.getUser(), exportData.getAttributes(), importResult);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `importRelations` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void importRelations(EntitiesImportCtx ctx, List<EntityRelation> relations, EntityImportResult<E> importResult, IdProvider idProvider) {
        var tenantId = ctx.getTenantId();
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (EntityRelation relation : relations) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!relation.getTo().equals(entity.getId())) {
                    relation.setTo(idProvider.getInternalId(relation.getTo()));
                }
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (!relation.getFrom().equals(entity.getId())) {
                    relation.setFrom(idProvider.getInternalId(relation.getFrom()));
                }
            }

            Map<EntityRelation, EntityRelation> relationsMap = new LinkedHashMap<>();
            relations.forEach(r -> relationsMap.put(r, r));

            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (importResult.getOldEntity() != null) {
                List<EntityRelation> existingRelations = new ArrayList<>();
                // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
                existingRelations.addAll(relationDao.findAllByTo(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                existingRelations.addAll(relationDao.findAllByFrom(tenantId, entity.getId(), RelationTypeGroup.COMMON));
                // dao is used here instead of service to avoid getting cached values, because relationService.deleteRelation will evict value from cache only after transaction is committed

                for (EntityRelation existingRelation : existingRelations) {
                    EntityRelation relation = relationsMap.get(existingRelation);
                    if (relation == null) {
                        importResult.setUpdatedRelatedEntities(true);
                        relationService.deleteRelation(ctx.getTenantId(), existingRelation.getFrom(), existingRelation.getTo(), existingRelation.getType(), existingRelation.getTypeGroup());
                        importResult.addSendEventsCallback(() -> {
                            entityNotificationService.logEntityRelationAction(tenantId, null,
                                    existingRelation, ctx.getUser(), ActionType.RELATION_DELETED, null, existingRelation);
                        });
                    } else if (Objects.equal(relation.getAdditionalInfo(), existingRelation.getAdditionalInfo())) {
                        relationsMap.remove(relation);
                    }
                }
            }
            if (!relationsMap.isEmpty()) {
                importResult.setUpdatedRelatedEntities(true);
                ctx.addRelations(relationsMap.values());
            }
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `importAttributes` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void importAttributes(User user, Map<String, List<AttributeExportData>> attributes, EntityImportResult<E> importResult) {
        E entity = importResult.getSavedEntity();
        importResult.addSaveReferencesCallback(() -> {
            attributes.forEach((scope, attributesExportData) -> {
                List<AttributeKvEntry> attributeKvEntries = attributesExportData.stream()
                        .map(attributeExportData -> {
                            KvEntry kvEntry;
                            String key = attributeExportData.getKey();
                            if (attributeExportData.getStrValue() != null) {
                                kvEntry = new StringDataEntry(key, attributeExportData.getStrValue());
                            } else if (attributeExportData.getBooleanValue() != null) {
                                kvEntry = new BooleanDataEntry(key, attributeExportData.getBooleanValue());
                            } else if (attributeExportData.getDoubleValue() != null) {
                                kvEntry = new DoubleDataEntry(key, attributeExportData.getDoubleValue());
                            } else if (attributeExportData.getLongValue() != null) {
                                kvEntry = new LongDataEntry(key, attributeExportData.getLongValue());
                            } else if (attributeExportData.getJsonValue() != null) {
                                kvEntry = new JsonDataEntry(key, attributeExportData.getJsonValue());
                            } else {
                                throw new IllegalArgumentException("Invalid attribute export data");
                            }
                            return new BaseAttributeKvEntry(kvEntry, attributeExportData.getLastUpdateTs());
                        })
                        .collect(Collectors.toList());
                // fixme: attributes are saved outside the transaction
                tsSubService.saveAndNotify(user.getTenantId(), entity.getId(), scope, attributeKvEntries, new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(@Nullable Void unused) {
                    }

                    @Override
                    public void onFailure(Throwable thr) {
                        log.error("Failed to import attributes for {} {}", entity.getId().getEntityType(), entity.getId(), thr);
                    }
                });
            });
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onEntitySaved` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void onEntitySaved(User user, E savedEntity, E oldEntity) throws ThingsboardException {
        entityNotificationService.logEntityAction(user.getTenantId(), savedEntity.getId(), savedEntity, null,
                oldEntity == null ? ActionType.ADDED : ActionType.UPDATED, user);
    }


    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `findExistingEntity` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected E findExistingEntity(EntitiesImportCtx ctx, E entity, IdProvider idProvider) {
        return (E) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(ctx.getTenantId(), entity.getId()))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entity.getId())))
                .or(() -> {
                    if (ctx.isFindExistingByName()) {
                        return Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndName(ctx.getTenantId(), getEntityType(), entity.getName()));
                    } else {
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `findInternalEntity` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <ID extends EntityId> HasId<ID> findInternalEntity(TenantId tenantId, ID externalId) {
        return (HasId<ID>) Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(tenantId, externalId))
                .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(tenantId, externalId)))
                .orElseThrow(() -> new MissingEntityException(externalId));
    }


    @SuppressWarnings("unchecked")
    @RequiredArgsConstructor
    /**
     * 中文说明：
     * 1. 类目的：`IdProvider` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
     * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Observer。
     */
    protected class IdProvider {
        /**
         * 字段说明：
         * 1. 保存 `ctx` 对应的配置、依赖、上下文或运行期状态。
         * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
         * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
         * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
         * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
         */
        private final EntitiesImportCtx ctx;
        private final EntityImportResult<E> importResult;

        /**
         * 方法说明：
         * 1. 职责：执行 `getInternalId` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public <ID extends EntityId> ID getInternalId(ID externalId) {
            return getInternalId(externalId, true);
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `getInternalId` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public <ID extends EntityId> ID getInternalId(ID externalId, boolean throwExceptionIfNotFound) {
            if (externalId == null || externalId.isNullUid()) return null;

            if (EntityType.TENANT.equals(externalId.getEntityType())) {
                return (ID) ctx.getTenantId();
            }

            EntityId localId = ctx.getInternalId(externalId);
            if (localId != null) {
                return (ID) localId;
            }

            HasId<ID> entity;
            try {
                entity = findInternalEntity(ctx.getTenantId(), externalId);
            } catch (Exception e) {
                if (throwExceptionIfNotFound) {
                    throw e;
                } else {
                    importResult.setUpdatedAllExternalIds(false);
                    return null;
                }
            }
            ctx.putInternalId(externalId, entity.getId());
            return entity.getId();
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `getInternalIdByUuid` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public Optional<EntityId> getInternalIdByUuid(UUID externalUuid, boolean fetchAllUUIDs, Set<EntityType> hints) {
            if (externalUuid.equals(EntityId.NULL_UUID)) return Optional.empty();

            for (EntityType entityType : EntityType.values()) {
                Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                if (externalId.isEmpty()) {
                    continue;
                }
                EntityId internalId = ctx.getInternalId(externalId.get());
                if (internalId != null) {
                    return Optional.of(internalId);
                }
            }

            if (fetchAllUUIDs) {
                Set<EntityType> processLast = Set.of(EntityType.TENANT);
                List<EntityType> entityTypes = new ArrayList<>(hints);
                for (EntityType entityType : EntityType.values()) {
                    if (!hints.contains(entityType) && !processLast.contains(entityType)) {
                        entityTypes.add(entityType);
                    }
                }
                entityTypes.addAll(processLast);

                for (EntityType entityType : entityTypes) {
                    Optional<EntityId> externalId = buildEntityId(entityType, externalUuid);
                    if (externalId.isEmpty() || ctx.isNotFound(externalId.get())) {
                        continue;
                    }
                    EntityId internalId = getInternalId(externalId.get(), false);
                    if (internalId != null) {
                        return Optional.of(internalId);
                    } else {
                        ctx.registerNotFound(externalId.get());
                    }
                }
            }

            importResult.setUpdatedAllExternalIds(false);
            return Optional.empty();
        }

        /**
         * 方法说明：
         * 1. 职责：执行 `buildEntityId` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        private Optional<EntityId> buildEntityId(EntityType entityType, UUID externalUuid) {
            try {
                return Optional.of(EntityIdFactory.getByTypeAndUuid(entityType, externalUuid));
            } catch (Exception e) {
                return Optional.empty();
            }
        }

    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getOldEntityField` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected <T extends EntityId, O> T getOldEntityField(O oldEntity, Function<O, T> getter) {
        return oldEntity == null ? null : getter.apply(oldEntity);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `replaceIdsRecursively` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void replaceIdsRecursively(EntitiesImportCtx ctx, IdProvider idProvider, JsonNode json,
                                         Set<String> skippedRootFields, Pattern includedFieldsPattern,
                                         LinkedHashSet<EntityType> hints) {
        JacksonUtil.replaceUuidsRecursively(json, skippedRootFields, includedFieldsPattern,
                uuid -> idProvider.getInternalIdByUuid(uuid, ctx.isFinalImportAttempt(), hints)
                        .map(EntityId::getId).orElse(uuid), true);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseEntityImportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
