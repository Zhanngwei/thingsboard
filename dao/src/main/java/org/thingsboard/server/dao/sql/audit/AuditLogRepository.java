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
package org.thingsboard.server.dao.sql.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AuditLogRepository` 是 ThingsBoard DAO 中定义 `Audit Log` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:actionTypes) IS NULL OR a.actionType in (:actionTypes)) " +
            "AND (:textSearch IS NULL OR ilike(a.entityType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.entityName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.userName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionStatus, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<AuditLogEntity> findByTenantId(
                                 @Param("tenantId") UUID tenantId,
                                 @Param("textSearch") String textSearch,
                                 @Param("startTime") Long startTime,
                                 @Param("endTime") Long endTime,
                                 @Param("actionTypes") List<ActionType> actionTypes,
                                 Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityId`：实体IDID。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.entityType = :entityType AND a.entityId = :entityId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:actionTypes) IS NULL OR a.actionType in (:actionTypes)) " +
            "AND (:textSearch IS NULL OR ilike(a.entityName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.userName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionStatus, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndEntityId(@Param("tenantId") UUID tenantId,
                                                            @Param("entityType") EntityType entityType,
                                                            @Param("entityId") UUID entityId,
                                                            @Param("textSearch") String textSearch,
                                                            @Param("startTime") Long startTime,
                                                            @Param("endTime") Long endTime,
                                                            @Param("actionTypes") List<ActionType> actionTypes,
                                                            Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.customerId = :customerId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:actionTypes) IS NULL OR a.actionType in (:actionTypes)) " +
            "AND (:textSearch IS NULL OR ilike(a.entityType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.entityName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.userName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionStatus, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                              @Param("customerId") UUID customerId,
                                                              @Param("textSearch") String textSearch,
                                                              @Param("startTime") Long startTime,
                                                              @Param("endTime") Long endTime,
                                                              @Param("actionTypes") List<ActionType> actionTypes,
                                                              Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `textSearch`：`textSearch` 参数。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
            "a.tenantId = :tenantId " +
            "AND a.userId = :userId " +
            "AND (:startTime IS NULL OR a.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR a.createdTime <= :endTime) " +
            "AND ((:actionTypes) IS NULL OR a.actionType in (:actionTypes)) " +
            "AND (:textSearch IS NULL OR ilike(a.entityType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.entityName, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionType, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(a.actionStatus, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<AuditLogEntity> findAuditLogsByTenantIdAndUserId(@Param("tenantId") UUID tenantId,
                                                          @Param("userId") UUID userId,
                                                          @Param("textSearch") String textSearch,
                                                          @Param("startTime") Long startTime,
                                                          @Param("endTime") Long endTime,
                                                          @Param("actionTypes") List<ActionType> actionTypes,
                                                          Pageable pageable);

}
