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

import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：为 Rule Engine 提供资产配置缓存读取和监听能力。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的资产配置缓存边界。
 * 3. 协作对象：与资产服务、资产配置服务、元数据节点、缓存刷新流程协作。
 * 4. 生命周期：由 Spring 缓存实现长期存在，监听器随规则节点生命周期注册和移除。
 * 5. 设计原因：规则节点可能频繁读取资产配置，缓存接口可以隔离数据库访问并提供配置变更通知。
 * 6. 技术关联：接口本身不直接涉及事务、MQTT、Actor、数据库；实现直接涉及缓存，可能在未命中时访问数据库，服务 Rule Engine。
 */
public interface RuleEngineAssetProfileCache {

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId);

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    AssetProfile get(TenantId tenantId, AssetId assetId);

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * - `profileListener`：`profileListener` 参数。
     * - `assetlistener`：`assetlistener` 参数。
     * 返回：无。
     */
    void addListener(TenantId tenantId, EntityId listenerId, Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetlistener);

    /**
     * 功能：删除或清理监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * 返回：无。
     */
    void removeListener(TenantId tenantId, EntityId listenerId);

}

/*
 * 本类总结：
 * 1. 核心职责：为规则节点提供资产配置缓存读取和变更监听。
 * 2. 核心流程：节点按资产或配置 ID 读取 AssetProfile，初始化时注册监听，销毁时移除监听。
 * 3. 关键依赖：AssetProfile、AssetId、AssetProfileId、TenantId 和缓存实现。
 * 4. 学习重点：资产配置缓存与设备配置缓存模式一致，用于降低规则节点对数据库的直接依赖。
 */
