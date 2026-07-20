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
package org.thingsboard.server.dao.model.sqlts.timescale.ts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_AVG;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_AVG_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_COUNT;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_COUNT_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MAX;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MAX_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MIN;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MIN_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_SUM;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_SUM_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FROM_WHERE_CLAUSE;

/**
 * 中文说明：
 * 1. `TimescaleTsKvEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractTsKvEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ts_kv")
@IdClass(TimescaleTsKvCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "timescaleAggregationMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TimescaleTsKvEntity.class,
                                columns = {
                                        @ColumnResult(name = "tsBucket", type = Long.class),
                                        @ColumnResult(name = "interval", type = Long.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "longCountValue", type = Long.class),
                                        @ColumnResult(name = "doubleCountValue", type = Long.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "aggType", type = String.class),
                                        @ColumnResult(name = "maxAggTs", type = Long.class),
                                }
                        ),
                }),
        @SqlResultSetMapping(
                name = "timescaleCountMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TimescaleTsKvEntity.class,
                                columns = {
                                        @ColumnResult(name = "tsBucket", type = Long.class),
                                        @ColumnResult(name = "interval", type = Long.class),
                                        @ColumnResult(name = "booleanValueCount", type = Long.class),
                                        @ColumnResult(name = "strValueCount", type = Long.class),
                                        @ColumnResult(name = "longValueCount", type = Long.class),
                                        @ColumnResult(name = "doubleValueCount", type = Long.class),
                                        @ColumnResult(name = "jsonValueCount", type = Long.class),
                                        @ColumnResult(name = "maxAggTs", type = Long.class),
                                }
                        )
                }),
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = FIND_AVG,
                query = FIND_AVG_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_MAX,
                query = FIND_MAX_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_MIN,
                query = FIND_MIN_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_SUM,
                query = FIND_SUM_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_COUNT,
                query = FIND_COUNT_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleCountMapping"
        )
})
public final class TimescaleTsKvEntity extends AbstractTsKvEntity {

    /**
     * 功能：创建 `TimescaleTsKvEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TimescaleTsKvEntity() {
    }

    /**
     * 功能：创建 `TimescaleTsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `tsBucket`：`tsBucket` 参数。
     * - `interval`：`interval` 参数。
     * - `longValue`：值。
     * - `doubleValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TimescaleTsKvEntity(Long tsBucket, Long interval, Long longValue, Double doubleValue, Long longCountValue, Long doubleCountValue, String strValue, String aggType, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!StringUtils.isEmpty(strValue)) {
            this.strValue = strValue;
        }
        if (!isAllNull(tsBucket, interval, longValue, doubleValue, longCountValue, doubleCountValue)) {
            this.ts = tsBucket + interval / 2;
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
     * 功能：创建 `TimescaleTsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `tsBucket`：`tsBucket` 参数。
     * - `interval`：`interval` 参数。
     * - `booleanValueCount`：值。
     * - `strValueCount`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TimescaleTsKvEntity(Long tsBucket, Long interval, Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount, Long jsonValueCount, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!isAllNull(tsBucket, interval, booleanValueCount, strValueCount, longValueCount, doubleValueCount, jsonValueCount)) {
            this.ts = tsBucket + interval / 2;
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
        return ts != null && (strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null);
    }
}
