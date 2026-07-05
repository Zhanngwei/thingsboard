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

import com.datastax.oss.driver.api.core.cql.Row;
import lombok.Data;
import org.thingsboard.server.common.data.UUIDConverter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`CassandraToSqlColumn` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Data
public class CassandraToSqlColumn {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    /**
     * `EMPTY_STR`常量，用于统一引用固定值。
     */
    private static final String EMPTY_STR = "";

    /**
     * 索引，用于控制数量、位置或分页范围。
     */
    private int index;
    private int sqlIndex;
    /**
     * 名称，用于标识或展示当前对象。
     */
    private String cassandraColumnName;
    private String sqlColumnName;
    /**
     * 类型，用于区分不同处理分支。
     */
    private CassandraToSqlColumnType type;
    private int sqlType;
    /**
     * `size` 字段，保存当前对象的对应属性。
     */
    private int size;
    private Class<? extends Enum> enumClass;
    /**
     * 当前操作是否被允许。
     */
    private boolean allowNullBoolean = false;

    /**
     * 功能：执行 `idColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn idColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ID);
    }

    /**
     * 功能：执行 `stringColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn stringColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.STRING);
    }

    /**
     * 功能：执行 `stringColumn` 对应的处理。
     * 参数：
     * - `cassandraColumnName`：名称。
     * - `sqlColumnName`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn stringColumn(String cassandraColumnName, String sqlColumnName) {
        return new CassandraToSqlColumn(cassandraColumnName, sqlColumnName);
    }

    /**
     * 功能：执行 `bigintColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn bigintColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.BIGINT);
    }

    /**
     * 功能：执行 `doubleColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn doubleColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.DOUBLE);
    }

    /**
     * 功能：执行 `booleanColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn booleanColumn(String name) {
        return booleanColumn(name, false);
    }

    /**
     * 功能：执行 `booleanColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `allowNullBoolean`：`allowNullBoolean` 参数。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn booleanColumn(String name, boolean allowNullBoolean) {
        return new CassandraToSqlColumn(name, name, CassandraToSqlColumnType.BOOLEAN, null, allowNullBoolean);
    }

    /**
     * 功能：执行 `jsonColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn jsonColumn(String name) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.JSON);
    }

    /**
     * 功能：执行 `enumToIntColumn` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `enumClass`：`enumClass` 参数。
     * 返回：处理结果。
     */
    public static CassandraToSqlColumn enumToIntColumn(String name, Class<? extends Enum> enumClass) {
        return new CassandraToSqlColumn(name, CassandraToSqlColumnType.ENUM_TO_INT, enumClass);
    }

    /**
     * 功能：创建 `CassandraToSqlColumn` 实例，并初始化必要字段。
     * 参数：
     * - `columnName`：名称。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumn(String columnName) {
        this(columnName, columnName, CassandraToSqlColumnType.STRING, null, false);
    }

    /**
     * 功能：创建 `CassandraToSqlColumn` 实例，并初始化必要字段。
     * 参数：
     * - `columnName`：名称。
     * - `type`：类型。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type) {
        this(columnName, columnName, type, null, false);
    }

    /**
     * 功能：创建 `CassandraToSqlColumn` 实例，并初始化必要字段。
     * 参数：
     * - `columnName`：名称。
     * - `type`：类型。
     * - `enumClass`：`enumClass` 参数。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumn(String columnName, CassandraToSqlColumnType type, Class<? extends Enum> enumClass) {
        this(columnName, columnName, type, enumClass, false);
    }

    /**
     * 功能：创建 `CassandraToSqlColumn` 实例，并初始化必要字段。
     * 参数：
     * - `cassandraColumnName`：名称。
     * - `sqlColumnName`：名称。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName) {
        this(cassandraColumnName, sqlColumnName, CassandraToSqlColumnType.STRING, null, false);
    }

    /**
     * 功能：创建 `CassandraToSqlColumn` 实例，并初始化必要字段。
     * 参数：
     * - `cassandraColumnName`：名称。
     * - `sqlColumnName`：名称。
     * - `type`：类型。
     * - `enumClass`：`enumClass` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public CassandraToSqlColumn(String cassandraColumnName, String sqlColumnName, CassandraToSqlColumnType type,
                                Class<? extends Enum> enumClass, boolean allowNullBoolean) {
        this.cassandraColumnName = cassandraColumnName;
        this.sqlColumnName = sqlColumnName;
        this.type = type;
        this.enumClass = enumClass;
        this.allowNullBoolean = allowNullBoolean;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `row`：`row` 参数。
     * 返回：文本结果。
     */
    public String getColumnValue(Row row) {
        if (row.isNull(index)) {
            if (this.type == CassandraToSqlColumnType.BOOLEAN && !this.allowNullBoolean) {
                return Boolean.toString(false);
            } else {
                return null;
            }
        } else {
            switch (this.type) {
                case ID:
                    return UUIDConverter.fromTimeUUID(row.getUuid(index));
                case DOUBLE:
                    return Double.toString(row.getDouble(index));
                case INTEGER:
                    return Integer.toString(row.getInt(index));
                case FLOAT:
                    return Float.toString(row.getFloat(index));
                case BIGINT:
                    return Long.toString(row.getLong(index));
                case BOOLEAN:
                    return Boolean.toString(row.getBoolean(index));
                case STRING:
                case JSON:
                case ENUM_TO_INT:
                default:
                    String value = row.getString(index);
                    return this.replaceNullChars(value);
            }
        }
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `sqlInsertStatement`：`sqlInsertStatement` 参数。
     * - `value`：值。
     * 返回：无。
     */
    public void setColumnValue(PreparedStatement sqlInsertStatement, String value) throws SQLException {
        if (value == null) {
            sqlInsertStatement.setNull(this.sqlIndex, this.sqlType);
        } else {
            switch (this.type) {
                case DOUBLE:
                    sqlInsertStatement.setDouble(this.sqlIndex, Double.parseDouble(value));
                    break;
                case INTEGER:
                    sqlInsertStatement.setInt(this.sqlIndex, Integer.parseInt(value));
                    break;
                case FLOAT:
                    sqlInsertStatement.setFloat(this.sqlIndex, Float.parseFloat(value));
                    break;
                case BIGINT:
                    sqlInsertStatement.setLong(this.sqlIndex, Long.parseLong(value));
                    break;
                case BOOLEAN:
                    sqlInsertStatement.setBoolean(this.sqlIndex, Boolean.parseBoolean(value));
                    break;
                case ENUM_TO_INT:
                    @SuppressWarnings("unchecked")
                    Enum<?> enumVal = Enum.valueOf(this.enumClass, value);
                    int intValue = enumVal.ordinal();
                    sqlInsertStatement.setInt(this.sqlIndex, intValue);
                    break;
                case JSON:
                case STRING:
                case ID:
                default:
                    sqlInsertStatement.setString(this.sqlIndex, value);
                    break;
            }
        }
    }

    /**
     * 功能：执行 `replaceNullChars` 对应的处理。
     * 参数：
     * - `strValue`：值。
     * 返回：文本结果。
     */
    private String replaceNullChars(String strValue) {
        if (strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }

}


/*
 * 本类总结：
 * 1. 核心职责：`CassandraToSqlColumn` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
