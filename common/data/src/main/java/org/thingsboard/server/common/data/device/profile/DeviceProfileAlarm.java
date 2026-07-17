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
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

/**
 * 中文说明：
 * 1. `DeviceProfileAlarm` 是 ThingsBoard Common Data 中承载设备配置信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
public class DeviceProfileAlarm implements Serializable {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "String value representing the alarm rule id", example = "highTemperatureAlarmID")
    private String id;
    /**
     * 告警，用于区分不同处理分支。
     */
    @Length(fieldName = "alarm type")
    @NoXss
    @ApiModelProperty(position = 2, value = "String value representing type of the alarm", example = "High Temperature Alarm")
    private String alarmType;

    /**
     * `createRules`映射关系，用于按键查找对应值。
     */
    @Valid
    @ApiModelProperty(position = 3, value = "Complex JSON object representing create alarm rules. The unique create alarm rule can be created for each alarm severity type. " +
            "There can be 5 create alarm rules configured per a single alarm type. See method implementation notes and AlarmRule model for more details")
    private TreeMap<AlarmSeverity, AlarmRule> createRules;
    /**
     * `clearRule` 字段，保存当前对象的对应属性。
     */
    @Valid
    @ApiModelProperty(position = 4, value = "JSON object representing clear alarm rule")
    private AlarmRule clearRule;

    // Hidden in advanced settings
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 5, value = "Propagation flag to specify if alarm should be propagated to parent entities of alarm originator", example = "true")
    private boolean propagate;
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 6, value = "Propagation flag to specify if alarm should be propagated to the owner (tenant or customer) of alarm originator", example = "true")
    private boolean propagateToOwner;
    /**
     * 是否向关联对象传播当前状态。
     */
    @ApiModelProperty(position = 7, value = "Propagation flag to specify if alarm should be propagated to the tenant entity", example = "true")
    private boolean propagateToTenant;

    /**
     * 关系列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(position = 8, value = "JSON array of relation types that should be used for propagation. " +
            "By default, 'propagateRelationTypes' array is empty which means that the alarm will be propagated based on any relation type to parent entities. " +
            "This parameter should be used only in case when 'propagate' parameter is set to true, otherwise, 'propagateRelationTypes' array will be ignored.")
    private List<String> propagateRelationTypes;

}
