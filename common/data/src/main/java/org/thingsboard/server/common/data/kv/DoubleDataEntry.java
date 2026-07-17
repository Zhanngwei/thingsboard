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
package org.thingsboard.server.common.data.kv;

import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `DoubleDataEntry` 是 ThingsBoard Common Data 中承载 `Double Entry` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BasicKvEntry`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class DoubleDataEntry extends BasicKvEntry {

    /**
     * 值，保存当前处理得到的具体内容。
     */
    private final Double value;

    /**
     * 功能：创建 `DoubleDataEntry` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：新创建的对象实例。
     */
    public DoubleDataEntry(String key, Double value) {
        super(key);
        this.value = value;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DataType getDataType() {
        return DataType.DOUBLE;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Double> getDoubleValue() {
        return Optional.ofNullable(value);
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DoubleDataEntry)) return false;
        if (!super.equals(o)) return false;
        DoubleDataEntry that = (DoubleDataEntry) o;
        return Objects.equals(value, that.value);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "DoubleDataEntry{" +
                "value=" + value +
                "} " + super.toString();
    }
    
    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getValueAsString() {
        return Double.toString(value);
    }
}
