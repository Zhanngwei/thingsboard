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
 * 1. 类目的：`RuleChainService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
