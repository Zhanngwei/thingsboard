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
 * 中文说明：
 * 1. `TbAlarmResult` 是 ThingsBoard Rule Engine Components 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
}
