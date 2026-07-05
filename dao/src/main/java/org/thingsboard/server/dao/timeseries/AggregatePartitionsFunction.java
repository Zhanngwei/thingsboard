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

import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.kv.AggTsKvEntry;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntryAggWrapper;
import org.thingsboard.server.dao.nosql.TbResultSet;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 20.02.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`AggregatePartitionsFunction` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@Slf4j
public class AggregatePartitionsFunction implements com.google.common.util.concurrent.AsyncFunction<List<TbResultSet>, Optional<TsKvEntryAggWrapper>> {

    /**
     * `LONG_CNT_POS`常量，用于统一引用固定值。
     */
    private static final int LONG_CNT_POS = 0;
    private static final int DOUBLE_CNT_POS = 1;
    /**
     * `BOOL_CNT_POS`常量，用于统一引用固定值。
     */
    private static final int BOOL_CNT_POS = 2;
    private static final int STR_CNT_POS = 3;
    /**
     * JSON常量，用于统一引用固定值。
     */
    private static final int JSON_CNT_POS = 4;
    private static final int MAX_TS_POS = 5;
    /**
     * `LONG_POS`常量，用于统一引用固定值。
     */
    private static final int LONG_POS = 6;
    private static final int DOUBLE_POS = 7;
    /**
     * `BOOL_POS`常量，用于统一引用固定值。
     */
    private static final int BOOL_POS = 8;
    private static final int STR_POS = 9;
    /**
     * JSON常量，用于统一引用固定值。
     */
    private static final int JSON_POS = 10;


    /**
     * `aggregation` 字段，保存当前对象的对应属性。
     */
    private final Aggregation aggregation;
    private final String key;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private final long ts;
    private final Executor executor;

    /**
     * 功能：创建 `AggregatePartitionsFunction` 实例，并初始化必要字段。
     * 参数：
     * - `aggregation`：`aggregation` 参数。
     * - `key`：键。
     * - `ts`：时间戳。
     * - `executor`：`executor` 参数。
     * 返回：新创建的对象实例。
     */
    public AggregatePartitionsFunction(Aggregation aggregation, String key, long ts, Executor executor) {
        this.aggregation = aggregation;
        this.key = key;
        this.ts = ts;
        this.executor = executor;
    }

    /**
     * 功能：执行 `apply` 对应的处理。
     * 参数：
     * - `rsList`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Optional<TsKvEntryAggWrapper>> apply(@Nullable List<TbResultSet> rsList) {
        log.trace("[{}][{}][{}] Going to aggregate data", key, ts, aggregation);
        if (rsList == null || rsList.isEmpty()) {
            return Futures.immediateFuture(Optional.empty());
        }
        return Futures.transform(
                Futures.allAsList(
                        rsList.stream().map(rs -> rs.allRows(this.executor))
                                .collect(Collectors.toList())),
                rowsList -> {
                    try {
                        AggregationResult aggResult = new AggregationResult();
                        for (List<Row> rs : rowsList) {
                            for (Row row : rs) {
                                processResultSetRow(row, aggResult);
                            }
                        }
                        return processAggregationResult(aggResult);
                    } catch (Exception e) {
                        log.error("[{}][{}][{}] Failed to aggregate data", key, ts, aggregation, e);
                        return Optional.empty();
                    }
                }, this.executor);
    }

    /**
     * 功能：处理`Result Set Row`。
     * 参数：
     * - `row`：`row` 参数。
     * - `aggResult`：`aggResult` 参数。
     * 返回：无。
     */
    private void processResultSetRow(Row row, AggregationResult aggResult) {
        long curCount = 0L;

        Long curLValue = null;
        Double curDValue = null;
        Boolean curBValue = null;
        String curSValue = null;
        String curJValue = null;

        long longCount = row.getLong(LONG_CNT_POS);
        long doubleCount = row.getLong(DOUBLE_CNT_POS);
        long boolCount = row.getLong(BOOL_CNT_POS);
        long strCount = row.getLong(STR_CNT_POS);
        long jsonCount = row.getLong(JSON_CNT_POS);
        long aggValuesLastTs = row.getLong(MAX_TS_POS);

        if (longCount > 0 || doubleCount > 0) {
            if (longCount > 0) {
                aggResult.dataType = DataType.LONG;
                curCount += longCount;
                curLValue = getLongValue(row);
            }
            if (doubleCount > 0) {
                aggResult.hasDouble = true;
                aggResult.dataType = DataType.DOUBLE;
                curCount += doubleCount;
                curDValue = getDoubleValue(row);
            }
        } else if (boolCount > 0) {
            aggResult.dataType = DataType.BOOLEAN;
            curCount = boolCount;
            curBValue = getBooleanValue(row);
        } else if (strCount > 0) {
            aggResult.dataType = DataType.STRING;
            curCount = strCount;
            curSValue = getStringValue(row);
        } else if (jsonCount > 0) {
            aggResult.dataType = DataType.JSON;
            curCount = jsonCount;
            curJValue = getJsonValue(row);
        } else {
            return;
        }

        aggResult.aggValuesLastTs = Math.max(aggResult.aggValuesLastTs, aggValuesLastTs);

        if (aggregation == Aggregation.COUNT) {
            aggResult.count += curCount;
        } else if (aggregation == Aggregation.AVG || aggregation == Aggregation.SUM) {
            processAvgOrSumAggregation(aggResult, curCount, curLValue, curDValue);
        } else if (aggregation == Aggregation.MIN) {
            processMinAggregation(aggResult, curLValue, curDValue, curBValue, curSValue, curJValue);
        } else if (aggregation == Aggregation.MAX) {
            processMaxAggregation(aggResult, curLValue, curDValue, curBValue, curSValue, curJValue);
        }
    }

