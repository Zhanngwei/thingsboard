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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Collection;
import java.util.List;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：为 Rule Engine 提供遥测、属性、latest 数据保存、删除和通知的统一 API。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的遥测与属性存储边界。
 * 3. 协作对象：与遥测节点、属性节点、TimeseriesService、AttributesService、设备通知流程和回调执行器协作。
 * 4. 生命周期：由 Spring 实现类长期存在，规则节点处理遥测/属性消息时通过 {@link TbContext} 获取并调用。
 * 5. 设计原因：遥测写入需要同时处理历史数据、latest 数据、属性范围、TTL 和设备通知，接口统一这些持久化语义。
 * 6. 技术关联：接口本身不直接涉及 MQTT 或 Actor；实现通常涉及数据库、缓存、异步回调和 Rule Engine 消息确认。
 */
public interface RuleEngineTelemetryService {

    /**
     * 中文说明：职责是保存单条时序数据并通知订阅者；tenantId/entityId/ts 分别表示租户、实体和值；返回 Future 表示异步完成。
     * 调用方通常是遥测节点；接口本身线程安全取决于实现，不直接涉及 MQTT/Actor，具体实现通常涉及数据库、latest 缓存和 Rule Engine。
     */
    ListenableFuture<Void> saveAndNotify(TenantId tenantId, EntityId entityId, TsKvEntry ts);

    /**
     * 中文说明：职责是批量保存时序数据并通过 callback 通知完成；ts 来自规则消息解析结果。
     * 调用时机是遥测节点处理 POST_TELEMETRY 类消息；接口不直接涉及事务/MQTT/Actor，具体实现通常写数据库并刷新订阅。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是按租户、客户、实体保存带 TTL 的时序数据并通知；id 表示客户上下文，ttl 表示数据保留时间。
     * 调用方为遥测保存节点；线程安全由实现保证，通常涉及数据库 TTL、latest 更新、缓存/订阅通知，不直接涉及 MQTT/Actor。
     */
    void saveAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存历史时序但不更新 latest 值；用于调用方只需要历史记录、不希望改变最新值的场景。
     * 输入含义与 saveAndNotify 类似；实现通常访问时序数据库并发送必要通知，接口本身不涉及事务/MQTT/Actor。
     */
    void saveWithoutLatestAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存属性并通知订阅者；scope 是属性范围，attributes 是待保存键值集合。
     * 调用方通常是属性规则节点；实现可能访问属性数据库、缓存和订阅通知，接口本身不直接涉及 MQTT/Actor。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存属性并按 notifyDevice 决定是否通知设备侧；用于区分服务端属性和需要下发的属性更新。
     * 调用时机是属性节点处理消息时；线程安全和事务由实现保证，接口本身不直接涉及 MQTT/Actor，但实现可能触发设备通知。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是只保存 latest 时序值并通知；ts 是最新值集合。
     * 调用方为只关心最新值更新的规则节点；实现通常涉及 latest 存储/缓存，不直接涉及 MQTT/Actor。
     */
    void saveLatestAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存 long 类型属性并通知；返回 Future 表示异步保存结果。
     * 调用方为属性节点或系统流程；实现通常访问属性数据库和缓存，接口本身不直接涉及事务、MQTT、Actor。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value);

    /**
     * 中文说明：职责是保存 String 类型属性并通知；参数含义为租户、实体、属性范围、键和值。
     * 调用时机是属性写入流程；实现通常涉及数据库/缓存/订阅通知，不直接涉及 MQTT/Actor。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value);

    /**
     * 中文说明：职责是保存 double 类型属性并通知；返回 Future 用于异步确认。
     * 调用方为属性规则节点；接口本身无状态，数据库和缓存行为由实现负责。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value);

    /**
     * 中文说明：职责是保存 boolean 类型属性并通知；用于布尔状态类属性写入。
     * 调用方为属性规则节点或系统流程；接口不直接涉及 MQTT/Actor，具体实现可能通知设备或订阅者。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value);

    /**
     * 中文说明：职责是保存 long 类型属性并通过 callback 返回完成结果。
     * 调用时机是需要回调驱动消息确认的属性节点；实现通常访问数据库/缓存，接口不直接涉及 MQTT/Actor。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存 String 类型属性并通过 callback 返回完成结果。
     * 调用方为属性节点；callback 通常用于 Rule Engine 消息成功/失败路由，数据库和缓存由实现负责。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存 double 类型属性并通过 callback 返回完成结果。
     * 调用时机是属性节点需要异步确认时；接口本身不涉及事务、MQTT、Actor，具体实现涉及存储和通知。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是保存 boolean 类型属性并通过 callback 返回完成结果。
     * 调用方为属性节点或系统流程；实现需保证并发写入一致性并通知订阅者。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是删除指定范围内的一组属性并通知订阅者。
     * keys 来源于规则消息或节点配置；调用方为属性删除节点；实现通常访问属性数据库和缓存，不直接涉及 MQTT/Actor。
     */
    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是删除属性并按 notifyDevice 决定是否通知设备侧。
     * 调用时机是属性删除节点处理消息时；实现可能触发设备属性更新通知，接口本身不直接涉及 MQTT/Actor。
     */
    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是删除 latest 时序键；keys 表示要删除的最新值键集合。
     * 调用方为遥测删除节点；实现通常访问 latest 存储/缓存并通知订阅者，不直接涉及 MQTT/Actor。
     */
    void deleteLatest(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback);

    /**
     * 中文说明：职责是删除实体所有 latest 时序键并返回被删除键集合。
     * callback 接收删除结果；调用方为遥测清理流程；实现通常访问 latest 数据库/缓存，接口本身不直接涉及 MQTT/Actor。
     */
    void deleteAllLatest(TenantId tenantId, EntityId entityId, FutureCallback<Collection<String>> callback);

    /**
     * 中文说明：职责是按时间范围删除时序数据并通知订阅者。
     * keys 是时序键，deleteTsKvQueries 是删除范围条件；调用方为遥测删除节点；实现通常访问时序数据库并刷新订阅，不直接涉及 MQTT/Actor。
     */
    void deleteTimeseriesAndNotify(TenantId tenantId, EntityId entityId, List<String> keys, List<DeleteTsKvQuery> deleteTsKvQueries, FutureCallback<Void> callback);
}

/*
 * 本类总结：
 * 1. 核心职责：为规则节点提供遥测和属性的保存、删除、latest 更新和通知能力。
 * 2. 核心流程：规则节点解析 TbMsg 后调用保存或删除方法，服务实现写入数据库/缓存并通过 FutureCallback 驱动消息确认或失败。
 * 3. 关键依赖：TsKvEntry、AttributeKvEntry、DeleteTsKvQuery、FutureCallback、TenantId、EntityId。
 * 4. 学习重点：遥测服务接口把历史数据、latest 数据、属性范围和通知语义统一到 Rule Engine 可调用的异步 API。
 */
