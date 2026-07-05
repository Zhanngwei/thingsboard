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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

/**
 * 中文说明：
 * 1. 类目的：`MigratorTool` 是 ThingsBoard Tools 模块 中的遥测迁移命令入口，用于解析命令行参数，组装 dump 解析器和迁移器，并启动离线迁移流程。
 * 2. 所属模块：位于 tools，服务于 ThingsBoard 的客户端访问、离线工具或独立协议接入边界。
 * 3. 协作模块：主要协作对象包括 Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
 * 4. 生命周期：由命令行 main 或迁移流程按需创建，处理完输入文件、SSTable writer 或 MQTT 会话后结束。
 * 5. 存在原因：命令入口只负责参数和流程装配，实际解析/写入交给专门组件，便于维护和测试。
 * 6. 事务：不参与在线事务；迁移工具生成离线 SSTable 文件，由 Cassandra 导入流程承担最终写入。
 * 7. 缓存：使用内存 Map/Set 缓存 dump 中的字典、实体类型和分区键，生命周期限定在单次迁移命令内。
 * 8. MQTT：只有 MqttSslClient 直接创建 MQTT SSL 连接并发布测试遥测，迁移工具不涉及 MQTT。
 * 9. Actor 通信：工具不直接发送 Actor 消息，导入后的数据被服务端读取时才可能进入后续运行时流程。
 * 10. 数据库：迁移工具面向 PostgreSQL dump 和 Cassandra SSTable 文件，属于离线数据库迁移辅助逻辑。
 * 11. Rule Engine：不执行 Rule Engine，迁移数据导入后才可能被服务端规则或查询流程消费。
 * 12. 设计模式：主要体现 Command。
 */
public class MigratorTool {

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);

        try {
            boolean castEnable = Boolean.parseBoolean(cmd.getOptionValue("castEnable"));
            File allTelemetrySource = new File(cmd.getOptionValue("telemetryFrom"));
            File tsSaveDir = null;
            File partitionsSaveDir = null;
            File latestSaveDir = null;

            RelatedEntitiesParser allEntityIdsAndTypes =
                    new RelatedEntitiesParser(new File(cmd.getOptionValue("relatedEntities")));
            DictionaryParser dictionaryParser = new DictionaryParser(allTelemetrySource);

            if(cmd.getOptionValue("latestTelemetryOut") != null) {
                latestSaveDir = new File(cmd.getOptionValue("latestTelemetryOut"));
            }
            if(cmd.getOptionValue("telemetryOut") != null) {
                tsSaveDir = new File(cmd.getOptionValue("telemetryOut"));
                partitionsSaveDir = new File(cmd.getOptionValue("partitionsOut"));
            }

            new PgCaMigrator(allTelemetrySource, tsSaveDir, partitionsSaveDir, latestSaveDir, allEntityIdsAndTypes, dictionaryParser, castEnable).migrate();

        } catch (Throwable th) {
            th.printStackTrace();
            throw new IllegalStateException("failed", th);
        }

    }

    /**
     * 功能：解析参数。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：处理结果。
     */
    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option telemetryAllFrom = new Option("telemetryFrom", "telemetryFrom", true, "telemetry source file");
        telemetryAllFrom.setRequired(true);
        options.addOption(telemetryAllFrom);

        Option latestTsOutOpt = new Option("latestOut", "latestTelemetryOut", true, "latest telemetry save dir");
        latestTsOutOpt.setRequired(false);
        options.addOption(latestTsOutOpt);

        Option tsOutOpt = new Option("tsOut", "telemetryOut", true, "sstable save dir");
        tsOutOpt.setRequired(false);
        options.addOption(tsOutOpt);

        Option partitionOutOpt = new Option("partitionsOut", "partitionsOut", true, "partitions save dir");
        partitionOutOpt.setRequired(false);
        options.addOption(partitionOutOpt);

        Option castOpt = new Option("castEnable", "castEnable", true, "cast String to Double if possible");
        castOpt.setRequired(true);
        options.addOption(castOpt);

        Option relatedOpt = new Option("relatedEntities", "relatedEntities", true, "related entities source file path");
        relatedOpt.setRequired(true);
        options.addOption(relatedOpt);

        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new BasicParser();

        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }
        return null;
    }

    /**
     * 本类总结：
     * 1. 核心职责：`MigratorTool` 负责解析命令行参数，组装 dump 解析器和迁移器，并启动离线迁移流程。
     * 2. 核心流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 3. 关键依赖：Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
     * 4. 设计重点：通过 Command 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
