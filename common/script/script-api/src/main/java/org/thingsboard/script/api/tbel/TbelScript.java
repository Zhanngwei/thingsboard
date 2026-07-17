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
package org.thingsboard.script.api.tbel;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbelScript` 是 ThingsBoard Common 中围绕脚本执行提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
public class TbelScript {

    /**
     * `scriptBody` 字段，保存当前对象的对应属性。
     */
    private final String scriptBody;
    private final String[] argNames;

    /**
     * 功能：保存或创建`Vars`。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：键值映射结果。
     */
    public Map createVars(Object[] args) {
        if (args == null || args.length != argNames.length) {
            throw new IllegalArgumentException("Invalid number of argument values");
        }
        var result = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            result.put(argNames[i], args[i]);
        }
        return result;
    }
}
