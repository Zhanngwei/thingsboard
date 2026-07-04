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
package org.thingsboard.rule.engine.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * 中文说明：`TbMathResult` 是数学结果辅助类，用于解析数学参数、计算结果并可写回消息、属性或时间序列。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
public class TbMathResult {

    /**
     * 字段说明：保存 `type`，表示类型匹配条件，供本类方法在规则节点处理流程中使用。
     */
    private TbMathArgumentType type;
    /**
     * 字段说明：保存 `key`，表示消息体、元数据、属性或遥测中的键名，供本类方法在规则节点处理流程中使用。
     */
    private String key;
    // 0 means integer, x > 0 means x decimal points after ".";
    /**
     * 字段说明：保存 `resultValuePrecision`，表示计算值或最近值，供本类方法在规则节点处理流程中使用。
     */
    private int resultValuePrecision;
    /**
     * 字段说明：保存 `addToBody`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private boolean addToBody;
    /**
     * 字段说明：保存 `addToMetadata`，表示消息元数据，供本类方法在规则节点处理流程中使用。
     */
    private boolean addToMetadata;
    /**
     * 字段说明：保存 `attributeScope`，表示属性作用域，供本类方法在规则节点处理流程中使用。
     */
    private String attributeScope;

    /*
     * 本类总结：`TbMathResult` 负责解析数学参数、计算结果并可写回消息、属性或时间序列；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
