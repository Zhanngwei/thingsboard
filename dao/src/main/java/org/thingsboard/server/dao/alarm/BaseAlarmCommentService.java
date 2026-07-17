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
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `BaseAlarmCommentService` 是 ThingsBoard DAO 中负责告警的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`AlarmCommentService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService {

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmCommentDao alarmCommentDao;

    /**
     * 告警，保存当前步骤读取或计算得到的内容。
     */
    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment createOrUpdateAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        boolean isCreated = alarmComment.getId() == null;
        AlarmComment result;
        if (isCreated) {
            result = createAlarmComment(tenantId, alarmComment);
        } else {
            result = updateAlarmComment(tenantId, alarmComment);
        }
        if (result != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(result)
                    .entityId(result.getAlarmId()).created(isCreated).build());
        }
        return result;
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment saveAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("Deleting Alarm Comment: {}", alarmComment);
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        AlarmComment result = alarmCommentDao.save(tenantId, alarmComment);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(result)
                .entityId(result.getAlarmId()).build());
        return result;
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId alarmId, PageLink pageLink) {
        log.trace("Executing findAlarmComments by alarmId [{}]", alarmId);
        return alarmCommentDao.findAlarmComments(tenantId, alarmId, pageLink);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmCommentId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmCommentId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findById(tenantId, alarmCommentId.getId());
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    private AlarmComment createAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType(AlarmCommentType.OTHER);
        }
        return alarmCommentDao.save(tenantId, alarmComment);
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `newAlarmComment`：`newAlarmComment` 参数。
     * 返回：处理结果。
     */
    private AlarmComment updateAlarmComment(TenantId tenantId, AlarmComment newAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        AlarmComment existing = alarmCommentDao.findAlarmCommentById(tenantId, newAlarmComment.getId().getId());
        if (existing != null) {
            if (newAlarmComment.getComment() != null) {
                JsonNode comment = newAlarmComment.getComment();
                ((ObjectNode) comment).put("edited", "true");
                ((ObjectNode) comment).put("editedOn", System.currentTimeMillis());
                existing.setComment(comment);
            }
            return alarmCommentDao.save(tenantId, existing);
        }
        return null;
    }
}
