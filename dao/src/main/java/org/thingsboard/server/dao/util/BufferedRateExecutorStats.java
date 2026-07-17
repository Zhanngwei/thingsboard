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
 * 1. `BufferedRateExecutorStats` 是 ThingsBoard DAO 中负责统计数据存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
