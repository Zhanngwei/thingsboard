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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestInfo;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `DefaultNotificationRequestService` 是 ThingsBoard DAO 中负责请求的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `NotificationRequestService`、`EntityDaoService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultNotificationRequestService implements NotificationRequestService, EntityDaoService {

    /**
     * 请求，用于读取或保存对应领域对象。
     */
    private final NotificationRequestDao notificationRequestDao;
    private final NotificationDao notificationDao;

    private final NotificationRequestValidator notificationRequestValidator = new NotificationRequestValidator();

    /**
     * 功能：保存或创建请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRequest`：请求对象。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequest saveNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest) {
        notificationRequestValidator.validate(notificationRequest, NotificationRequest::getTenantId);
        return notificationRequestDao.save(tenantId, notificationRequest);
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequest findNotificationRequestById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findById(tenantId, id.getId());
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequestInfo findNotificationRequestInfoById(TenantId tenantId, NotificationRequestId id) {
        return notificationRequestDao.findInfoById(tenantId, id);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequest> findNotificationRequestsByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return notificationRequestDao.findByTenantIdAndOriginatorTypeAndPageLink(tenantId, originatorType, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequestInfo> findNotificationRequestsInfosByTenantIdAndOriginatorType(TenantId tenantId, EntityType originatorType, PageLink pageLink) {
        return notificationRequestDao.findInfosByTenantIdAndOriginatorTypeAndPageLink(tenantId, originatorType, pageLink);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestStatus`：请求对象。
     * - `ruleId`：`ruleId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<NotificationRequestId> findNotificationRequestsIdsByStatusAndRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId) {
        return notificationRequestDao.findIdsByRuleId(tenantId, requestStatus, ruleId);
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
    public List<NotificationRequest> findNotificationRequestsByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId) {
        return notificationRequestDao.findByRuleIdAndOriginatorEntityId(tenantId, ruleId, originatorEntityId);
    }

    /**
     * 功能：删除或清理请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationRequest(TenantId tenantId, NotificationRequestId requestId) {
        notificationRequestDao.removeById(tenantId, requestId.getId());
        notificationDao.deleteByRequestId(tenantId, requestId);
    }

    /**
     * 功能：获取通知。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<NotificationRequest> findScheduledNotificationRequests(PageLink pageLink) {
        return notificationRequestDao.findAllByStatus(NotificationRequestStatus.SCHEDULED, pageLink);
    }

    /**
     * 功能：更新请求。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `requestId`：请求ID。
     * - `requestStatus`：请求对象。
     * - `stats`：`stats` 参数。
     * 返回：无。
     */
    @Override
    public void updateNotificationRequest(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats) {
        notificationRequestDao.updateById(tenantId, requestId, requestStatus, stats);
    }

    // notifications themselves are left in the database until removed by ttl
    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteNotificationRequestsByTenantId(TenantId tenantId) {
        notificationRequestDao.removeByTenantId(tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findNotificationRequestById(tenantId, new NotificationRequestId(entityId.getId())));
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


    /**
     * 中文说明：
     * 1. `NotificationRequestValidator` 是 ThingsBoard DAO 中承载请求信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 直接依赖的类型边界包括 `DataValidator`。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    private static class NotificationRequestValidator extends DataValidator<NotificationRequest> {

    }

}
