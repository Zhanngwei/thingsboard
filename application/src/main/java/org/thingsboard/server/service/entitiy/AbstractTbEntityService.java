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
package org.thingsboard.server.service.entitiy;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AbstractTbEntityService` 是 ThingsBoard Application 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class AbstractTbEntityService {

    /**
     * 是否满足错误信息条件。
     */
    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    protected DbCallbackExecutorService dbExecutor;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    protected TbNotificationEntityService notificationEntityService;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    protected EdgeService edgeService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmService alarmService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    protected AlarmSubscriptionService alarmSubscriptionService;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    protected CustomerService customerService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService tbClusterService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    @Lazy
    private EntitiesVersionControlService vcService;

    /**
     * 功能：删除或清理`Alarms By Originator Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    protected void removeAlarmsByOriginatorId(TenantId tenantId, EntityId entityId) {
        PageData<AlarmId> alarms =
                alarmService.findAlarmIdsByOriginatorId(tenantId, entityId, new TimePageLink(Integer.MAX_VALUE));

        alarms.getData().forEach(alarmId -> alarmService.delAlarm(tenantId, alarmId));
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * 返回：判断结果。
     */
    protected <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * - `notFoundMessage`：待处理消息。
     * 返回：判断结果。
     */
    protected <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * 返回：判断结果。
     */
    protected <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * - `notFoundMessage`：待处理消息。
     * 返回：判断结果。
     */
    protected <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    /**
     * 功能：执行 `emptyId` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    /**
     * 功能：执行 `autoCommit` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, entityIds);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }
}
