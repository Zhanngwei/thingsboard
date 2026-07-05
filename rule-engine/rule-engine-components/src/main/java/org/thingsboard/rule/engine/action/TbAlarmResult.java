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

/**
 * 中文说明：`TbAlarmResult` 是告警结果辅助类，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@Data
@AllArgsConstructor
public class TbAlarmResult {
    boolean isCreated;
    boolean isUpdated;
    boolean isSeverityUpdated;
    boolean isCleared;
    Alarm alarm;

    /**
     * 功能：创建 `TbAlarmResult` 实例，并初始化必要字段。
     * 参数：
     * - `isCreated`：`isCreated` 参数。
     * - `isUpdated`：`isUpdated` 参数。
     * - `isCleared`：`isCleared` 参数。
     * - `alarm`：`alarm` 参数。
     * 返回：新创建的对象实例。
     */
    public TbAlarmResult(boolean isCreated, boolean isUpdated, boolean isCleared, Alarm alarm) {
        this.isCreated = isCreated;
        this.isUpdated = isUpdated;
        this.isCleared = isCleared;
        this.alarm = alarm;
    }

    /**
     * 功能：执行 `fromAlarmResult` 对应的处理。
     * 参数：
     * - `result`：键值映射。
     * 返回：处理结果。
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
