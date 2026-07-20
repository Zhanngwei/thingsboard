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
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `AbstractSqlTimeseriesDao` 是 ThingsBoard DAO 中负责时序数据存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `BaseAbstractSqlTimeseriesDao`、`AggregationTimeseriesDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@SuppressWarnings("UnstableApiUsage")
@Slf4j
public abstract class AbstractSqlTimeseriesDao extends BaseAbstractSqlTimeseriesDao implements AggregationTimeseriesDao {

    protected static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${sql.ts.batch_size:1000}")
    protected int tsBatchSize;

    /**
     * 最大延迟，用于控制时间范围或等待时长。
     */
    @Value("${sql.ts.batch_max_delay:100}")
    protected long tsMaxDelay;

    /**
     * 统计日志输出间隔，用于控制时间范围或等待时长。
     */
    @Value("${sql.ts.stats_print_interval_ms:1000}")
    protected long tsStatsPrintIntervalMs;

    /**
     * 批处理线程数，用于控制处理规模或位置。
     */
    @Value("${sql.ts.batch_threads:4}")
    protected int tsBatchThreads;

    /**
     * 批处理线程数，用于控制处理规模或位置。
     */
    @Value("${sql.timescale.batch_threads:4}")
    protected int timescaleBatchThreads;

    /**
     * 是否启用`batch sort`。
     */
    @Value("${sql.batch_sort:true}")
    protected boolean batchSortEnabled;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    @Value("${sql.ttl.ts.ts_key_value_ttl:0}")
    private long systemTtl;

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：
     * - `systemTtl`：`systemTtl` 参数。
     * 返回：无。
     */
    public void cleanup(long systemTtl) {
        log.info("Going to cleanup old timeseries data using ttl: {}s", systemTtl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_timeseries_by_ttl(?,?,?)")) {
            stmt.setObject(1, ModelConstants.NULL_UUID);
            stmt.setLong(2, systemTtl);
            stmt.setLong(3, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total telemetry removed stats by TTL for entities: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during timeseries TTL task execution ", e);
        }
    }

    /**
     * 功能：处理`Find All Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `queries`：数据列表。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<List<ReadTsKvQueryResult>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<ReadTsKvQueryResult>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<>() {
            @Nullable
            @Override
            public List<ReadTsKvQueryResult> apply(@Nullable List<ReadTsKvQueryResult> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream().filter(Objects::nonNull).collect(Collectors.toList());
            }
        }, service);
    }

    /**
     * 功能：执行 `computeTtl` 对应的处理。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * 返回：数值结果。
     */
    protected long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `tsKvEntry`：`tsKvEntry` 参数。
     * - `ttl`：`ttl` 参数。
     * 返回：数值结果。
     */
    protected int getDataPointDays(TsKvEntry tsKvEntry, long ttl) {
        return tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
    }
}
