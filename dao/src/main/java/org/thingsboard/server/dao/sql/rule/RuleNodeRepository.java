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
package org.thingsboard.server.dao.sql.rule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleNodeRepository` 是 ThingsBoard DAO 中定义规则节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RuleNodeRepository extends JpaRepository<RuleNodeEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleType`：类型。
     * - `searchText`：`searchText` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.rule_chain_id in " +
            "(select id from rule_chain rc WHERE rc.tenant_id = :tenantId) AND r.type = :ruleType " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    List<RuleNodeEntity> findRuleNodesByTenantIdAndType(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取类型。
     * 参数：
     * - `ruleType`：类型。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
                                                        @Param("ruleType") String ruleType,
                                                        @Param("searchText") String searchText);

    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.type = :ruleType " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    Page<RuleNodeEntity> findAllRuleNodesByType(@Param("ruleType") String ruleType,
                                                @Param("searchText") String searchText,
                                                Pageable pageable);

    /**
     * 功能：获取类型。
     * 参数：
     * - `ruleType`：类型。
     * - `version`：`version` 参数。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(nativeQuery = true, value = "SELECT * FROM rule_node r WHERE r.type = :ruleType " +
            " AND r.configuration_version < :version " +
            " AND (:searchText IS NULL OR r.configuration ILIKE CONCAT('%', :searchText, '%'))")
    Page<RuleNodeEntity> findAllRuleNodesByTypeAndVersionLessThan(@Param("ruleType") String ruleType,
                                                                  @Param("version") int version,
                                                                  @Param("searchText") String searchText,
                                                                  Pageable pageable);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleType`：类型。
     * - `version`：`version` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r.id FROM RuleNodeEntity r WHERE r.type = :ruleType AND r.configurationVersion < :version")
    Page<UUID> findAllRuleNodeIdsByTypeAndVersionLessThan(@Param("ruleType") String ruleType,
                                                          @Param("version") int version,
                                                          Pageable pageable);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `externalIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<RuleNodeEntity> findRuleNodesByRuleChainIdAndExternalIdIn(UUID ruleChainId, List<UUID> externalIds);

    /**
     * 功能：删除或清理`By Id In`。
     * 参数：
     * - `ids`：数据列表。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RuleNodeEntity e where e.id in :ids")
    void deleteByIdIn(@Param("ids") List<UUID> ids);

}
