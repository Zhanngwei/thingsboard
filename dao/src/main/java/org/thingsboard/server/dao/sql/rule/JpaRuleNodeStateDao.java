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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNodeState;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleNodeStateEntity;
import org.thingsboard.server.dao.rule.RuleNodeStateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaRuleNodeStateDao` 是 ThingsBoard DAO 中负责规则节点存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`RuleNodeStateDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaRuleNodeStateDao extends JpaAbstractDao<RuleNodeStateEntity, RuleNodeState> implements RuleNodeStateDao {

    /**
     * 规则节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleNodeStateRepository ruleNodeStateRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<RuleNodeStateEntity> getEntityClass() {
        return RuleNodeStateEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<RuleNodeStateEntity, UUID> getRepository() {
        return ruleNodeStateRepository;
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNodeState> findByRuleNodeId(UUID ruleNodeId, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeStateRepository.findByRuleNodeId(ruleNodeId, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        return DaoUtil.getData(ruleNodeStateRepository.findByRuleNodeIdAndEntityId(ruleNodeId, entityId));
    }

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    @Transactional
    @Override
    public void removeByRuleNodeId(UUID ruleNodeId) {
        ruleNodeStateRepository.removeByRuleNodeId(ruleNodeId);
    }

    /**
     * 功能：删除或清理规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Transactional
    @Override
    public void removeByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        ruleNodeStateRepository.removeByRuleNodeIdAndEntityId(ruleNodeId, entityId);
    }
}
