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
package org.thingsboard.server.dao.sql.dashboard;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.dashboard.DashboardInfoDao;
import org.thingsboard.server.dao.model.sql.DashboardInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaDashboardInfoDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
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
public class JpaDashboardInfoDao extends JpaAbstractDao<DashboardInfoEntity, DashboardInfo> implements DashboardInfoDao {

    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    @Autowired
    private DashboardInfoRepository dashboardInfoRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<DashboardInfoEntity> getEntityClass() {
        return DashboardInfoEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<DashboardInfoEntity, UUID> getRepository() {
        return dashboardInfoRepository;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(UUID tenantId, PageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("mobileOrder", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        return DaoUtil.toPageData(dashboardInfoRepository
                .findMobileByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, sortOrders)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        sortOrders.add(new SortOrder("mobileOrder", SortOrder.Direction.ASC));
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        return DaoUtil.toPageData(dashboardInfoRepository
                .findMobileByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, sortOrders)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find dashboards by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(dashboardInfoRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public DashboardInfo findFirstByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(dashboardInfoRepository.findFirstByTenantIdAndTitle(tenantId, name));
    }

    /**
     * 功能：获取`Title By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：文本结果。
     */
    @Override
    public String findTitleById(UUID tenantId, UUID dashboardId) {
        return dashboardInfoRepository.findTitleByTenantIdAndId(tenantId, dashboardId);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<DashboardInfo> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return DaoUtil.convertDataList(dashboardInfoRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, limit));
    }

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageLink`：`imageLink` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<DashboardInfo> findByImageLink(String imageLink, int limit) {
        return DaoUtil.convertDataList(dashboardInfoRepository.findByImageLink(imageLink, limit));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaDashboardInfoDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
