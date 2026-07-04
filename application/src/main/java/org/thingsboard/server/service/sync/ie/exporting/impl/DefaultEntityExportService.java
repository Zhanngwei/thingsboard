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
package org.thingsboard.server.service.sync.ie.exporting.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.sync.ie.AttributeExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.sync.ie.exporting.EntityExportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Primary
/**
 * 中文说明：
 * 1. 类目的：`DefaultEntityExportService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
public class DefaultEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> implements EntityExportService<I, E, D> {

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
    protected ExportableEntitiesService exportableEntitiesService;
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
     * 1. 保存 `attributesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AttributesService attributesService;
    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `imageService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected ImageService imageService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getExportData` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public final D getExportData(EntitiesExportCtx<?> ctx, I entityId) throws ThingsboardException {
        D exportData = newExportData();

        E entity = exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), entityId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entity == null) {
            throw new IllegalArgumentException(entityId.getEntityType() + " [" + entityId.getId() + "] not found");
        }

        exportData.setEntity(entity);
        exportData.setEntityType(entityId.getEntityType());
        setAdditionalExportData(ctx, entity, exportData);

        var externalId = entity.getExternalId() != null ? entity.getExternalId() : entity.getId();
        ctx.putExternalId(entityId, externalId);
        entity.setId(externalId);
        entity.setTenantId(null);

        return exportData;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `setAdditionalExportData` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws ThingsboardException {
        var exportSettings = ctx.getSettings();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (exportSettings.isExportRelations()) {
            List<EntityRelation> relations = exportRelations(ctx, entity);
            relations.forEach(relation -> {
                relation.setFrom(getExternalIdOrElseInternal(ctx, relation.getFrom()));
                relation.setTo(getExternalIdOrElseInternal(ctx, relation.getTo()));
            });
            exportData.setRelations(relations);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (exportSettings.isExportAttributes()) {
            Map<String, List<AttributeExportData>> attributes = exportAttributes(ctx, entity);
            exportData.setAttributes(attributes);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `exportRelations` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<EntityRelation> exportRelations(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<EntityRelation> relations = new ArrayList<>();

        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        List<EntityRelation> inboundRelations = relationDao.findAllByTo(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(inboundRelations);

        // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
        List<EntityRelation> outboundRelations = relationDao.findAllByFrom(ctx.getTenantId(), entity.getId(), RelationTypeGroup.COMMON);
        relations.addAll(outboundRelations);
        return relations;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `exportAttributes` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Map<String, List<AttributeExportData>> exportAttributes(EntitiesExportCtx<?> ctx, E entity) throws ThingsboardException {
        List<String> scopes;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (entity.getId().getEntityType() == EntityType.DEVICE) {
            scopes = List.of(DataConstants.SERVER_SCOPE, DataConstants.SHARED_SCOPE);
        } else {
            scopes = Collections.singletonList(DataConstants.SERVER_SCOPE);
        }
        Map<String, List<AttributeExportData>> attributes = new LinkedHashMap<>();
        scopes.forEach(scope -> {
            try {
                attributes.put(scope, attributesService.findAll(ctx.getTenantId(), entity.getId(), scope).get().stream()
                        .map(attribute -> {
                            AttributeExportData attributeExportData = new AttributeExportData();
                            attributeExportData.setKey(attribute.getKey());
                            attributeExportData.setLastUpdateTs(attribute.getLastUpdateTs());
                            attributeExportData.setStrValue(attribute.getStrValue().orElse(null));
                            attributeExportData.setDoubleValue(attribute.getDoubleValue().orElse(null));
                            attributeExportData.setLongValue(attribute.getLongValue().orElse(null));
                            attributeExportData.setBooleanValue(attribute.getBooleanValue().orElse(null));
                            attributeExportData.setJsonValue(attribute.getJsonValue().orElse(null));
                            return attributeExportData;
                        })
                        .collect(Collectors.toList()));
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return attributes;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getExternalIdOrElseInternal` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected <ID extends EntityId> ID getExternalIdOrElseInternal(EntitiesExportCtx<?> ctx, ID internalId) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (internalId == null || internalId.isNullUid()) return internalId;
        var result = ctx.getExternalId(internalId);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (result == null) {
            result = Optional.ofNullable(exportableEntitiesService.getExternalIdByInternal(internalId))
                    .orElse(internalId);
            ctx.putExternalId(internalId, result);
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getExternalIdOrElseInternalByUuid` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected UUID getExternalIdOrElseInternalByUuid(EntitiesExportCtx<?> ctx, UUID internalUuid) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = ctx.getExternalId(internalId);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (externalId != null) {
                return externalId.getId();
            }
        }
        for (EntityType entityType : EntityType.values()) {
            EntityId internalId;
            try {
                internalId = EntityIdFactory.getByTypeAndUuid(entityType, internalUuid);
            } catch (Exception e) {
                continue;
            }
            EntityId externalId = exportableEntitiesService.getExternalIdByInternal(internalId);
            if (externalId != null) {
                ctx.putExternalId(internalId, externalId);
                return externalId.getId();
            }
        }
        return internalUuid;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `newExportData` 对应的版本同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务、事件监听或同步任务触发时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEntityExportService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
