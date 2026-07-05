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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.RuleNodeEntity;
import org.thingsboard.server.dao.rule.RuleNodeDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`JpaRuleNodeDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Slf4j
@Component
@SqlDao
public class JpaRuleNodeDao extends JpaAbstractDao<RuleNodeEntity, RuleNode> implements RuleNodeDao {

    /**
     * 规则节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private RuleNodeRepository ruleNodeRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<RuleNodeEntity> getEntityClass() {
        return RuleNodeEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<RuleNodeEntity, UUID> getRepository() {
        return ruleNodeRepository;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `configurationSearch`：配置对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findRuleNodesByTenantIdAndType(TenantId tenantId, String type, String configurationSearch) {
        return DaoUtil.convertDataList(ruleNodeRepository.findRuleNodesByTenantIdAndType(tenantId.getId(), type, configurationSearch));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNode> findAllRuleNodesByType(String type, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeRepository
                .findAllRuleNodesByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNode> findAllRuleNodesByTypeAndVersionLessThan(String type, int version, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeRepository
                .findAllRuleNodesByTypeAndVersionLessThan(
                        type,
                        version,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `type`：类型。
     * - `version`：`version` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<RuleNodeId> findAllRuleNodeIdsByTypeAndVersionLessThan(String type, int version, PageLink pageLink) {
        return DaoUtil.pageToPageData(ruleNodeRepository
                        .findAllRuleNodeIdsByTypeAndVersionLessThan(
                                type,
                                version,
                                DaoUtil.toPageable(pageLink)))
                .mapData(RuleNodeId::new);
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findAllRuleNodeByIds(List<RuleNodeId> ruleNodeIds) {
        return DaoUtil.convertDataList(ruleNodeRepository.findAllById(
                ruleNodeIds.stream().map(RuleNodeId::getId).collect(Collectors.toList())));
    }

    /**
     * 功能：获取`By External Ids`。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `externalIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNode> findByExternalIds(RuleChainId ruleChainId, List<RuleNodeId> externalIds) {
        return DaoUtil.convertDataList(ruleNodeRepository.findRuleNodesByRuleChainIdAndExternalIdIn(ruleChainId.getId(),
                externalIds.stream().map(RuleNodeId::getId).collect(Collectors.toList())));
    }

    /**
     * 功能：删除或清理`By Id In`。
     * 参数：
     * - `ruleNodeIds`：数据列表。
     * 返回：无。
     */
    @Override
    public void deleteByIdIn(List<RuleNodeId> ruleNodeIds) {
        ruleNodeRepository.deleteAllById(ruleNodeIds.stream().map(RuleNodeId::getId).collect(Collectors.toList()));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_NODE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaRuleNodeDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
