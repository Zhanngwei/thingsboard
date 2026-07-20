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
 * 1. `DictionaryParser` 是 ThingsBoard Tools 中转换 `Dictionary` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
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
}
