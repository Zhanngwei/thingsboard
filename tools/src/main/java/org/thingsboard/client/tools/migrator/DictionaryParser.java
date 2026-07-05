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
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. 类目的：`DictionaryParser` 是 ThingsBoard Tools 模块 中的PostgreSQL dump 解析器，用于从 PostgreSQL COPY dump 中提取遥测 key 字典或实体 UUID 到 EntityType 的映射。
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
public class DictionaryParser {
    private Map<String, String> dictionaryParsed = new HashMap<>();

    /**
     * 功能：创建 `DictionaryParser` 实例，并初始化必要字段。
     * 参数：
     * - `sourceFile`：`sourceFile` 参数。
     * 返回：新创建的对象实例。
     */
    public DictionaryParser(File sourceFile) throws IOException {
        parseDictionaryDump(FileUtils.lineIterator(sourceFile));
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `keyId`：键ID。
     * 返回：文本结果。
     */
    public String getKeyByKeyId(String keyId) {
        return dictionaryParsed.get(keyId);
    }

    /**
     * 功能：判断`Block Finished`。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    /**
     * 功能：判断`Block Started`。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockStarted(String line) {
        return line.startsWith("COPY public.ts_kv_dictionary (");
    }

    /**
     * 功能：解析`Dictionary Dump`。
     * 参数：
     * - `iterator`：`iterator` 参数。
     * 返回：无。
     */
    private void parseDictionaryDump(LineIterator iterator) throws IOException {
        try {
            String tempLine;
            while (iterator.hasNext()) {
                tempLine = iterator.nextLine();

                if (isBlockStarted(tempLine)) {
                    processBlock(iterator);
                }
            }
        } finally {
            iterator.close();
        }
    }

    /**
     * 功能：处理`Block`。
     * 参数：
     * - `lineIterator`：`lineIterator` 参数。
     * 返回：无。
     */
    private void processBlock(LineIterator lineIterator) {
        String tempLine;
        String[] lineSplited;
        while(lineIterator.hasNext()) {
            tempLine = lineIterator.nextLine();
            if(isBlockFinished(tempLine)) {
                return;
            }

            lineSplited = tempLine.split("\t");
            dictionaryParsed.put(lineSplited[1], lineSplited[0]);
        }
    }
    /**
     * 本类总结：
     * 1. 核心职责：`DictionaryParser` 负责从 PostgreSQL COPY dump 中提取遥测 key 字典或实体 UUID 到 EntityType 的映射。
     * 2. 核心流程：读取命令行参数或 dump 文件，解析字典和实体类型，构造 Cassandra SSTable 行，或建立 MQTT SSL 连接发送测试遥测。
     * 3. 关键依赖：Apache Commons CLI/IO、Cassandra CQLSSTableWriter、Paho MQTT、SSL KeyStore、ThingsBoard common data。
     * 4. 设计重点：通过 Parser / Adapter 把外部协议、文件格式、启动参数或 REST 细节封装在边界类中，让核心业务模块保持清晰。
     * 5. 学习重点：关注生命周期边界、线程安全假设、远端事务归属、缓存/数据库间接性、MQTT/Actor/Rule Engine 的进入点以及为什么该类只承担当前边界职责。
     */
}
