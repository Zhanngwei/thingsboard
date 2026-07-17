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
package org.thingsboard.server.common.data.alarm;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 中文说明：
 * 1. `AlarmInfo` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Alarm`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@ApiModel
public class AlarmInfo extends Alarm {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343093519543363L;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 19, value = "Alarm originator name", example = "Thermostat")
    private String originatorName;

    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 20, value = "Alarm originator label", example = "Thermostat label")
    private String originatorLabel;

    /**
     * `assignee` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 21, value = "Alarm assignee")
    private AlarmAssignee assignee;

    /**
     * 功能：创建 `AlarmInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AlarmInfo() {
        super();
    }

    /**
     * 功能：创建 `AlarmInfo` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmInfo(Alarm alarm) {
        super(alarm);
    }

    /**
     * 功能：创建 `AlarmInfo` 实例，并初始化必要字段。
     * 参数：
     * - `alarmInfo`：`alarmInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmInfo(AlarmInfo alarmInfo) {
        super(alarmInfo);
        this.originatorName = alarmInfo.originatorName;
        this.originatorLabel = alarmInfo.originatorLabel;
        this.assignee = alarmInfo.getAssignee();
    }

    /**
     * 功能：创建 `AlarmInfo` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `originatorName`：名称。
     * - `originatorLabel`：`originatorLabel` 参数。
     * - `assignee`：`assignee` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmInfo(Alarm alarm, String originatorName, String originatorLabel, AlarmAssignee assignee) {
        super(alarm);
        this.originatorName = originatorName;
        this.originatorLabel = originatorLabel;
        this.assignee = assignee;
    }

}
