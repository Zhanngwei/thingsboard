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

@EqualsAndHashCode
/**
 * 中文说明：`EntityKeyValue` 是实体键值辅助类，用于维护设备配置、告警规则、快照和设备运行状态。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
class EntityKeyValue {

    @Getter
    /**
     * 字段说明：保存 `dataType`，表示消息体数据，供本类方法在规则节点处理流程中使用。
     */
    private DataType dataType;
    /**
     * 字段说明：保存 `lngValue`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private Long lngValue;
    /**
     * 字段说明：保存 `dblValue`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private Double dblValue;
    /**
     * 字段说明：保存 `boolValue`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private Boolean boolValue;
    /**
     * 字段说明：保存 `strValue`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private String strValue;

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public Long getLngValue() {
        return dataType == DataType.LONG ? lngValue : null;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void setLngValue(Long lngValue) {
        this.dataType = DataType.LONG;
        this.lngValue = lngValue;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public Double getDblValue() {
        return dataType == DataType.DOUBLE ? dblValue : null;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void setDblValue(Double dblValue) {
        this.dataType = DataType.DOUBLE;
        this.dblValue = dblValue;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public Boolean getBoolValue() {
        return dataType == DataType.BOOLEAN ? boolValue : null;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void setBoolValue(Boolean boolValue) {
        this.dataType = DataType.BOOLEAN;
        this.boolValue = boolValue;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public String getStrValue() {
        return dataType == DataType.STRING ? strValue : null;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void setStrValue(String strValue) {
        this.dataType = DataType.STRING;
        this.strValue = strValue;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void setJsonValue(String jsonValue) {
        this.dataType = DataType.JSON;
        this.strValue = jsonValue;
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public String getJsonValue() {
        return dataType == DataType.JSON ? strValue : null;
    }

    /**
     * 方法说明：写入本地对象字段或构造输出数据，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    boolean isSet() {
        return dataType != null;
    }

    /**
     * 方法说明：执行 `fromString` 对应的辅助逻辑，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    static EntityKeyValue fromString(String s) {
        EntityKeyValue result = new EntityKeyValue();
        result.setStrValue(s);
        return result;
    }

    /**
     * 方法说明：执行 `fromBool` 对应的辅助逻辑，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    static EntityKeyValue fromBool(boolean b) {
        EntityKeyValue result = new EntityKeyValue();
        result.setBoolValue(b);
        return result;
    }

    /**
     * 方法说明：执行 `fromLong` 对应的辅助逻辑，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    static EntityKeyValue fromLong(long l) {
        EntityKeyValue result = new EntityKeyValue();
        result.setLngValue(l);
        return result;
    }

    /**
     * 方法说明：执行 `fromDouble` 对应的辅助逻辑，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    static EntityKeyValue fromDouble(double d) {
        EntityKeyValue result = new EntityKeyValue();
        result.setDblValue(d);
        return result;
    }

    /**
     * 方法说明：执行 `fromJson` 对应的辅助逻辑，供 `EntityKeyValue` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
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
