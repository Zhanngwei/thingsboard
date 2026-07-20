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
package org.thingsboard.server.dao.model.sqlts.ts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * 中文说明：
 * 1. `TsKvEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractTsKvEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "ts_kv")
@IdClass(TsKvCompositeKey.class)
public final class TsKvEntity extends AbstractTsKvEntity {

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity() {
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `strValue`：值。
     * - `aggValuesLastTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(String strValue, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        this.strValue = strValue;
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `longValue`：值。
     * - `doubleValue`：值。
     * - `longCountValue`：值。
     * - `doubleCountValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(Long longValue, Double doubleValue, Long longCountValue, Long doubleCountValue, String aggType, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!isAllNull(longValue, doubleValue, longCountValue, doubleCountValue)) {
            switch (aggType) {
                case AVG:
                    double sum = 0.0;
                    if (longValue != null) {
                        sum += longValue;
                    }
                    if (doubleValue != null) {
                        sum += doubleValue;
                    }
                    long totalCount = longCountValue + doubleCountValue;
                    if (totalCount > 0) {
                        this.doubleValue = sum / (longCountValue + doubleCountValue);
                    } else {
                        this.doubleValue = 0.0;
                    }
                    this.aggValuesCount = totalCount;
                    break;
                case SUM:
                    if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue + (longValue != null ? longValue.doubleValue() : 0.0);
                    } else {
                        this.longValue = longValue;
                    }
                    break;
                case MIN:
                case MAX:
                    if (longCountValue > 0 && doubleCountValue > 0) {
                        this.doubleValue = MAX.equals(aggType) ? Math.max(doubleValue, longValue.doubleValue()) : Math.min(doubleValue, longValue.doubleValue());
                    } else if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue;
                    } else if (longCountValue > 0) {
                        this.longValue = longValue;
                    }
                    break;
            }
        }
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `booleanValueCount`：值。
     * - `strValueCount`：值。
     * - `longValueCount`：值。
     * - `doubleValueCount`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount, Long jsonValueCount, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!isAllNull(booleanValueCount, strValueCount, longValueCount, doubleValueCount)) {
            if (booleanValueCount != 0) {
                this.longValue = booleanValueCount;
            } else if (strValueCount != 0) {
                this.longValue = strValueCount;
            } else if (jsonValueCount != 0) {
                this.longValue = jsonValueCount;
            } else {
                this.longValue = longValueCount + doubleValueCount;
            }
        }
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null;
    }
}
