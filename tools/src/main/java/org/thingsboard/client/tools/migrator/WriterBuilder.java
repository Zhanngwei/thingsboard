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
package org.thingsboard.client.tools.migrator;

import org.apache.cassandra.io.sstable.CQLSSTableWriter;

import java.io.File;

/**
 * 中文说明：
 * 1. `WriterBuilder` 是 ThingsBoard Tools 中创建或提供 `Writer` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public class WriterBuilder {

    /**
     * 时间戳常量，用于统一引用固定值。
     */
    private static final String tsSchema = "CREATE TABLE thingsboard.ts_kv_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    partition bigint,\n" +
            "    ts bigint,\n" +
            "    bool_v boolean,\n" +
            "    str_v text,\n" +
            "    long_v bigint,\n" +
            "    dbl_v double,\n" +
            "    json_v text,\n" +
            "    PRIMARY KEY (( entity_type, entity_id, key, partition ), ts)\n" +
            ");";

    /**
     * `latestSchema`常量，用于统一引用固定值。
     */
    private static final String latestSchema = "CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_latest_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    ts bigint,\n" +
            "    bool_v boolean,\n" +
            "    str_v text,\n" +
            "    long_v bigint,\n" +
            "    dbl_v double,\n" +
            "    json_v text,\n" +
            "    PRIMARY KEY (( entity_type, entity_id ), key)\n" +
            ") WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };";

    /**
     * 分区常量，用于统一引用固定值。
     */
    private static final String partitionSchema = "CREATE TABLE IF NOT EXISTS thingsboard.ts_kv_partitions_cf (\n" +
            "    entity_type text, // (DEVICE, CUSTOMER, TENANT)\n" +
            "    entity_id timeuuid,\n" +
            "    key text,\n" +
            "    partition bigint,\n" +
            "    PRIMARY KEY (( entity_type, entity_id, key ), partition)\n" +
            ") WITH CLUSTERING ORDER BY ( partition ASC )\n" +
            "  AND compaction = { 'class' :  'LeveledCompactionStrategy'  };";

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `dir`：`dir` 参数。
     * 返回：处理结果。
     */
    public static CQLSSTableWriter getTsWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(tsSchema)
                .using("INSERT INTO thingsboard.ts_kv_cf (entity_type, entity_id, key, partition, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    /**
     * 功能：获取`Latest Writer`。
     * 参数：
     * - `dir`：`dir` 参数。
     * 返回：处理结果。
     */
    public static CQLSSTableWriter getLatestWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(latestSchema)
                .using("INSERT INTO thingsboard.ts_kv_latest_cf (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    /**
     * 功能：获取分区。
     * 参数：
     * - `dir`：`dir` 参数。
     * 返回：处理结果。
     */
    public static CQLSSTableWriter getPartitionWriter(File dir) {
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(partitionSchema)
                .using("INSERT INTO thingsboard.ts_kv_partitions_cf (entity_type, entity_id, key, partition) " +
                        "VALUES (?, ?, ?, ?)")
                .build();
    }
}
