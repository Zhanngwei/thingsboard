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
import lombok.EqualsAndHashCode;

import java.time.ZoneId;

/**
 * 中文说明：
 * 1. `BaseReadTsKvQuery` 是 ThingsBoard Common Data 中承载 `Read Ts Kv` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseTsKvQuery`、`ReadTsKvQuery`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseReadTsKvQuery extends BaseTsKvQuery implements ReadTsKvQuery {

    /**
     * 参数集合，表示当前对象的对应属性。
     */
    private final AggregationParams aggParameters;
    private final int limit;
    /**
     * `order` 字段，保存当前对象的对应属性。
     */
    private final String order;

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `interval`：`interval` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation) {
        this(key, startTs, endTs, interval, limit, aggregation, "DESC");
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `interval`：`interval` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs, long interval, int limit, Aggregation aggregation, String descOrder) {
        this(key, startTs, endTs, AggregationParams.of(aggregation, IntervalType.MILLISECONDS, ZoneId.systemDefault(), interval), limit, descOrder);
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `parameters`：`parameters` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs, AggregationParams parameters, int limit) {
        this(key, startTs, endTs, parameters, limit, "DESC");
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `parameters`：`parameters` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs, AggregationParams parameters, int limit, String order) {
        super(key, startTs, endTs);
        this.aggParameters = parameters;
        this.limit = limit;
        this.order = order;
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs) {
        this(key, startTs, endTs, AggregationParams.milliseconds(Aggregation.AVG, endTs - startTs), 1, "DESC");
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `limit`：数量限制。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(String key, long startTs, long endTs, int limit, String order) {
        this(key, startTs, endTs, AggregationParams.none(), limit, order);
    }

    /**
     * 功能：创建 `BaseReadTsKvQuery` 实例，并初始化必要字段。
     * 参数：
     * - `query`：`query` 参数。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public BaseReadTsKvQuery(ReadTsKvQuery query, long startTs, long endTs) {
        super(query.getId(), query.getKey(), startTs, endTs);
        this.aggParameters = query.getAggParameters();
        this.limit = query.getLimit();
        this.order = query.getOrder();
    }
}
