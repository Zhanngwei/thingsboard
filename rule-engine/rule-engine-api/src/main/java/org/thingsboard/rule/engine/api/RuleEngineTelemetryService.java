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
 * 1. `RuleEngineTelemetryService` 是 ThingsBoard Rule Engine API 中定义遥测数据能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
