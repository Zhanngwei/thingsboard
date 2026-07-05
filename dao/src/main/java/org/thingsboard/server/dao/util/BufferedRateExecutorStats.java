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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 中文说明：
 * 1. 类目的：`BufferedRateExecutorStats` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
 */
@Slf4j
@Getter
public class BufferedRateExecutorStats {
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String TENANT_ID_TAG = "tenantId";


    /**
     * `TOTAL_ADDED`常量，用于统一引用固定值。
     */
    private static final String TOTAL_ADDED = "totalAdded";
    private static final String TOTAL_LAUNCHED = "totalLaunched";
    /**
     * `TOTAL_RELEASED`常量，用于统一引用固定值。
     */
    private static final String TOTAL_RELEASED = "totalReleased";
    private static final String TOTAL_FAILED = "totalFailed";
    /**
     * `TOTAL_EXPIRED`常量，用于统一引用固定值。
     */
    private static final String TOTAL_EXPIRED = "totalExpired";
    private static final String TOTAL_REJECTED = "totalRejected";
    /**
     * 频率常量，用于统一引用固定值。
     */
    private static final String TOTAL_RATE_LIMITED = "totalRateLimited";

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final StatsFactory statsFactory;

    private final ConcurrentMap<TenantId, DefaultCounter> rateLimitedTenants = new ConcurrentHashMap<>();

    private final List<StatsCounter> statsCounters = new ArrayList<>();

    /**
     * `totalAdded` 字段，保存当前对象的对应属性。
     */
    private final StatsCounter totalAdded;
    private final StatsCounter totalLaunched;
    /**
     * `totalReleased` 字段，保存当前对象的对应属性。
     */
    private final StatsCounter totalReleased;
    private final StatsCounter totalFailed;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    private final StatsCounter totalExpired;
    private final StatsCounter totalRejected;
    /**
     * 频率，表示当前对象的对应属性。
     */
    private final StatsCounter totalRateLimited;

    /**
     * 功能：创建 `BufferedRateExecutorStats` 实例，并初始化必要字段。
     * 参数：
     * - `statsFactory`：`statsFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public BufferedRateExecutorStats(StatsFactory statsFactory) {
        this.statsFactory = statsFactory;

        String key = StatsType.RATE_EXECUTOR.getName();

        this.totalAdded = statsFactory.createStatsCounter(key, TOTAL_ADDED);
        this.totalLaunched = statsFactory.createStatsCounter(key, TOTAL_LAUNCHED);
        this.totalReleased = statsFactory.createStatsCounter(key, TOTAL_RELEASED);
        this.totalFailed = statsFactory.createStatsCounter(key, TOTAL_FAILED);
        this.totalExpired = statsFactory.createStatsCounter(key, TOTAL_EXPIRED);
        this.totalRejected = statsFactory.createStatsCounter(key, TOTAL_REJECTED);
        this.totalRateLimited = statsFactory.createStatsCounter(key, TOTAL_RATE_LIMITED);

        this.statsCounters.add(totalAdded);
        this.statsCounters.add(totalLaunched);
        this.statsCounters.add(totalReleased);
        this.statsCounters.add(totalFailed);
        this.statsCounters.add(totalExpired);
        this.statsCounters.add(totalRejected);
        this.statsCounters.add(totalRateLimited);
    }

    /**
     * 功能：执行 `incrementRateLimitedTenant` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void incrementRateLimitedTenant(TenantId tenantId){
        rateLimitedTenants.computeIfAbsent(tenantId,
                tId -> {
                    String key = StatsType.RATE_EXECUTOR.getName() + ".tenant";
                    return statsFactory.createDefaultCounter(key, TENANT_ID_TAG, tId.toString());
                }
        )
                .increment();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BufferedRateExecutorStats` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
