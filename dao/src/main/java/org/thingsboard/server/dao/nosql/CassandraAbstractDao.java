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
 * 1. `CassandraAbstractDao` 是 ThingsBoard DAO 中负责 `Cassandra` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
