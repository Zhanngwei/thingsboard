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
package org.thingsboard.server.service.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.rule.trigger.AlarmTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.entitiy.alarm.TbAlarmCommentService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

import java.util.Collection;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`DefaultAlarmSubscriptionService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultAlarmSubscriptionService extends AbstractSubscriptionService implements AlarmSubscriptionService {

    /**
     * 告警，提供当前类调用的业务操作。
     */
    private final AlarmService alarmService;
    private final TbAlarmCommentService alarmCommentService;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final TbApiUsageReportClient apiUsageClient;
    private final TbApiUsageStateService apiUsageStateService;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final NotificationRuleProcessor notificationRuleProcessor;

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getExecutorPrefix() {
        return "alarm";
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request) {
        boolean creationEnabled = apiUsageStateService.getApiUsageState(request.getTenantId()).isAlarmCreationEnabled();
        var result = alarmService.createAlarm(request, creationEnabled);
        if (result.isCreated()) {
            apiUsageClient.report(request.getTenantId(), null, ApiUsageRecordKey.CREATED_ALARMS_COUNT);
        }
        return withWsCallback(request, result);
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        return withWsCallback(alarmService.updateAlarm(request));
    }

    /**
     * 功能：执行 `acknowledgeAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ackTs`：时间戳。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs) {
        return withWsCallback(alarmService.acknowledgeAlarm(tenantId, alarmId, ackTs));
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `clearTs`：时间戳。
     * - `details`：`details` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details) {
        return withWsCallback(alarmService.clearAlarm(tenantId, alarmId, clearTs, details));
    }

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `assigneeId`：`assigneeId`ID。
     * - `assignTs`：时间戳。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTs) {
        return withWsCallback(alarmService.assignAlarm(tenantId, alarmId, assigneeId, assignTs));
    }

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `assignTs`：时间戳。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long assignTs) {
        return withWsCallback(alarmService.unassignAlarm(tenantId, alarmId, assignTs));
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：判断结果。
     */
    @Override
    public Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId) {
        AlarmApiCallResult result = alarmService.delAlarm(tenantId, alarmId);
        onAlarmDeleted(result);
        return result.isSuccessful();
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmByIdAsync(tenantId, alarmId);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmById(tenantId, alarmId);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId) {
        return alarmService.findAlarmInfoById(tenantId, alarmId);
    }

    /**
     * 功能：获取`Alarms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        return alarmService.findAlarms(tenantId, query);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return alarmService.findCustomerAlarms(tenantId, customerId, query);
    }

    /**
     * 功能：获取`Alarms V2`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        return alarmService.findAlarmsV2(tenantId, query);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        return alarmService.findCustomerAlarmsV2(tenantId, customerId, query);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarmSearchStatus`：`alarmSearchStatus` 参数。
     * - `alarmStatus`：`alarmStatus` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus, String assigneeId) {
        return alarmService.findHighestAlarmSeverity(tenantId, entityId, alarmSearchStatus, alarmStatus, assigneeId);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `orderedEntityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmService.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    @Override
    public Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmService.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink) {
        return alarmService.findAlarmTypesByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `result`：键值映射。
     * 返回：无。
     */
    private void onAlarmUpdated(AlarmApiCallResult result) {
        wsCallBackExecutor.submit(() -> {
            AlarmInfo alarm = result.getAlarm();
            TenantId tenantId = alarm.getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                            subscriptionManagerService.onAlarmUpdate(tenantId, entityId, alarm, TbCallback.EMPTY);
                        }, () -> TbSubscriptionUtils.toAlarmUpdateProto(tenantId, entityId, alarm)
                );
            }
            notificationRuleProcessor.process(AlarmTrigger.builder()
                    .tenantId(tenantId)
                    .alarmUpdate(result)
                    .build());
        });
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `result`：键值映射。
     * 返回：无。
     */
    private void onAlarmDeleted(AlarmApiCallResult result) {
        wsCallBackExecutor.submit(() -> {
            AlarmInfo alarm = result.getAlarm();
            TenantId tenantId = alarm.getTenantId();
            for (EntityId entityId : result.getPropagatedEntitiesList()) {
                forwardToSubscriptionManagerService(tenantId, entityId, subscriptionManagerService -> {
                    subscriptionManagerService.onAlarmDeleted(tenantId, entityId, alarm, TbCallback.EMPTY);
                }, () -> {
                    return TbSubscriptionUtils.toAlarmDeletedProto(tenantId, entityId, alarm);
                });
            }
            notificationRuleProcessor.process(AlarmTrigger.builder()
                    .tenantId(tenantId)
                    .alarmUpdate(result)
                    .build());
        });
    }

    /**
     * 中文说明：
     * 1. 类目的：`AlarmUpdateCallback` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private class AlarmUpdateCallback implements FutureCallback<AlarmApiCallResult> {
        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `result`：键值映射。
         * 返回：无。
         */
        @Override
        public void onSuccess(@Nullable AlarmApiCallResult result) {
            onAlarmUpdated(result);
        }

        /**
         * 功能：处理失败信息。
         * 参数：
         * - `t`：`t` 参数。
         * 返回：无。
         */
        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to update alarm", t);
        }
    }

    /**
     * 功能：执行 `withWsCallback` 对应的处理。
     * 参数：
     * - `result`：键值映射。
     * 返回：键值映射结果。
     */
    private AlarmApiCallResult withWsCallback(AlarmApiCallResult result) {
        return withWsCallback(null, result);
    }

    /**
     * 功能：执行 `withWsCallback` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `result`：键值映射。
     * 返回：键值映射结果。
     */
    private AlarmApiCallResult withWsCallback(AlarmModificationRequest request, AlarmApiCallResult result) {
        if (result.isSuccessful() && result.isModified()) {
            Futures.addCallback(Futures.immediateFuture(result), new AlarmUpdateCallback(), wsCallBackExecutor);
            if (result.isSeverityChanged()) {
                AlarmInfo alarm = result.getAlarm();
                AlarmComment.AlarmCommentBuilder alarmComment = AlarmComment.builder()
                        .alarmId(alarm.getId())
                        .type(AlarmCommentType.SYSTEM)
                        .comment(JacksonUtil.newObjectNode().put("text",
                                String.format("Alarm severity was updated from %s to %s", result.getOldSeverity(), alarm.getSeverity())));
                if (request != null && request.getUserId() != null) {
                    alarmComment.userId(request.getUserId());
                }
                try {
                    alarmCommentService.saveAlarmComment(alarm, alarmComment.build(), null);
                } catch (ThingsboardException e) {
                    log.error("Failed to save alarm comment", e);
                }
            }
        }
        return result;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultAlarmSubscriptionService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
