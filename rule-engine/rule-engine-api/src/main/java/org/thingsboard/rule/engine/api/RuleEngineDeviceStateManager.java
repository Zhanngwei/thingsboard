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
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `connectTime`：`connectTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `activityTime`：`activityTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `disconnectTime`：`disconnectTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `inactivityTime`：`inactivityTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
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
