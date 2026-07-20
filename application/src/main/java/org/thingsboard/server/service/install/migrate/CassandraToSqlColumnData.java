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
package org.thingsboard.server.service.install.migrate;

import lombok.Data;

/**
 * 中文说明：
 * 1. `CassandraToSqlColumnData` 是 ThingsBoard Application 中承载 `Cassandra To Sql` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class CassandraToSqlColumnData {

    /**
     * 值，保存当前处理得到的具体内容。
     */
    private String value;
    private String originalValue;
    /**
     * 计数器，用于控制处理规模或位置。
     */
    private int constraintCounter = 0;

    /**
     * 功能：创建 `CassandraToSqlColumnData` 实例，并初始化必要字段。
     * 参数：
     * - `value`：值。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumnData(String value) {
        this.value = value;
        this.originalValue = value;
    }

    /**
     * 功能：执行 `nextContraintCounter` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int nextContraintCounter() {
        return ++constraintCounter;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `column`：`column` 参数。
     * 返回：文本结果。
     */
    public String getNextConstraintStringValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String newValue = this.originalValue + counter;
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = this.originalValue.substring(0, this.originalValue.length()-overflow) + counter;
        }
        return newValue;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `column`：`column` 参数。
     * 返回：文本结果。
     */
    public String getNextConstraintEmailValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String[] emailValues = this.originalValue.split("@");
        String newValue = emailValues[0] + "+" + counter + "@" + emailValues[1];
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = emailValues[0].substring(0, emailValues[0].length()-overflow) + "+" + counter + "@" + emailValues[1];
        }
        return newValue;
    }

    /**
     * 功能：获取值。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getLogValue() {
        if (this.value != null && this.value.length() > 255) {
            return this.value.substring(0, 255) + "...[truncated " + (this.value.length() - 255) + " symbols]";
        }
        return this.value;
    }

}
