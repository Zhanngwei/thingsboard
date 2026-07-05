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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by igor on 3/12/18.
 */
/**
 * 中文说明：
 * 1. 类目的：`RuleChainDao` 是 ThingsBoard DAO 模块 中的规则链持久化服务类型，用于维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 生命周期：由规则链编辑、部署、导入导出、节点状态保存或规则引擎加载流程调用。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Command。
 */
public interface RuleChainDao extends Dao<RuleChain>, TenantEntityDao, ExportableEntityDao<RuleChainId, RuleChain> {

    /**
     * Find rule chains by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findRuleChainsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find rule chains by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findRuleChainsByTenantIdAndType(UUID tenantId, RuleChainType type, PageLink pageLink);

    /**
     * Find root rule chain by tenantId and type
     *
     * @param tenantId the tenantId
     * @param type the type
     * @return the rule chain object
     */
    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * 返回：处理结果。
     */
    RuleChain findRootRuleChainByTenantIdAndType(UUID tenantId, RuleChainType type);

    /**
     * Find rule chains by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId the edgeId
     * @param pageLink the page link
     * @return the list of rule chain objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findRuleChainsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    /**
     * Find auto assign to edge rule chains by tenantId.
     *
     * @param tenantId the tenantId
     * @return the list of rule chain objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<RuleChain> findAutoAssignToEdgeRuleChainsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    Collection<RuleChain> findByTenantIdAndTypeAndName(TenantId tenantId, RuleChainType type, String name);

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChainDao` 在 ThingsBoard DAO 模块 中承担规则链持久化服务类型职责，核心目的是维护规则链、规则节点、节点状态和规则引擎元数据的数据库访问。
 * 2. 核心流程：读写规则链元数据后触发缓存失效、审计记录和规则引擎侧的配置更新流程。
 * 3. 关键依赖：主要依赖或协作对象包括RuleChainService、RuleNodeStateService、Rule Engine、Actor、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
