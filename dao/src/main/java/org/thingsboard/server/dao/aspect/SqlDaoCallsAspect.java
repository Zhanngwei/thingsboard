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
package org.thingsboard.server.dao.aspect;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.exception.JDBCConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.join;

@Aspect
@ConditionalOnProperty(prefix = "sql", value = "log_tenant_stats", havingValue = "true")
@Component
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`SqlDaoCallsAspect` 是 ThingsBoard DAO 模块 中的数据库调用统计切面类型，用于拦截 DAO 方法调用并统计数据库访问耗时、调用次数和快照指标。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring AOP、DAO 实现、Stats 模块、日志和运维监控流程。
 * 4. 生命周期：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Aspect / Observer / Decorator。
 */
public class SqlDaoCallsAspect {

    private final Set<String> invalidTenantDbCallMethods = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<TenantId, DbCallStats> statsMap = new ConcurrentHashMap<>();

    @Value("${sql.batch_sort:true}")
    /**
     * 字段说明：
     * 1. 保存 `batchSortEnabled` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private boolean batchSortEnabled;

    /**
     * 字段说明：
     * 1. 保存 `DEADLOCK_DETECTED_ERROR` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String DEADLOCK_DETECTED_ERROR = "deadlock detected";


    @Scheduled(initialDelayString = "${sql.log_tenant_stats_interval_ms:60000}",
            fixedDelayString = "${sql.log_tenant_stats_interval_ms:60000}")
    /**
     * 方法说明：
     * 1. 职责：执行 `printStats` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void printStats() {
        List<DbCallStatsSnapshot> snapshots = snapshot();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (snapshots.isEmpty()) return;
        try {
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (log.isTraceEnabled()) {
                logTopNTenants(snapshots, Comparator.comparing(DbCallStatsSnapshot::getTotalTiming).reversed(), 0, snapshot -> {
                    logSnapshot(snapshot, 0, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::trace);
                });

                Map<String, Map<TenantId, MethodCallStatsSnapshot>> byMethodStats = new HashMap<>();
                // 循环处理批量实体、属性、遥测或测试数据时，需要关注单项失败对整体事务和缓存状态的影响。
                for (DbCallStatsSnapshot snapshot : snapshots) {
                    snapshot.getMethodStats().forEach((method, stats) -> {
                        byMethodStats.computeIfAbsent(method, m -> new HashMap<>())
                                .put(snapshot.getTenantId(), stats);
                    });
                }
                byMethodStats.forEach((method, byTenantStats) -> {
                    log.trace("Top tenants for method {} by calls:", method);
                    byTenantStats.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.comparing(MethodCallStatsSnapshot::getExecutions).reversed()))
                            .limit(10)
                            .forEach(e -> {
                                TenantId tenantId = e.getKey();
                                MethodCallStatsSnapshot methodStats = e.getValue();
                                log.trace("[{}] calls: {}, failures: {}, timing: {}", tenantId,
                                        methodStats.getExecutions(), methodStats.getFailures(), methodStats.getTiming());
                            });
                });
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            } else if (log.isDebugEnabled()) {
                log.debug("Total calls statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalCalls).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getExecutions).reversed(), "executions", log::debug));
                log.debug("Total timing statistics below:");
                logTopNTenants(snapshots, Comparator.comparingLong(DbCallStatsSnapshot::getTotalTiming).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::debug));
                log.debug("Total errors statistics below:");
                logTopNTenants(snapshots, Comparator.comparingInt(DbCallStatsSnapshot::getTotalFailure).reversed(), 10,
                        s -> logSnapshot(s, 10, Comparator.comparing(MethodCallStatsSnapshot::getFailures).reversed(), "failures", log::debug));
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            } else if (log.isInfoEnabled()) {
                log.info("Total timing statistics below:");
                logTopNTenants(snapshots, Comparator.comparingLong(DbCallStatsSnapshot::getTotalTiming).reversed(), 3,
                        s -> logSnapshot(s, 3, Comparator.comparing(MethodCallStatsSnapshot::getTiming).reversed(), "timing", log::info));
            }
        } finally {
            statsMap.clear();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logSnapshot` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void logSnapshot(DbCallStatsSnapshot snapshot, int limit, Comparator<MethodCallStatsSnapshot> methodStatsComparator, String sortingKey, Consumer<String> logger) {
        logger.accept(String.format("[%s]: calls: %s, failures: %s, exec time: %s ",
                snapshot.getTenantId(), snapshot.getTotalCalls(), snapshot.getTotalFailure(), snapshot.getTotalTiming()));
        var stream = snapshot.getMethodStats().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(methodStatsComparator));
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (limit > 0) {
            logger.accept(String.format("[%s] Top %s methods by %s:", snapshot.getTenantId(), limit, sortingKey));
            stream = stream.limit(limit);
        }
        stream.forEach(e -> {
            MethodCallStatsSnapshot methodStats = e.getValue();
            logger.accept(String.format("[%s]: method: %s, calls: %s, failures: %s, exec time: %s", snapshot.getTenantId(), e.getKey(),
                    methodStats.getExecutions(), methodStats.getFailures(), methodStats.getTiming()));
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `snapshot` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private List<DbCallStatsSnapshot> snapshot() {
        return statsMap.values().stream().map(DbCallStats::snapshot).collect(Collectors.toList());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `logTopNTenants` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void logTopNTenants(List<DbCallStatsSnapshot> snapshots, Comparator<DbCallStatsSnapshot> comparator,
                                int n, Consumer<DbCallStatsSnapshot> logFunction) {
        var stream = snapshots.stream().sorted(comparator);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (n > 0) {
            stream = stream.limit(n);
        }
        stream.forEach(logFunction);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Around("@within(org.thingsboard.server.dao.util.SqlDao)")
    /**
     * 方法说明：
     * 1. 职责：执行 `handleSqlCall` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public Object handleSqlCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        var methodName = signature.toShortString();
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (invalidTenantDbCallMethods.contains(methodName)) {
            //Simply call the method if tenant is not found
            return joinPoint.proceed();
        }
        var tenantId = getTenantId(signature, methodName, joinPoint.getArgs());
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (tenantId == null || tenantId.isNullUid()) {
            //Simply call the method if tenant is null
            return joinPoint.proceed();
        }
        var startTime = System.currentTimeMillis();
        try {
            var result = joinPoint.proceed();
            // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
            if (result instanceof ListenableFuture) {
                // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                Futures.addCallback((ListenableFuture) result,
                        // 异步结果会在回调或 Future 完成后继续转换，调用方不能假设这里已经同步完成数据库访问。
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable Object result) {
                                reportSuccessfulMethodExecution(tenantId, methodName, startTime);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                reportFailedMethodExecution(tenantId, methodName, startTime, t, joinPoint);
                            }
                        },
                        MoreExecutors.directExecutor());
            } else {
                reportSuccessfulMethodExecution(tenantId, methodName, startTime);
            }
            return result;
        // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
        } catch (Throwable t) {
            reportFailedMethodExecution(tenantId, methodName, startTime, t, joinPoint);
            throw t;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportFailedMethodExecution` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void reportFailedMethodExecution(TenantId tenantId, String method, long startTime, Throwable t, ProceedingJoinPoint joinPoint) {
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (t != null) {
            if (ExceptionUtils.indexOfThrowable(t, JDBCConnectionException.class) >= 0) {
                return;
            }
            if (StringUtils.containedByAny(DEADLOCK_DETECTED_ERROR, ExceptionUtils.getRootCauseMessage(t), ExceptionUtils.getMessage(t))) {
                if (!batchSortEnabled) {
                    log.warn("Deadlock was detected for method {} (tenant: {}). You might need to enable 'sql.batch_sort' option.", method, tenantId);
                } else {
                    log.error("Deadlock was detected for method {} (tenant: {}). Arguments passed: \n{}\n The error: ",
                            method, tenantId, join(joinPoint.getArgs(), System.lineSeparator()), t);
                }
            }
        }
        reportMethodExecution(tenantId, method, false, startTime);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportSuccessfulMethodExecution` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void reportSuccessfulMethodExecution(TenantId tenantId, String method, long startTime) {
        reportMethodExecution(tenantId, method, true, startTime);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `reportMethodExecution` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void reportMethodExecution(TenantId tenantId, String method, boolean success, long startTime) {
        statsMap.computeIfAbsent(tenantId, DbCallStats::new)
                .onMethodCall(method, success, System.currentTimeMillis() - startTime);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantId` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    TenantId getTenantId(MethodSignature signature, String methodName, Object[] args) {
        if (args == null || args.length == 0) {
            addAndLogInvalidMethods(methodName);
            return null;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof TenantId) {
                return (TenantId) arg;
            } else if (arg instanceof UUID) {
                if (signature.getParameterNames() != null && StringUtils.equals(signature.getParameterNames()[i], "tenantId")) {
                    log.trace("Method {} uses UUID for tenantId param instead of TenantId class", methodName);
                    return TenantId.fromUUID((UUID) arg);
                }
            }
        }
        if (ArrayUtils.contains(signature.getParameterTypes(), TenantId.class) ||
                ArrayUtils.contains(signature.getParameterNames(), "tenantId")) {
            log.debug("Null was submitted as tenantId to method {}. Args: {}", methodName, Arrays.toString(args));
        } else {
            addAndLogInvalidMethods(methodName);
        }
        return null;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addAndLogInvalidMethods` 对应的数据库调用统计切面类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 Spring AOP 在应用启动时织入，随 DAO 方法调用持续采样并生成统计快照时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void addAndLogInvalidMethods(String methodName) {
        invalidTenantDbCallMethods.add(methodName);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SqlDaoCallsAspect` 在 ThingsBoard DAO 模块 中承担数据库调用统计切面类型职责，核心目的是拦截 DAO 方法调用并统计数据库访问耗时、调用次数和快照指标。
 * 2. 核心流程：在 DAO 方法执行前后采集时间和状态，将调用统计累积到可查询的快照对象中。
 * 3. 关键依赖：主要依赖或协作对象包括Spring AOP、DAO 实现、Stats 模块、日志和运维监控流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
