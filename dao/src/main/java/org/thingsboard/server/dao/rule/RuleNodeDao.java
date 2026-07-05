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
 * 1. 类目的：`RuleNodeDao` 是 ThingsBoard DAO 模块 中的规则链持久化服务类型，用于维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 生命周期：由规则链编辑、部署、导入导出、节点状态保存或规则引擎加载流程调用。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Command。
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

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeDao` 在 ThingsBoard DAO 模块 中承担规则链持久化服务类型职责，核心目的是维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 核心流程：读写规则链元数据后触发缓存失效、审计记录和规则引擎侧的配置更新流程。
 * 3. 关键依赖：主要依赖或协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
