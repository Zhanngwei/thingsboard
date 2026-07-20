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
 * 1. `CassandraAbstractDatabaseSchemaService` 是 ThingsBoard Application 中负责 `Cassandra Database Schema` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `DatabaseSchemaService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
