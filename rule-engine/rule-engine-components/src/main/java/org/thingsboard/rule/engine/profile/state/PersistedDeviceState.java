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
package org.thingsboard.rule.engine.profile.state;

import lombok.Data;

import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：保存单个设备在设备画像节点中的可持久化告警状态集合。
 * 2. 所属模块：属于 ThingsBoard Rule Engine 的 device profile/profile state 子模块，是 `DeviceState` 可恢复状态的外部表示。
 * 3. 协作对象：与 `DeviceState`、`PersistedAlarmState`、`TbDeviceProfileNode` 和 Rule Node State 存储机制协作。
 * 4. 生命周期：设备画像节点为设备加载状态时反序列化本对象，处理设备消息后更新告警状态，并在需要持久化时写回。
 * 5. 设计原因：使用设备级 DTO 作为持久化根对象，避免把包含服务依赖、配置对象和运行期缓存的 `DeviceState` 直接序列化。
 * 6. 技术关联：本类本身不直接访问数据库、缓存、MQTT、Actor 或事务；Rule Engine 节点状态保存流程会间接涉及数据库。
 * 7. 显式方法：本类没有手写方法，访问器由 Lombok 生成；线程安全由外层 `DeviceState` 和设备画像节点的状态更新流程保证。
 * 8. 设计模式：可视为 Memento/DTO，是设备画像告警状态持久化的根快照。
 */
@Data
public class PersistedDeviceState {

    /**
     * 告警映射关系，用于按键查找对应值。
     */
    Map<String, PersistedAlarmState> alarmStates;

}

/*
 * 本类总结：
 * 1. 核心职责：作为设备画像节点持久化状态的设备级根对象，聚合该设备所有告警类型的状态快照。
 * 2. 核心流程：节点状态恢复时创建本对象，`DeviceState` 根据消息更新 `alarmStates`，随后由 Rule Node State 机制保存。
 * 3. 关键依赖：依赖 `PersistedAlarmState` 表达单个告警类型状态，并由 `DeviceState` 在运行期读取和更新。
 * 4. 学习重点：关注设备级状态根对象如何把 Rule Engine 运行期状态压缩成可序列化、可恢复的最小结构。
 */
