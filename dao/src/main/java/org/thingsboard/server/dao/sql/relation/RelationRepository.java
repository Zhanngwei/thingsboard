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
 * 1. `RelationRepository` 是 ThingsBoard DAO 中定义实体关系能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
