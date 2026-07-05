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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.util.AbstractBufferedRateExecutor;
import org.thingsboard.server.dao.util.AsyncTaskContext;
import org.thingsboard.server.dao.util.NoSqlAnyDao;
import org.thingsboard.server.cache.limits.RateLimitService;

import javax.annotation.PreDestroy;

/**
 * Created by ashvayka on 24.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`CassandraBufferedRateWriteExecutor` 是 ThingsBoard DAO 模块 中的NoSQL/Cassandra 持久化类型，用于封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 生命周期：由 Spring 容器创建并在 Cassandra 查询生命周期内处理异步结果、分页和异常转换。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Async Callback / Template。
 */
@Component
@Slf4j
@NoSqlAnyDao
public class CassandraBufferedRateWriteExecutor extends AbstractBufferedRateExecutor<CassandraStatementTask, TbResultSetFuture, TbResultSet> {

    /**
     * 名称常量，用于统一引用固定值。
     */
    static final String BUFFER_NAME = "Write";

    /**
     * 功能：创建 `CassandraBufferedRateWriteExecutor` 实例，并初始化必要字段。
     * 参数：
     * - `queueLimit`：队列名称或队列对象。
     * - `concurrencyLimit`：数量限制。
     * - `maxWaitTime`：`maxWaitTime` 参数。
     * - `dispatcherThreads`：`dispatcherThreads` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public CassandraBufferedRateWriteExecutor(
            @Value("${cassandra.query.buffer_size}") int queueLimit,
            @Value("${cassandra.query.concurrent_limit}") int concurrencyLimit,
            @Value("${cassandra.query.permit_max_wait_time}") long maxWaitTime,
            @Value("${cassandra.query.dispatcher_threads:2}") int dispatcherThreads,
            @Value("${cassandra.query.callback_threads:4}") int callbackThreads,
            @Value("${cassandra.query.poll_ms:50}") long pollMs,
            @Value("${cassandra.query.tenant_rate_limits.print_tenant_names}") boolean printTenantNames,
            @Value("${cassandra.query.print_queries_freq:0}") int printQueriesFreq,
            @Autowired StatsFactory statsFactory,
            @Autowired EntityService entityService,
            @Autowired RateLimitService rateLimitService) {
        super(queueLimit, concurrencyLimit, maxWaitTime, dispatcherThreads, callbackThreads, pollMs, printQueriesFreq, statsFactory,
                entityService, rateLimitService, printTenantNames);
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${cassandra.query.rate_limit_print_interval_ms}")
    @Override
    public void printStats() {
        super.printStats();
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stop() {
        super.stop();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getBufferName() {
        return BUFFER_NAME;
    }

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    protected SettableFuture<TbResultSet> create() {
        return SettableFuture.create();
    }

    /**
     * 功能：执行 `wrap` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `future`：`future` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected TbResultSetFuture wrap(CassandraStatementTask task, SettableFuture<TbResultSet> future) {
        return new TbResultSetFuture(future);
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `taskCtx`：处理上下文。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<TbResultSet> execute(AsyncTaskContext<CassandraStatementTask, TbResultSet> taskCtx) {
        CassandraStatementTask task = taskCtx.getTask();
        return task.executeAsync(
                statement ->
                        this.submit(new CassandraStatementTask(task.getTenantId(), task.getSession(), statement))
        );
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraBufferedRateWriteExecutor` 在 ThingsBoard DAO 模块 中承担NoSQL/Cassandra 持久化类型职责，核心目的是封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 核心流程：构造 Cassandra 语句并异步执行，随后把 ResultSet 转换为 DAO API 需要的领域结果。
 * 3. 关键依赖：主要依赖或协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
