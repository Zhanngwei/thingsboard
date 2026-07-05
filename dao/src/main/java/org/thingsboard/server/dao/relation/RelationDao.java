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
package org.thingsboard.server.dao.relation;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChainType;

import java.util.Collection;
import java.util.List;

/**
 * Created by ashvayka on 25.04.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`RelationDao` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Graph Query。
 */
public interface RelationDao {

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFrom(TenantId tenantId, EntityId from);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup);

    /**
     * 功能：获取`All By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByTo(TenantId tenantId, EntityId to);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findAllByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：获取关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    boolean saveRelation(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：保存或创建`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relations`：数据列表。
     * 返回：无。
     */
    void saveRelations(TenantId tenantId, Collection<EntityRelation> relations);

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    boolean deleteRelation(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup);

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteOutboundRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：删除或清理`Outbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    void deleteOutboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    void deleteInboundRelations(TenantId tenantId, EntityId entity);

    /**
     * 功能：删除或清理`Inbound Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    void deleteInboundRelations(TenantId tenantId, EntityId entity, RelationTypeGroup relationTypeGroup);

    /**
     * 功能：删除或清理`Outbound Relations Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Boolean> deleteOutboundRelationsAsync(TenantId tenantId, EntityId entity);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> findRuleNodeToRuleChainRelations(RuleChainType ruleChainType, int limit);
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationDao` 在 ThingsBoard DAO 模块 中承担实体与关系持久化服务类型职责，核心目的是维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 核心流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
 * 3. 关键依赖：主要依赖或协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
