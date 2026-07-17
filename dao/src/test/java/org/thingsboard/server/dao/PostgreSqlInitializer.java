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
package org.thingsboard.server.dao;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 中文说明：
 * 1. `PostgreSqlInitializer` 是 ThingsBoard DAO 中负责 `Postgre Sql Initializer` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
public class PostgreSqlInitializer {

    private static final List<String> sqlFiles = List.of(
            "sql/schema-ts-psql.sql",
            "sql/schema-entities.sql",
            "sql/schema-entities-idx.sql",
            "sql/schema-entities-idx-psql-addon.sql",
            "sql/schema-views-and-functions.sql",
            "sql/system-data.sql",
            "sql/system-test-psql.sql");
    /**
     * 文件常量，用于统一引用固定值。
     */
    private static final String dropAllTablesSqlFile = "sql/psql/drop-all-tables.sql";

    /**
     * 功能：初始化或启动`Db`。
     * 参数：
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    public static void initDb(Connection conn) {
        cleanUpDb(conn);
        log.info("initialize Postgres DB...");
        try {
            for (String sqlFile : sqlFiles) {
                URL sqlFileUrl = Resources.getResource(sqlFile);
                String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to init the Postgres database. Reason: " + e.getMessage(), e);
        }
        log.info("Postgres DB is initialized!");
    }

    /**
     * 功能：删除或清理`Up Db`。
     * 参数：
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    private static void cleanUpDb(Connection conn) {
        log.info("clean up Postgres DB...");
        try {
            URL dropAllTableSqlFileUrl = Resources.getResource(dropAllTablesSqlFile);
            String dropAllTablesSql = Resources.toString(dropAllTableSqlFileUrl, Charsets.UTF_8);
            conn.createStatement().execute(dropAllTablesSql);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to clean up the Postgres database. Reason: " + e.getMessage(), e);
        }
    }
}
