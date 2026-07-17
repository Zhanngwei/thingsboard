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
package org.thingsboard.server.common.data;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 13.07.17.
 */
/**
 * 中文说明：
 * 1. `UUIDConverter` 是 ThingsBoard Common Data 中转换 `UUID` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 它直接协作于源模型、目标模型和相关编解码类型。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
public class UUIDConverter {

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `src`：`src` 参数。
     * 返回：处理结果。
     */
    public static UUID fromString(String src) {
        return UUID.fromString(src.substring(7, 15) + "-" + src.substring(3, 7) + "-1"
                + src.substring(0, 3) + "-" + src.substring(15, 19) + "-" + src.substring(19));
    }

    /**
     * 功能：执行 `fromTimeUUID` 对应的处理。
     * 参数：
     * - `src`：`src` 参数。
     * 返回：文本结果。
     */
    public static String fromTimeUUID(UUID src) {
        if (src.version() != 1) {
            throw new IllegalArgumentException("Only Time-Based UUID (Version 1) is supported!");
        }
        String str = src.toString();
        // 58e0a7d7-eebc-11d8-9669-0800200c9a66 => 1d8eebc58e0a7d796690800200c9a66. Note that [11d8] -> [1d8]
        return str.substring(15, 18) + str.substring(9, 13) + str.substring(0, 8) + str.substring(19, 23) + str.substring(24);
    }

    /**
     * 功能：执行 `fromTimeUUIDs` 对应的处理。
     * 参数：
     * - `uuids`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static List<String> fromTimeUUIDs(List<UUID> uuids) {
        if (uuids == null) {
            return null;
        }
        return uuids.stream().map(UUIDConverter::fromTimeUUID).collect(Collectors.toList());
    }

}
