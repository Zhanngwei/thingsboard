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
package org.thingsboard.server.dao.sql.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;

import java.util.concurrent.TimeUnit;


/**
 * 中文说明：
 * 1. `SqlEventCleanupRepository` 是 ThingsBoard DAO 中负责事件存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDaoListeningExecutorService`、`EventCleanupRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Repository
public class SqlEventCleanupRepository extends JpaAbstractDaoListeningExecutorService implements EventCleanupRepository {

    /**
     * 分区，保存当前对象的配置选项。
     */
    @Autowired
    private EventPartitionConfiguration partitionConfiguration;
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private SqlPartitioningRepository partitioningRepository;

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `eventExpTime`：`eventExpTime` 参数。
     * - `debug`：`debug` 参数。
     * 返回：无。
     */
    @Override
    public void cleanupEvents(long eventExpTime, boolean debug) {
        for (EventType eventType : EventType.values()) {
            if (eventType.isDebug() == debug) {
                cleanupEvents(eventType, eventExpTime);
            }
        }
    }

    /**
     * 功能：执行 `migrateEvents` 对应的处理。
     * 参数：
     * - `regularEventTs`：时间戳。
     * - `debugEventTs`：时间戳。
     * 返回：无。
     */
    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {
        regularEventTs = Math.max(regularEventTs, 1480982400000L);
        debugEventTs = Math.max(debugEventTs, 1480982400000L);

        callMigrateFunctionByPartitions("regular", "migrate_regular_events", regularEventTs, partitionConfiguration.getRegularPartitionSizeInHours());
        callMigrateFunctionByPartitions("debug", "migrate_debug_events", debugEventTs, partitionConfiguration.getDebugPartitionSizeInHours());

        try {
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_regular_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP PROCEDURE IF EXISTS migrate_debug_events(bigint, bigint, int)");
            jdbcTemplate.execute("DROP TABLE IF EXISTS event");
        } catch (DataAccessException e) {
            log.error("Error occurred during drop of the `events` table", e);
            throw e;
        }
    }

    /**
     * 功能：执行 `callMigrateFunctionByPartitions` 对应的处理。
     * 参数：
     * - `logTag`：`logTag` 参数。
     * - `functionName`：名称。
     * - `startTs`：时间戳。
     * - `partitionSizeInHours`：分区标识或分区信息。
     * 返回：无。
     */
    private void callMigrateFunctionByPartitions(String logTag, String functionName, long startTs, int partitionSizeInHours) {
        long currentTs = System.currentTimeMillis();
        var regularPartitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTs - startTs) / regularPartitionStepInMs;
        if (numberOfPartitions > 1000) {
            log.error("Please adjust your {} events partitioning configuration. " +
                            "Configuration with partition size of {} hours and corresponding TTL will use {} (>1000) partitions which is not recommended!",
                    logTag, partitionSizeInHours, numberOfPartitions);
            throw new RuntimeException("Please adjust your " + logTag + " events partitioning configuration. " +
                    "Configuration with partition size of " + partitionSizeInHours + " hours and corresponding TTL will use " +
                    +numberOfPartitions + " (>1000) partitions which is not recommended!");
        }
        while (startTs < currentTs) {
            var endTs = startTs + regularPartitionStepInMs;
            log.info("Migrate {} events for time period: [{},{}]", logTag, startTs, endTs);
            callMigrateFunction(functionName, startTs, startTs + regularPartitionStepInMs, partitionSizeInHours);
            startTs = endTs;
        }
        log.info("Migrate {} events done.", logTag);
    }

    /**
     * 功能：执行 `callMigrateFunction` 对应的处理。
     * 参数：
     * - `functionName`：名称。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - `partitionSizeInHours`：分区标识或分区信息。
     * 返回：无。
     */
    private void callMigrateFunction(String functionName, long startTs, long endTs, int partitionSizeInHours) {
        try {
            jdbcTemplate.update("CALL " + functionName + "(?, ?, ?)", startTs, endTs, partitionSizeInHours);
        } catch (DataAccessException e) {
            if (e.getMessage() == null || !e.getMessage().contains("relation \"event\" does not exist")) {
                log.error("[{}] SQLException occurred during execution of {} with parameters {} and {}", functionName, startTs, partitionSizeInHours, e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `eventType`：类型。
     * - `eventExpTime`：`eventExpTime` 参数。
     * 返回：无。
     */
    private void cleanupEvents(EventType eventType, long eventExpTime) {
        partitioningRepository.dropPartitionsBefore(eventType.getTable(), eventExpTime, partitionConfiguration.getPartitionSizeInMs(eventType));
    }

}
