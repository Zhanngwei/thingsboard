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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.NotificationRuleEntity;
import org.thingsboard.server.dao.model.sql.NotificationRuleInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `NotificationRuleRepository` 是 ThingsBoard DAO 中定义通知能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@Repository
public interface NotificationRuleRepository extends JpaRepository<NotificationRuleEntity, UUID>, ExportableEntityRepository<NotificationRuleEntity> {

    String RULE_INFO_QUERY = "SELECT new org.thingsboard.server.dao.model.sql.NotificationRuleInfoEntity(r, t.name, t.configuration) " +
            "FROM NotificationRuleEntity r INNER JOIN NotificationTemplateEntity t ON r.templateId = t.id";

    /**
     * 功能：获取搜索文本。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT r FROM NotificationRuleEntity r WHERE r.tenantId = :tenantId " +
            "AND (:searchText is NULL OR ilike(r.name, concat('%', :searchText, '%')) = true)")
    Page<NotificationRuleEntity> findByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                             @Param("searchText") String searchText,
                                                             Pageable pageable);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchString`：`searchString` 参数。
     * 返回：判断结果。
     */
    @Query("SELECT count(r) > 0 FROM NotificationRuleEntity r WHERE r.tenantId = :tenantId " +
            "AND CAST(r.recipientsConfig AS text) LIKE concat('%', :searchString, '%')")
    boolean existsByTenantIdAndRecipientsConfigContaining(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `triggerType`：类型。
     * - `enabled`：`enabled` 参数。
     * 返回：匹配的数据集合。
     */
                                                          @Param("searchString") String searchString);
    List<NotificationRuleEntity> findAllByTenantIdAndTriggerTypeAndEnabled(UUID tenantId, NotificationRuleTriggerType triggerType, boolean enabled);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query(RULE_INFO_QUERY + " WHERE r.id = :id")
    NotificationRuleInfoEntity findInfoById(@Param("id") UUID id);

    /**
     * 功能：获取搜索文本。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query(RULE_INFO_QUERY + " WHERE r.tenantId = :tenantId AND (:searchText IS NULL OR ilike(r.name, concat('%', :searchText, '%')) = true)")
    Page<NotificationRuleInfoEntity> findInfosByTenantIdAndSearchText(@Param("tenantId") UUID tenantId,
                                                                      @Param("searchText") String searchText,
                                                                      Pageable pageable);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationRuleEntity r WHERE r.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    NotificationRuleEntity findByTenantIdAndName(UUID tenantId, String name);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    Page<NotificationRuleEntity> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * 功能：获取`External Id By Internal`。
     * 参数：
     * - `internalId`：`internalId`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM NotificationRuleEntity WHERE id = :id")
    UUID getExternalIdByInternal(@Param("id") UUID internalId);

}
