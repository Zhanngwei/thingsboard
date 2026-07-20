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

import lombok.Data;
import org.thingsboard.server.common.data.query.TsValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. `ReadTsKvQueryResult` 是 ThingsBoard Common Data 中承载 `Read Ts Kv` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class ReadTsKvQueryResult {

    /**
     * 查询条件ID，用于定位对应业务对象。
     */
    private final int queryId;
    // Holds the data list;
    /**
     * 数据列表，用于保存一组待处理对象。
     */
    private final List<TsKvEntry> data;
    // Holds the max ts of the records that match aggregation intervals (not the ts of the aggregation window, but the ts of the last record among all the intervals)
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long lastEntryTs;

    /**
     * 功能：执行 `toTsValues` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TsValue[] toTsValues() {
        if (data != null && !data.isEmpty()) {
            List<TsValue> queryValues = new ArrayList<>();
            for (TsKvEntry v : data) {
                queryValues.add(v.toTsValue()); // TODO: add count here.
            }
            return queryValues.toArray(new TsValue[queryValues.size()]);
        } else {
            return new TsValue[0];
        }
    }

    /**
     * 功能：执行 `toTsValue` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    public TsValue toTsValue(ReadTsKvQuery query) {
        if (data == null || data.isEmpty()) {
            if (Aggregation.SUM.equals(query.getAggregation()) || Aggregation.COUNT.equals(query.getAggregation())) {
                long ts = query.getStartTs() + (query.getEndTs() - query.getStartTs()) / 2;
                return new TsValue(ts, "0");
            } else {
                return TsValue.EMPTY;
            }
        }
        if (data.size() > 1) {
            throw new RuntimeException("Query Result has multiple data points!");
        }
        return data.get(0).toTsValue();
    }

}
