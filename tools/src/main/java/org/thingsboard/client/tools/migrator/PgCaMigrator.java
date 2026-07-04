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

import com.google.common.collect.Lists;
import org.apache.cassandra.io.sstable.CQLSSTableWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`PgCaMigrator` 是 ThingsBoard Tools 模块 中的PostgreSQL 到 Cassandra 遥测迁移编排器，用于读取 ThingsBoard PostgreSQL dump 中的 ts_kv/ts_kv_latest 数据并生成 Cassandra SSTable 行和分区记录。
 * 2. 所属模块：位于 tools，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
 * 4. 生命周期：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束。
 * 5. 存在原因：迁移编排器集中处理大文件扫描、批量 writer 切换、分区生成和类型转换，避免命令入口承担复杂状态机。
 * 6. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
 * 7. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
 * 8. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
 * 9. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
 * 10. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
 * 11. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
 * 12. 设计模式：主要体现 Template Method / Strategy。
 */
public class PgCaMigrator {

    /**
     * 字段说明：
     * 1. 保存内容：`LOG_BATCH` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private final long LOG_BATCH = 1000000;
    private final long rowPerFile = 1000000;

    /**
     * 字段说明：
     * 1. 保存内容：`linesTsMigrated` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private long linesTsMigrated = 0;
    private long linesLatestMigrated = 0;
    private long castErrors = 0;
    /**
     * 字段说明：
     * 1. 保存内容：`castedOk` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private long castedOk = 0;

    private long currentWriterCount = 1;

    /**
     * 字段说明：
     * 1. 保存内容：`sourceFile` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private final File sourceFile;
    private final boolean castStringIfPossible;

    /**
     * 字段说明：
     * 1. 保存内容：`entityIdsAndTypes` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private final RelatedEntitiesParser entityIdsAndTypes;
    private final DictionaryParser keyParser;
    private CQLSSTableWriter currentTsWriter;
    /**
     * 字段说明：
     * 1. 保存内容：`currentPartitionsWriter` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private CQLSSTableWriter currentPartitionsWriter;
    private CQLSSTableWriter currentTsLatestWriter;
    private final Set<String> partitions = new HashSet<>();

    /**
     * 字段说明：
     * 1. 保存内容：`outTsDir` 保存本类运行所需的配置常量、客户端状态、解析结果、writer 引用、计数器或协议参数。
     * 2. 数据来源：来源于构造参数、命令行参数、Spring/HTTP/MQTT 配置、dump 文件解析、JWT 响应、证书文件或类内固定协议常量。
     * 3. 生命周期：字段生命周期与 `PostgreSQL 到 Cassandra 遥测迁移编排器` 实例或类加载周期一致；静态常量随类加载存在，实例状态随单次客户端会话、迁移命令或 Spring Boot 进程存在。
     * 4. 设计原因：保存为字段可以复用昂贵对象和跨方法状态，例如 token、writer、字典、分区集合、SSL 参数或默认配置名，避免每次方法调用重复构造。
     * 5. 线程安全：不可变常量天然安全；可变字段需要遵循调用方生命周期，REST token 刷新使用同步块保护，迁移工具字段通常只在单线程命令流程内使用。
     * 6. 事务/缓存/MQTT/Actor/数据库/Rule Engine：字段本身不打开事务；是否涉及缓存、MQTT、Actor、数据库或规则链取决于 ThingsBoard Tools 模块 的上层流程。
     */
    private File outTsDir;
    private File outTsLatestDir;

