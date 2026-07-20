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
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.KeyFilterPredicate;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import java.io.Serializable;

/**
 * 中文说明：
 * 1. `AlarmConditionFilter` 是 ThingsBoard Common Data 中处理告警的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@ApiModel
@Data
public class AlarmConditionFilter implements Serializable {

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Valid
    @ApiModelProperty(position = 1, value = "JSON object for specifying alarm condition by specific key")
    private AlarmConditionFilterKey key;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    @ApiModelProperty(position = 2, value = "String representation of the type of the value", example = "NUMERIC")
    private EntityKeyValueType valueType;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    @NoXss
    @ApiModelProperty(position = 3, value = "Value used in Constant comparison. For other types, such as TIME_SERIES or ATTRIBUTE, the predicate condition is used")
    private Object value;
    /**
     * `predicate` 字段，保存当前对象的对应属性。
     */
    @Valid
    @ApiModelProperty(position = 4, value = "JSON object representing filter condition")
    private KeyFilterPredicate predicate;

}
