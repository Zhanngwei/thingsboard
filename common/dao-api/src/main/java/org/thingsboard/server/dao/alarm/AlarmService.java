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
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.Collection;
import java.util.Set;


/**
 * 中文说明：
 * 1. 类目的：`AlarmService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface AlarmService extends EntityDaoService {

    /*
     *  New API, since 3.5.
     */

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request);

    /**
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     */
    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * - `alarmCreationEnabled`：`alarmCreationEnabled` 参数。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled);

    /**
     * Designed to update existing alarm. Accepts only part of the alarm fields.
     */
    /**
     * 功能：更新告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult updateAlarm(AlarmUpdateRequest request);

    /**
     * 功能：执行 `acknowledgeAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ackTs`：时间戳。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `clearTs`：时间戳。
     * - `details`：`details` 参数。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details);

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `assigneeId`：`assigneeId`ID。
     * - `ts`：时间戳。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long ts);

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ts`：时间戳。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long ts);

    /**
     * 功能：执行 `delAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult delAlarm(TenantId tenantId, AlarmId alarmId);

    /**
     * 功能：执行 `delAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `checkAndDeleteAlarmType`：类型。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult delAlarm(TenantId tenantId, AlarmId alarmId, boolean checkAndDeleteAlarmType);

    /**
     * 功能：执行 `delAlarmTypes` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `types`：类型。
     * 返回：无。
     */
    void delAlarmTypes(TenantId tenantId, Set<String> types);

    // Other API
    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    /**
     * 功能：获取`Alarms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    /**
     * 功能：获取`Alarms V2`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query);

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query);

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
    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                           AlarmStatus alarmStatus, String assigneeId);

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `orderedEntityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                        AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmId> findAlarmIdsByAssigneeId(TenantId tenantId, UserId userId, PageLink pageLink);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorId`：`originatorId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmId> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, PageLink pageLink);

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteEntityAlarmRecordsByTenantId(TenantId tenantId);

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, AlarmCountQuery query);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink);
}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
