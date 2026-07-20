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
 * 1. `CassandraBufferedRateWriteExecutor` 是 ThingsBoard DAO 中处理 `Cassandra Buffered Rate` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractBufferedRateExecutor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
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
