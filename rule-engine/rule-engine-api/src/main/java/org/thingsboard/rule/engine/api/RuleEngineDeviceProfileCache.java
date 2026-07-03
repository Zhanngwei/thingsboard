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
     * 中文说明：
     * 1. 方法职责：按设备配置 ID 获取设备配置。
     * 2. 输入参数：tenantId 是租户边界，deviceProfileId 是设备配置标识。
     * 3. 返回值：对应 DeviceProfile。
     * 4. 调用时机：规则节点需要根据配置 ID 读取设备配置时调用。
     * 5. 调用方：设备配置相关节点、设备状态处理流程。
     * 6. 使用流程：属于 Rule Engine 配置读取流程。
     * 7. 线程安全：接口无状态，实现应保证缓存并发访问安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及 MQTT/Actor；实现涉及缓存，可能访问数据库，不在接口层声明事务。
     */
    DeviceProfile get(TenantId tenantId, DeviceProfileId deviceProfileId);

    /**
     * 中文说明：
     * 1. 方法职责：按设备 ID 获取其关联的设备配置。
     * 2. 输入参数：tenantId 是租户边界，deviceId 是设备标识。
     * 3. 返回值：设备当前关联的 DeviceProfile。
     * 4. 调用时机：规则节点只有设备 ID、需要配置上下文时调用。
     * 5. 调用方：设备状态节点、设备配置节点和其它设备相关规则节点。
     * 6. 使用流程：属于 Rule Engine 设备上下文补全流程。
     * 7. 线程安全：实现应保证缓存和设备到配置映射的并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现涉及缓存，可能访问数据库，服务 Rule Engine。
     */
    DeviceProfile get(TenantId tenantId, DeviceId deviceId);

    /**
     * 中文说明：
     * 1. 方法职责：注册设备配置变更监听器。
     * 2. 输入参数：tenantId 是租户，listenerId 是监听者标识，profileListener 处理配置变更，devicelistener 处理设备到配置关系变更。
     * 3. 返回值：无。
     * 4. 调用时机：规则节点初始化或需要感知配置变化时调用。
     * 5. 调用方：设备状态节点、设备配置相关节点。
     * 6. 使用流程：属于 Rule Engine 节点生命周期中的缓存监听注册流程。
     * 7. 线程安全：实现需支持多个节点并发注册和回调。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现直接涉及缓存监听，服务 Rule Engine。
     */
    void addListener(TenantId tenantId, EntityId listenerId, Consumer<DeviceProfile> profileListener, BiConsumer<DeviceId, DeviceProfile> devicelistener);

    /**
     * 中文说明：
     * 1. 方法职责：移除指定监听者的设备配置监听。
     * 2. 输入参数：tenantId 是租户，listenerId 是注册时使用的监听者标识。
     * 3. 返回值：无。
     * 4. 调用时机：规则节点销毁或不再需要配置监听时调用。
     * 5. 调用方：设备配置相关节点的 destroy 生命周期。
     * 6. 使用流程：属于 Rule Engine 节点资源清理流程。
     * 7. 线程安全：实现需支持并发移除和回调中的一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不涉及事务、MQTT、Actor、数据库；实现涉及缓存监听清理，服务 Rule Engine。
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
