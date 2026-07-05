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
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：保存某一种设备画像告警的可持久化状态，包含创建规则状态和清除规则状态。
 * 2. 所属模块：属于 ThingsBoard Rule Engine 的 device profile/profile state 子模块，服务于 `TbDeviceProfileNode` 的告警生命周期计算。
 * 3. 协作对象：与 `AlarmState`、`PersistedAlarmRuleState`、`PersistedDeviceState`、`AlarmSeverity` 和规则节点状态存储协作。
 * 4. 生命周期：由设备画像节点在设备状态初始化、告警规则评估、状态保存和状态恢复过程中创建、更新或反序列化。
 * 5. 设计原因：把同一告警类型下不同严重级别的创建规则与清除规则分开持久化，避免运行期 `AlarmState` 与存储格式强耦合。
 * 6. 技术关联：本类是纯状态载体，不直接涉及事务、缓存、MQTT、Actor 通信或数据库访问；数据库读写由外层 Rule Node State 机制间接完成。
 * 7. 显式方法：本类没有手写方法，访问器由 Lombok 生成；对象本身不保证线程安全，由外层设备状态流程控制并发。
 * 8. 设计模式：可视为 Memento/DTO，用于保存告警状态恢复所需的最小快照。
 */
@Data
public class PersistedAlarmState {

    /**
     * `createRuleStates`映射关系，用于按键查找对应值。
     */
    private Map<AlarmSeverity, PersistedAlarmRuleState> createRuleStates;
    /**
     * 状态，表示当前对象所处状态。
     */
    private PersistedAlarmRuleState clearRuleState;

}

/*
 * 本类总结：
 * 1. 核心职责：聚合同一告警类型的创建规则状态和清除规则状态，作为设备级持久化状态的一部分。
 * 2. 核心流程：设备画像节点恢复设备状态时读取本对象，告警创建/清除评估更新内部规则状态，保存节点状态时写回。
 * 3. 关键依赖：依赖 `AlarmSeverity` 作为创建规则索引，依赖 `PersistedAlarmRuleState` 表达单条规则的可恢复快照。
 * 4. 学习重点：关注告警创建与清除状态为何分离，以及 Rule Engine 如何避免把运行期告警对象直接持久化。
 */