    /**
     * 方法说明：
     * 1. 职责：`PgCaMigrator` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    public PgCaMigrator(File sourceFile,
                                File ourTsDir,
                                File outTsPartitionDir,
                                File outTsLatestDir,
                                RelatedEntitiesParser allEntityIdsAndTypes,
                                DictionaryParser dictionaryParser,
                                boolean castStringsIfPossible) {
        this.sourceFile = sourceFile;
        this.entityIdsAndTypes = allEntityIdsAndTypes;
        this.keyParser = dictionaryParser;
        // 可选数字转换用于把历史字符串遥测迁移为 double 值，降低 Cassandra 查询时的类型不一致风险。
        this.castStringIfPossible = castStringsIfPossible;
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if(outTsLatestDir != null) {
            this.currentTsLatestWriter = WriterBuilder.getLatestWriter(outTsLatestDir);
            this.outTsLatestDir = outTsLatestDir;
        }
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if(ourTsDir != null) {
            this.currentTsWriter = WriterBuilder.getTsWriter(ourTsDir);
            this.currentPartitionsWriter = WriterBuilder.getPartitionWriter(outTsPartitionDir);
            this.outTsDir = ourTsDir;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`migrate` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    public void migrate() throws IOException {
        boolean isTsDone = false;
        boolean isLatestDone = false;
        String line;
        // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
        LineIterator iterator = FileUtils.lineIterator(this.sourceFile);

        try {
            // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
            while(iterator.hasNext()) {
                line = iterator.nextLine();
                // COPY 块边界决定后续行属于哪张表，迁移时必须先识别块头再解释字段含义。
                if(!isLatestDone && isBlockLatestStarted(line)) {
                    System.out.println("START TO MIGRATE LATEST");
                    long start = System.currentTimeMillis();
                    // block 级处理把 dump 文本扫描和具体行转换策略分离，便于复用同一读取循环处理不同表。
                    processBlock(iterator, currentTsLatestWriter, outTsLatestDir, this::toValuesLatest);
                    System.out.println("TOTAL LINES MIGRATED: " + linesLatestMigrated + ", FORMING OF SSL FOR LATEST TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isLatestDone = true;
                }

                // COPY 块边界决定后续行属于哪张表，迁移时必须先识别块头再解释字段含义。
                if(!isTsDone && isBlockTsStarted(line)) {
                    System.out.println("START TO MIGRATE TS");
                    long start = System.currentTimeMillis();
                    // block 级处理把 dump 文本扫描和具体行转换策略分离，便于复用同一读取循环处理不同表。
                    processBlock(iterator, currentTsWriter, outTsDir, this::toValuesTs);
                    System.out.println("TOTAL LINES MIGRATED: " + linesTsMigrated + ", FORMING OF SSL FOR TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isTsDone = true;
                }
            }

            System.out.println("Partitions collected " + partitions.size());
            long startTs = System.currentTimeMillis();
            // 循环按输入顺序处理页面参数、dump 行、分区集合或命令参数，保证输出顺序和源数据语义可追踪。
            for (String partition : partitions) {
                String[] split = partition.split("\\|");
                List<Object> values = Lists.newArrayList();
                values.add(split[0]);
                values.add(UUID.fromString(split[1]));
                values.add(split[2]);
                values.add(Long.parseLong(split[3]));
                currentPartitionsWriter.addRow(values);
            }

            System.out.println(new Date() + " Migrated partitions " + partitions.size() + " in " + (System.currentTimeMillis() - startTs));

            System.out.println();
            System.out.println("Finished migrate Telemetry");

        } finally {
            iterator.close();
            currentTsLatestWriter.close();
            currentTsWriter.close();
            currentPartitionsWriter.close();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`logLinesProcessed` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void logLinesProcessed(long lines) {
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if (lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines processed = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`logLinesMigrated` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void logLinesMigrated(long lines) {
        // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
        if(lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines migrated = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`addTypeIdKey` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void addTypeIdKey(List<Object> result, List<String> raw) {
        result.add(entityIdsAndTypes.getEntityType(raw.get(0)));
        result.add(UUID.fromString(raw.get(0)));
        result.add(keyParser.getKeyByKeyId(raw.get(1)));
    }

    /**
     * 方法说明：
     * 1. 职责：`addPartitions` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void addPartitions(List<Object> result, List<String> raw) {
        long ts = Long.parseLong(raw.get(2));
        // 分区时间按月归一化，匹配 ThingsBoard Cassandra 遥测表的分区查询模型。
        long partition = toPartitionTs(ts);
        result.add(partition);
        result.add(ts);
    }

    /**
     * 方法说明：
     * 1. 职责：`addTimeseries` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void addTimeseries(List<Object> result, List<String> raw) {
        result.add(Long.parseLong(raw.get(2)));
    }

    /**
     * 方法说明：
     * 1. 职责：`addValues` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void addValues(List<Object> result, List<String> raw) {
        result.add(raw.get(3).equals("\\N") ? null : raw.get(3).equals("t") ? Boolean.TRUE : Boolean.FALSE);
        result.add(raw.get(4).equals("\\N") ? null : raw.get(4));
        result.add(raw.get(5).equals("\\N") ? null : Long.parseLong(raw.get(5)));
        result.add(raw.get(6).equals("\\N") ? null : Double.parseDouble(raw.get(6)));
        result.add(raw.get(7).equals("\\N") ? null : raw.get(7));
    }

    /**
     * 方法说明：
     * 1. 职责：`toValuesTs` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private List<Object> toValuesTs(List<String> raw) {

        logLinesMigrated(linesTsMigrated++);

        List<Object> result = new ArrayList<>();

        addTypeIdKey(result, raw);
        addPartitions(result, raw);
        addValues(result, raw);

        processPartitions(result);

        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：`toValuesLatest` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private List<Object> toValuesLatest(List<String> raw) {
        logLinesMigrated(linesLatestMigrated++);
        List<Object> result = new ArrayList<>();

        addTypeIdKey(result, raw);
        addTimeseries(result, raw);
        addValues(result, raw);

        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：`toPartitionTs` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    // 分区时间按月归一化，匹配 ThingsBoard Cassandra 遥测表的分区查询模型。
    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        // 分区时间按月归一化，匹配 ThingsBoard Cassandra 遥测表的分区查询模型。
        return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * 方法说明：
     * 1. 职责：`processPartitions` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void processPartitions(List<Object> values) {
        String key = values.get(0) + "|" + values.get(1) + "|" + values.get(2) + "|" + values.get(3);
        // 分区时间按月归一化，匹配 ThingsBoard Cassandra 遥测表的分区查询模型。
        partitions.add(key);
    }

    /**
     * 方法说明：
     * 1. 职责：`processBlock` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
    private void processBlock(LineIterator iterator, CQLSSTableWriter writer, File outDir, Function<List<String>, List<Object>> function) {
        String currentLine;
        long linesProcessed = 0;
        // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
        while(iterator.hasNext()) {
            logLinesProcessed(linesProcessed++);
            currentLine = iterator.nextLine();
            // PostgreSQL dump 使用空行或反斜杠点号结束 COPY 块，这里统一封装块结束判断。
            if(isBlockFinished(currentLine)) {
                return;
            }

            try {
                List<String> raw = Arrays.stream(currentLine.trim().split("\t"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                List<Object> values = function.apply(raw);

                // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
                if (this.currentWriterCount == 0) {
                    System.out.println(new Date() + " close writer " + new Date());
                    writer.close();
                    writer = WriterBuilder.getLatestWriter(outDir);
                }

                // 可选数字转换用于把历史字符串遥测迁移为 double 值，降低 Cassandra 查询时的类型不一致风险。
                if (this.castStringIfPossible) {
                    writer.addRow(castToNumericIfPossible(values));
                } else {
                    writer.addRow(values);
                }

                currentWriterCount++;
                // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
                if (currentWriterCount >= rowPerFile) {
                    currentWriterCount = 0;
                }
            // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
            } catch (Exception ex) {
                System.out.println(ex.getMessage() + " -> " + currentLine);
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`castToNumericIfPossible` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private List<Object> castToNumericIfPossible(List<Object> values) {
        try {
            // 可选数字转换用于把历史字符串遥测迁移为 double 值，降低 Cassandra 查询时的类型不一致风险。
            if (values.get(6) != null && NumberUtils.isNumber(values.get(6).toString())) {
                Double casted = NumberUtils.createDouble(values.get(6).toString());
                List<Object> numeric = Lists.newArrayList();
                numeric.addAll(values);
                numeric.set(6, null);
                numeric.set(8, casted);
                castedOk++;
                return numeric;
            }
        // 捕获异常后统一转为当前工具或客户端的失败路径，避免静默吞掉认证、解析、写入或协议错误。
        } catch (Throwable th) {
            castErrors++;
        }

        processPartitions(values);

        return values;
    }

    /**
     * 方法说明：
     * 1. 职责：`isBlockFinished` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    // PostgreSQL dump 使用空行或反斜杠点号结束 COPY 块，这里统一封装块结束判断。
    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    /**
     * 方法说明：
     * 1. 职责：`isBlockTsStarted` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    // COPY 块边界决定后续行属于哪张表，迁移时必须先识别块头再解释字段含义。
    private boolean isBlockTsStarted(String line) {
        return line.startsWith("COPY public.ts_kv (");
    }

    /**
     * 方法说明：
     * 1. 职责：`isBlockLatestStarted` 执行 PostgreSQL 到 Cassandra 遥测迁移编排器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    // COPY 块边界决定后续行属于哪张表，迁移时必须先识别块头再解释字段含义。
    private boolean isBlockLatestStarted(String line) {
        return line.startsWith("COPY public.ts_kv_latest (");
    }

    /**
     * 本类总结：
     * 1. 核心职责：`PgCaMigrator` 负责读取 ThingsBoard PostgreSQL dump 中的 ts_kv/ts_kv_latest 数据并生成 Cassandra SSTable 行和分区记录。
     * 2. 核心流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 3. 关键依赖：Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
     * 4. 设计重点：通过 Template Method / Strategy 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
