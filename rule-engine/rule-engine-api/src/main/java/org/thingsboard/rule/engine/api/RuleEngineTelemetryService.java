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
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAndNotify(TenantId tenantId, EntityId entityId, TsKvEntry ts);

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Without Latest And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveWithoutLatestAndNotify(TenantId tenantId, CustomerId id, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAndNotify(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Latest And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void saveLatestAndNotify(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Void> saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, long value, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, String value, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, double value, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Attr And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAttrAndNotify(TenantId tenantId, EntityId entityId, String scope, String key, boolean value, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`And Notify`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void deleteAndNotify(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void deleteLatest(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void deleteAllLatest(TenantId tenantId, EntityId entityId, FutureCallback<Collection<String>> callback);

    /**
     * 功能：删除或清理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `deleteTsKvQueries`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
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
