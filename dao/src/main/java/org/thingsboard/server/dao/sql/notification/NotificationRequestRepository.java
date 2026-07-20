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

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.dao.model.sql.NotificationRequestEntity;
import org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `NotificationRequestRepository` 是 ThingsBoard DAO 中定义请求能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@Repository
public interface NotificationRequestRepository extends JpaRepository<NotificationRequestEntity, UUID> {

    String REQUEST_INFO_QUERY = "SELECT new org.thingsboard.server.dao.model.sql.NotificationRequestInfoEntity(r, t.name, t.configuration) " +
            "FROM NotificationRequestEntity r LEFT JOIN NotificationTemplateEntity t ON r.templateId = t.id";

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<NotificationRequestEntity> findByTenantIdAndOriginatorEntityType(UUID tenantId, EntityType originatorType, Pageable pageable);

    /**
     * 功能：获取搜索文本。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(REQUEST_INFO_QUERY + " WHERE r.tenantId = :tenantId AND r.originatorEntityType = :originatorType " +
            "AND (:searchText is NULL OR (t.name IS NOT NULL AND ilike(t.name, concat('%', :searchText, '%')) = true))")
    Page<NotificationRequestInfoEntity> findInfosByTenantIdAndOriginatorEntityTypeAndSearchText(@Param("tenantId") UUID tenantId,
                                                                                                @Param("originatorType") EntityType originatorType,
                                                                                                @Param("searchText") String searchText,
                                                                                                Pageable pageable);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query(REQUEST_INFO_QUERY + " WHERE r.id = :id")
    NotificationRequestInfoEntity findInfoById(@Param("id") UUID id);

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `ruleId`：`ruleId`ID。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r.id FROM NotificationRequestEntity r WHERE r.status = :status AND r.ruleId = :ruleId")
    List<UUID> findAllIdsByStatusAndRuleId(@Param("status") NotificationRequestStatus status,
    /**
     * 功能：获取实体ID。
     * 参数：
     * - `ruleId`：`ruleId`ID。
     * - `originatorEntityId`：实体IDID。
     * - `originatorEntityType`：实体对象。
     * 返回：匹配的数据集合。
     */
                                           @Param("ruleId") UUID ruleId);
    List<NotificationRequestEntity> findAllByRuleIdAndOriginatorEntityIdAndOriginatorEntityType(UUID ruleId, UUID originatorEntityId, EntityType originatorEntityType);

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<NotificationRequestEntity> findAllByStatus(NotificationRequestStatus status, Pageable pageable);

    /**
     * 功能：更新状态。
     * 参数：
     * - `id`：`id`ID。
     * - `status`：`status` 参数。
     * - `stats`：`stats` 参数。
     * 返回：无。
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationRequestEntity r SET r.status = :status, r.stats = :stats WHERE r.id = :id")
    void updateStatusAndStatsById(@Param("id") UUID id,
    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `targetIdStr`：`targetIdStr` 参数。
     * 返回：判断结果。
     */
                                  @Param("status") NotificationRequestStatus status,
                                  @Param("stats") JsonNode stats);
    boolean existsByTenantIdAndStatusAndTargetsContaining(UUID tenantId, NotificationRequestStatus status, String targetIdStr);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `templateId`：`templateId`ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndStatusAndTemplateId(UUID tenantId, NotificationRequestStatus status, UUID templateId);

    /**
     * 功能：删除或清理创建时间。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationRequestEntity r WHERE r.createdTime < :ts")
    int deleteAllByCreatedTimeBefore(@Param("ts") long ts);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationRequestEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

}
