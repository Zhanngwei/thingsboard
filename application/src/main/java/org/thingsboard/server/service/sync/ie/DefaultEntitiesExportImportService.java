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
package org.thingsboard.server.service.sync.ie;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.impl.BaseEntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.impl.DefaultEntityExportService;
import org.thingsboard.server.service.sync.ie.importing.EntityImportService;
import org.thingsboard.server.service.sync.ie.importing.impl.MissingEntityException;
import org.thingsboard.server.service.sync.vc.LoadEntityException;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultEntitiesExportImportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
public class DefaultEntitiesExportImportService implements EntitiesExportImportService {

    private final Map<EntityType, EntityExportService<?, ?, ?>> exportServices = new HashMap<>();
    private final Map<EntityType, EntityImportService<?, ?, ?>> importServices = new HashMap<>();

    /**
     * 字段说明：
     * 1. 保存 `relationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final RelationService relationService;
    private final RateLimitService rateLimitService;
    /**
     * 字段说明：
     * 1. 保存 `entityNotificationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbNotificationEntityService entityNotificationService;

    protected static final List<EntityType> SUPPORTED_ENTITY_TYPES = List.of(
            EntityType.CUSTOMER, EntityType.RULE_CHAIN, EntityType.TB_RESOURCE,
            EntityType.DASHBOARD, EntityType.ASSET_PROFILE, EntityType.ASSET,
            EntityType.DEVICE_PROFILE, EntityType.DEVICE,
            EntityType.ENTITY_VIEW, EntityType.WIDGET_TYPE, EntityType.WIDGETS_BUNDLE,
            EntityType.NOTIFICATION_TEMPLATE, EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_RULE
    );


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `exportEntity` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_EXPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities export is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }

        EntityType entityType = entityId.getEntityType();
        EntityExportService<I, E, EntityExportData<E>> exportService = getExportService(entityType);

        return exportService.getExportData(ctx, entityId);
    }

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
    public <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws ThingsboardException {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (!rateLimitService.checkRateLimit(LimitedApi.ENTITY_IMPORT, ctx.getTenantId())) {
            throw new ThingsboardException("Rate limit for entities import is exceeded", ThingsboardErrorCode.TOO_MANY_REQUESTS);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (exportData.getEntity() == null || exportData.getEntity().getId() == null) {
            throw new DataValidationException("Invalid entity data");
        }

        EntityType entityType = exportData.getEntityType();
        EntityImportService<I, E, EntityExportData<E>> importService = getImportService(entityType);

        EntityImportResult<E> importResult = importService.importEntity(ctx, exportData);
        ctx.putInternalId(exportData.getExternalId(), importResult.getSavedEntity().getId());

        ctx.addReferenceCallback(exportData.getExternalId(), importResult.getSaveReferencesCallback());
        ctx.addEventCallback(importResult.getSendEventsCallback());
        return importResult;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveReferencesAndRelations` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void saveReferencesAndRelations(EntitiesImportCtx ctx) throws ThingsboardException {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Map.Entry<EntityId, ThrowingRunnable> callbackEntry : ctx.getReferenceCallbacks().entrySet()) {
            EntityId externalId = callbackEntry.getKey();
            ThrowingRunnable saveReferencesCallback = callbackEntry.getValue();
            try {
                saveReferencesCallback.run();
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (MissingEntityException e) {
                throw new LoadEntityException(externalId, e);
            }
        }

        relationService.saveRelations(ctx.getTenantId(), new ArrayList<>(ctx.getRelations()));

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityRelation relation : ctx.getRelations()) {
            entityNotificationService.logEntityRelationAction(ctx.getTenantId(), null,
                    relation, ctx.getUser(), ActionType.RELATION_ADD_OR_UPDATE, null, relation);
        }
    }


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityTypeComparatorForImport` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Comparator<EntityType> getEntityTypeComparatorForImport() {
        return Comparator.comparing(SUPPORTED_ENTITY_TYPES::indexOf);
    }


    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `getExportService` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityExportService<I, E, D> getExportService(EntityType entityType) {
        EntityExportService<?, ?, ?> exportService = exportServices.get(entityType);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (exportService == null) {
            throw new IllegalArgumentException("Export for entity type " + entityType + " is not supported");
        }
        return (EntityExportService<I, E, D>) exportService;
    }

    @SuppressWarnings("unchecked")
    /**
     * 方法说明：
     * 1. 职责：执行 `getImportService` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private <I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> EntityImportService<I, E, D> getImportService(EntityType entityType) {
        EntityImportService<?, ?, ?> importService = importServices.get(entityType);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (importService == null) {
            throw new IllegalArgumentException("Import for entity type " + entityType + " is not supported");
        }
        return (EntityImportService<I, E, D>) importService;
    }

    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setExportServices` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setExportServices(DefaultEntityExportService<?, ?, ?> defaultExportService,
                                   Collection<BaseEntityExportService<?, ?, ?>> exportServices) {
        exportServices.stream()
                .sorted(Comparator.comparing(exportService -> exportService.getSupportedEntityTypes().size(), Comparator.reverseOrder()))
                .forEach(exportService -> {
                    exportService.getSupportedEntityTypes().forEach(entityType -> {
                        this.exportServices.put(entityType, exportService);
                    });
                });
        SUPPORTED_ENTITY_TYPES.forEach(entityType -> {
            this.exportServices.putIfAbsent(entityType, defaultExportService);
        });
    }

    @Autowired
    /**
     * 方法说明：
     * 1. 职责：执行 `setImportServices` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void setImportServices(Collection<EntityImportService<?, ?, ?>> importServices) {
        importServices.forEach(entityImportService -> {
            this.importServices.put(entityImportService.getEntityType(), entityImportService);
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEntitiesExportImportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
