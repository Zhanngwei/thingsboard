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
     * 中文说明：
     * 1. 方法职责：按资产配置 ID 获取资产配置。
     * 2. 输入参数：tenantId 是租户边界，assetProfileId 是资产配置标识。
     * 3. 返回值：对应 AssetProfile。
     * 4. 调用时机：规则节点需要根据配置 ID 获取资产配置时调用。
     * 5. 调用方：资产相关元数据节点和配置处理流程。
     * 6. 使用流程：属于 Rule Engine 资产上下文读取流程。
     * 7. 线程安全：实现应保证缓存并发访问安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现涉及缓存，可能访问数据库，服务 Rule Engine。
     */
    AssetProfile get(TenantId tenantId, AssetProfileId assetProfileId);

    /**
     * 中文说明：
     * 1. 方法职责：按资产 ID 获取其关联的资产配置。
     * 2. 输入参数：tenantId 是租户边界，assetId 是资产标识。
     * 3. 返回值：资产当前关联的 AssetProfile。
     * 4. 调用时机：规则节点只有资产 ID、需要配置上下文时调用。
     * 5. 调用方：资产元数据节点和资产相关规则节点。
     * 6. 使用流程：属于 Rule Engine 资产上下文补全流程。
     * 7. 线程安全：实现应保证缓存和资产到配置映射并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现涉及缓存，可能访问数据库。
     */
    AssetProfile get(TenantId tenantId, AssetId assetId);

    /**
     * 中文说明：
     * 1. 方法职责：注册资产配置变更监听器。
     * 2. 输入参数：tenantId 是租户，listenerId 是监听者标识，profileListener 处理配置变更，assetlistener 处理资产到配置关系变更。
     * 3. 返回值：无。
     * 4. 调用时机：规则节点初始化并需要感知资产配置变化时调用。
     * 5. 调用方：资产相关规则节点。
     * 6. 使用流程：属于 Rule Engine 节点生命周期中的缓存监听注册流程。
     * 7. 线程安全：实现需支持多个节点并发注册和回调。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及事务、MQTT、Actor、数据库；实现涉及缓存监听。
     */
    void addListener(TenantId tenantId, EntityId listenerId, Consumer<AssetProfile> profileListener, BiConsumer<AssetId, AssetProfile> assetlistener);

    /**
     * 中文说明：
     * 1. 方法职责：移除指定监听者的资产配置监听。
     * 2. 输入参数：tenantId 是租户，listenerId 是注册时使用的监听者标识。
     * 3. 返回值：无。
     * 4. 调用时机：规则节点销毁或不再需要资产配置监听时调用。
     * 5. 调用方：资产相关规则节点的 destroy 生命周期。
     * 6. 使用流程：属于 Rule Engine 节点资源清理流程。
     * 7. 线程安全：实现需支持并发移除和回调一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及事务、MQTT、Actor、数据库；实现涉及缓存监听清理。
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
