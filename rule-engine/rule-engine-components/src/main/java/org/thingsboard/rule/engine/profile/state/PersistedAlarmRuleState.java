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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：
 * 1. 类目的：保存单条设备画像告警规则的可持久化运行状态，用于在规则节点重启或状态恢复时继续计算持续时间和重复次数。
 * 2. 所属模块：属于 ThingsBoard Rule Engine 的 device profile/profile state 子模块，是 `TbDeviceProfileNode` 告警计算状态的一部分。
 * 3. 协作对象：与 `AlarmRuleState`、`PersistedAlarmState`、`PersistedDeviceState` 和 Rule Node State 持久化机制协作。
 * 4. 生命周期：由设备画像节点在加载、更新和保存设备告警状态时创建或反序列化；字段值随设备告警规则评估周期更新。
 * 5. 设计原因：使用独立 DTO 隔离可持久化字段，避免把运行期规则对象、缓存对象或 DAO 对象直接写入规则节点状态。
 * 6. 技术关联：本类本身不直接涉及事务、缓存、MQTT、Actor 通信、数据库或 Rule Engine 执行；数据库持久化由外层 Rule Node State 流程间接完成。
 * 7. 显式方法：本类没有手写方法，构造器与访问器由 Lombok 生成，线程安全取决于外层状态管理是否串行化访问。
 * 8. 设计模式：可视为 Memento/DTO，用于保存告警规则运行状态快照。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersistedAlarmRuleState {

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long lastEventTs;
    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    private long duration;
    /**
     * 事件，用于控制数量、位置或分页范围。
     */
    private long eventCount;

}

/*
 * 本类总结：
 * 1. 核心职责：以最小 DTO 形式保存单条告警规则的时间戳、持续时间和命中次数。
 * 2. 核心流程：外层设备画像节点加载状态后反序列化本对象，规则评估过程更新字段，节点状态保存时再次序列化。
 * 3. 关键依赖：依赖 `AlarmRuleState` 的运行期评估结果，并被 `PersistedAlarmState` 聚合到设备级持久化状态中。
 * 4. 学习重点：阅读本类时应关注 Rule Engine 如何把运行期告警条件评估拆分为可恢复的轻量状态快照。
 */
