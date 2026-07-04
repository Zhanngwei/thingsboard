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
package org.thingsboard.rule.engine.action;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;

@Data
@AllArgsConstructor
/**
 * 中文说明：`TbAlarmResult` 是告警结果辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class TbAlarmResult {
    boolean isCreated;
    boolean isUpdated;
    boolean isSeverityUpdated;
    boolean isCleared;
    Alarm alarm;

    /**
     * 方法说明：构造 `TbAlarmResult` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    public TbAlarmResult(boolean isCreated, boolean isUpdated, boolean isCleared, Alarm alarm) {
        this.isCreated = isCreated;
        this.isUpdated = isUpdated;
        this.isCleared = isCleared;
        this.alarm = alarm;
    }

    /**
     * 方法说明：执行 `fromAlarmResult` 对应的辅助逻辑，供 `TbAlarmResult` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public static TbAlarmResult fromAlarmResult(AlarmApiCallResult result) {
        boolean isSeverityChanged = result.isSeverityChanged();
        return new TbAlarmResult(
                result.isCreated(),
                result.isModified() && !isSeverityChanged,
                isSeverityChanged,
                result.isCleared(),
                result.getAlarm());
    }
    /*
     * 本类总结：`TbAlarmResult` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
