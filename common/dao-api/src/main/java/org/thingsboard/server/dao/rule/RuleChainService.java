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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainData;
import org.thingsboard.server.common.data.rule.RuleChainImportResult;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleChainUpdateResult;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Created by igor on 3/12/18.
 */
/**
 * 中文说明：
 * 1. `RuleChainService` 是 ThingsBoard Common 中定义规则链能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RuleChainService extends EntityDaoService {

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：处理结果。
     */
    RuleChain saveRuleChain(RuleChain ruleChain);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    boolean setRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainMetaData`：待处理数据。
     * - `ruleNodeUpdater`：`ruleNodeUpdater` 参数。
     * 返回：处理结果。
     */
    RuleChainUpdateResult saveRuleChainMetaData(TenantId tenantId, RuleChainMetaData ruleChainMetaData, Function<RuleNode, RuleNode> ruleNodeUpdater);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    RuleChainMetaData loadRuleChainMetaData(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：处理结果。
     */
    RuleChain findRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：处理结果。
     */
    RuleNode findRuleNodeById(TenantId tenantId, RuleNodeId ruleNodeId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<RuleChain> findRuleChainByIdAsync(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<RuleNode> findRuleNodeByIdAsync(TenantId tenantId, RuleNodeId ruleNodeId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    RuleChain getRootTenantRuleChain(TenantId tenantId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> getRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> getReferencingRuleChainNodes(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNodeId`：规则节点ID。
     * 返回：匹配的数据集合。
     */
    List<EntityRelation> getRuleNodeRelations(TenantId tenantId, RuleNodeId ruleNodeId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findTenantRuleChainsByType(TenantId tenantId, RuleChainType type, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    Collection<RuleChain> findTenantRuleChainsByTypeAndName(TenantId tenantId, RuleChainType type, String name);

    /**
     * 功能：删除或清理规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    void deleteRuleChainById(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteRuleChainsByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `exportTenantRuleChains` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：处理结果。
     */
    RuleChainData exportTenantRuleChains(TenantId tenantId, PageLink pageLink) throws ThingsboardException;

    /**
     * 功能：执行 `importTenantRuleChains` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainData`：待处理数据。
     * - `overwrite`：`overwrite` 参数。
     * - `ruleNodeUpdater`：`ruleNodeUpdater` 参数。
     * 返回：匹配的数据集合。
     */
    List<RuleChainImportResult> importTenantRuleChains(TenantId tenantId, RuleChainData ruleChainData, boolean overwrite, Function<RuleNode, RuleNode> ruleNodeUpdater);

    /**
     * 功能：执行 `assignRuleChainToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    RuleChain assignRuleChainToEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId);

    /**
     * 功能：执行 `unassignRuleChainFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * - `edgeId`：边缘节点ID。
     * - `remove`：`remove` 参数。
     * 返回：处理结果。
     */
    RuleChain unassignRuleChainFromEdge(TenantId tenantId, RuleChainId ruleChainId, EdgeId edgeId, boolean remove);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    RuleChain getEdgeTemplateRootRuleChain(TenantId tenantId);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    boolean setEdgeTemplateRootRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    boolean setAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：执行 `unsetAutoAssignToEdgeRuleChain` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：判断结果。
     */
    boolean unsetAutoAssignToEdgeRuleChain(TenantId tenantId, RuleChainId ruleChainId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * - `toString`：`toString` 参数。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String name, String toString);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type);

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
    @Deprecated(forRemoval = true, since = "3.6.3")
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
     * 功能：获取`All Rule Nodes By Ids`。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    List<RuleNode> findAllRuleNodesByIds(List<RuleNodeId> ruleNodeIds);

    /**
     * 功能：保存或创建规则节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：处理结果。
     */
    RuleNode saveRuleNode(TenantId tenantId, RuleNode ruleNode);

    /**
     * 功能：删除或清理`Rule Nodes`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    void deleteRuleNodes(TenantId tenantId, RuleChainId ruleChainId);

}
