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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

/**
 * 中文说明：
 * 1. `AbstractSqlTsDatabaseUpgradeService` 是 ThingsBoard Application 中负责 `Sql Ts Database` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class AbstractSqlTsDatabaseUpgradeService {

    /**
     * `CALL_REGEX`常量，用于统一引用固定值。
     */
    protected static final String CALL_REGEX = "call ";
    protected static final String DROP_TABLE = "DROP TABLE ";
    /**
     * `DROP_PROCEDURE_IF_EXISTS`常量，用于统一引用固定值。
     */
    protected static final String DROP_PROCEDURE_IF_EXISTS = "DROP PROCEDURE IF EXISTS ";
    protected static final String TS_KV_SQL = "ts_kv.sql";
    /**
     * 目录常量，用于统一引用固定值。
     */
    protected static final String PATH_TO_USERS_PUBLIC_FOLDER = "C:\\Users\\Public";
    protected static final String THINGSBOARD_WINDOWS_UPGRADE_DIR = "THINGSBOARD_WINDOWS_UPGRADE_DIR";

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
     * 功能：获取SQL。
     * 参数：
     * - `conn`：`conn` 参数。
     * - `fileName`：名称。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    protected abstract void loadSql(Connection conn, String fileName, String version);

    /**
     * 功能：获取`Functions`。
     * 参数：
     * - `sqlFile`：`sqlFile` 参数。
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    protected void loadFunctions(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
        conn.createStatement().execute(sql); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
    }

    /**
     * 功能：校验版本号。
     * 参数：
     * - `conn`：`conn` 参数。
     * 返回：判断结果。
     */
    protected boolean checkVersion(Connection conn) {
        boolean versionValid = false;
        try {
            Statement statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT current_setting('server_version_num')");
            resultSet.next();
            if(resultSet.getLong(1) > 110000) {
                versionValid = true;
            }
            statement.close();
        } catch (Exception e) {
            log.info("Failed to check current PostgreSQL version due to: {}", e.getMessage());
        }
        return versionValid;
    }

    /**
     * 功能：判断`Old Schema`。
     * 参数：
     * - `conn`：`conn` 参数。
     * - `fromVersion`：`fromVersion` 参数。
     * 返回：判断结果。
     */
    protected boolean isOldSchema(Connection conn, long fromVersion) {
        boolean isOldSchema = true;
        try {
            Statement statement = conn.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS tb_schema_settings ( schema_version bigint NOT NULL, CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version));");
            Thread.sleep(1000);
            ResultSet resultSet = statement.executeQuery("SELECT schema_version FROM tb_schema_settings;");
            if (resultSet.next()) {
                isOldSchema = resultSet.getLong(1) <= fromVersion;
            } else {
                resultSet.close();
                statement.execute("INSERT INTO tb_schema_settings (schema_version) VALUES (" + fromVersion + ")");
            }
            statement.close();
        } catch (InterruptedException | SQLException e) {
            log.info("Failed to check current PostgreSQL schema due to: {}", e.getMessage());
        }
        return isOldSchema;
    }

    /**
     * 功能：执行查询条件。
     * 参数：
     * - `conn`：`conn` 参数。
     * - `query`：`query` 参数。
     * 返回：无。
     */
    protected void executeQuery(Connection conn, String query) {
        try {
            Statement statement = conn.createStatement();
            statement.execute(query); //NOSONAR, ignoring because method used to execute thingsboard database upgrade script
            SQLWarning warnings = statement.getWarnings();
            if (warnings != null) {
                log.info("{}", warnings.getMessage());
                SQLWarning nextWarning = warnings.getNextWarning();
                while (nextWarning != null) {
                    log.info("{}", nextWarning.getMessage());
                    nextWarning = nextWarning.getNextWarning();
                }
            }
            Thread.sleep(2000);
            log.info("Successfully executed query: {}", query);
        } catch (InterruptedException | SQLException e) {
            log.error("Failed to execute query: {} due to: {}", query, e.getMessage());
            throw new RuntimeException("Failed to execute query:" + query + " due to: ", e);
        }
    }
}