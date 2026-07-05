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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.dao.cassandra.CassandraInstallCluster;
import org.thingsboard.server.service.install.cql.CQLStatementsParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`CassandraAbstractDatabaseSchemaService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public abstract class CassandraAbstractDatabaseSchemaService implements DatabaseSchemaService {

    /**
     * `CASSANDRA_DIR`常量，用于统一引用固定值。
     */
    private static final String CASSANDRA_DIR = "cassandra";
    private static final String CASSANDRA_STANDARD_KEYSPACE = "thingsboard";

    /**
     * 集群，用于支撑当前网络或外部服务交互。
     */
    @Autowired
    @Qualifier("CassandraInstallCluster")
    private CassandraInstallCluster cluster;

    /**
     * `installScripts` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private InstallScripts installScripts;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Value("${cassandra.keyspace_name}")
    private String keyspaceName;

    /**
     * `schemaCql` 字段，保存当前对象的对应属性。
     */
    private final String schemaCql;

    /**
     * 功能：创建 `CassandraAbstractDatabaseSchemaService` 实例，并初始化必要字段。
     * 参数：
     * - `schemaCql`：`schemaCql` 参数。
     * 返回：新创建的对象实例。
     */
    protected CassandraAbstractDatabaseSchemaService(String schemaCql) {
        this.schemaCql = schemaCql;
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
        log.info("Installing Cassandra DataBase schema part: " + schemaCql);
        Path schemaFile = Paths.get(installScripts.getDataDir(), CASSANDRA_DIR, schemaCql);
        loadCql(schemaFile);
    }

    /**
     * 功能：保存或创建`Database Indexes`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void createDatabaseIndexes() throws Exception {
    }

    /**
     * 功能：获取`Cql`。
     * 参数：
     * - `cql`：`cql` 参数。
     * 返回：无。
     */
    private void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> cluster.getSession().execute(getCassandraKeyspaceName(statement)));
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：文本结果。
     */
    private String getCassandraKeyspaceName(String statement) {
        return statement.replaceFirst(CASSANDRA_STANDARD_KEYSPACE, keyspaceName);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraAbstractDatabaseSchemaService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
