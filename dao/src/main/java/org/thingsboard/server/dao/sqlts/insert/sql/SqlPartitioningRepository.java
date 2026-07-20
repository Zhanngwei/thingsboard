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
package org.thingsboard.server.dao.sqlts.insert.sql;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.timeseries.SqlPartition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `SqlPartitioningRepository` 是 ThingsBoard DAO 中负责 `Sql Partitioning` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Repository
@Slf4j
public class SqlPartitioningRepository {

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * `SELECT_PARTITIONS_STMT`常量，用于统一引用固定值。
     */
    private static final String SELECT_PARTITIONS_STMT = "SELECT tablename from pg_tables WHERE schemaname = 'public' and tablename like concat(?, '_%')";

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final int PSQL_VERSION_14 = 140000;
    private volatile Integer currentServerVersion;

    private final Map<String, Map<Long, SqlPartition>> tablesPartitions = new ConcurrentHashMap<>();
    private final ReentrantLock partitionCreationLock = new ReentrantLock();

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `partition`：分区标识或分区信息。
     * 返回：无。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void save(SqlPartition partition) {
        jdbcTemplate.execute(partition.getQuery());
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `table`：`table` 参数。
     * - `entityTs`：时间戳。
     * - `partitionDurationMs`：分区标识或分区信息。
     * 返回：无。
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // executing non-transactionally, so that parent transaction is not aborted on partition save error
    public void createPartitionIfNotExists(String table, long entityTs, long partitionDurationMs) {
        long partitionStartTs = calculatePartitionStartTime(entityTs, partitionDurationMs);
        Map<Long, SqlPartition> partitions = tablesPartitions.computeIfAbsent(table, t -> new ConcurrentHashMap<>());
        if (!partitions.containsKey(partitionStartTs)) {
            SqlPartition partition = new SqlPartition(table, partitionStartTs, getPartitionEndTime(partitionStartTs, partitionDurationMs), Long.toString(partitionStartTs));
            partitionCreationLock.lock();
            try {
                if (partitions.containsKey(partitionStartTs)) return;
                log.info("Saving partition {}-{} for table {}", partition.getStart(), partition.getEnd(), table);
                save(partition);
                log.trace("Adding partition to map: {}", partition);
                partitions.put(partition.getStart(), partition);
            } catch (Exception e) {
                String error = ExceptionUtils.getRootCauseMessage(e);
                if (StringUtils.containsAny(error, "would overlap partition", "already exists")) {
                    partitions.put(partition.getStart(), partition);
                    log.debug("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                } else {
                    log.warn("Couldn't save partition {}-{} for table {}: {}", partition.getStart(), partition.getEnd(), table, error);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    /**
     * 功能：执行 `dropPartitionsBefore` 对应的处理。
     * 参数：
     * - `table`：`table` 参数。
     * - `ts`：时间戳。
     * - `partitionDurationMs`：分区标识或分区信息。
     * 返回：数值结果。
     */
    public long dropPartitionsBefore(String table, long ts, long partitionDurationMs) {
        List<Long> partitions = fetchPartitions(table);
        long lastDroppedPartitionEndTime = -1;
        for (Long partitionStartTime : partitions) {
            long partitionEndTime = getPartitionEndTime(partitionStartTime, partitionDurationMs);
            if (partitionEndTime < ts) {
                log.info("[{}] Detaching expired partition: [{}-{}]", table, partitionStartTime, partitionEndTime);
                boolean success = detachAndDropPartition(table, partitionStartTime);
                if (success) {
                    log.info("[{}] Detached expired partition: {}", table, partitionStartTime);
                    lastDroppedPartitionEndTime = Math.max(partitionEndTime, lastDroppedPartitionEndTime);
                }
            } else {
                log.debug("[{}] Skipping valid partition: {}", table, partitionStartTime);
            }
        }
        return lastDroppedPartitionEndTime;
    }

    /**
     * 功能：删除或清理`Partitions Cache`。
     * 参数：
     * - `table`：`table` 参数。
     * - `expTime`：`expTime` 参数。
     * - `partitionDurationMs`：分区标识或分区信息。
     * 返回：无。
     */
    public void cleanupPartitionsCache(String table, long expTime, long partitionDurationMs) {
        Map<Long, SqlPartition> partitions = tablesPartitions.get(table);
        if (partitions == null) return;
        partitions.keySet().removeIf(startTime -> getPartitionEndTime(startTime, partitionDurationMs) < expTime);
    }

    /**
     * 功能：执行 `detachAndDropPartition` 对应的处理。
     * 参数：
     * - `table`：`table` 参数。
     * - `partitionTs`：时间戳。
     * 返回：判断结果。
     */
    private boolean detachAndDropPartition(String table, long partitionTs) {
        Map<Long, SqlPartition> cachedPartitions = tablesPartitions.get(table);
        if (cachedPartitions != null) cachedPartitions.remove(partitionTs);

        String tablePartition = table + "_" + partitionTs;
        String detachPsqlStmtStr = "ALTER TABLE " + table + " DETACH PARTITION " + tablePartition;

        // hotfix of ERROR: partition "integration_debug_event_1678323600000" already pending detach in partitioned table "public.integration_debug_event"
        // https://github.com/thingsboard/thingsboard/issues/8271
        // if (getCurrentServerVersion() >= PSQL_VERSION_14) {
        //    detachPsqlStmtStr += " CONCURRENTLY";
        // }

        String dropStmtStr = "DROP TABLE " + tablePartition;
        try {
            jdbcTemplate.execute(detachPsqlStmtStr);
            jdbcTemplate.execute(dropStmtStr);
            return true;
        } catch (DataAccessException e) {
            log.error("[{}] Error occurred trying to detach and drop the partition {} ", table, partitionTs, e);
        }
        return false;
    }

    /**
     * 功能：获取分区。
     * 参数：
     * - `startTime`：开始时间戳。
     * - `partitionDurationMs`：分区标识或分区信息。
     * 返回：数值结果。
     */
    private static long getPartitionEndTime(long startTime, long partitionDurationMs) {
        return startTime + partitionDurationMs;
    }

    /**
     * 功能：获取`Partitions`。
     * 参数：
     * - `table`：`table` 参数。
     * 返回：匹配的数据集合。
     */
    public List<Long> fetchPartitions(String table) {
        List<Long> partitions = new ArrayList<>();
        List<String> partitionsTables = jdbcTemplate.queryForList(SELECT_PARTITIONS_STMT, String.class, table);
        for (String partitionTableName : partitionsTables) {
            String partitionTsStr = partitionTableName.substring(table.length() + 1);
            try {
                partitions.add(Long.parseLong(partitionTsStr));
            } catch (NumberFormatException nfe) {
                log.debug("Failed to parse table name: {}", partitionTableName);
            }
        }
        return partitions;
    }

    /**
     * 功能：执行 `calculatePartitionStartTime` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `partitionDuration`：分区标识或分区信息。
     * 返回：数值结果。
     */
    public long calculatePartitionStartTime(long ts, long partitionDuration) {
        return ts - (ts % partitionDuration);
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：数值结果。
     */
    private synchronized int getCurrentServerVersion() {
        if (currentServerVersion == null) {
            try {
                currentServerVersion = jdbcTemplate.queryForObject("SELECT current_setting('server_version_num')", Integer.class);
            } catch (Exception e) {
                log.warn("Error occurred during fetch of the server version", e);
            }
            if (currentServerVersion == null) {
                currentServerVersion = 0;
            }
        }
        return currentServerVersion;
    }

}
