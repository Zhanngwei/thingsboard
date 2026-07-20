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
package org.thingsboard.server.dao.util;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.cache.limits.RateLimitService;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * Created by ashvayka on 24.10.18.
 */
/**
 * 中文说明：
 * 1. `AbstractBufferedRateExecutor` 是 ThingsBoard DAO 中处理 `Buffered Rate Executor` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AsyncTask`、`BufferedRateExecutor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class AbstractBufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture<V>, V> implements BufferedRateExecutor<T, F> {

    /**
     * `CONCURRENCY_LEVEL`常量，用于统一引用固定值。
     */
    public static final String CONCURRENCY_LEVEL = "currBuffer";

    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private final long maxWaitTime;
    private final long pollMs;
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private final BlockingQueue<AsyncTaskContext<T, V>> queue;
    private final ExecutorService dispatcherExecutor;
    /**
     * 回调，负责处理对应任务或消息。
     */
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private final int concurrencyLimit;
    private final int printQueriesFreq;

    private final AtomicInteger printQueriesIdx = new AtomicInteger(0);

    /**
     * `concurrencyLevel` 字段，保存当前对象的对应属性。
     */
    protected final AtomicInteger concurrencyLevel;
    protected final BufferedRateExecutorStats stats;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityService entityService;
    private final RateLimitService rateLimitService;

    /**
     * 是否满足租户条件。
     */
    private final boolean printTenantNames;
    private final Map<TenantId, String> tenantNamesCache = new HashMap<>();

    /**
     * 功能：创建 `AbstractBufferedRateExecutor` 实例，并初始化必要字段。
     * 参数：
     * - `queueLimit`：队列名称或队列对象。
     * - `concurrencyLimit`：数量限制。
     * - `maxWaitTime`：`maxWaitTime` 参数。
     * - `dispatcherThreads`：`dispatcherThreads` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AbstractBufferedRateExecutor(int queueLimit, int concurrencyLimit, long maxWaitTime, int dispatcherThreads,
                                        int callbackThreads, long pollMs, int printQueriesFreq, StatsFactory statsFactory,
                                        EntityService entityService, RateLimitService rateLimitService, boolean printTenantNames) {
        this.maxWaitTime = maxWaitTime;
        this.pollMs = pollMs;
        this.concurrencyLimit = concurrencyLimit;
        this.printQueriesFreq = printQueriesFreq;
        this.queue = new LinkedBlockingDeque<>(queueLimit);
        this.dispatcherExecutor = Executors.newFixedThreadPool(dispatcherThreads, ThingsBoardThreadFactory.forName("nosql-" + getBufferName() + "-dispatcher"));
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(callbackThreads, "nosql-" + getBufferName() + "-callback");
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("nosql-" + getBufferName() + "-timeout"));
        this.stats = new BufferedRateExecutorStats(statsFactory);
        String concurrencyLevelKey = StatsType.RATE_EXECUTOR.getName() + "." + CONCURRENCY_LEVEL + getBufferName(); //metric name may change with buffer name suffix
        this.concurrencyLevel = statsFactory.createGauge(concurrencyLevelKey, new AtomicInteger(0));

        this.entityService = entityService;
        this.rateLimitService = rateLimitService;
        this.printTenantNames = printTenantNames;

        for (int i = 0; i < dispatcherThreads; i++) {
            dispatcherExecutor.submit(this::dispatch);
        }
    }

    /**
     * 功能：执行 `submit` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：处理结果。
     */
    @Override
    public F submit(T task) {
        SettableFuture<V> settableFuture = create();
        F result = wrap(task, settableFuture);

        boolean perTenantLimitReached = false;
        TenantId tenantId = task.getTenantId();
        if (tenantId != null && !tenantId.isSysTenantId()) {
            if (!rateLimitService.checkRateLimit(LimitedApi.CASSANDRA_QUERIES, tenantId)) {
                stats.incrementRateLimitedTenant(tenantId);
                stats.getTotalRateLimited().increment();
                settableFuture.setException(new TenantRateLimitException());
                perTenantLimitReached = true;
            }
        } else if (tenantId == null) {
            log.info("[{}] Invalid task received: {}", getBufferName(), task);
        }

        if (!perTenantLimitReached) {
            try {
                stats.getTotalAdded().increment();
                queue.add(new AsyncTaskContext<>(UUID.randomUUID(), task, settableFuture, System.currentTimeMillis()));
            } catch (IllegalStateException e) {
                stats.getTotalRejected().increment();
                settableFuture.setException(e);
            }
        }
        return result;
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `create` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    protected abstract SettableFuture<V> create();

    /**
     * 功能：执行 `wrap` 对应的处理。
     * 参数：
     * - `task`：`task` 参数。
     * - `future`：`future` 参数。
     * 返回：处理结果。
     */
    protected abstract F wrap(T task, SettableFuture<V> future);

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `taskCtx`：处理上下文。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<V> execute(AsyncTaskContext<T, V> taskCtx);

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public abstract String getBufferName();

    /**
     * 功能：执行 `dispatch` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void dispatch() {
        log.info("[{}] Buffered rate executor thread started", getBufferName());
        while (!Thread.interrupted()) {
            int curLvl = concurrencyLevel.get();
            AsyncTaskContext<T, V> taskCtx = null;
            try {
                if (curLvl <= concurrencyLimit) {
                    taskCtx = queue.take();
                    final AsyncTaskContext<T, V> finalTaskCtx = taskCtx;
                    if (printQueriesFreq > 0) {
                        if (printQueriesIdx.incrementAndGet() >= printQueriesFreq) {
                            printQueriesIdx.set(0);
                            String query = queryToString(finalTaskCtx);
                            log.info("[{}][{}] Cassandra query: {}", getBufferName(), taskCtx.getId(), query);
                        }
                    }
                    logTask("Processing", finalTaskCtx);
                    concurrencyLevel.incrementAndGet();
                    long timeout = finalTaskCtx.getCreateTime() + maxWaitTime - System.currentTimeMillis();
                    if (timeout > 0) {
                        stats.getTotalLaunched().increment();
                        ListenableFuture<V> result = execute(finalTaskCtx);
                        result = Futures.withTimeout(result, timeout, TimeUnit.MILLISECONDS, timeoutExecutor);
                        Futures.addCallback(result, new FutureCallback<V>() {
                            @Override
                            public void onSuccess(@Nullable V result) {
                                logTask("Releasing", finalTaskCtx);
                                stats.getTotalReleased().increment();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().set(result);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (t instanceof TimeoutException) {
                                    logTask("Expired During Execution", finalTaskCtx);
                                } else {
                                    logTask("Failed", finalTaskCtx);
                                }
                                stats.getTotalFailed().increment();
                                concurrencyLevel.decrementAndGet();
                                finalTaskCtx.getFuture().setException(t);
                                log.debug("[{}] Failed to execute task: {}", finalTaskCtx.getId(), finalTaskCtx.getTask(), t);
                            }
                        }, callbackExecutor);
                    } else {
                        logTask("Expired Before Execution", finalTaskCtx);
                        stats.getTotalExpired().increment();
                        concurrencyLevel.decrementAndGet();
                        taskCtx.getFuture().setException(new TimeoutException());
                    }
                } else {
                    Thread.sleep(pollMs);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable e) {
                if (taskCtx != null) {
                    log.debug("[{}] Failed to execute task: {}", taskCtx.getId(), taskCtx, e);
                    stats.getTotalFailed().increment();
                    concurrencyLevel.decrementAndGet();
                } else {
                    log.debug("Failed to queue task:", e);
                }
            }
        }
        log.info("[{}] Buffered rate executor thread stopped", getBufferName());
    }

    /**
     * 功能：执行 `logTask` 对应的处理。
     * 参数：
     * - `action`：`action` 参数。
     * - `taskCtx`：处理上下文。
     * 返回：无。
     */
    private void logTask(String action, AsyncTaskContext<T, V> taskCtx) {
        if (log.isTraceEnabled()) {
            if (taskCtx.getTask() instanceof CassandraStatementTask) {
                String query = queryToString(taskCtx);
                log.trace("[{}] {} task: {}, BoundStatement query: {}", taskCtx.getId(), action, taskCtx, query);
            } else {
                log.trace("[{}] {} task: {}", taskCtx.getId(), action, taskCtx);
            }
        } else {
            log.debug("[{}] {} task", taskCtx.getId(), action);
        }
    }

    /**
     * 功能：获取`To String`。
     * 参数：
     * - `taskCtx`：处理上下文。
     * 返回：文本结果。
     */
    private String queryToString(AsyncTaskContext<T, V> taskCtx) {
        CassandraStatementTask cassStmtTask = (CassandraStatementTask) taskCtx.getTask();
        if (cassStmtTask.getStatement() instanceof BoundStatement) {
            BoundStatement stmt = (BoundStatement) cassStmtTask.getStatement();
            String query = stmt.getPreparedStatement().getQuery();
            try {
                query = toStringWithValues(stmt, ProtocolVersion.V5);
            } catch (Exception e) {
                log.warn("Can't convert to query with values", e);
            }
            return query;
        } else {
            return "Not Cassandra Statement Task";
        }
    }

    /**
     * 功能：执行 `toStringWithValues` 对应的处理。
     * 参数：
     * - `boundStatement`：`boundStatement` 参数。
     * - `protocolVersion`：`protocolVersion` 参数。
     * 返回：文本结果。
     */
    private static String toStringWithValues(BoundStatement boundStatement, ProtocolVersion protocolVersion) {
        CodecRegistry codecRegistry = boundStatement.codecRegistry();
        PreparedStatement preparedStatement = boundStatement.getPreparedStatement();
        String query = preparedStatement.getQuery();
        ColumnDefinitions defs = preparedStatement.getVariableDefinitions();
        int index = 0;
        for (ColumnDefinition def : defs) {
            DataType type = def.getType();
            TypeCodec<Object> codec = codecRegistry.codecFor(type);
            if (boundStatement.getBytesUnsafe(index) != null) {
                Object value = codec.decode(boundStatement.getBytesUnsafe(index), protocolVersion);
                String replacement = Matcher.quoteReplacement(codec.format(value));
                query = query.replaceFirst("\\?", replacement);
            }
            index++;
        }
        return query;
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：数值结果。
     */
    protected int getQueueSize() {
        return queue.size();
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void printStats() {
        int queueSize = getQueueSize();
        int rateLimitedTenantsCount = (int) stats.getRateLimitedTenants().values().stream()
                .filter(defaultCounter -> defaultCounter.get() > 0)
                .count();

        if (queueSize > 0
                || rateLimitedTenantsCount > 0
                || concurrencyLevel.get() > 0
                || stats.getStatsCounters().stream().anyMatch(counter -> counter.get() > 0)
        ) {
            StringBuilder statsBuilder = new StringBuilder();

            statsBuilder.append("queueSize").append(" = [").append(queueSize).append("] ");
            stats.getStatsCounters().forEach(counter -> {
                statsBuilder.append(counter.getName()).append(" = [").append(counter.get()).append("] ");
            });
            statsBuilder.append("totalRateLimitedTenants").append(" = [").append(rateLimitedTenantsCount).append("] ");
            statsBuilder.append(CONCURRENCY_LEVEL).append(" = [").append(concurrencyLevel.get()).append("] ");

            stats.getStatsCounters().forEach(StatsCounter::clear);
            log.info("[{}] Permits {}", getBufferName(), statsBuilder);
        }

        stats.getRateLimitedTenants().entrySet().stream()
                .filter(entry -> entry.getValue().get() > 0)
                .forEach(entry -> {
                    TenantId tenantId = entry.getKey();
                    DefaultCounter counter = entry.getValue();
                    int rateLimitedRequests = counter.get();
                    counter.clear();
                    if (printTenantNames) {
                        String name = tenantNamesCache.computeIfAbsent(tenantId, tId -> {
                            String defaultName = "N/A";
                            try {
                                return entityService.fetchEntityName(TenantId.SYS_TENANT_ID, tenantId).orElse(defaultName);
                            } catch (Exception e) {
                                log.error("[{}][{}] Failed to get tenant name", getBufferName(), tenantId, e);
                                return defaultName;
                            }
                        });
                        log.info("[{}][{}][{}] Rate limited requests: {}", getBufferName(), tenantId, name, rateLimitedRequests);
                    } else {
                        log.info("[{}][{}] Rate limited requests: {}", getBufferName(), tenantId, rateLimitedRequests);
                    }
                });
    }
}
