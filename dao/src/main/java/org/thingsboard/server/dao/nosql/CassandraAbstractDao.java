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
package org.thingsboard.server.dao.nosql;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.util.BufferedRateExecutor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 中文说明：
 * 1. 类目的：`CassandraAbstractDao` 是 ThingsBoard DAO 模块 中的NoSQL/Cassandra 持久化类型，用于封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 生命周期：由 Spring 容器创建并在 Cassandra 查询生命周期内处理异步结果、分页和异常转换。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Async Callback / Template。
 */
@Slf4j
public abstract class CassandraAbstractDao {

    /**
     * 集群，用于支撑当前网络或外部服务交互。
     */
    @Autowired
    @Qualifier("CassandraCluster")
    protected CassandraCluster cluster;

    private ConcurrentMap<String, PreparedStatement> preparedStatementMap = new ConcurrentHashMap<>();

    /**
     * 频率，表示当前对象的对应属性。
     */
    @Autowired
    private CassandraBufferedRateReadExecutor rateReadLimiter;

    /**
     * 频率，表示当前对象的对应属性。
     */
    @Autowired
    private CassandraBufferedRateWriteExecutor rateWriteLimiter;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private GuavaSession session;

    /**
     * `defaultReadLevel` 字段，保存当前对象的对应属性。
     */
    private ConsistencyLevel defaultReadLevel;
    private ConsistencyLevel defaultWriteLevel;

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：处理结果。
     */
    private GuavaSession getSession() {
        if (session == null) {
            session = cluster.getSession();
            defaultReadLevel = cluster.getDefaultReadConsistencyLevel();
            defaultWriteLevel = cluster.getDefaultWriteConsistencyLevel();
        }
        return session;
    }

    /**
     * 功能：执行 `prepare` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    protected PreparedStatement prepare(String query) {
        return preparedStatementMap.computeIfAbsent(query, i -> getSession().prepare(i));
    }

    /**
     * 功能：执行`Read`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    protected AsyncResultSet executeRead(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultReadLevel, rateReadLimiter);
    }

    /**
     * 功能：执行`Write`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    protected AsyncResultSet executeWrite(TenantId tenantId, Statement statement) {
        return execute(tenantId, statement, defaultWriteLevel, rateWriteLimiter);
    }

    /**
     * 功能：执行`Async Read`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    protected TbResultSetFuture executeAsyncRead(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultReadLevel, rateReadLimiter);
    }

    /**
     * 功能：执行`Async Write`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * 返回：匹配的数据集合。
     */
    protected TbResultSetFuture executeAsyncWrite(TenantId tenantId, Statement statement) {
        return executeAsync(tenantId, statement, defaultWriteLevel, rateWriteLimiter);
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * - `level`：`level` 参数。
     * - `rateExecutor`：`rateExecutor` 参数。
     * 返回：匹配的数据集合。
     */
    private AsyncResultSet execute(TenantId tenantId, Statement statement, ConsistencyLevel level,
                                   BufferedRateExecutor<CassandraStatementTask, TbResultSetFuture> rateExecutor) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra statement {}", statementToString(statement));
        }
        return executeAsync(tenantId, statement, level, rateExecutor).getUninterruptibly();
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `statement`：`statement` 参数。
     * - `level`：`level` 参数。
     * - `rateExecutor`：`rateExecutor` 参数。
     * 返回：匹配的数据集合。
     */
    private TbResultSetFuture executeAsync(TenantId tenantId, Statement statement, ConsistencyLevel level,
                                           BufferedRateExecutor<CassandraStatementTask, TbResultSetFuture> rateExecutor) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement = statement.setConsistencyLevel(level);
        }
        return rateExecutor.submit(new CassandraStatementTask(tenantId, getSession(), statement));
    }

    /**
     * 功能：执行 `statementToString` 对应的处理。
     * 参数：
     * - `statement`：`statement` 参数。
     * 返回：文本结果。
     */
    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement) statement).getPreparedStatement().getQuery();
        } else {
            return statement.toString();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraAbstractDao` 在 ThingsBoard DAO 模块 中承担NoSQL/Cassandra 持久化类型职责，核心目的是封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 核心流程：构造 Cassandra 语句并异步执行，随后把 ResultSet 转换为 DAO API 需要的领域结果。
 * 3. 关键依赖：主要依赖或协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
