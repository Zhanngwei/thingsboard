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
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.dao.model.sql.NotificationEntity;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `NotificationRepository` 是 ThingsBoard DAO 中定义通知能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    /**
     * 功能：获取状态。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * - `searchText`：`searchText` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.deliveryMethod = :deliveryMethod " +
            "AND n.recipientId = :recipientId AND n.status <> :status " +
            "AND (:searchText is NULL OR ilike(n.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(n.text, concat('%', :searchText, '%')) = true)")
    Page<NotificationEntity> findByDeliveryMethodAndRecipientIdAndStatusNot(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                                            @Param("recipientId") UUID recipientId,
                                                                            @Param("status") NotificationStatus status,
                                                                            @Param("searchText") String searchText,
                                                                            Pageable pageable);

    /**
     * 功能：获取`By Delivery Method And Recipient Id`。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `searchText`：`searchText` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.deliveryMethod = :deliveryMethod AND n.recipientId = :recipientId " +
            "AND (:searchText is NULL OR ilike(n.subject, concat('%', :searchText, '%')) = true " +
            "OR ilike(n.text, concat('%', :searchText, '%')) = true)")
    Page<NotificationEntity> findByDeliveryMethodAndRecipientId(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                                @Param("recipientId") UUID recipientId,
                                                                @Param("searchText") String searchText,
                                                                Pageable pageable);

    /**
     * 功能：更新状态。
     * 参数：
     * - `id`：`id`ID。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * 返回：数值结果。
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status " +
            "WHERE n.id = :id AND n.recipientId = :recipientId AND n.status <> :status")
    int updateStatusByIdAndRecipientId(@Param("id") UUID id,
    /**
     * 功能：统计状态数量。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * 返回：数值结果。
     */
                                       @Param("recipientId") UUID recipientId,
                                       @Param("status") NotificationStatus status);
    int countByDeliveryMethodAndRecipientIdAndStatusNot(NotificationDeliveryMethod deliveryMethod, UUID recipientId, NotificationStatus status);

    /**
     * 功能：删除或清理`By Id And Recipient Id`。
     * 参数：
     * - `id`：`id`ID。
     * - `recipientId`：`recipientId`ID。
     * 返回：数值结果。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.id = :id AND n.recipientId = :recipientId")
    int deleteByIdAndRecipientId(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `requestId`：请求ID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.requestId = :requestId")
    void deleteByRequestId(@Param("requestId") UUID requestId);

    /**
     * 功能：删除或清理`By Recipient Id`。
     * 参数：
     * - `recipientId`：`recipientId`ID。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.recipientId = :recipientId")
    void deleteByRecipientId(@Param("recipientId") UUID recipientId);

    /**
     * 功能：更新状态。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipientId`：`recipientId`ID。
     * - `status`：`status` 参数。
     * 返回：数值结果。
     */
    @Modifying
    @Transactional
    @Query("UPDATE NotificationEntity n SET n.status = :status " +
            "WHERE n.deliveryMethod = :deliveryMethod AND n.recipientId = :recipientId AND n.status <> :status")
    int updateStatusByDeliveryMethodAndRecipientIdAndStatusNot(@Param("deliveryMethod") NotificationDeliveryMethod deliveryMethod,
                                                               @Param("recipientId") UUID recipientId,
                                                               @Param("status") NotificationStatus status);

}
