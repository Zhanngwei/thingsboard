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
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`AbstractBufferedRateExecutor` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
 */
public abstract class AbstractBufferedRateExecutor<T extends AsyncTask, F extends ListenableFuture<V>, V> implements BufferedRateExecutor<T, F> {

    /**
     * 字段说明：
     * 1. 保存 `CONCURRENCY_LEVEL` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    public static final String CONCURRENCY_LEVEL = "currBuffer";

    /**
     * 字段说明：
     * 1. 保存 `maxWaitTime` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final long maxWaitTime;
    private final long pollMs;
    /**
     * 字段说明：
     * 1. 保存 `queue` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final BlockingQueue<AsyncTaskContext<T, V>> queue;
    private final ExecutorService dispatcherExecutor;
    /**
     * 字段说明：
     * 1. 保存 `callbackExecutor` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final ExecutorService callbackExecutor;
    private final ScheduledExecutorService timeoutExecutor;
    /**
     * 字段说明：
     * 1. 保存 `concurrencyLimit` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final int concurrencyLimit;
    private final int printQueriesFreq;

    private final AtomicInteger printQueriesIdx = new AtomicInteger(0);

    /**
     * 字段说明：
     * 1. 保存 `concurrencyLevel` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    protected final AtomicInteger concurrencyLevel;
    protected final BufferedRateExecutorStats stats;

    /**
     * 字段说明：
     * 1. 保存 `entityService` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final EntityService entityService;
    private final RateLimitService rateLimitService;

    /**
     * 字段说明：
     * 1. 保存 `printTenantNames` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private final boolean printTenantNames;
    private final Map<TenantId, String> tenantNamesCache = new HashMap<>();

    /**
     * 方法说明：
     * 1. 职责：执行 `AbstractBufferedRateExecutor` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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

        // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
        for (int i = 0; i < dispatcherThreads; i++) {
            dispatcherExecutor.submit(this::dispatch);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `submit` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public F submit(T task) {
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        SettableFuture<V> settableFuture = create();
        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
        F result = wrap(task, settableFuture);

        boolean perTenantLimitReached = false;
        TenantId tenantId = task.getTenantId();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (tenantId != null && !tenantId.isSysTenantId()) {
            // Cassandra 访问通常是异步或分页的，需要在这里维护查询语句、结果转换和失败处理边界。
            if (!rateLimitService.checkRateLimit(LimitedApi.CASSANDRA_QUERIES, tenantId)) {
                stats.incrementRateLimitedTenant(tenantId);
                stats.getTotalRateLimited().increment();
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                settableFuture.setException(new TenantRateLimitException());
                perTenantLimitReached = true;
            }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        } else if (tenantId == null) {
            log.info("[{}] Invalid task received: {}", getBufferName(), task);
        }

        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (!perTenantLimitReached) {
            try {
                stats.getTotalAdded().increment();
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                queue.add(new AsyncTaskContext<>(UUID.randomUUID(), task, settableFuture, System.currentTimeMillis()));
            // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
            } catch (IllegalStateException e) {
                stats.getTotalRejected().increment();
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                settableFuture.setException(e);
            }
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void stop() {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (dispatcherExecutor != null) {
            dispatcherExecutor.shutdownNow();
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `create` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected abstract SettableFuture<V> create();

    /**
     * 方法说明：
     * 1. 职责：执行 `wrap` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected abstract F wrap(T task, SettableFuture<V> future);

    /**
     * 方法说明：
     * 1. 职责：执行 `execute` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected abstract ListenableFuture<V> execute(AsyncTaskContext<T, V> taskCtx);

    /**
     * 方法说明：
     * 1. 职责：执行 `getBufferName` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public abstract String getBufferName();

    /**
     * 方法说明：
     * 1. 职责：执行 `dispatch` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `logTask` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `queryToString` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `toStringWithValues` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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
     * 方法说明：
     * 1. 职责：执行 `getQueueSize` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    protected int getQueueSize() {
        return queue.size();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `printStats` 对应的DAO 工具和配置类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
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

/*
 * 本类总结：
 * 1. 核心职责：`AbstractBufferedRateExecutor` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
