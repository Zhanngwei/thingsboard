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
 * 1. 类目的：`WriterBuilder` 是 ThingsBoard Tools 模块 中的Cassandra SSTable Writer 工厂，用于集中定义 ThingsBoard 遥测相关 Cassandra 表结构和 insert 语句，并创建对应 CQLSSTableWriter。
 * 2. 所属模块：位于 tools，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
 * 4. 生命周期：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束。
 * 5. 存在原因：把 writer 构造和 CQL schema 集中到工厂类，迁移流程可以专注行数据转换，避免重复硬编码表结构。
 * 6. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
 * 7. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
 * 8. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
 * 9. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
 * 10. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
 * 11. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
 * 12. 设计模式：主要体现 Factory / Builder。
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
    /**
     * 本类总结：
     * 1. 核心职责：`WriterBuilder` 负责集中定义 ThingsBoard 遥测相关 Cassandra 表结构和 insert 语句，并创建对应 CQLSSTableWriter。
     * 2. 核心流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 3. 关键依赖：Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
     * 4. 设计重点：通过 Factory / Builder 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
