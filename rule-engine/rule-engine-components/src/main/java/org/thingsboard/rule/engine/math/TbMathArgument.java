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

/**
 * 中文说明：`TbMathArgument` 是数学参数辅助类，用于解析数学参数、计算结果并可写回消息、属性或时间序列。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbMathArgument {

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    private TbMathArgumentType type;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String key;
    /**
     * 属性，表示当前对象的对应属性。
     */
    private String attributeScope;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private Double defaultValue;

    /**
     * 功能：创建 `TbMathArgument` 实例，并初始化必要字段。
     * 参数：
     * - `type`：类型。
     * - `key`：键。
     * 返回：新创建的对象实例。
     */
    public TbMathArgument(TbMathArgumentType type, String key) {
       this(key, type, key, null, null);
    }

    /**
     * 功能：创建 `TbMathArgument` 实例，并初始化必要字段。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * - `key`：键。
     * 返回：新创建的对象实例。
     */
    public TbMathArgument(String name, TbMathArgumentType type, String key) {
       this(name, type, key, null, null);
    }

    /*
     * 本类总结：`TbMathArgument` 负责解析数学参数、计算结果并可写回消息、属性或时间序列；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
