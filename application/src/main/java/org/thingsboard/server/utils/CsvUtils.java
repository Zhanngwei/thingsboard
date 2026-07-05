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
package org.thingsboard.server.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.CharSequenceReader;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. 类目的：`CsvUtils` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CsvUtils {

    /**
     * 功能：解析`Csv`。
     * 参数：
     * - `content`：`content` 参数。
     * - `delimiter`：数量限制。
     * 返回：匹配的数据集合。
     */
    public static List<List<String>> parseCsv(String content, Character delimiter) throws Exception {
        CSVFormat csvFormat = delimiter.equals(',') ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withDelimiter(delimiter);

        List<CSVRecord> records;
        try (CharSequenceReader reader = new CharSequenceReader(content)) {
            records = csvFormat.parse(reader).getRecords();
        }

        return records.stream()
                .map(record -> Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                        .map(record::get)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CsvUtils` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
