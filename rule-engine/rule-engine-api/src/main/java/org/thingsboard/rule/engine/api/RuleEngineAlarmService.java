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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
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
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;

import java.util.Collection;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：为 Rule Engine 提供告警创建、更新、确认、清除、分配、删除和查询的统一 API。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的告警子系统边界。
 * 3. 协作对象：与告警规则节点、告警 DAO/服务实现、实体关系查询、租户隔离和页面查询模型协作。
 * 4. 生命周期：由 Spring 实现类长期存在，规则节点处理告警消息或查询告警状态时通过 {@link TbContext} 获取并调用。
 * 5. 设计原因：告警操作涉及唯一活跃告警约束、状态迁移和查询分页，规则节点通过接口复用统一服务而不是直接访问 DAO。
 * 6. 技术关联：接口本身不直接涉及 MQTT 或 Actor；实现通常涉及数据库、缓存、事务和 Rule Engine 告警流程。
 */
public interface RuleEngineAlarmService {

    /*
     *  New API, since 3.5.
     */

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request);

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
     * - `assignTs`：时间戳。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTs);

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `assignTs`：时间戳。
     * 返回：键值映射结果。
     */
    AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long assignTs);

    // Other API
    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：判断结果。
     */
    Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId);

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
    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

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
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    default ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return Futures.immediateFuture(findAlarmInfoById(tenantId, alarmId));
    }

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
    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus, String assigneeId);

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `orderedEntityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

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
 * 1. 核心职责：为规则节点提供完整告警写入、状态迁移和查询能力。
 * 2. 核心流程：告警节点调用创建/更新/确认/清除等方法，查询节点调用分页和聚合查询方法，服务实现负责数据库和缓存一致性。
 * 3. 关键依赖：Alarm、AlarmInfo、AlarmApiCallResult、TenantId、EntityId、分页查询模型和告警服务实现。
 * 4. 学习重点：告警服务是 Rule Engine 与告警持久化/状态机之间的边界，活跃告警唯一性需要实现层保证原子性。
 */
