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
package org.thingsboard.server.service.entitiy.alarm;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

/**
 * 中文说明：
 * 1. `DefaultTbAlarmCommentService` 是 ThingsBoard Application 中负责告警的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbAlarmCommentService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@AllArgsConstructor
public class DefaultTbAlarmCommentService extends AbstractTbEntityService implements TbAlarmCommentService{

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    private AlarmCommentService alarmCommentService;

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `alarmComment`：`alarmComment` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment saveAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        ActionType actionType = alarmComment.getId() == null ? ActionType.ADDED_COMMENT : ActionType.UPDATED_COMMENT;
        if (user != null) {
            alarmComment.setUserId(user.getId());
        }
        try {
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.createOrUpdateAlarmComment(alarm.getTenantId(), alarmComment));
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getId(), alarm, alarm.getCustomerId(), actionType, user, savedAlarmComment);

            return savedAlarmComment;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(alarm.getTenantId(), emptyId(EntityType.ALARM), alarm, actionType, user, e, alarmComment);
            throw e;
        }
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `alarmComment`：`alarmComment` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void deleteAlarmComment(Alarm alarm, AlarmComment alarmComment, User user) throws ThingsboardException {
        if (alarmComment.getType() == AlarmCommentType.OTHER) {
            alarmComment.setType(AlarmCommentType.SYSTEM);
            alarmComment.setUserId(null);
            alarmComment.setComment(JacksonUtil.newObjectNode().put("text",
                    String.format("User %s deleted his comment",
                            (user.getFirstName() == null || user.getLastName() == null) ? user.getName() : user.getFirstName() + " " + user.getLastName())));
            AlarmComment savedAlarmComment = checkNotNull(alarmCommentService.saveAlarmComment(alarm.getTenantId(), alarmComment));
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getId(), alarm, alarm.getCustomerId(), ActionType.DELETED_COMMENT, user, savedAlarmComment);
        } else {
            throw new ThingsboardException("System comment could not be deleted", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }
}
