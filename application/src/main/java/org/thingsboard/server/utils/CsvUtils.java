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
 * 1. `CsvUtils` 是 ThingsBoard Application 中处理 `Csv Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
