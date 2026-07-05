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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbAlarmService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@AllArgsConstructor
@Slf4j
public class DefaultTbAlarmService extends AbstractTbEntityService implements TbAlarmService {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbAlarmCommentService alarmCommentService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Alarm save(Alarm alarm, User user) throws ThingsboardException {
        ActionType actionType = alarm.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = alarm.getTenantId();
        try {
            AlarmApiCallResult result;
            if (alarm.getId() == null) {
                result = alarmSubscriptionService.createAlarm(AlarmCreateOrUpdateActiveRequest.fromAlarm(alarm, user.getId()));
            } else {
                result = alarmSubscriptionService.updateAlarm(AlarmUpdateRequest.fromAlarm(alarm, user.getId()));
            }
            if (!result.isSuccessful()) {
                throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            AlarmInfo resultAlarm = result.getAlarm();
            if (alarm.isAcknowledged() && !resultAlarm.isAcknowledged()) {
                resultAlarm = ack(resultAlarm, alarm.getAckTs(), user);
            }
            if (alarm.isCleared() && !resultAlarm.isCleared()) {
                resultAlarm = clear(resultAlarm, alarm.getClearTs(), user);
            }
            UserId newAssignee = alarm.getAssigneeId();
            UserId curAssignee = resultAlarm.getAssigneeId();
            if (newAssignee != null && !newAssignee.equals(curAssignee)) {
                resultAlarm = assign(resultAlarm, newAssignee, alarm.getAssignTs(), user);
            } else if (newAssignee == null && curAssignee != null) {
                resultAlarm = unassign(alarm, alarm.getAssignTs(), user);
            }
            if (result.isModified()) {
                notificationEntityService.logEntityAction(tenantId, alarm.getOriginator(), resultAlarm,
                        resultAlarm.getCustomerId(), actionType, user);
            }
            return new Alarm(resultAlarm);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ALARM), alarm, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo ack(Alarm alarm, User user) throws ThingsboardException {
        return ack(alarm, System.currentTimeMillis(), user);
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `ackTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo ack(Alarm alarm, long ackTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.acknowledgeAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(ackTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            String systemComment = String.format("Alarm was acknowledged by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ACK", systemComment);
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_ACK, user);
        } else {
            throw new ThingsboardException("Alarm was already acknowledged!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    /**
     * 功能：执行 `clear` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo clear(Alarm alarm, User user) throws ThingsboardException {
        return clear(alarm, System.currentTimeMillis(), user);
    }

    /**
     * 功能：执行 `clear` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `clearTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo clear(Alarm alarm, long clearTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.clearAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(clearTs), null);
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isCleared()) {
            String systemComment = String.format("Alarm was cleared by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "CLEAR", systemComment);
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_CLEAR, user);
        } else {
            throw new ThingsboardException("Alarm was already cleared!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    /**
     * 功能：执行 `assign` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `assigneeId`：`assigneeId`ID。
     * - `assignTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo assign(Alarm alarm, UserId assigneeId, long assignTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.assignAlarm(alarm.getTenantId(), alarm.getId(), assigneeId, getOrDefault(assignTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            AlarmAssignee assignee = alarmInfo.getAssignee();
            String systemComment = String.format("Alarm was assigned by user %s to user %s", user.getTitle(), assignee.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ASSIGN", systemComment, assignee.getId());
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_ASSIGNED, user);
        } else {
            throw new ThingsboardException("Alarm was already assigned to this user!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    /**
     * 功能：执行 `unassign` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `unassignTs`：时间戳。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo unassign(Alarm alarm, long unassignTs, User user) throws ThingsboardException {
        AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(alarm.getTenantId(), alarm.getId(), getOrDefault(unassignTs));
        if (!result.isSuccessful()) {
            throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        AlarmInfo alarmInfo = result.getAlarm();
        if (result.isModified()) {
            String systemComment = String.format("Alarm was unassigned by user %s", user.getTitle());
            addSystemAlarmComment(alarmInfo, user, "ASSIGN", systemComment);
            notificationEntityService.logEntityAction(alarm.getTenantId(), alarm.getOriginator(), alarmInfo,
                    alarmInfo.getCustomerId(), ActionType.ALARM_UNASSIGNED, user);
        } else {
            throw new ThingsboardException("Alarm was already unassigned!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return alarmInfo;
    }

    /**
     * 功能：执行 `unassignDeletedUserAlarms` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `unassignTs`：时间戳。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<AlarmId> unassignDeletedUserAlarms(TenantId tenantId, User user, long unassignTs) {
        List<AlarmId> totalAlarmIds = new ArrayList<>();
        PageLink pageLink = new PageLink(100, 0, null, new SortOrder("id", SortOrder.Direction.ASC));
        while (true) {
            PageData<AlarmId> pageData = alarmService.findAlarmIdsByAssigneeId(user.getTenantId(), user.getId(), pageLink);
            List<AlarmId> alarmIds = pageData.getData();
            if (alarmIds.isEmpty()) {
                break;
            }
            processAlarmsUnassignment(tenantId, user, alarmIds, unassignTs);
            totalAlarmIds.addAll(alarmIds);
            pageLink = pageLink.nextPageLink();
        }
        return totalAlarmIds;
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * 返回：判断结果。
     */
    @Override
    public Boolean delete(Alarm alarm, User user) {
        TenantId tenantId = alarm.getTenantId();
        notificationEntityService.logEntityAction(tenantId, alarm.getOriginator(), alarm, alarm.getCustomerId(),
                ActionType.ALARM_DELETE, user);
        return alarmSubscriptionService.deleteAlarm(tenantId, alarm.getId());
    }

    /**
     * 功能：获取`Or Default`。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    private static long getOrDefault(long ts) {
        return ts > 0 ? ts : System.currentTimeMillis();
    }

    /**
     * 功能：处理`Alarms Unassignment`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * - `alarmIds`：数据列表。
     * - `unassignTs`：时间戳。
     * 返回：无。
     */
    private void processAlarmsUnassignment(TenantId tenantId, User user, List<AlarmId> alarmIds, long unassignTs) {
        for (AlarmId alarmId : alarmIds) {
            log.trace("[{}] Unassigning alarm {} userId {}", tenantId, alarmId, user.getId());
            AlarmApiCallResult result = alarmSubscriptionService.unassignAlarm(user.getTenantId(), alarmId, unassignTs);
            if (!result.isSuccessful()) {
                log.error("[{}] Cannot unassign alarm {} userId {}", tenantId, alarmId, user.getId());
                continue;
            }
            if (result.isModified()) {
                String comment = String.format("Alarm was unassigned because user %s - was deleted", user.getTitle());
                addSystemAlarmComment(result.getAlarm(), null, "ASSIGN", comment);
                notificationEntityService.logEntityAction(result.getAlarm().getTenantId(), result.getAlarm().getOriginator(), result.getAlarm(), result.getAlarm().getCustomerId(), ActionType.ALARM_UNASSIGNED, null);
            }
        }
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * - `subType`：类型。
     * - `commentText`：`commentText` 参数。
     * 返回：无。
     */
    private void addSystemAlarmComment(Alarm alarm, User user, String subType, String commentText) {
        addSystemAlarmComment(alarm, user, subType, commentText, null);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `user`：`user` 参数。
     * - `subType`：类型。
     * - `commentText`：`commentText` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void addSystemAlarmComment(Alarm alarm, User user, String subType, String commentText, UserId assigneeId) {
        ObjectNode commentNode = JacksonUtil.newObjectNode();
        commentNode.put("text", commentText)
                .put("subtype", subType);
        if (user != null) {
            commentNode.put("userId", user.getId().getId().toString());
        }
        if (assigneeId != null) {
            commentNode.put("assigneeId", assigneeId.getId().toString());
        }
        AlarmComment alarmComment = AlarmComment.builder()
                .alarmId(alarm.getId())
                .type(AlarmCommentType.SYSTEM)
                .comment(commentNode)
                .build();
        try {
            alarmCommentService.saveAlarmComment(alarm, alarmComment, user);
        } catch (ThingsboardException e) {
            log.error("Failed to save alarm comment", e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbAlarmService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
