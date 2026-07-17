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
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao.DESC_ORDER;

/**
 * Created by ashvayka on 21.02.17.
 */
/**
 * 中文说明：
 * 1. `TsKvQueryCursor` 是 ThingsBoard DAO 中承载 `Ts Kv Query` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `QueryCursor`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class TsKvQueryCursor extends QueryCursor {

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    @Getter
    private final List<TsKvEntry> data;
    /**
     * `orderBy` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final String orderBy;

    /**
     * 分区，用于定位消息或数据所属分区。
     */
    private int partitionIndex;
    private int currentLimit;

    /**
     * 功能：创建 `TsKvQueryCursor` 实例，并初始化必要字段。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityId`：实体IDID。
     * - `baseQuery`：`baseQuery` 参数。
     * - `partitions`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    public TsKvQueryCursor(String entityType, UUID entityId, ReadTsKvQuery baseQuery, List<Long> partitions) {
        super(entityType, entityId, baseQuery, partitions);
        this.orderBy = baseQuery.getOrder();
        this.partitionIndex = isDesc() ? partitions.size() - 1 : 0;
        this.data = new ArrayList<>();
        this.currentLimit = baseQuery.getLimit();
    }

    /**
     * 功能：判断分区。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean hasNextPartition() {
        return isDesc() ? partitionIndex >= 0 : partitionIndex <= partitions.size() - 1;
    }

    /**
     * 功能：判断`Full`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isFull() {
        return currentLimit <= 0;
    }

    /**
     * 功能：获取分区。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getNextPartition() {
        long partition = partitions.get(partitionIndex);
        if (isDesc()) {
            partitionIndex--;
        } else {
            partitionIndex++;
        }
        return partition;
    }

    /**
     * 功能：获取数量限制。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getCurrentLimit() {
        return currentLimit;
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `newData`：待处理数据。
     * 返回：无。
     */
    public void addData(List<TsKvEntry> newData) {
        currentLimit -= newData.size();
        data.addAll(newData);
    }

    /**
     * 功能：判断`Desc`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isDesc() {
        return orderBy.equals(DESC_ORDER);
    }
}
