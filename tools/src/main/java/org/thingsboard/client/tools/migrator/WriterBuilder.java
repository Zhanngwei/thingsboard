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
     * 字段说明：
     * 1. 保存内容：`tsSchema` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `Cassandra SSTable Writer 工厂` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
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
     * 字段说明：
     * 1. 保存内容：`latestSchema` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `Cassandra SSTable Writer 工厂` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
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
     * 字段说明：
     * 1. 保存内容：`partitionSchema` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `Cassandra SSTable Writer 工厂` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
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
     * 方法说明：
     * 1. 职责：`getTsWriter` 执行 Cassandra SSTable Writer 工厂 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
     * 9. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
     * 10. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
     * 11. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
     * 12. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
     * 13. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
     */
    public static CQLSSTableWriter getTsWriter(File dir) {
        // 使用 CQLSSTableWriter 直接生成离线 SSTable，避免迁移时通过在线服务逐条写入造成事务和吞吐瓶颈。
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(tsSchema)
                .using("INSERT INTO thingsboard.ts_kv_cf (entity_type, entity_id, key, partition, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：`getLatestWriter` 执行 Cassandra SSTable Writer 工厂 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
     * 9. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
     * 10. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
     * 11. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
     * 12. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
     * 13. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
     */
    public static CQLSSTableWriter getLatestWriter(File dir) {
        // 使用 CQLSSTableWriter 直接生成离线 SSTable，避免迁移时通过在线服务逐条写入造成事务和吞吐瓶颈。
        return CQLSSTableWriter.builder()
                .inDirectory(dir)
                .forTable(latestSchema)
                .using("INSERT INTO thingsboard.ts_kv_latest_cf (entity_type, entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .build();
    }

    /**
     * 方法说明：
     * 1. 职责：`getPartitionWriter` 执行 Cassandra SSTable Writer 工厂 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
     * 2. 输入参数：参数通常表示 REST DTO/ID、分页条件、文件路径、dump 行、Cassandra 行值、命令行参数、证书配置、MQTT 消息或 Spring Boot 启动参数。
     * 3. 返回值：返回服务端 DTO、转换后的 KV/行值、writer、更新后的参数数组、Optional/PageData/byte[]，或通过 `void` 的副作用完成发送、写入、启动、关闭和断言式失败。
     * 4. 调用时机：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束；由 SDK 调用方、命令行入口、迁移编排器、SpringApplication 或类内辅助流程触发。
     * 5. 调用方：可能是外部 Java 客户端、测试/运维脚本、MigratorTool、PgCaMigrator、Spring Boot launcher 或本类其它辅助方法。
     * 6. 使用流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 7. 线程安全：方法本身不额外声明全局线程安全；REST token 刷新依赖同步块，迁移/解析方法按单线程大文件扫描设计，transport 启动方法在进程启动线程中执行。
     * 8. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
     * 9. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
     * 10. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
     * 11. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
     * 12. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
     * 13. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
     */
    public static CQLSSTableWriter getPartitionWriter(File dir) {
        // 使用 CQLSSTableWriter 直接生成离线 SSTable，避免迁移时通过在线服务逐条写入造成事务和吞吐瓶颈。
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
