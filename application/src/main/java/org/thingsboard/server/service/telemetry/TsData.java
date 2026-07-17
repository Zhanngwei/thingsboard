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
package org.thingsboard.server.service.telemetry;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 中文说明：
 * 1. `TsData` 是 ThingsBoard Application 中承载 `Ts` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Comparable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
public class TsData implements Comparable<TsData>{

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long ts;
    private final Object value;

    /**
     * 功能：创建 `TsData` 实例，并初始化必要字段。
     * 参数：
     * - `ts`：时间戳。
     * - `value`：值。
     * 返回：新创建的对象实例。
     */
    public TsData(long ts, Object value) {
        super();
        this.ts = ts;
        this.value = value;
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 1, value = "Timestamp last updated timeseries, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public long getTs() {
        return ts;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 2, value = "Object representing value of timeseries key", example = "20", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public Object getValue() {
        return value;
    }

    /**
     * 功能：执行 `compareTo` 对应的处理。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：数值结果。
     */
    @Override
    public int compareTo(TsData o) {
        return Long.compare(ts, o.ts);
    }

}
