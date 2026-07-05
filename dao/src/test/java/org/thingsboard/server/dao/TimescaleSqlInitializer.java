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
 * 1. 类目的：`TimescaleSqlInitializer` 是 ThingsBoard DAO 测试模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 测试上下文和 DAO 测试套件按用例创建、初始化和销毁。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Slf4j
public class TimescaleSqlInitializer {

    private static final List<String> sqlFiles = List.of(
            "sql/schema-timescale.sql",
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
        log.info("initialize Timescale DB...");
        try {
            for (String sqlFile : sqlFiles) {
                URL sqlFileUrl = Resources.getResource(sqlFile);
                String sql = Resources.toString(sqlFileUrl, Charsets.UTF_8);
                conn.createStatement().execute(sql);
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to init the Timescale database. Reason: " + e.getMessage(), e);
        }
        log.info("Timescale DB is initialized!");
    }

    /**
     * 功能：删除或清理`Up Db`。
     * 参数：
     * - `conn`：`conn` 参数。
     * 返回：无。
     */
    private static void cleanUpDb(Connection conn) {
        log.info("clean up Timescale DB...");
        try {
            URL dropAllTableSqlFileUrl = Resources.getResource(dropAllTablesSqlFile);
            String dropAllTablesSql = Resources.toString(dropAllTableSqlFileUrl, Charsets.UTF_8);
            conn.createStatement().execute(dropAllTablesSql);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Unable to clean up the Timescale database. Reason: " + e.getMessage(), e);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TimescaleSqlInitializer` 在 ThingsBoard DAO 测试模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
