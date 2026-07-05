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
 * 1. 类目的：`SqlEventCleanupRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`SqlEventCleanupRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
