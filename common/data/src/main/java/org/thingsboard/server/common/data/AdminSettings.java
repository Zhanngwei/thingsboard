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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.thingsboard.server.common.data.id.AdminSettingsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `AdminSettings` 是 ThingsBoard Common Data 中描述 `Admin` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasTenantId`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@ApiModel
public class AdminSettings extends BaseData<AdminSettingsId> implements HasTenantId {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7670322981725511892L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @NoXss
    @Length(fieldName = "key")
    private String key;
    private transient JsonNode jsonValue;
    
    /**
     * 功能：创建 `AdminSettings` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AdminSettings() {
        super();
    }

    /**
     * 功能：创建 `AdminSettings` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public AdminSettings(AdminSettingsId id) {
        super(id);
    }
    
    /**
     * 功能：创建 `AdminSettings` 实例，并初始化必要字段。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：新创建的对象实例。
     */
    public AdminSettings(AdminSettings adminSettings) {
        super(adminSettings);
        this.tenantId = adminSettings.getTenantId();
        this.key = adminSettings.getKey();
        this.jsonValue = adminSettings.getJsonValue();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @ApiModelProperty(position = 1, value = "The Id of the Administration Settings, auto-generated, UUID")
    @Override
    public AdminSettingsId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the settings creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * 功能：更新租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 4, value = "The Administration Settings key, (e.g. 'general' or 'mail')", example = "mail")
    public String getKey() {
        return key;
    }

    /**
     * 功能：更新键。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 5, value = "JSON representation of the Administration Settings value")
    public JsonNode getJsonValue() {
        return jsonValue;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `jsonValue`：值。
     * 返回：无。
     */
    public void setJsonValue(JsonNode jsonValue) {
        this.jsonValue = jsonValue;
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((jsonValue == null) ? 0 : jsonValue.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        return result;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `obj`：`obj` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        AdminSettings other = (AdminSettings) obj;
        if (jsonValue == null) {
            if (other.jsonValue != null)
                return false;
        } else if (!jsonValue.equals(other.jsonValue))
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AdminSettings [key=");
        builder.append(key);
        builder.append(", jsonValue=");
        builder.append(jsonValue);
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
