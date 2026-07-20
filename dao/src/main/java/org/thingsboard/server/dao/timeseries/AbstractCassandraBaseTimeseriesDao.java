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
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.nosql.CassandraAbstractAsyncDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `AbstractCassandraBaseTimeseriesDao` 是 ThingsBoard DAO 中负责时序数据存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `CassandraAbstractAsyncDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
public abstract class AbstractCassandraBaseTimeseriesDao extends CassandraAbstractAsyncDao {
    /**
     * `DESC_ORDER`常量，用于统一引用固定值。
     */
    public static final String DESC_ORDER = "DESC";
    public static final String GENERATED_QUERY_FOR_ENTITY_TYPE_AND_ENTITY_ID = "Generated query [{}] for entityType {} and entityId {}";
    /**
     * `INSERT_INTO`常量，用于统一引用固定值。
     */
    public static final String INSERT_INTO = "INSERT INTO ";
    public static final String SELECT_PREFIX = "SELECT ";
    /**
     * `EQUALS_PARAM`常量，用于统一引用固定值。
     */
    public static final String EQUALS_PARAM = " = ? ";

    /**
     * 功能：执行 `toKvEntry` 对应的处理。
     * 参数：
     * - `row`：`row` 参数。
     * - `key`：键。
     * 返回：处理结果。
     */
    public static KvEntry toKvEntry(Row row, String key) {
        KvEntry kvEntry = null;
        String strV = row.get(ModelConstants.STRING_VALUE_COLUMN, String.class);
        if (strV != null) {
            kvEntry = new StringDataEntry(key, strV);
        } else {
            Long longV = row.get(ModelConstants.LONG_VALUE_COLUMN, Long.class);
            if (longV != null) {
                kvEntry = new LongDataEntry(key, longV);
            } else {
                Double doubleV = row.get(ModelConstants.DOUBLE_VALUE_COLUMN, Double.class);
                if (doubleV != null) {
                    kvEntry = new DoubleDataEntry(key, doubleV);
                } else {
                    Boolean boolV = row.get(ModelConstants.BOOLEAN_VALUE_COLUMN, Boolean.class);
                    if (boolV != null) {
                        kvEntry = new BooleanDataEntry(key, boolV);
                    } else {
                        String jsonV = row.get(ModelConstants.JSON_VALUE_COLUMN, String.class);
                        if (StringUtils.isNoneEmpty(jsonV)) {
                            kvEntry = new JsonDataEntry(key, jsonV);
                        } else {
                            log.warn("All values in key-value row are nullable ");
                        }
                    }
                }
            }
        }
        return kvEntry;
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `rows`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected List<TsKvEntry> convertResultToTsKvEntryList(List<Row> rows) {
        List<TsKvEntry> entries = new ArrayList<>(rows.size());
        if (!rows.isEmpty()) {
            rows.forEach(row -> entries.add(convertResultToTsKvEntry(row)));
        }
        return entries;
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：处理结果。
     */
    private TsKvEntry convertResultToTsKvEntry(Row row) {
        String key = row.getString(ModelConstants.KEY_COLUMN);
        long ts = row.getLong(ModelConstants.TS_COLUMN);
        return new BasicTsKvEntry(ts, toKvEntry(row, key));
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `key`：键。
     * - `row`：`row` 参数。
     * 返回：处理结果。
     */
    protected TsKvEntry convertResultToTsKvEntry(String key, Row row) {
        if (row != null) {
            return getBasicTsKvEntry(key, row);
        } else {
            return new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(key, null));
        }
    }

    /**
     * 功能：转换时间戳。
     * 参数：
     * - `key`：键。
     * - `row`：`row` 参数。
     * 返回：可能存在的结果。
     */
    protected Optional<TsKvEntry> convertResultToTsKvEntryOpt(String key, Row row) {
        if (row != null) {
            return Optional.of(getBasicTsKvEntry(key, row));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `key`：键。
     * - `row`：`row` 参数。
     * 返回：处理结果。
     */
    private BasicTsKvEntry getBasicTsKvEntry(String key, Row row) {
        Optional<String> foundKeyOpt = getKey(row);
        long ts = row.getLong(ModelConstants.TS_COLUMN);
        return new BasicTsKvEntry(ts, toKvEntry(row, foundKeyOpt.orElse(key)));
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<String> getKey(Row row){
       try{
           return Optional.ofNullable(row.getString(ModelConstants.KEY_COLUMN));
       } catch (IllegalArgumentException e){
           return Optional.empty();
       }
    }

}
