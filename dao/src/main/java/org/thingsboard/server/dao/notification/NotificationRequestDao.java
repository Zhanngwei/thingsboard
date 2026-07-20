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
package org.thingsboard.server.dao.notification;

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
import org.thingsboard.server.dao.Dao;

import java.util.List;

/**
 * 中文说明：
 * 1. `NotificationRequestDao` 是 ThingsBoard DAO 中定义请求能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface NotificationRequestDao extends Dao<NotificationRequest> {

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<NotificationRequest> findByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<NotificationRequestInfo> findInfosByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    /**
     * 功能：获取`Ids By Rule Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestStatus`：请求对象。
     * - `ruleId`：`ruleId`ID。
     * 返回：匹配的数据集合。
     */
    List<NotificationRequestId> findIdsByRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId);

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleId`：`ruleId`ID。
     * - `originatorEntityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    List<NotificationRequest> findByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId);

    /**
     * 功能：获取状态。
     * 参数：
     * - `status`：`status` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<NotificationRequest> findAllByStatus(NotificationRequestStatus status, PageLink pageLink);

    /**
     * 功能：更新`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * - `requestStatus`：请求对象。
     * - `stats`：`stats` 参数。
     * 返回：无。
     */
    void updateById(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `targetId`：目标对象ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndStatusAndTargetId(TenantId tenantId, NotificationRequestStatus status, NotificationTargetId targetId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `status`：`status` 参数。
     * - `templateId`：`templateId`ID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndStatusAndTemplateId(TenantId tenantId, NotificationRequestStatus status, NotificationTemplateId templateId);

    /**
     * 功能：删除或清理创建时间。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    int removeAllByCreatedTimeBefore(long ts);

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    NotificationRequestInfo findInfoById(TenantId tenantId, NotificationRequestId id);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void removeByTenantId(TenantId tenantId);

}
