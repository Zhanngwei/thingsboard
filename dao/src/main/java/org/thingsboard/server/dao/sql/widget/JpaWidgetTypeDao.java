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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WidgetTypeDetailsEntity;
import org.thingsboard.server.dao.model.sql.WidgetTypeInfoEntity;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetCompositeKey;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaWidgetTypeDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Component
@SqlDao
public class JpaWidgetTypeDao extends JpaAbstractDao<WidgetTypeDetailsEntity, WidgetTypeDetails> implements WidgetTypeDao {

    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetTypeRepository widgetTypeRepository;

    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetTypeInfoRepository widgetTypeInfoRepository;

    /**
     * 部件包，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetsBundleWidgetRepository widgetsBundleWidgetRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<WidgetTypeDetailsEntity> getEntityClass() {
        return WidgetTypeDetailsEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<WidgetTypeDetailsEntity, UUID> getRepository() {
        return widgetTypeRepository;
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, UUID widgetTypeId) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeById(widgetTypeId));
    }

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：判断结果。
     */
    @Override
    public boolean existsByTenantIdAndId(TenantId tenantId, UUID widgetTypeId) {
        return widgetTypeRepository.existsByTenantIdAndId(tenantId.getId(), widgetTypeId);
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findSystemWidgetTypes(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findSystemWidgetTypes(
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink, WidgetTypeInfoEntity.SEARCH_COLUMNS_MAP)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findAllTenantWidgetTypesByTenantId(
                                tenantId,
                                NULL_UUID,
                                pageLink.getTextSearch(),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink, WidgetTypeInfoEntity.SEARCH_COLUMNS_MAP)));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantId(UUID tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findTenantWidgetTypesByTenantId(
                                tenantId,
                                pageLink.getTextSearch(),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink, WidgetTypeInfoEntity.SEARCH_COLUMNS_MAP)));
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetType> findWidgetTypesByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesByWidgetsBundleId(widgetsBundleId));
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesDetailsByWidgetsBundleId(widgetsBundleId));
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        boolean deprecatedFilterEnabled = !DeprecatedFilter.ALL.equals(deprecatedFilter);
        boolean deprecatedFilterBool = DeprecatedFilter.DEPRECATED.equals(deprecatedFilter);
        boolean widgetTypesEmpty = widgetTypes == null || widgetTypes.isEmpty();
        return DaoUtil.toPageData(
                widgetTypeInfoRepository
                        .findWidgetTypesInfosByWidgetsBundleId(
                                widgetsBundleId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                fullSearch,
                                deprecatedFilterEnabled,
                                deprecatedFilterBool,
                                widgetTypesEmpty,
                                widgetTypes == null ? Collections.emptyList() : widgetTypes,
                                DaoUtil.toPageable(pageLink, WidgetTypeInfoEntity.SEARCH_COLUMNS_MAP)));
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findWidgetFqnsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return widgetTypeRepository.findWidgetFqnsByWidgetsBundleId(widgetsBundleId);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    @Override
    public WidgetType findByTenantIdAndFqn(UUID tenantId, String fqn) {
        return DaoUtil.getData(widgetTypeRepository.findWidgetTypeByTenantIdAndFqn(tenantId, fqn));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbResourceId`：`tbResourceId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeDetails> findWidgetTypesInfosByTenantIdAndResourceId(UUID tenantId, UUID tbResourceId) {
        return DaoUtil.convertDataList(widgetTypeRepository.findWidgetTypesInfosByTenantIdAndResourceId(tenantId, tbResourceId));
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetFqns`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeId> findWidgetTypeIdsByTenantIdAndFqns(UUID tenantId, List<String> widgetFqns) {
        var idFqnPairs = widgetTypeRepository.findWidgetTypeIdsByTenantIdAndFqns(tenantId, widgetFqns);
        idFqnPairs.sort(Comparator.comparingInt(o -> widgetFqns.indexOf(o.getFqn())));
        return idFqnPairs.stream()
                .map(id -> new WidgetTypeId(id.getId())).collect(Collectors.toList());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `externalId`：`externalId`ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeDetails findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(widgetTypeRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeDetails> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                widgetTypeRepository
                        .findTenantWidgetTypeDetailsByTenantId(
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
    public PageData<WidgetTypeId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(widgetTypeRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink))
                .map(WidgetTypeId::new));
    }

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeId getExternalIdByInternal(WidgetTypeId internalId) {
        return Optional.ofNullable(widgetTypeRepository.getExternalIdById(internalId.getId()))
                .map(WidgetTypeId::new).orElse(null);
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetsBundleWidget> findWidgetsBundleWidgetsByWidgetsBundleId(UUID tenantId, UUID widgetsBundleId) {
        return DaoUtil.convertDataList(widgetsBundleWidgetRepository.findAllByWidgetsBundleId(widgetsBundleId));
    }

    /**
     * 功能：保存或创建部件包。
     * 参数：
     * - `widgetsBundleWidget`：`widgetsBundleWidget` 参数。
     * 返回：无。
     */
    @Override
    public void saveWidgetsBundleWidget(WidgetsBundleWidget widgetsBundleWidget) {
        widgetsBundleWidgetRepository.save(new WidgetsBundleWidgetEntity(widgetsBundleWidget));
    }

    /**
     * 功能：删除或清理部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：无。
     */
    @Override
    public void removeWidgetTypeFromWidgetsBundle(UUID widgetsBundleId, UUID widgetTypeId) {
        widgetsBundleWidgetRepository.deleteById(new WidgetsBundleWidgetCompositeKey(widgetsBundleId, widgetTypeId));
    }

    /**
     * 功能：获取部件。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeId> findAllWidgetTypesIds(PageLink pageLink) {
        return DaoUtil.pageToPageData(widgetTypeRepository.findAllIds(DaoUtil.toPageable(pageLink)).map(WidgetTypeId::new));
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `imageUrl`：`imageUrl` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeInfo> findByTenantAndImageLink(TenantId tenantId, String imageUrl, int limit) {
        return DaoUtil.convertDataList(widgetTypeInfoRepository.findByTenantAndImageUrl(tenantId.getId(), imageUrl, limit));
    }

    /**
     * 功能：获取图片资源。
     * 参数：
     * - `imageUrl`：`imageUrl` 参数。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeInfo> findByImageLink(String imageUrl, int limit) {
        return DaoUtil.convertDataList(widgetTypeInfoRepository.findByImageUrl(imageUrl, limit));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`JpaWidgetTypeDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
