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

import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.postgresql.util.PSQLException;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Data
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`CassandraToSqlTable` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class CassandraToSqlTable {

    /**
     * 字段说明：
     * 1. 保存 `DEFAULT_BATCH_SIZE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int DEFAULT_BATCH_SIZE = 10000;

    /**
     * 字段说明：
     * 1. 保存 `cassandraCf` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String cassandraCf;
    private String sqlTableName;

    /**
     * 字段说明：
     * 1. 保存 `columns` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private List<CassandraToSqlColumn> columns;

    /**
     * 字段说明：
     * 1. 保存 `batchSize` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int batchSize = DEFAULT_BATCH_SIZE;

    /**
     * 字段说明：
     * 1. 保存 `sqlInsertStatement` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private PreparedStatement sqlInsertStatement;

    /**
     * 方法说明：
     * 1. 职责：执行 `CassandraToSqlTable` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public CassandraToSqlTable(String tableName, CassandraToSqlColumn... columns) {
        this(tableName, tableName, DEFAULT_BATCH_SIZE, columns);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `CassandraToSqlTable` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public CassandraToSqlTable(String tableName, String sqlTableName, CassandraToSqlColumn... columns) {
        this(tableName, sqlTableName, DEFAULT_BATCH_SIZE, columns);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `CassandraToSqlTable` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public CassandraToSqlTable(String tableName, int batchSize, CassandraToSqlColumn... columns) {
        this(tableName, tableName, batchSize, columns);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `CassandraToSqlTable` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public CassandraToSqlTable(String cassandraCf, String sqlTableName, int batchSize, CassandraToSqlColumn... columns) {
        this.cassandraCf = cassandraCf;
        this.sqlTableName = sqlTableName;
        this.batchSize = batchSize;
        this.columns = Arrays.asList(columns);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i=0;i<columns.length;i++) {
            this.columns.get(i).setIndex(i);
            this.columns.get(i).setSqlIndex(i+1);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `migrateToSql` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void migrateToSql(GuavaSession session, Connection conn) throws SQLException {
        log.info("[{}] Migrating data from cassandra '{}' Column Family to '{}' SQL table...", this.sqlTableName, this.cassandraCf, this.sqlTableName);
        DatabaseMetaData metadata = conn.getMetaData();
        java.sql.ResultSet resultSet = metadata.getColumns(null, null, this.sqlTableName, null);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (resultSet.next()) {
            String name = resultSet.getString("COLUMN_NAME");
            int sqlType = resultSet.getInt("DATA_TYPE");
            int size = resultSet.getInt("COLUMN_SIZE");
            CassandraToSqlColumn column = this.getColumn(name);
            column.setSize(size);
            column.setSqlType(sqlType);
        }
        this.sqlInsertStatement = createSqlInsertStatement(conn);
        Statement cassandraSelectStatement = createCassandraSelectStatement();
        cassandraSelectStatement.setPageSize(100);
        ResultSet rs = session.execute(cassandraSelectStatement);
        Iterator<Row> iter = rs.iterator();
        int rowCounter = 0;
        List<CassandraToSqlColumnData[]> batchData;
        boolean hasNext;
        do {
            batchData = this.extractBatchData(iter);
            hasNext = batchData.size() == this.batchSize;
            this.batchInsert(batchData, conn);
            rowCounter += batchData.size();
            log.info("[{}] {} records migrated so far...", this.sqlTableName, rowCounter);
        } while (hasNext);
        this.sqlInsertStatement.close();
        log.info("[{}] {} total records migrated.", this.sqlTableName, rowCounter);
        log.info("[{}] Finished migration data from cassandra '{}' Column Family to '{}' SQL table.",
                this.sqlTableName, this.cassandraCf, this.sqlTableName);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractBatchData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<CassandraToSqlColumnData[]> extractBatchData(Iterator<Row> iter) {
        List<CassandraToSqlColumnData[]> batchData = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        while (iter.hasNext() && batchData.size() < this.batchSize) {
            Row row = iter.next();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (row != null) {
                CassandraToSqlColumnData[] data = this.extractRowData(row);
                batchData.add(data);
            }
        }
        return batchData;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractRowData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private CassandraToSqlColumnData[] extractRowData(Row row) {
        CassandraToSqlColumnData[] data = new CassandraToSqlColumnData[this.columns.size()];
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (CassandraToSqlColumn column: this.columns) {
            String value = column.getColumnValue(row);
            data[column.getIndex()] = new CassandraToSqlColumnData(value);
        }
        return this.validateColumnData(data);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validateColumnData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected CassandraToSqlColumnData[] validateColumnData(CassandraToSqlColumnData[] data) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i=0;i<data.length;i++) {
            CassandraToSqlColumn column = this.columns.get(i);
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (column.getType() == CassandraToSqlColumnType.STRING) {
                CassandraToSqlColumnData columnData = data[i];
                String value = columnData.getValue();
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (value != null && value.length() > column.getSize()) {
                    log.warn("[{}] Value size [{}] exceeds maximum size [{}] of column [{}] and will be truncated!",
                            this.sqlTableName,
                            value.length(), column.getSize(), column.getSqlColumnName());
                    log.warn("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
                    value = value.substring(0, column.getSize());
                    columnData.setOriginalValue(value);
                    columnData.setValue(value);
                }
            }
        }
        return data;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `batchInsert` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void batchInsert(List<CassandraToSqlColumnData[]> batchData, Connection conn) throws SQLException {
        boolean retry = false;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (CassandraToSqlColumnData[] data : batchData) {
            // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
            for (CassandraToSqlColumn column: this.columns) {
                column.setColumnValue(this.sqlInsertStatement, data[column.getIndex()].getValue());
            }
            try {
                this.sqlInsertStatement.executeUpdate();
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (SQLException e) {
                // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                if (this.handleInsertException(batchData, data, conn, e)) {
                    retry = true;
                    break;
                } else {
                    throw e;
                }
            }
        }
        if (retry) {
            this.batchInsert(batchData, conn);
        } else {
            conn.commit();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleInsertException` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private boolean handleInsertException(List<CassandraToSqlColumnData[]> batchData,
                                          CassandraToSqlColumnData[] data,
                                          Connection conn, SQLException ex) throws SQLException {
        conn.commit();
        String constraint = extractConstraintName(ex).orElse(null);
        if (constraint != null) {
            if (this.onConstraintViolation(batchData, data, constraint)) {
                return true;
            } else {
                log.error("[{}] Unhandled constraint violation [{}] during insert!", this.sqlTableName, constraint);
                log.error("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
            }
        } else {
            log.error("[{}] Unhandled exception during insert!", this.sqlTableName);
            log.error("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
        }
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `dataToString` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String dataToString(CassandraToSqlColumnData[] data) {
        StringBuffer stringData = new StringBuffer("{\n");
        for (int i=0;i<data.length;i++) {
            String columnName = this.columns.get(i).getSqlColumnName();
            String value = data[i].getLogValue();
            stringData.append("\"").append(columnName).append("\": ").append("[").append(value).append("]\n");
        }
        stringData.append("}");
        return stringData.toString();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `onConstraintViolation` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected boolean onConstraintViolation(List<CassandraToSqlColumnData[]> batchData,
                                            CassandraToSqlColumnData[] data, String constraint) {
        return false;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleUniqueNameViolation` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void handleUniqueNameViolation(CassandraToSqlColumnData[] data, String entityType) {
        CassandraToSqlColumn nameColumn = this.getColumn("name");
        CassandraToSqlColumn searchTextColumn = this.getColumn("search_text");
        CassandraToSqlColumnData nameColumnData = data[nameColumn.getIndex()];
        CassandraToSqlColumnData searchTextColumnData = data[searchTextColumn.getIndex()];
        String prevName = nameColumnData.getValue();
        String newName = nameColumnData.getNextConstraintStringValue(nameColumn);
        nameColumnData.setValue(newName);
        searchTextColumnData.setValue(searchTextColumnData.getNextConstraintStringValue(searchTextColumn));
        String id = UUIDConverter.fromString(this.getColumnData(data, "id").getValue()).toString();
        log.warn("Found {} with duplicate name [id:[{}]]. Attempting to rename {} from '{}' to '{}'...", entityType, id, entityType, prevName, newName);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `handleUniqueEmailViolation` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void handleUniqueEmailViolation(CassandraToSqlColumnData[] data) {
        CassandraToSqlColumn emailColumn = this.getColumn("email");
        CassandraToSqlColumn searchTextColumn = this.getColumn("search_text");
        CassandraToSqlColumnData emailColumnData = data[emailColumn.getIndex()];
        CassandraToSqlColumnData searchTextColumnData = data[searchTextColumn.getIndex()];
        String prevEmail = emailColumnData.getValue();
        String newEmail = emailColumnData.getNextConstraintEmailValue(emailColumn);
        emailColumnData.setValue(newEmail);
        searchTextColumnData.setValue(searchTextColumnData.getNextConstraintEmailValue(searchTextColumn));
        String id = UUIDConverter.fromString(this.getColumnData(data, "id").getValue()).toString();
        log.warn("Found user with duplicate email [id:[{}]]. Attempting to rename email from '{}' to '{}'...", id, prevEmail, newEmail);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `ignoreRecord` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected void ignoreRecord(List<CassandraToSqlColumnData[]> batchData, CassandraToSqlColumnData[] data) {
        log.warn("[{}] Affected data:\n{}", this.sqlTableName, this.dataToString(data));
        int index = batchData.indexOf(data);
        if (index > 0) {
            batchData.remove(index);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getColumn` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected CassandraToSqlColumn getColumn(String sqlColumnName) {
        return this.columns.stream().filter(col -> col.getSqlColumnName().equals(sqlColumnName)).findFirst().get();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getColumnData` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected CassandraToSqlColumnData getColumnData(CassandraToSqlColumnData[] data, String sqlColumnName) {
        CassandraToSqlColumn column = this.getColumn(sqlColumnName);
        return data[column.getIndex()];
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `extractConstraintName` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Optional<String> extractConstraintName(SQLException ex) {
        final String sqlState = JdbcExceptionHelper.extractSqlState( ex );
        if (sqlState != null) {
            String sqlStateClassCode = JdbcExceptionHelper.determineSqlStateClassCode( sqlState );
            if ( sqlStateClassCode != null ) {
                if (Arrays.asList(
                        "23",	// "integrity constraint violation"
                        "27",	// "triggered data change violation"
                        "44"	// "with check option violation"
                ).contains(sqlStateClassCode)) {
                    if (ex instanceof PSQLException) {
                        return Optional.of(((PSQLException)ex).getServerErrorMessage().getConstraint());
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createCassandraSelectStatement` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Statement createCassandraSelectStatement() {
        StringBuilder selectStatementBuilder = new StringBuilder();
        selectStatementBuilder.append("SELECT ");
        for (CassandraToSqlColumn column : columns) {
            selectStatementBuilder.append(column.getCassandraColumnName()).append(",");
        }
        selectStatementBuilder.deleteCharAt(selectStatementBuilder.length() - 1);
        selectStatementBuilder.append(" FROM ").append(cassandraCf);
        return SimpleStatement.newInstance(selectStatementBuilder.toString());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createSqlInsertStatement` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private PreparedStatement createSqlInsertStatement(Connection conn) throws SQLException {
        StringBuilder insertStatementBuilder = new StringBuilder();
        insertStatementBuilder.append("INSERT INTO ").append(this.sqlTableName).append(" (");
        for (CassandraToSqlColumn column : columns) {
            insertStatementBuilder.append(column.getSqlColumnName()).append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(") VALUES (");
        for (CassandraToSqlColumn column : columns) {
            if (column.getType() == CassandraToSqlColumnType.JSON) {
                insertStatementBuilder.append("cast(? AS json)");
            } else {
                insertStatementBuilder.append("?");
            }
            insertStatementBuilder.append(",");
        }
        insertStatementBuilder.deleteCharAt(insertStatementBuilder.length() - 1);
        insertStatementBuilder.append(")");
        return conn.prepareStatement(insertStatementBuilder.toString());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraToSqlTable` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
