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
package org.thingsboard.server.common.data.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.getJson;
import static org.thingsboard.server.common.data.BaseDataWithAdditionalInfo.setJson;

/**
 * 中文说明：
 * 1. `UserSettings` 是 ThingsBoard Common Data 中描述用户行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@ApiModel
@Data
public class UserSettings implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2628320657987010348L;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "JSON object with User id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private UserId userId;

    /**
     * 类型集合，用于去重保存或快速判断对象是否存在。
     */
    @ApiModelProperty(position = 2, value = "Type of the settings.")
    @NoXss
    @Length(fieldName = "type", max = 50)
    private UserSettingsType type;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @ApiModelProperty(position = 3, value = "JSON object with user settings.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @NoXss
    @Length(fieldName = "settings", max = 100000)
    private transient JsonNode settings;

    /**
     * 配置列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] settingsBytes;

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public JsonNode getSettings() {
        return getJson(() -> settings, () -> settingsBytes);
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `settings`：配置对象。
     * 返回：无。
     */
    public void setSettings(JsonNode settings) {
        setJson(settings, json -> this.settings = json, bytes -> this.settingsBytes = bytes);
    }
}
