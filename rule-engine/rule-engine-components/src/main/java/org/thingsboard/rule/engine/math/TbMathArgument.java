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
 * 中文说明：
 * 1. `TbMathArgument` 是 ThingsBoard Rule Engine Components 中围绕 `Tb Math Argument` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
}
