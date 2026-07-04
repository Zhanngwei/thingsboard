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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`RelatedEntitiesParser` 是 ThingsBoard Tools 模块 中的PostgreSQL dump 解析器，用于从 PostgreSQL COPY dump 中提取遥测 key 字典或实体 UUID 到 EntityType 的映射。
 * 2. 所属模块：位于 tools，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
 * 4. 生命周期：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束。
 * 5. 存在原因：单独解析器隔离 dump 文本格式和迁移主流程，便于复用并降低 PgCaMigrator 的状态复杂度。
 * 6. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
 * 7. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
 * 8. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
 * 9. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
 * 10. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
 * 11. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
 * 12. 设计模式：主要体现 Parser / Adapter。
 */
public class RelatedEntitiesParser {
    private final Map<String, String> allEntityIdsAndTypes = new HashMap<>();
    
    private final Map<String, EntityType> tableNameAndEntityType = Map.ofEntries(
            Map.entry("COPY public.alarm ", EntityType.ALARM),
            Map.entry("COPY public.asset ", EntityType.ASSET),
            Map.entry("COPY public.customer ", EntityType.CUSTOMER),
            Map.entry("COPY public.dashboard ", EntityType.DASHBOARD),
            Map.entry("COPY public.device ", EntityType.DEVICE),
            Map.entry("COPY public.rule_chain ", EntityType.RULE_CHAIN),
            Map.entry("COPY public.rule_node ", EntityType.RULE_NODE),
            Map.entry("COPY public.tenant ", EntityType.TENANT),
            Map.entry("COPY public.tb_user ", EntityType.USER),
            Map.entry("COPY public.entity_view ", EntityType.ENTITY_VIEW),
            Map.entry("COPY public.widgets_bundle ", EntityType.WIDGETS_BUNDLE),
            Map.entry("COPY public.widget_type ", EntityType.WIDGET_TYPE),
            Map.entry("COPY public.tenant_profile ", EntityType.TENANT_PROFILE),
            Map.entry("COPY public.device_profile ", EntityType.DEVICE_PROFILE),
            Map.entry("COPY public.asset_profile ", EntityType.ASSET_PROFILE),
            Map.entry("COPY public.api_usage_state ", EntityType.API_USAGE_STATE)
    );

    /**
     * 方法说明：
     * 1. 职责：`RelatedEntitiesParser` 执行 PostgreSQL dump 解析器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    public RelatedEntitiesParser(File source) throws IOException {
        // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
        processAllTables(FileUtils.lineIterator(source));
    }

    /**
     * 方法说明：
     * 1. 职责：`getEntityType` 执行 PostgreSQL dump 解析器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    public String getEntityType(String uuid) {
        return this.allEntityIdsAndTypes.get(uuid);
    }

    /**
     * 方法说明：
     * 1. 职责：`isBlockFinished` 执行 PostgreSQL dump 解析器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
     * 1. 职责：`processAllTables` 执行 PostgreSQL dump 解析器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void processAllTables(LineIterator lineIterator) throws IOException {
        String currentLine;
        try {
            // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
            while (lineIterator.hasNext()) {
                // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
                currentLine = lineIterator.nextLine();
                for(Map.Entry<String, EntityType> entry : tableNameAndEntityType.entrySet()) {
                    // 条件分支用于区分配置是否存在、dump 块边界、类型选择、token 生命周期或协议启动参数覆盖场景。
                    if(currentLine.startsWith(entry.getKey())) {
                        // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
                        processBlock(lineIterator, entry.getValue());
                    }
                }
            }
        } finally {
            // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
            lineIterator.close();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：`processBlock` 执行 PostgreSQL dump 解析器 的一个明确步骤，完成请求发送、参数转换、文件解析、writer 构造、TLS/MQTT 操作或 Spring Boot 启动参数处理。
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
    private void processBlock(LineIterator lineIterator, EntityType entityType) {
        String currentLine;
        // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
        while(lineIterator.hasNext()) {
            // 使用流式逐行读取处理大 dump 文件，避免一次性加载全部遥测数据造成内存压力。
            currentLine = lineIterator.nextLine();
            // PostgreSQL dump 使用空行或反斜杠点号结束 COPY 块，这里统一封装块结束判断。
            if(isBlockFinished(currentLine)) {
                return;
            }
            allEntityIdsAndTypes.put(currentLine.split("\t")[0], entityType.name());
        }
    }
    /**
     * 本类总结：
     * 1. 核心职责：`RelatedEntitiesParser` 负责从 PostgreSQL COPY dump 中提取遥测 key 字典或实体 UUID 到 EntityType 的映射。
     * 2. 核心流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 3. 关键依赖：Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
     * 4. 设计重点：通过 Parser / Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
