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
 * 1. `SqlAbstractDatabaseSchemaService` 是 ThingsBoard Application 中负责 `Sql Database Schema` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `DatabaseSchemaService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
