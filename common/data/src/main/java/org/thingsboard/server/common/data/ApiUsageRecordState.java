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

import lombok.Data;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `ApiUsageRecordState` 是 ThingsBoard Common Data 中承载用量统计信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class ApiUsageRecordState implements Serializable {

    /**
     * 功能项，表示当前对象的对应属性。
     */
    private final ApiFeature apiFeature;
    private final ApiUsageRecordKey key;
    /**
     * 阈值，用于判断是否达到处理条件。
     */
    private final long threshold;
    private final long value;

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getValueAsString() {
        return valueAsString(value);
    }

    /**
     * 功能：获取阈值。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getThresholdAsString() {
        return valueAsString(threshold);
    }

    /**
     * 功能：执行 `valueAsString` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    private String valueAsString(long value) {
        if (value > 1_000_000 && value % 1_000_000 < 10_000) {
            return value / 1_000_000 + "M";
        } else if (value > 10_000) {
            return String.format("%.2fM", ((double) value) / 1_000_000);
        } else {
            return value + "";
        }
    }

}
