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
package org.thingsboard.server.dao.sql.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.edge.EdgeEventDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. `JpaBaseEdgeEventDao` 是 ThingsBoard DAO 中负责事件存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaPartitionedAbstractDao`、`EdgeEventDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@SqlDao
@RequiredArgsConstructor
@Slf4j
public class JpaBaseEdgeEventDao extends JpaPartitionedAbstractDao<EdgeEventEntity, EdgeEvent> implements EdgeEventDao {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final UUID systemTenantId = NULL_UUID;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final ScheduledLogExecutorComponent logExecutor;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final StatsFactory statsFactory;

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    private final EdgeEventRepository edgeEventRepository;

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    private final EdgeEventInsertRepository edgeEventInsertRepository;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final SqlPartitioningRepository partitioningRepository;

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${sql.edge_events.batch_size:1000}")
    private int batchSize;

    /**
     * 最大延迟，用于控制时间范围或等待时长。
     */
    @Value("${sql.edge_events.batch_max_delay:100}")
    private long maxDelay;

    /**
     * 统计日志输出间隔，用于控制时间范围或等待时长。
     */
    @Value("${sql.edge_events.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;

    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${sql.edge_events.partition_size:168}")
    private int partitionSizeInHours;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    @Value("${sql.ttl.edge_events.edge_events_ttl:2628000}")
    private long edgeEventsTtl;

    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String TABLE_NAME = ModelConstants.EDGE_EVENT_TABLE_NAME;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private TbSqlBlockingQueueWrapper<EdgeEventEntity> queue;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<EdgeEventEntity> getEntityClass() {
        return EdgeEventEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<EdgeEventEntity, UUID> getRepository() {
        return edgeEventRepository;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Edge Events")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("edge.events")
                .batchSortEnabled(true)
                .build();
        Function<EdgeEventEntity, Integer> hashcodeFunction = entity -> {
            if (entity.getEntityId() != null) {
                return entity.getEntityId().hashCode();
            } else {
                return NULL_UUID.hashCode();
            }
        };
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, 1, statsFactory);
        queue.init(logExecutor, edgeEventInsertRepository::save,
                Comparator.comparing(EdgeEventEntity::getTs)
        );
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    /**
     * 功能：保存或创建`Async`。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        log.debug("Save edge event [{}] ", edgeEvent);
        if (edgeEvent.getId() == null) {
            UUID timeBased = Uuids.timeBased();
            edgeEvent.setId(new EdgeEventId(timeBased));
            edgeEvent.setCreatedTime(Uuids.unixTimestamp(timeBased));
        } else if (edgeEvent.getCreatedTime() == 0L) {
            UUID eventId = edgeEvent.getId().getId();
            if (eventId.version() == 1) {
                edgeEvent.setCreatedTime(Uuids.unixTimestamp(eventId));
            } else {
                edgeEvent.setCreatedTime(System.currentTimeMillis());
            }
        }
        if (StringUtils.isEmpty(edgeEvent.getUid())) {
            edgeEvent.setUid(edgeEvent.getId().toString());
        }
        EdgeEventEntity entity = new EdgeEventEntity(edgeEvent);
        createPartition(entity);
        return save(entity);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> save(EdgeEventEntity entity) {
        log.debug("Save edge event [{}] ", entity);
        if (entity.getTenantId() == null) {
            log.trace("Save system edge event with predefined id {}", systemTenantId);
            entity.setTenantId(systemTenantId);
        }
        if (entity.getUuid() == null) {
            entity.setUuid(Uuids.timeBased());
        }

        return addToQueue(entity);
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> addToQueue(EdgeEventEntity entity) {
        return queue.add(entity);
    }


    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `seqIdStart`：`seqIdStart` 参数。
     * - `seqIdEnd`：`seqIdEnd` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink) {
        List<SortOrder> sortOrders = new ArrayList<>();
        if (pageLink.getSortOrder() != null) {
            sortOrders.add(pageLink.getSortOrder());
        }
        sortOrders.add(new SortOrder("seqId"));
        return DaoUtil.toPageData(
                edgeEventRepository
                        .findEdgeEventsByTenantIdAndEdgeId(
                                tenantId,
                                edgeId.getId(),
                                pageLink.getTextSearch(),
                                pageLink.getStartTime(),
                                pageLink.getEndTime(),
                                seqIdStart,
                                seqIdEnd,
                                DaoUtil.toPageable(pageLink, sortOrders)));
    }

    /**
     * 功能：删除或清理`Events`。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * 返回：无。
     */
    @Override
    public void cleanupEvents(long ttl) {
        partitioningRepository.dropPartitionsBefore(TABLE_NAME, ttl, TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    /**
     * 功能：执行 `migrateEdgeEvents` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void migrateEdgeEvents() {
        long startTime = edgeEventsTtl > 0 ? System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(edgeEventsTtl) : 1629158400000L;

        long currentTime = System.currentTimeMillis();
        var partitionStepInMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        long numberOfPartitions = (currentTime - startTime) / partitionStepInMs;

        if (numberOfPartitions > 1000) {
            String error = "Please adjust your edge event partitioning configuration. Configuration with partition size " +
                    "of " + partitionSizeInHours + " hours and corresponding TTL will use " + numberOfPartitions + " " +
                    "(> 1000) partitions which is not recommended!";
            log.error(error);
            throw new RuntimeException(error);
        }

        while (startTime < currentTime) {
            var endTime = startTime + partitionStepInMs;
            log.info("Migrating edge event for time period: {} - {}", startTime, endTime);
            callMigrationFunction(startTime, endTime, partitionStepInMs);
            startTime = endTime;
        }
        log.info("Event edge migration finished");
        jdbcTemplate.execute("DROP TABLE IF EXISTS old_edge_event");
    }

    /**
     * 功能：执行 `callMigrationFunction` 对应的处理。
     * 参数：
     * - `startTime`：开始时间戳。
     * - `endTime`：结束时间戳。
     * - `partitionSIzeInMs`：分区标识或分区信息。
     * 返回：无。
     */
    private void callMigrationFunction(long startTime, long endTime, long partitionSIzeInMs) {
        jdbcTemplate.update("CALL migrate_edge_event(?, ?, ?)", startTime, endTime, partitionSIzeInMs);
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    @Override
    public void createPartition(EdgeEventEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

}
