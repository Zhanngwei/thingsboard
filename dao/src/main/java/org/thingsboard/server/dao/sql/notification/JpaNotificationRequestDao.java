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
package org.thingsboard.server.dao.sql.notification;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTargetId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;
import org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`JpaNotificationRequestDao` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
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
@RequiredArgsConstructor
public class JpaNotificationRequestDao extends JpaAbstractDao<NotificationRequestEntity, NotificationRequest> implements NotificationRequestDao {

    /**
     * 请求，用于读取或保存对应领域对象。
     */
    private final NotificationRequestRepository notificationRequestRepository;

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequest> findByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRequestRepository.findByTenantIdAndOriginatorEntityType(tenantId.getId(),
                originatorType, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequestInfo> findInfosByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return DaoUtil.pageToPageData(notificationRequestRepository.findInfosByTenantIdAndOriginatorEntityTypeAndSearchText(tenantId.getId(),
                        originatorType, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink, Map.of(
                                "templateName", "t.name"
                        ))))
                .mapData(NotificationRequestInfoEntity::toData);
    }

    /**
     * 功能：获取`Ids By Rule Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestStatus`：请求对象。
     * - `ruleId`：`ruleId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationRequestId> findIdsByRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId) {
        return notificationRequestRepository.findAllIdsByStatusAndRuleId(requestStatus, ruleId.getId()).stream()
                .map(NotificationRequestId::new).collect(Collectors.toList());
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleId`：`ruleId`ID。
     * - `originatorEntityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationRequest> findByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId) {
        return DaoUtil.convertDataList(notificationRequestRepository.findAllByRuleIdAndOriginatorEntityIdAndOriginatorEntityType(ruleId.getId(), originatorEntityId.getId(), originatorEntityId.getEntityType()));
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequest> findAllByStatus(NotificationRequestStatus status, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRequestRepository.findAllByStatus(status, DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：更新`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * - `requestStatus`：请求对象。
     * - `stats`：`stats` 参数。
     * 返回：无。
     */
    @Override
    public void updateById(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats) {
        notificationRequestRepository.updateStatusAndStatsById(requestId.getId(), requestStatus, JacksonUtil.valueToTree(stats));
    }

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `targetId`：目标对象ID。
     * 返回：判断结果。
     */
    @Override
    public boolean existsByTenantIdAndStatusAndTargetId(TenantId tenantId, NotificationRequestStatus status, NotificationTargetId targetId) {
        return notificationRequestRepository.existsByTenantIdAndStatusAndTargetsContaining(tenantId.getId(), status, targetId.getId().toString());
    }

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `templateId`：`templateId`ID。
     * 返回：判断结果。
     */
    @Override
    public boolean existsByTenantIdAndStatusAndTemplateId(TenantId tenantId, NotificationRequestStatus status, NotificationTemplateId templateId) {
        return notificationRequestRepository.existsByTenantIdAndStatusAndTemplateId(tenantId.getId(), status, templateId.getId());
    }

    /**
     * 功能：删除或清理创建时间。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    @Override
    public int removeAllByCreatedTimeBefore(long ts) {
        return notificationRequestRepository.deleteAllByCreatedTimeBefore(ts);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequestInfo findInfoById(TenantId tenantId, NotificationRequestId id) {
        NotificationRequestInfoEntity info = notificationRequestRepository.findInfoById(id.getId());
        return info != null ? info.toData() : null;
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationRequestRepository.deleteByTenantId(tenantId.getId());
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<NotificationRequestEntity> getEntityClass() {
        return NotificationRequestEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<NotificationRequestEntity, UUID> getRepository() {
        return notificationRequestRepository;
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_REQUEST;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`JpaNotificationRequestDao` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
