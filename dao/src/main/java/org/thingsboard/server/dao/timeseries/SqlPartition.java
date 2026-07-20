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

import lombok.Data;

/**
 * 中文说明：
 * 1. `SqlPartition` 是 ThingsBoard DAO 中负责分区存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Data
public class SqlPartition {

    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String TS_KV = "ts_kv";

    /**
     * `start` 字段，保存当前对象的对应属性。
     */
    private long start;
    private long end;
    /**
     * 分区，用于定位消息或数据所属分区。
     */
    private String partitionDate;
    private String query;

    /**
     * 功能：创建 `SqlPartition` 实例，并初始化必要字段。
     * 参数：
     * - `table`：`table` 参数。
     * - `start`：`start` 参数。
     * - `end`：`end` 参数。
     * - `partitionDate`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    public SqlPartition(String table, long start, long end, String partitionDate) {
        this.start = start;
        this.end = end;
        this.partitionDate = partitionDate;
        this.query = createStatement(table, start, end, partitionDate);
    }

    /**
     * 功能：保存或创建语句。
     * 参数：
     * - `table`：`table` 参数。
     * - `start`：`start` 参数。
     * - `end`：`end` 参数。
     * - `partitionDate`：分区标识或分区信息。
     * 返回：文本结果。
     */
    private String createStatement(String table, long start, long end, String partitionDate) {
        return "CREATE TABLE IF NOT EXISTS " + table + "_" + partitionDate + " PARTITION OF " + table + " FOR VALUES FROM (" + start + ") TO (" + end + ")";
    }
}