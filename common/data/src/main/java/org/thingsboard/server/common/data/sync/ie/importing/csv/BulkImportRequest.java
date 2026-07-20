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
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Data;

import java.util.List;

/**
 * 中文说明：
 * 1. `BulkImportRequest` 是 ThingsBoard Common Data 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class BulkImportRequest {
    /**
     * 文件，用于定位本地文件或目录。
     */
    private String file;
    private Mapping mapping;

    /**
     * 中文说明：
     * 1. `Mapping` 是 ThingsBoard Common Data 中承载 `Mapping` 信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    public static class Mapping {
        /**
         * `columns`列表，用于保存一组待处理对象。
         */
        private List<ColumnMapping> columns;
        private Character delimiter;
        /**
         * 是否满足`update`条件。
         */
        private Boolean update;
        private Boolean header;
    }

    /**
     * 中文说明：
     * 1. `ColumnMapping` 是 ThingsBoard Common Data 中承载 `Column Mapping` 信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    public static class ColumnMapping {
        /**
         * 类型，用于区分不同处理分支。
         */
        private BulkImportColumnType type;
        private String key;
    }

}
