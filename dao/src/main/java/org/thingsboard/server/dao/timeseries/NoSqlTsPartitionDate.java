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
package org.thingsboard.server.dao.timeseries;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `NoSqlTsPartitionDate` 是 ThingsBoard DAO 中定义分区固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum NoSqlTsPartitionDate {

    MINUTES("yyyy-MM-dd-HH-mm", ChronoUnit.MINUTES), HOURS("yyyy-MM-dd-HH", ChronoUnit.HOURS), DAYS("yyyy-MM-dd", ChronoUnit.DAYS), MONTHS("yyyy-MM", ChronoUnit.MONTHS), YEARS("yyyy", ChronoUnit.YEARS),INDEFINITE("",ChronoUnit.FOREVER);

    /**
     * `pattern` 字段，保存当前对象的对应属性。
     */
    private final String pattern;
    private final transient TemporalUnit truncateUnit;
    public final static LocalDateTime EPOCH_START = LocalDateTime.ofEpochSecond(0,0, ZoneOffset.UTC);

    /**
     * 功能：创建 `NoSqlTsPartitionDate` 实例，并初始化必要字段。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * - `truncateUnit`：`truncateUnit` 参数。
     * 返回：新创建的对象实例。
     */
    NoSqlTsPartitionDate(String pattern, TemporalUnit truncateUnit) {
        this.pattern = pattern;
        this.truncateUnit = truncateUnit;
    }


    /**
     * 功能：获取`Pattern`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * 功能：获取单位。
     * 参数：无。
     * 返回：处理结果。
     */
    public TemporalUnit getTruncateUnit() {
        return truncateUnit;
    }

    /**
     * 功能：执行 `truncatedTo` 对应的处理。
     * 参数：
     * - `time`：`time` 参数。
     * 返回：处理结果。
     */
    public LocalDateTime truncatedTo(LocalDateTime time) {
        switch (this){
            case MONTHS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case YEARS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            case INDEFINITE:
                 return EPOCH_START;
            default:
                return time.truncatedTo(truncateUnit);
        }
    }

    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    public static Optional<NoSqlTsPartitionDate> parse(String name) {
        NoSqlTsPartitionDate partition = null;
        if (name != null) {
            for (NoSqlTsPartitionDate partitionDate : NoSqlTsPartitionDate.values()) {
                if (partitionDate.name().equalsIgnoreCase(name)) {
                    partition = partitionDate;
                    break;
                }
            }
        }
        return Optional.ofNullable(partition);
    }
}
