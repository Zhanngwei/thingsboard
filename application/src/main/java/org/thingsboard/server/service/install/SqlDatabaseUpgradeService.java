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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.thingsboard.server.service.install.update.DefaultDataUpdateService;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. 类目的：`SqlDatabaseUpgradeService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Profile("install")
@Slf4j
public class SqlDatabaseUpgradeService implements DatabaseEntitiesUpgradeService {

    /**
     * SQL常量，用于统一引用固定值。
     */
    private static final String SCHEMA_UPDATE_SQL = "schema_update.sql";

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Value("${spring.datasource.url}")
    private String dbUrl;

    /**
     * 用户，用于标识或展示当前对象。
     */
    @Value("${spring.datasource.username}")
    private String dbUserName;

    /**
     * 密码，用于认证或安全校验。
     */
    @Value("${spring.datasource.password}")
    private String dbPassword;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * 功能：执行 `upgradeDatabase` 对应的处理。
     * 参数：
     * - `fromVersion`：`fromVersion` 参数。
     * 返回：无。
     */
    @Override
    public void upgradeDatabase(String fromVersion) throws Exception {
        switch (fromVersion) {
            case "3.5.0":
                updateSchema("3.5.0", 3005000, "3.5.1", 3005001, null);
                break;
            case "3.5.1":
                updateSchema("3.5.1", 3005001, "3.6.0", 3006000, conn -> {
                    String[] entityNames = new String[]{"device", "component_descriptor", "customer", "dashboard", "rule_chain", "rule_node", "ota_package",
                            "asset_profile", "asset", "device_profile", "tb_user", "tenant_profile", "tenant", "widgets_bundle", "entity_view", "edge"};
                    for (String entityName : entityNames) {
                        try {
                            conn.createStatement().execute("ALTER TABLE " + entityName + " DROP COLUMN search_text CASCADE");
                        } catch (Exception e) {
                        }
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE component_descriptor ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("ALTER TABLE rule_node ADD COLUMN IF NOT EXISTS configuration_version int DEFAULT 0;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_rule_node_type_configuration_version ON rule_node(type, configuration_version);");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("UPDATE rule_node SET " +
                                "configuration = (configuration::jsonb || '{\"updateAttributesOnlyOnValueChange\": \"false\"}'::jsonb)::varchar, " +
                                "configuration_version = 1 " +
                                "WHERE type = 'org.thingsboard.rule.engine.telemetry.TbMsgAttributesNode' AND configuration_version < 1;");
                    } catch (Exception e) {
                    }
                    try {
                        conn.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_notification_recipient_id_unread ON notification(recipient_id) WHERE status <> 'READ';");
                    } catch (Exception e) {
                    }
                });
                break;
            case "3.6.0":
                updateSchema("3.6.0", 3006000, "3.6.1", 3006001, null);
                break;
            case "3.6.1":
                updateSchema("3.6.1", 3006001, "3.6.2", 3006002, connection -> {
                    try {
                        Path saveAttributesNodeUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", "3.6.1", "save_attributes_node_update.sql");
                        loadSql(saveAttributesNodeUpdateFile, connection);
                    } catch (Exception e) {
                        log.warn("Failed to execute update script for save attributes rule nodes due to: ", e);
                    }
                    try {
                        connection.createStatement().execute("CREATE INDEX IF NOT EXISTS idx_asset_profile_id ON asset(tenant_id, asset_profile_id);");
                    } catch (Exception e) {
                    }
                });
                break;
            case "3.6.2":
                updateSchema("3.6.2", 3006002, "3.6.3", 3006003, null);
                break;
            case "3.6.3":
                updateSchema("3.6.3", 3006003, "3.6.4", 3006004, null);
                break;
            default:
                throw new RuntimeException("Unable to upgrade SQL database, unsupported fromVersion: " + fromVersion);
        }
    }

    /**
     * 功能：更新`Schema`。
     * 参数：
     * - `oldVersionStr`：`oldVersionStr` 参数。
     * - `oldVersion`：`oldVersion` 参数。
     * - `newVersionStr`：`newVersionStr` 参数。
     * - `newVersion`：`newVersion` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void updateSchema(String oldVersionStr, int oldVersion, String newVersionStr, int newVersion, Consumer<Connection> additionalAction) {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            log.info("Updating schema ...");
            if (isOldSchema(conn, oldVersion)) {
                Path schemaUpdateFile = Paths.get(installScripts.getDataDir(), "upgrade", oldVersionStr, SCHEMA_UPDATE_SQL);
                loadSql(schemaUpdateFile, conn);
                if (additionalAction != null) {
                    additionalAction.accept(conn);
                }
                conn.createStatement().execute("UPDATE tb_schema_settings SET schema_version = " + newVersion + ";");
                log.info("Schema updated to version {}", newVersionStr);
            } else {
                log.info("Skip schema re-update to version {}. Use env flag 'SKIP_SCHEMA_VERSION_CHECK' to force the re-update.", newVersionStr);
            }
        } catch (Exception e) {
            log.error("Failed updating schema!!!", e);
        }
    }

    /**
     * 功能：获取SQL。
     * 参数：
     * - `sqlFile`：`sqlFile` 参数。
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    private void loadSql(Path sqlFile, Connection conn) throws Exception {
        String sql = new String(Files.readAllBytes(sqlFile), Charset.forName("UTF-8"));
        Statement st = conn.createStatement();
        st.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(3));
        st.execute(sql);//NOSONAR, ignoring because method used to execute thingsboard database upgrade script
        printWarnings(st);
        Thread.sleep(5000);
    }

    /**
     * 功能：执行 `printWarnings` 对应的处理。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：无。
     */
    protected void printWarnings(Statement statement) throws SQLException {
        SQLWarning warnings = statement.getWarnings();
        if (warnings != null) {
            log.info("{}", warnings.getMessage());
            SQLWarning nextWarning = warnings.getNextWarning();
            while (nextWarning != null) {
                log.info("{}", nextWarning.getMessage());
                nextWarning = nextWarning.getNextWarning();
            }
        }
    }

    /**
     * 功能：判断`Old Schema`。
     * 参数：
     * - `conn`：`conn` 参数。
     * - `fromVersion`：`fromVersion` 参数。
     * 返回：判断结果。
     */
    protected boolean isOldSchema(Connection conn, long fromVersion) {
        if (DefaultDataUpdateService.getEnv("SKIP_SCHEMA_VERSION_CHECK", false)) {
            log.info("Skipped DB schema version check due to SKIP_SCHEMA_VERSION_CHECK set to true!");
            return true;
        }
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
}

/*
 * 本类总结：
 * 1. 核心职责：`SqlDatabaseUpgradeService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
