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
package org.thingsboard.server.common.data.device.profile;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;

/**
 * 中文说明：
 * 1. `AlarmRule` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
public class AlarmRule implements Serializable {

    /**
     * `condition` 字段，保存当前对象的对应属性。
     */
    @Valid
    @ApiModelProperty(position = 1, value = "JSON object representing the alarm rule condition")
    private AlarmCondition condition;
    /**
     * `schedule` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 2, value = "JSON object representing time interval during which the rule is active")
    private AlarmSchedule schedule;
    // Advanced
    /**
     * 告警对象，用于描述当前业务场景。
     */
    @NoXss
    @ApiModelProperty(position = 3, value = "String value representing the additional details for an alarm rule")
    private String alarmDetails;
    /**
     * 仪表盘ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with the dashboard Id representing the reference to alarm details dashboard used by mobile application")
    private DashboardId dashboardId;

}
