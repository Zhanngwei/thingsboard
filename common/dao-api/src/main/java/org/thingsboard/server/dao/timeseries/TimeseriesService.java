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
package org.thingsboard.server.dao.timeseries;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.DeleteTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvLatestRemovingResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`TimeseriesService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface TimeseriesService {

    /**
     * 功能：获取`All By Queries`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<ReadTsKvQueryResult>> findAllByQueries(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries);

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TsKvEntry>> findAll(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries);

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Optional<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, String keys);

    /**
     * 功能：获取`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TsKvEntry>> findLatest(TenantId tenantId, EntityId entityId, Collection<String> keys);

    /**
     * 功能：获取`Latest Sync`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    List<TsKvEntry> findLatestSync(TenantId tenantId, EntityId entityId, Collection<String> keys);

    /**
     * 功能：获取`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TsKvEntry>> findAllLatest(TenantId tenantId, EntityId entityId);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：数据列表。
     * - `ttl`：`ttl` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntry, long ttl);

    /**
     * 功能：保存或创建`Without Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：数据列表。
     * - `ttl`：`ttl` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Integer> saveWithoutLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntry, long ttl);

    /**
     * 功能：保存或创建`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tsKvEntry`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Void>> saveLatest(TenantId tenantId, EntityId entityId, List<TsKvEntry> tsKvEntry);

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TsKvLatestRemovingResult>> remove(TenantId tenantId, EntityId entityId, List<DeleteTsKvQuery> queries);

    /**
     * 功能：删除或清理`Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TsKvLatestRemovingResult>> removeLatest(TenantId tenantId, EntityId entityId, Collection<String> keys);

    /**
     * 功能：删除或清理`All Latest`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Collection<String>> removeAllLatest(TenantId tenantId, EntityId entityId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds);

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：
     * - `systemTtl`：`systemTtl` 参数。
     * 返回：无。
     */
    void cleanup(long systemTtl);
}

/*
 * 本类总结：
 * 1. 核心职责：`TimeseriesService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
