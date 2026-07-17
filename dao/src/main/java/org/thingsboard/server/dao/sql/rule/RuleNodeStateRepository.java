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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.RuleNodeStateEntity;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleNodeStateRepository` 是 ThingsBoard DAO 中定义规则节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RuleNodeStateRepository extends JpaRepository<RuleNodeStateEntity, UUID> {

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT e FROM RuleNodeStateEntity e WHERE e.ruleNodeId = :ruleNodeId")
    Page<RuleNodeStateEntity> findByRuleNodeId(@Param("ruleNodeId") UUID ruleNodeId, Pageable pageable);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Query("SELECT e FROM RuleNodeStateEntity e WHERE e.ruleNodeId = :ruleNodeId and e.entityId = :entityId")
    RuleNodeStateEntity findByRuleNodeIdAndEntityId(@Param("ruleNodeId") UUID ruleNodeId, @Param("entityId") UUID entityId);

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    void removeByRuleNodeId(@Param("ruleNodeId") UUID ruleNodeId);

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void removeByRuleNodeIdAndEntityId(@Param("ruleNodeId") UUID ruleNodeId, @Param("entityId") UUID entityId);
}
