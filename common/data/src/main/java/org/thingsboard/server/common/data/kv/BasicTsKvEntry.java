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

import javax.validation.Valid;
import java.util.Objects;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `BasicTsKvEntry` 是 ThingsBoard Common Data 中承载 `Basic Ts Kv` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TsKvEntry`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class BasicTsKvEntry implements TsKvEntry {
    /**
     * 数据常量，用于统一引用固定值。
     */
    private static final int MAX_CHARS_PER_DATA_POINT = 512;
    protected final long ts;
    /**
     * `kv` 字段，保存当前对象的对应属性。
     */
    @Valid
    private final KvEntry kv;

    /**
     * 功能：创建 `BasicTsKvEntry` 实例，并初始化必要字段。
     * 参数：
     * - `ts`：时间戳。
     * - `kv`：`kv` 参数。
     * 返回：新创建的对象实例。
     */
    public BasicTsKvEntry(long ts, KvEntry kv) {
        this.ts = ts;
        this.kv = kv;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getKey() {
        return kv.getKey();
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DataType getDataType() {
        return kv.getDataType();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<String> getStrValue() {
        return kv.getStrValue();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Long> getLongValue() {
        return kv.getLongValue();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Boolean> getBooleanValue() {
        return kv.getBooleanValue();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Double> getDoubleValue() {
        return kv.getDoubleValue();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<String> getJsonValue() {
        return kv.getJsonValue();
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getValue() {
        return kv.getValue();
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getTs() {
        return ts;
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
        if (!(o instanceof BasicTsKvEntry)) return false;
        BasicTsKvEntry that = (BasicTsKvEntry) o;
        return getTs() == that.getTs() &&
                Objects.equals(kv, that.kv);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return Objects.hash(getTs(), kv);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "BasicTsKvEntry{" +
                "ts=" + ts +
                ", kv=" + kv +
                '}';
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getValueAsString() {
        return kv.getValueAsString();
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getDataPoints() {
        int length;
        switch (getDataType()) {
            case STRING:
                length = getStrValue().get().length();
                break;
            case JSON:
                length = getJsonValue().get().length();
                break;
            default:
                return 1;
        }
        return Math.max(1, (length + MAX_CHARS_PER_DATA_POINT - 1) / MAX_CHARS_PER_DATA_POINT);
    }

}
