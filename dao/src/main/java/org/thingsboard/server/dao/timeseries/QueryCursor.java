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
package org.thingsboard.server.dao.timeseries;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `QueryCursor` 是 ThingsBoard DAO 中承载 `Query Cursor` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class QueryCursor {

    /**
     * 实体，用于区分不同处理分支。
     */
    @Getter
    protected final String entityType;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Getter
    protected final UUID entityId;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Getter
    protected final String key;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private final long startTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private final long endTs;

    /**
     * `partitions`列表，用于保存一组待处理对象。
     */
    final List<Long> partitions;
    private int partitionIndex;

    /**
     * 功能：创建 `QueryCursor` 实例，并初始化必要字段。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityId`：实体IDID。
     * - `baseQuery`：`baseQuery` 参数。
     * - `partitions`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    public QueryCursor(String entityType, UUID entityId, TsKvQuery baseQuery, List<Long> partitions) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.key = baseQuery.getKey();
        this.startTs = baseQuery.getStartTs();
        this.endTs = baseQuery.getEndTs();
        this.partitions = partitions;
        this.partitionIndex = partitions.size() - 1;
    }

    /**
     * 功能：判断分区。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasNextPartition() {
        return partitionIndex >= 0;
    }

    /**
     * 功能：获取分区。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        partitionIndex--;
        return partition;
    }

}
