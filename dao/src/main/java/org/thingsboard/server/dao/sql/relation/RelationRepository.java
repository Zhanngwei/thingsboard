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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.model.sql.RelationCompositeKey;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`RelationRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface RelationRepository
        extends JpaRepository<RelationEntity, RelationCompositeKey>, JpaSpecificationExecutor<RelationEntity> {

    /**
     * 功能：获取关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroup(UUID fromId,
                                                                        String fromType,
                                                                        String relationTypeGroup);

    /**
     * 功能：获取关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * - `relationTypeGroups`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeGroupIn(UUID fromId,
                                                                          String fromType,
                                                                          List<String> relationTypeGroups);

    /**
     * 功能：获取关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByFromIdAndFromTypeAndRelationTypeAndRelationTypeGroup(UUID fromId,
                                                                                       String fromType,
                                                                                       String relationType,
                                                                                       String relationTypeGroup);

    /**
     * 功能：获取关系。
     * 参数：
     * - `toId`：`toId`ID。
     * - `toType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroup(UUID toId,
                                                                    String toType,
                                                                    String relationTypeGroup);

    /**
     * 功能：获取关系。
     * 参数：
     * - `toId`：`toId`ID。
     * - `toType`：类型。
     * - `relationTypeGroups`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeGroupIn(UUID toId,
                                                                      String toType,
                                                                      List<String> relationTypeGroups);

    /**
     * 功能：获取关系。
     * 参数：
     * - `toId`：`toId`ID。
     * - `toType`：类型。
     * - `relationType`：类型。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByToIdAndToTypeAndRelationTypeAndRelationTypeGroup(UUID toId,
                                                                                   String toType,
                                                                                   String relationType,
                                                                                   String relationTypeGroup);

    /**
     * 功能：获取类型。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * 返回：匹配的数据集合。
     */
    List<RelationEntity> findAllByFromIdAndFromType(UUID fromId,
                                                    String fromType);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainType`：类型。
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r FROM RelationEntity r WHERE " +
            "r.relationTypeGroup = 'RULE_NODE' AND r.toType = 'RULE_CHAIN' " +
            "AND r.toId in (SELECT id from RuleChainEntity where type = :ruleChainType )")
    List<RelationEntity> findRuleNodeToRuleChainRelations(@Param("ruleChainType") RuleChainType ruleChainType, Pageable page);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    @Transactional
    <S extends RelationEntity> S save(S entity);

    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Transactional
    void deleteById(RelationCompositeKey id);

    /**
     * 功能：删除或清理类型。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.fromId = :fromId and r.fromType = :fromType")
    void deleteByFromIdAndFromType(@Param("fromId") UUID fromId, @Param("fromType") String fromType);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `toId`：`toId`ID。
     * - `toType`：类型。
     * - `relationTypeGroups`：类型。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.toId = :toId and r.toType = :toType and r.relationTypeGroup in :relationTypeGroups")
    void deleteByToIdAndToTypeAndRelationTypeGroupIn(@Param("toId") UUID toId, @Param("toType") String toType, @Param("relationTypeGroups") List<String> relationTypeGroups);

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `fromId`：`fromId`ID。
     * - `fromType`：类型。
     * - `relationTypeGroups`：类型。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RelationEntity r where r.fromId = :fromId and r.fromType = :fromType and r.relationTypeGroup in :relationTypeGroups")
    void deleteByFromIdAndFromTypeAndRelationTypeGroupIn(@Param("fromId") UUID fromId, @Param("fromType") String fromType, @Param("relationTypeGroups") List<String> relationTypeGroups);

}

/*
 * 本类总结：
 * 1. 核心职责：`RelationRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
