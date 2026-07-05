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
package org.thingsboard.rule.engine.profile;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.common.data.kv.DataType;

/**
 * 中文说明：`EntityKeyValue` 是实体键值辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@EqualsAndHashCode
class EntityKeyValue {

    /**
     * 数据，用于区分不同处理分支。
     */
    @Getter
    private DataType dataType;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private Long lngValue;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private Double dblValue;
    /**
     * 是否满足值条件。
     */
    private Boolean boolValue;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private String strValue;

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：数值结果。
     */
    public Long getLngValue() {
        return dataType == DataType.LONG ? lngValue : null;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `lngValue`：值。
     * 返回：无。
     */
    public void setLngValue(Long lngValue) {
        this.dataType = DataType.LONG;
        this.lngValue = lngValue;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：数值结果。
     */
    public Double getDblValue() {
        return dataType == DataType.DOUBLE ? dblValue : null;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `dblValue`：值。
     * 返回：无。
     */
    public void setDblValue(Double dblValue) {
        this.dataType = DataType.DOUBLE;
        this.dblValue = dblValue;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：判断结果。
     */
    public Boolean getBoolValue() {
        return dataType == DataType.BOOLEAN ? boolValue : null;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `boolValue`：值。
     * 返回：无。
     */
    public void setBoolValue(Boolean boolValue) {
        this.dataType = DataType.BOOLEAN;
        this.boolValue = boolValue;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getStrValue() {
        return dataType == DataType.STRING ? strValue : null;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `strValue`：值。
     * 返回：无。
     */
    public void setStrValue(String strValue) {
        this.dataType = DataType.STRING;
        this.strValue = strValue;
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `jsonValue`：值。
     * 返回：无。
     */
    public void setJsonValue(String jsonValue) {
        this.dataType = DataType.JSON;
        this.strValue = jsonValue;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getJsonValue() {
        return dataType == DataType.JSON ? strValue : null;
    }

    /**
     * 功能：判断`Set`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isSet() {
        return dataType != null;
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：处理结果。
     */
    static EntityKeyValue fromString(String s) {
        EntityKeyValue result = new EntityKeyValue();
        result.setStrValue(s);
        return result;
    }

    /**
     * 功能：执行 `fromBool` 对应的处理。
     * 参数：
     * - `b`：`b` 参数。
     * 返回：处理结果。
     */
    static EntityKeyValue fromBool(boolean b) {
        EntityKeyValue result = new EntityKeyValue();
        result.setBoolValue(b);
        return result;
    }

    /**
     * 功能：执行 `fromLong` 对应的处理。
     * 参数：
     * - `l`：`l` 参数。
     * 返回：处理结果。
     */
    static EntityKeyValue fromLong(long l) {
        EntityKeyValue result = new EntityKeyValue();
        result.setLngValue(l);
        return result;
    }

    /**
     * 功能：执行 `fromDouble` 对应的处理。
     * 参数：
     * - `d`：`d` 参数。
     * 返回：处理结果。
     */
    static EntityKeyValue fromDouble(double d) {
        EntityKeyValue result = new EntityKeyValue();
        result.setDblValue(d);
        return result;
    }

    /**
     * 功能：执行 `fromJson` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：处理结果。
     */
    static EntityKeyValue fromJson(String s) {
        EntityKeyValue result = new EntityKeyValue();
        result.setJsonValue(s);
        return result;
    }

    /*
     * 本类总结：`EntityKeyValue` 负责维护设备配置、告警规则、快照和设备运行状态；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
