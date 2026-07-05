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
package org.thingsboard.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 中文说明：
 * 1. 类目的：`SqlAbstractDatabaseSchemaService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public abstract class SqlAbstractDatabaseSchemaService implements DatabaseSchemaService {

    /**
     * SQL常量，用于统一引用固定值。
     */
    protected static final String SQL_DIR = "sql";

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Value("${spring.datasource.url}")
    protected String dbUrl;

    /**
     * 用户，用于标识或展示当前对象。
     */
    @Value("${spring.datasource.username}")
    protected String dbUserName;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${spring.datasource.password}")
    protected String dbPassword;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    protected InstallScripts installScripts;

    /**
     * SQL，表示当前对象的对应属性。
     */
    private final String schemaSql;
    private final String schemaIdxSql;

    /**
     * 功能：创建 `SqlAbstractDatabaseSchemaService` 实例，并初始化必要字段。
     * 参数：
     * - `schemaSql`：`schemaSql` 参数。
     * - `schemaIdxSql`：`schemaIdxSql` 参数。
     * 返回：新创建的对象实例。
     */
    protected SqlAbstractDatabaseSchemaService(String schemaSql, String schemaIdxSql) {
        this.schemaSql = schemaSql;
        this.schemaIdxSql = schemaIdxSql;
    }

    /**
     * 功能：保存或创建`Database Schema`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void createDatabaseSchema() throws Exception {
        this.createDatabaseSchema(true);
    }

    /**
     * 功能：保存或创建`Database Schema`。
     * 参数：
     * - `createIndexes`：`createIndexes` 参数。
     * 返回：无。
     */
    @Override
    public void createDatabaseSchema(boolean createIndexes) throws Exception {
        log.info("Installing SQL DataBase schema part: " + schemaSql);
        executeQueryFromFile(schemaSql);

        if (createIndexes) {
            this.createDatabaseIndexes();
        }
    }

    /**
     * 功能：保存或创建`Database Indexes`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void createDatabaseIndexes() throws Exception {
        if (schemaIdxSql != null) {
            log.info("Installing SQL DataBase schema indexes part: " + schemaIdxSql);
            executeQueryFromFile(schemaIdxSql);
        }
    }

    /**
     * 功能：执行文件。
     * 参数：
     * - `schemaIdxSql`：`schemaIdxSql` 参数。
     * 返回：无。
     */
    void executeQueryFromFile(String schemaIdxSql) throws SQLException, IOException {
        Path schemaIdxFile = Paths.get(installScripts.getDataDir(), SQL_DIR, schemaIdxSql);
        String sql = Files.readString(schemaIdxFile);
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to load initial thingsboard database schema
        }
    }

    /**
     * 功能：执行查询条件。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    protected void executeQuery(String query) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            conn.createStatement().execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            log.info("Successfully executed query: {}", query);
            Thread.sleep(5000);
        } catch (InterruptedException | SQLException e) {
            log.error("Failed to execute query: {} due to: {}", query, e.getMessage());
            throw new RuntimeException("Failed to execute query: " + query, e);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SqlAbstractDatabaseSchemaService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
