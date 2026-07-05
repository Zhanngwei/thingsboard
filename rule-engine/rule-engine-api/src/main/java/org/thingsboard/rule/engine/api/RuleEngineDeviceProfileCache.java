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

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. 职责：为 Rule Engine 提供设备配置缓存读取和监听能力。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的设备配置缓存边界。
 * 3. 协作对象：与设备配置服务、设备状态节点、设备配置节点、租户缓存刷新流程协作。
 * 4. 生命周期：由 Spring 缓存实现长期存在，监听器随规则节点生命周期注册和移除。
 * 5. 设计原因：规则节点频繁读取设备配置，缓存接口可避免节点直接访问数据库并支持配置变更通知。
 * 6. 技术关联：接口本身不直接涉及事务、MQTT、Actor、数据库；实现直接涉及缓存，可能在未命中时访问数据库，服务 Rule Engine。
 */
public interface RuleEngineDeviceProfileCache {

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceProfile get(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * - `profileListener`：`profileListener` 参数。
     * - `devicelistener`：设备信息或设备标识。
     * 返回：无。
     */
    void addListener(TenantId tenantId, EntityId listenerId, Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> devicelistener);

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
 * 1. 核心职责：为规则节点提供设备配置缓存读取和变更监听。
 * 2. 核心流程：节点按设备或配置 ID 读取 DeviceProfile，初始化时注册监听，销毁时移除监听。
 * 3. 关键依赖：DeviceProfile、DeviceId、DeviceProfileId、TenantId 和缓存实现。
 * 4. 学习重点：配置缓存让规则节点避免频繁数据库访问，并能响应配置变更。
 */
