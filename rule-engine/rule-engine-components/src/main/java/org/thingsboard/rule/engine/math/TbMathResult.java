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
 * 1. `TbMathResult` 是 ThingsBoard Rule Engine Components 中承载 `Tb Math Result` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbMathResult {

    /**
     * 类型，用于区分不同处理分支。
     */
    private TbMathArgumentType type;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String key;
    // 0 means integer, x > 0 means x decimal points after ".";
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private int resultValuePrecision;
    /**
     * 是否满足`addToBody`条件。
     */
    private boolean addToBody;
    /**
     * 是否满足`addToMetadata`条件。
     */
    private boolean addToMetadata;
    /**
     * 属性，表示当前对象的对应属性。
     */
    private String attributeScope;
}
