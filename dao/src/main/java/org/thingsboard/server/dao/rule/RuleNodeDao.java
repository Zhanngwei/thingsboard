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
package org.thingsboard.server.dao.rule;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.Dao;

import java.util.List;

/**
 * Created by igor on 3/12/18.
 */
/**
 * 中文说明：
 * 1. `RuleNodeDao` 是 ThingsBoard DAO 中定义规则节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RuleNodeDao extends Dao<RuleNode> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `configurationSearch`：配置对象。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String configurationSearch);

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink);

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleNode> findAllRuleNodesByTypeAndVersionLessThan(String type, int version, PageLink pageLink);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleNodeId> findAllRuleNodeIdsByTypeAndVersionLessThan(String type, int version, PageLink pageLink);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findAllRuleNodeByIds(List<RuleNodeId> ruleNodeIds);

    /**
     * 功能：获取`By External Ids`。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `externalIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findByExternalIds(RuleChainId ruleChainId, List<RuleNodeId> externalIds);

    /**
     * 功能：删除或清理`By Id In`。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：无。
     */
    void deleteByIdIn(List<RuleNodeId> ruleNodeIds);

}