    /**
     * 功能：处理`Avg Or Sum Aggregation`。
     * 参数：
     * - `aggResult`：`aggResult` 参数。
     * - `curCount`：`curCount` 参数。
     * - `curLValue`：值。
     * - `curDValue`：值。
     * 返回：无。
     */
    private void processAvgOrSumAggregation(AggregationResult aggResult, long curCount, Long curLValue, Double curDValue) {
        aggResult.count += curCount;
        if (curDValue != null) {
            aggResult.dValue = aggResult.dValue == null ? curDValue : aggResult.dValue + curDValue;
        }
        if (curLValue != null) {
            aggResult.lValue = aggResult.lValue == null ? curLValue : aggResult.lValue + curLValue;
        }
    }

    /**
     * 功能：处理`Min Aggregation`。
     * 参数：
     * - `aggResult`：`aggResult` 参数。
     * - `curLValue`：值。
     * - `curDValue`：值。
     * - `curBValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void processMinAggregation(AggregationResult aggResult, Long curLValue, Double curDValue, Boolean curBValue, String curSValue, String curJValue) {
        if (curDValue != null || curLValue != null) {
            if (curDValue != null) {
                aggResult.dValue = aggResult.dValue == null ? curDValue : Math.min(aggResult.dValue, curDValue);
            }
            if (curLValue != null) {
                aggResult.lValue = aggResult.lValue == null ? curLValue : Math.min(aggResult.lValue, curLValue);
            }
        } else if (curBValue != null) {
            aggResult.bValue = aggResult.bValue == null ? curBValue : aggResult.bValue && curBValue;
        } else if (curSValue != null && (aggResult.sValue == null || curSValue.compareTo(aggResult.sValue) < 0)) {
            aggResult.sValue = curSValue;
        } else if (curJValue != null && (aggResult.jValue == null || curJValue.compareTo(aggResult.jValue) < 0)) {
            aggResult.jValue = curJValue;
        }
    }

    /**
     * 功能：处理`Max Aggregation`。
     * 参数：
     * - `aggResult`：`aggResult` 参数。
     * - `curLValue`：值。
     * - `curDValue`：值。
     * - `curBValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void processMaxAggregation(AggregationResult aggResult, Long curLValue, Double curDValue, Boolean curBValue, String curSValue, String curJValue) {
        if (curDValue != null || curLValue != null) {
            if (curDValue != null) {
                aggResult.dValue = aggResult.dValue == null ? curDValue : Math.max(aggResult.dValue, curDValue);
            }
            if (curLValue != null) {
                aggResult.lValue = aggResult.lValue == null ? curLValue : Math.max(aggResult.lValue, curLValue);
            }
        } else if (curBValue != null) {
            aggResult.bValue = aggResult.bValue == null ? curBValue : aggResult.bValue || curBValue;
        } else if (curSValue != null && (aggResult.sValue == null || curSValue.compareTo(aggResult.sValue) > 0)) {
            aggResult.sValue = curSValue;
        } else if (curJValue != null && (aggResult.jValue == null || curJValue.compareTo(aggResult.jValue) > 0)) {
            aggResult.jValue = curJValue;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：判断结果。
     */
    private Boolean getBooleanValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getBoolean(BOOL_POS);
        } else {
            return null; //NOSONAR, null is used for further comparison
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：文本结果。
     */
    private String getStringValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getString(STR_POS);
        } else {
            return null;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：文本结果。
     */
    private String getJsonValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            return row.getString(JSON_POS);
        } else {
            return null;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：数值结果。
     */
    private Long getLongValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX
                || aggregation == Aggregation.SUM || aggregation == Aggregation.AVG) {
            return row.getLong(LONG_POS);
        } else {
            return null;
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：数值结果。
     */
    private Double getDoubleValue(Row row) {
        if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX
                || aggregation == Aggregation.SUM || aggregation == Aggregation.AVG) {
            return row.getDouble(DOUBLE_POS);
        } else {
            return null;
        }
    }

    /**
     * 功能：处理`Aggregation Result`。
     * 参数：
     * - `aggResult`：`aggResult` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<TsKvEntryAggWrapper> processAggregationResult(AggregationResult aggResult) {
        Optional<TsKvEntry> result;
        if (aggResult.dataType == null) {
            result = Optional.empty();
        } else if (aggregation == Aggregation.COUNT) {
            result = Optional.of(new BasicTsKvEntry(ts, new LongDataEntry(key, aggResult.count)));
        } else if (aggregation == Aggregation.AVG || aggregation == Aggregation.SUM) {
            result = processAvgOrSumResult(aggregation, aggResult);
        } else if (aggregation == Aggregation.MIN || aggregation == Aggregation.MAX) {
            result = processMinOrMaxResult(aggResult);
        } else {
            result = Optional.empty();
        }
        if (result.isEmpty()) {
            log.trace("[{}][{}][{}] Aggregated data is empty.", key, ts, aggregation);
        }
        return result.map(tsKvEntry -> new TsKvEntryAggWrapper(tsKvEntry, aggResult.aggValuesLastTs));
    }

    /**
     * 功能：处理`Avg Or Sum Result`。
     * 参数：
     * - `aggregation`：`aggregation` 参数。
     * - `aggResult`：`aggResult` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<TsKvEntry> processAvgOrSumResult(Aggregation aggregation, AggregationResult aggResult) {
        if (aggResult.count == 0 || (aggResult.dataType == DataType.DOUBLE && aggResult.dValue == null) || (aggResult.dataType == DataType.LONG && aggResult.lValue == null)) {
            return Optional.empty();
        } else if (aggResult.dataType == DataType.DOUBLE || aggResult.dataType == DataType.LONG) {
            if (aggregation == Aggregation.AVG || aggResult.hasDouble) {
                double sum = Optional.ofNullable(aggResult.dValue).orElse(0.0d) + Optional.ofNullable(aggResult.lValue).orElse(0L);
                DoubleDataEntry doubleDataEntry = new DoubleDataEntry(key, aggregation == Aggregation.SUM ? sum : (sum / aggResult.count));
                TsKvEntry result = aggregation == Aggregation.AVG ? new AggTsKvEntry(ts, doubleDataEntry, aggResult.count) : new BasicTsKvEntry(ts, doubleDataEntry);
                return Optional.of(result);
            } else {
                LongDataEntry longDataEntry = new LongDataEntry(key, aggregation == Aggregation.SUM ? aggResult.lValue : (aggResult.lValue / aggResult.count));
                return Optional.of(new BasicTsKvEntry(ts, longDataEntry));
            }
        }
        return Optional.empty();
    }

    /**
     * 功能：处理`Min Or Max Result`。
     * 参数：
     * - `aggResult`：`aggResult` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<TsKvEntry> processMinOrMaxResult(AggregationResult aggResult) {
        if (aggResult.dataType == DataType.DOUBLE || aggResult.dataType == DataType.LONG) {
            if (aggResult.hasDouble) {
                double currentD = aggregation == Aggregation.MIN ? Optional.ofNullable(aggResult.dValue).orElse(Double.MAX_VALUE) : Optional.ofNullable(aggResult.dValue).orElse(Double.MIN_VALUE);
                double currentL = aggregation == Aggregation.MIN ? Optional.ofNullable(aggResult.lValue).orElse(Long.MAX_VALUE) : Optional.ofNullable(aggResult.lValue).orElse(Long.MIN_VALUE);
                return Optional.of(new BasicTsKvEntry(ts, new DoubleDataEntry(key, aggregation == Aggregation.MIN ? Math.min(currentD, currentL) : Math.max(currentD, currentL))));
            } else {
                return Optional.of(new BasicTsKvEntry(ts, new LongDataEntry(key, aggResult.lValue)));
            }
        } else if (aggResult.dataType == DataType.STRING) {
            return Optional.of(new BasicTsKvEntry(ts, new StringDataEntry(key, aggResult.sValue)));
        } else if (aggResult.dataType == DataType.JSON) {
            return Optional.of(new BasicTsKvEntry(ts, new JsonDataEntry(key, aggResult.jValue)));
        } else {
            return Optional.of(new BasicTsKvEntry(ts, new BooleanDataEntry(key, aggResult.bValue)));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`AggregationResult` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
     * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Repository / Strategy / Template。
     */
    private class AggregationResult {
        /**
         * 数据，用于区分不同处理分支。
         */
        DataType dataType = null;
        Boolean bValue = null;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        String sValue = null;
        String jValue = null;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        Double dValue = null;
        Long lValue = null;
        /**
         * 数量，用于控制数量、位置或分页范围。
         */
        long count = 0;
        boolean hasDouble = false;
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        long aggValuesLastTs = 0;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AggregatePartitionsFunction` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
