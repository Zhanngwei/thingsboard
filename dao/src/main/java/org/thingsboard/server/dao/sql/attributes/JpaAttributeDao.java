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
package org.thingsboard.server.dao.sql.attributes;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `JpaAttributeDao` 是 ThingsBoard DAO 中负责属性存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDaoListeningExecutorService`、`AttributesDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@Slf4j
@SqlDao
public class JpaAttributeDao extends JpaAbstractDaoListeningExecutorService implements AttributesDao {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Autowired
    ScheduledLogExecutorComponent logExecutor;

    /**
     * 属性，用于读取或保存对应领域对象。
     */
    @Autowired
    private AttributeKvRepository attributeKvRepository;

    /**
     * 属性，用于读取或保存对应领域对象。
     */
    @Autowired
    private AttributeKvInsertRepository attributeKvInsertRepository;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    private StatsFactory statsFactory;

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    @Value("${sql.attributes.batch_size:1000}")
    private int batchSize;

    /**
     * 最大延迟，用于控制时间范围或等待时长。
     */
    @Value("${sql.attributes.batch_max_delay:100}")
    private long maxDelay;

    /**
     * 统计日志输出间隔，用于控制时间范围或等待时长。
     */
    @Value("${sql.attributes.stats_print_interval_ms:1000}")
    private long statsPrintIntervalMs;

    /**
     * 批处理线程数，用于控制处理规模或位置。
     */
    @Value("${sql.attributes.batch_threads:4}")
    private int batchThreads;

    /**
     * 是否启用`batch sort`。
     */
    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private TbSqlBlockingQueueWrapper<AttributeKvEntity> queue;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("Attributes")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("attributes")
                .batchSortEnabled(batchSortEnabled)
                .build();

        Function<AttributeKvEntity, Integer> hashcodeFunction = entity -> entity.getId().getEntityId().hashCode();
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, v -> attributeKvInsertRepository.saveOrUpdate(v),
                Comparator.comparing((AttributeKvEntity attributeKvEntity) -> attributeKvEntity.getId().getEntityId())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getEntityType().name())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeType())
                        .thenComparing(attributeKvEntity -> attributeKvEntity.getId().getAttributeKey())
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
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKey`：键。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, String attributeKey) {
        AttributeKvCompositeKey compositeKey =
                getAttributeKvCompositeKey(entityId, attributeType, attributeKey);
        return Optional.ofNullable(DaoUtil.getData(attributeKvRepository.findById(compositeKey)));
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKeys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<AttributeKvEntry> find(TenantId tenantId, EntityId entityId, String attributeType, Collection<String> attributeKeys) {
        List<AttributeKvCompositeKey> compositeKeys =
                attributeKeys
                        .stream()
                        .map(attributeKey ->
                                getAttributeKvCompositeKey(entityId, attributeType, attributeKey))
                        .collect(Collectors.toList());
        return DaoUtil.convertDataList(Lists.newArrayList(attributeKvRepository.findAllById(compositeKeys)));
    }

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<AttributeKvEntry> findAll(TenantId tenantId, EntityId entityId, String attributeType) {
        return DaoUtil.convertDataList(Lists.newArrayList(
                        attributeKvRepository.findAllByEntityTypeAndEntityIdAndAttributeType(
                                entityId.getEntityType(),
                                entityId.getId(),
                                attributeType)));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        if (deviceProfileId != null) {
            return attributeKvRepository.findAllKeysByDeviceProfileId(tenantId.getId(), deviceProfileId.getId());
        } else {
            return attributeKvRepository.findAllKeysByTenantId(tenantId.getId());
        }
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributeKvRepository
                .findAllKeysByEntityIds(entityType.name(), entityIds.stream().map(EntityId::getId).collect(Collectors.toList()));
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIdsAndAttributeType(TenantId tenantId, EntityType entityType, List<EntityId> entityIds, String attributeType) {
        return attributeKvRepository
                .findAllKeysByEntityIdsAndAttributeType(entityType.name(), entityIds.stream().map(EntityId::getId).collect(Collectors.toList()), attributeType);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attribute`：`attribute` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String attributeType, AttributeKvEntry attribute) {
        AttributeKvEntity entity = new AttributeKvEntity();
        entity.setId(new AttributeKvCompositeKey(entityId.getEntityType(), entityId.getId(), attributeType, attribute.getKey()));
        entity.setLastUpdateTs(attribute.getLastUpdateTs());
        entity.setStrValue(attribute.getStrValue().orElse(null));
        entity.setDoubleValue(attribute.getDoubleValue().orElse(null));
        entity.setLongValue(attribute.getLongValue().orElse(null));
        entity.setBooleanValue(attribute.getBooleanValue().orElse(null));
        entity.setJsonValue(attribute.getJsonValue().orElse(null));
        return addToQueue(entity);
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `entity`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<String> addToQueue(AttributeKvEntity entity) {
        return Futures.transform(queue.add(entity), v -> entity.getId().getAttributeKey(), MoreExecutors.directExecutor());
    }

    /**
     * 功能：删除或清理`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<ListenableFuture<String>> removeAll(TenantId tenantId, EntityId entityId, String attributeType, List<String> keys) {
        List<ListenableFuture<String>> futuresList = new ArrayList<>(keys.size());
        for (String key : keys) {
            futuresList.add(service.submit(() -> {
                attributeKvRepository.delete(entityId.getEntityType(), entityId.getId(), attributeType, key);
                return key;
            }));
        }
        return futuresList;
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKey`：键。
     * 返回：处理结果。
     */
    private AttributeKvCompositeKey getAttributeKvCompositeKey(EntityId entityId, String attributeType, String attributeKey) {
        return new AttributeKvCompositeKey(
                entityId.getEntityType(),
                entityId.getId(),
                attributeType,
                attributeKey);
    }
}
