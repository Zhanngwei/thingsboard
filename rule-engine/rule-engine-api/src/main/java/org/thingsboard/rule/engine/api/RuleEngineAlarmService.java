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
     * Designed for atomic operations over active alarms.
     * Only one active alarm may exist for the pair {originatorId, alarmType}
     *
     * 中文说明：
     * 1. 方法职责：创建新告警或更新同一 originator/type 下已有活跃告警。
     * 2. 输入参数：request 包含租户、发起实体、告警类型、严重级别、详情和时间戳等数据。
     * 3. 返回值：AlarmApiCallResult，表示创建/更新后的告警和操作结果。
     * 4. 调用时机：告警节点根据规则消息触发告警创建或活跃告警更新时调用。
     * 5. 调用方：告警创建规则节点、告警相关服务。
     * 6. 使用流程：属于 Rule Engine 告警状态迁移流程。
     * 7. 线程安全：接口无状态；实现必须保证同一 originator/type 活跃告警的并发原子性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常涉及数据库事务、缓存刷新和 Rule Engine 告警处理。
     */
    AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request);

    /**
     * Designed to update existing alarm. Accepts only part of the alarm fields.
     *
     * 中文说明：
     * 1. 方法职责：按请求更新已存在告警的部分字段。
     * 2. 输入参数：request 包含告警 ID、租户和需要更新的字段。
     * 3. 返回值：AlarmApiCallResult，表示更新结果和告警数据。
     * 4. 调用时机：规则节点需要修改告警详情、状态或相关属性时调用。
     * 5. 调用方：告警更新规则节点。
     * 6. 使用流程：属于 Rule Engine 告警更新流程。
     * 7. 线程安全：接口无状态；实现需保证并发更新一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常涉及数据库事务和缓存/通知刷新。
     */
    AlarmApiCallResult updateAlarm(AlarmUpdateRequest request);

    /**
     * 中文说明：
     * 1. 方法职责：确认指定告警。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识，ackTs 是确认时间戳。
     * 3. 返回值：AlarmApiCallResult，表示确认后的告警结果。
     * 4. 调用时机：告警确认节点或用户操作触发确认时调用。
     * 5. 调用方：告警规则节点、告警管理流程。
     * 6. 使用流程：属于 Rule Engine 告警状态迁移流程。
     * 7. 线程安全：实现需保证并发确认/清除的一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库并可能使用事务和缓存。
     */
    AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs);

    /**
     * 中文说明：
     * 1. 方法职责：清除指定告警并记录清除详情。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识，clearTs 是清除时间戳，details 是清除详情。
     * 3. 返回值：AlarmApiCallResult，表示清除后的告警结果。
     * 4. 调用时机：告警清除节点或用户清除操作触发时调用。
     * 5. 调用方：告警规则节点、告警管理流程。
     * 6. 使用流程：属于 Rule Engine 告警关闭流程。
     * 7. 线程安全：实现需保证并发清除、确认、分配操作一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常涉及数据库、事务和缓存刷新。
     */
    AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details);

    /**
     * 中文说明：
     * 1. 方法职责：把告警分配给指定用户。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警，assigneeId 是负责人，assignTs 是分配时间戳。
     * 3. 返回值：AlarmApiCallResult，表示分配后的告警结果。
     * 4. 调用时机：告警分配节点或用户操作触发时调用。
     * 5. 调用方：告警管理流程、告警规则节点。
     * 6. 使用流程：属于告警负责人状态变更流程。
     * 7. 线程安全：实现需保证负责人变更并发一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库并可能触发通知。
     */
    AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTs);

    /**
     * 中文说明：
     * 1. 方法职责：取消告警负责人。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警，assignTs 是取消分配时间戳。
     * 3. 返回值：AlarmApiCallResult，表示取消分配后的告警结果。
     * 4. 调用时机：告警取消分配节点或用户操作触发时调用。
     * 5. 调用方：告警管理流程、告警规则节点。
     * 6. 使用流程：属于告警负责人状态变更流程。
     * 7. 线程安全：实现需保证并发状态变更一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库并可能触发通知。
     */
    AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long assignTs);

    // Other API
    /**
     * 中文说明：
     * 1. 方法职责：删除指定告警。
     * 2. 输入参数：tenantId 是租户边界，alarmId 是告警标识。
     * 3. 返回值：Boolean 表示是否删除成功。
     * 4. 调用时机：告警删除节点或管理流程触发时调用。
     * 5. 调用方：告警规则节点、告警管理 API。
     * 6. 使用流程：属于告警生命周期终止流程。
     * 7. 线程安全：实现需保证删除与查询/更新并发一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常涉及数据库和缓存清理。
     */
    Boolean deleteAlarm(TenantId tenantId, AlarmId alarmId);

    /**
     * 中文说明：
     * 1. 方法职责：异步按 ID 查询告警。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识。
     * 3. 返回值：ListenableFuture 包装的 Alarm。
     * 4. 调用时机：节点或服务需要非阻塞读取告警时调用。
     * 5. 调用方：告警规则节点、异步服务流程。
     * 6. 使用流程：属于 Rule Engine 告警查询流程。
     * 7. 线程安全：接口无状态；实现需保证异步 DAO/缓存并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现可能访问数据库和缓存。
     */
    ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId);

    /**
     * 中文说明：
     * 1. 方法职责：同步按 ID 查询告警。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识。
     * 3. 返回值：匹配的 Alarm。
     * 4. 调用时机：节点需要立即获得告警数据时调用。
     * 5. 调用方：告警规则节点、告警管理流程。
     * 6. 使用流程：属于 Rule Engine 告警查询流程。
     * 7. 线程安全：接口无状态；实现需保证 DAO/缓存并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库或缓存。
     */
    Alarm findAlarmById(TenantId tenantId, AlarmId alarmId);

    /**
     * 中文说明：
     * 1. 方法职责：查询指定发起实体和类型的最新活跃告警。
     * 2. 输入参数：tenantId 是租户，originator 是告警发起实体，type 是告警类型。
     * 3. 返回值：最新活跃 Alarm，未找到时由实现决定返回 null。
     * 4. 调用时机：创建或更新告警前需要判断是否已有活跃告警时调用。
     * 5. 调用方：告警规则节点和告警服务实现。
     * 6. 使用流程：属于活跃告警去重和更新流程。
     * 7. 线程安全：实现需保证与 createAlarm 的并发一致。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库和缓存。
     */
    Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    /**
     * 中文说明：
     * 1. 方法职责：查询指定发起实体和类型的最新告警，不限活跃状态。
     * 2. 输入参数：tenantId 是租户，originator 是告警发起实体，type 是告警类型。
     * 3. 返回值：最新 Alarm。
     * 4. 调用时机：规则节点需要读取最近告警历史状态时调用。
     * 5. 调用方：告警规则节点、告警查询流程。
     * 6. 使用流程：属于 Rule Engine 告警查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库。
     */
    Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type);

    /**
     * 中文说明：
     * 1. 方法职责：按 ID 查询告警扩展信息。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识。
     * 3. 返回值：AlarmInfo，包含告警及扩展展示信息。
     * 4. 调用时机：节点或 UI/API 需要告警详情信息时调用。
     * 5. 调用方：告警规则节点、告警查询流程。
     * 6. 使用流程：属于告警详情查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库和实体信息缓存。
     */
    AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId);

    /**
     * 中文说明：
     * 1. 方法职责：异步按 ID 查询告警扩展信息。
     * 2. 输入参数：tenantId 是租户，alarmId 是告警标识。
     * 3. 返回值：ListenableFuture 包装的 AlarmInfo。
     * 4. 调用时机：调用方需要异步接口但当前默认实现只提供同步查询包装时调用。
     * 5. 调用方：异步告警处理流程。
     * 6. 使用流程：属于 Rule Engine 告警查询兼容流程。
     * 7. 线程安全：默认实现无共享状态，线程安全；实际查询线程安全取决于 findAlarmInfoById 实现。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：默认实现不直接涉及 MQTT/Actor；底层同步查询可能访问数据库/缓存。
     */
    default ListenableFuture<AlarmInfo> findAlarmInfoByIdAsync(TenantId tenantId, AlarmId alarmId) {
        return Futures.immediateFuture(findAlarmInfoById(tenantId, alarmId));
    }

    /**
     * 中文说明：
     * 1. 方法职责：按 AlarmQuery 分页查询租户告警。
     * 2. 输入参数：tenantId 是租户，query 是告警查询条件。
     * 3. 返回值：分页 AlarmInfo。
     * 4. 调用时机：规则节点或管理流程需要查询告警列表时调用。
     * 5. 调用方：告警查询节点、告警管理 API。
     * 6. 使用流程：属于 Rule Engine/管理端告警分页查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库。
     */
    PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query);

    /**
     * 中文说明：
     * 1. 方法职责：按客户和 AlarmQuery 分页查询告警。
     * 2. 输入参数：tenantId 是租户，customerId 是客户，query 是查询条件。
     * 3. 返回值：分页 AlarmInfo。
     * 4. 调用时机：客户视角查询告警列表时调用。
     * 5. 调用方：告警管理 API、客户级规则流程。
     * 6. 使用流程：属于客户隔离的告警查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库。
     */
    PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query);

    /**
     * 中文说明：
     * 1. 方法职责：按新版 AlarmQueryV2 分页查询租户告警。
     * 2. 输入参数：tenantId 是租户，query 是新版告警查询条件。
     * 3. 返回值：分页 AlarmInfo。
     * 4. 调用时机：需要使用 V2 查询能力时调用。
     * 5. 调用方：告警管理 API、规则节点查询流程。
     * 6. 使用流程：属于新版告警查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库。
     */
    PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query);

    /**
     * 中文说明：
     * 1. 方法职责：按客户和新版 AlarmQueryV2 分页查询告警。
     * 2. 输入参数：tenantId 是租户，customerId 是客户，query 是新版查询条件。
     * 3. 返回值：分页 AlarmInfo。
     * 4. 调用时机：客户视角需要使用 V2 查询能力时调用。
     * 5. 调用方：告警管理 API、客户级规则流程。
     * 6. 使用流程：属于客户隔离的新版告警查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库。
     */
    PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query);

    /**
     * 中文说明：
     * 1. 方法职责：查询实体当前满足条件的最高告警严重级别。
     * 2. 输入参数：tenantId 是租户，entityId 是实体，alarmSearchStatus/alarmStatus 是状态过滤，assigneeId 是负责人过滤。
     * 3. 返回值：最高 AlarmSeverity，未命中时由实现返回空或默认值。
     * 4. 调用时机：规则节点或 UI 需要计算实体告警摘要时调用。
     * 5. 调用方：告警查询流程、实体状态展示流程。
     * 6. 使用流程：属于告警聚合查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库，可能使用缓存。
     */
    AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus, AlarmStatus alarmStatus, String assigneeId);

    /**
     * 中文说明：
     * 1. 方法职责：按实体集合和 AlarmDataQuery 查询告警数据。
     * 2. 输入参数：tenantId 是租户，query 是告警数据查询条件，orderedEntityIds 是按调用方顺序排列的实体集合。
     * 3. 返回值：分页 AlarmData。
     * 4. 调用时机：仪表盘、规则节点或 API 需要按实体批量查询告警数据时调用。
     * 5. 调用方：告警数据查询流程。
     * 6. 使用流程：属于 Rule Engine/查询层告警数据聚合流程。
     * 7. 线程安全：实现需保证批量查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库和实体查询服务。
     */
    PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds);

    /**
     * 中文说明：
     * 1. 方法职责：分页查询租户下存在的告警类型。
     * 2. 输入参数：tenantId 是租户，pageLink 是分页和搜索条件。
     * 3. 返回值：分页 EntitySubtype，表示告警类型集合。
     * 4. 调用时机：规则节点配置、UI 查询或过滤条件构建时调用。
     * 5. 调用方：告警管理 API、规则节点配置流程。
     * 6. 使用流程：属于告警元数据查询流程。
     * 7. 线程安全：实现需保证查询并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现通常访问数据库，可能使用缓存。
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
