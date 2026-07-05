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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`AttributesDao` 是 ThingsBoard DAO 模块 中的属性持久化服务类型，用于处理客户端、共享、服务端属性的保存、查询、删除、缓存和通知边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括AttributesService、AttributesDao、缓存、Transport、Rule Engine 和设备会话。
 * 4. 生命周期：由属性 API、设备传输层或规则链流程触发，并随单次属性读写事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Cache-Aside。
 */
public interface AttributesDao {

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKey`：键。
     * 返回：可能存在的结果。
     */
    Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey);

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKey`：键。
     * 返回：匹配的数据集合。
     */
    List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKey);

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, String attributeType);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attribute`：`attribute` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute);

    /**
     * 功能：删除或清理`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds);

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    List<String> findAllKeysByEntityIdsAndAttributeType(TenantId tenantId, EntityType entityType, List<EntityId> entityIds, String attributeType);
}

/*
 * 本类总结：
 * 1. 核心职责：`AttributesDao` 在 ThingsBoard DAO 模块 中承担属性持久化服务类型职责，核心目的是处理客户端、共享、服务端属性的保存、查询、删除、缓存和通知边界。
 * 2. 核心流程：校验实体和属性键空间后读写数据库，必要时更新缓存并通知上层属性变更流程。
 * 3. 关键依赖：主要依赖或协作对象包括AttributesService、AttributesDao、缓存、Transport、Rule Engine 和设备会话。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
