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
package org.thingsboard.server.common.data.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 中文说明：
 * 1. `TsValue` 是 ThingsBoard Common Data 中承载 `Ts Value` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TsValue {

    public static final TsValue EMPTY = new TsValue(0, "");

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long ts;
    private final String value;
    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private final Long count;

    /**
     * 功能：创建 `TsValue` 实例，并初始化必要字段。
     * 参数：
     * - `ts`：时间戳。
     * - `value`：值。
     * 返回：新创建的对象实例。
     */
    public TsValue(long ts, String value) {
        this(ts, value, null);
    }

}
