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

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TbCallback;

/**
 * 中文说明：
 * 1. 职责：定义 Rule Engine 侧设备连接、活跃、断开和不活跃状态变化的处理入口。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的设备状态管理边界。
 * 3. 协作对象：与设备状态节点、传输层会话事件、设备配置、队列回调 {@link TbCallback} 协作。
 * 4. 生命周期：由 Spring 实现类长期存在，随设备事件被多次调用；每次调用通过 callback 完成确认。
 * 5. 设计原因：设备状态计算需要被 Rule Engine 与传输事件共享，通过接口隔离状态机实现和调用方。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现可能使用缓存/数据库并由传输或 Rule Engine 事件触发。
 */
public interface RuleEngineDeviceStateManager {

    /**
     * 中文说明：
     * 1. 方法职责：处理设备连接事件。
     * 2. 输入参数：tenantId 是租户，deviceId 是设备，connectTime 是连接时间戳，callback 用于异步确认。
     * 3. 返回值：无；处理结果通过 callback 通知。
     * 4. 调用时机：传输层或规则节点感知设备建立连接时调用。
     * 5. 调用方：设备状态节点、传输状态桥接服务。
     * 6. 使用流程：属于设备状态 Rule Engine 流程。
     * 7. 线程安全：接口无状态，实现需要保护设备状态缓存和并发更新。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能更新缓存/数据库，直接服务 Rule Engine。
     */
    void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback);

    /**
     * 中文说明：
     * 1. 方法职责：处理设备活跃事件。
     * 2. 输入参数：tenantId 是租户，deviceId 是设备，activityTime 是活跃时间戳，callback 用于异步确认。
     * 3. 返回值：无；处理结果通过 callback 通知。
     * 4. 调用时机：收到遥测、属性或其它设备活动消息时调用。
     * 5. 调用方：设备状态节点、传输事件处理流程。
     * 6. 使用流程：属于设备状态刷新和 Rule Engine 消息处理流程。
     * 7. 线程安全：实现需要处理同一设备并发活动事件。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor/数据库；实现可能访问缓存或持久化状态，直接涉及 Rule Engine。
     */
    void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback);

    /**
     * 中文说明：
     * 1. 方法职责：处理设备断开连接事件。
     * 2. 输入参数：tenantId 是租户，deviceId 是设备，disconnectTime 是断开时间戳，callback 用于异步确认。
     * 3. 返回值：无；处理结果通过 callback 通知。
     * 4. 调用时机：传输层检测到设备会话关闭时调用。
     * 5. 调用方：传输状态桥接服务、设备状态节点。
     * 6. 使用流程：属于设备状态迁移流程。
     * 7. 线程安全：实现需要保证连接/断开事件顺序和并发更新一致性。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及事务、MQTT、Actor、数据库；实现可能更新缓存或状态存储。
     */
    void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback);

    /**
     * 中文说明：
     * 1. 方法职责：处理设备不活跃事件。
     * 2. 输入参数：tenantId 是租户，deviceId 是设备，inactivityTime 是判定不活跃的时间戳，callback 用于异步确认。
     * 3. 返回值：无；处理结果通过 callback 通知。
     * 4. 调用时机：设备状态调度器或规则节点检测到设备超时未活动时调用。
     * 5. 调用方：设备状态管理流程。
     * 6. 使用流程：属于设备状态 Rule Engine 流程，可触发后续告警或消息路由。
     * 7. 线程安全：实现需要处理定时检测与实时活动事件并发。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT、Actor；实现可能访问缓存/数据库并直接服务 Rule Engine。
     */
    void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback);

}

/*
 * 本类总结：
 * 1. 核心职责：抽象设备在线状态事件的 Rule Engine 处理入口。
 * 2. 核心流程：传输或规则节点上报状态事件，状态管理实现更新设备状态并通过 TbCallback 确认。
 * 3. 关键依赖：TenantId、DeviceId、TbCallback、设备状态缓存/存储实现。
 * 4. 学习重点：设备状态在传输事件和 Rule Engine 流程之间共享，需要通过接口隔离状态机细节。
 */
