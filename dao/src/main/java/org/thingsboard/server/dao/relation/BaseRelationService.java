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
package org.thingsboard.server.dao.relation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.sql.relation.JpaRelationQueryExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by ashvayka on 28.04.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseRelationService` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Graph Query。
 */
@Service
@Slf4j
public class BaseRelationService implements RelationService {

    /**
     * 关系，用于读取或保存对应领域对象。
     */
    private final RelationDao relationDao;
    private final EntityService entityService;
    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    private final TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache;
    private final ApplicationEventPublisher eventPublisher;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final JpaExecutorService executor;
    private final JpaRelationQueryExecutorService relationsExecutor;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    protected ScheduledExecutorService timeoutExecutorService;

    /**
     * 关系，表示当前对象的对应属性。
     */
    @Value("${sql.relations.query_timeout:20}")
    private Integer relationQueryTimeout;

    /**
     * 功能：创建 `BaseRelationService` 实例，并初始化必要字段。
     * 参数：
     * - `relationDao`：`relationDao` 参数。
     * - `entityService`：服务对象。
     * - `cache`：`cache` 参数。
     * - `eventPublisher`：`eventPublisher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public BaseRelationService(RelationDao relationDao, @Lazy EntityService entityService,
                               TbTransactionalCache<RelationCacheKey, RelationCacheValue> cache,
                               ApplicationEventPublisher eventPublisher, JpaExecutorService executor,
                               JpaRelationQueryExecutorService relationsExecutor) {
        this.relationDao = relationDao;
        this.entityService = entityService;
        this.cache = cache;
        this.eventPublisher = eventPublisher;
        this.executor = executor;
        this.relationsExecutor = relationsExecutor;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("relations-query-timeout"));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = EntityRelationEvent.class)
    public void handleEvictEvent(EntityRelationEvent event) {
        List<RelationCacheKey> keys = new ArrayList<>(5);
        keys.add(new RelationCacheKey(event.getFrom(), event.getTo(), event.getType(), event.getTypeGroup()));
        keys.add(new RelationCacheKey(event.getFrom(), null, event.getType(), event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(event.getFrom(), null, null, event.getTypeGroup(), EntitySearchDirection.FROM));
        keys.add(new RelationCacheKey(null, event.getTo(), event.getType(), event.getTypeGroup(), EntitySearchDirection.TO));
        keys.add(new RelationCacheKey(null, event.getTo(), null, event.getTypeGroup(), EntitySearchDirection.TO));
        cache.evict(keys);
    }

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    @Override
    public ListenableFuture<Boolean> checkRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelationAsync(tenantId, from, to, relationType, typeGroup);
    }

    /**
     * 功能：校验关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    @Override
    public boolean checkRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing checkRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        return relationDao.checkRelation(tenantId, from, to, relationType, typeGroup);
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public EntityRelation getRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        RelationCacheKey cacheKey = new RelationCacheKey(from, to, relationType, typeGroup);
        return cache.getAndPutInTransaction(cacheKey,
                () -> {
                    log.trace("FETCH EntityRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
                    return relationDao.getRelation(tenantId, from, to, relationType, typeGroup);
                },
                RelationCacheValue::getRelation,
                relations -> RelationCacheValue.builder().relation(relations).build(), false);
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean saveRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelation [{}]", relation);
        validate(relation);
        var result = relationDao.saveRelation(tenantId, relation);
        publishEvictEvent(EntityRelationEvent.from(relation));
        eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_ADD_OR_UPDATE));
        return result;
    }

    /**
     * 功能：保存或创建`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relations`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveRelations(TenantId tenantId, List<EntityRelation> relations) {
        log.trace("Executing saveRelations [{}]", relations);
        for (EntityRelation relation : relations) {
            validate(relation);
        }
        for (List<EntityRelation> partition : Lists.partition(relations, 1024)) {
            relationDao.saveRelations(tenantId, partition);
        }
        for (EntityRelation relation : relations) {
            publishEvictEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_ADD_OR_UPDATE));
        }
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> saveRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing saveRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.saveRelationAsync(tenantId, relation);
        future.addListener(() -> {
            handleEvictEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_ADD_OR_UPDATE));
        }, MoreExecutors.directExecutor());
        return future;
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing DeleteRelation [{}]", relation);
        validate(relation);
        var result = relationDao.deleteRelation(tenantId, relation);
        //TODO: evict cache only if the relation was deleted. Note: relationDao.deleteRelation requires improvement.
        publishEvictEvent(EntityRelationEvent.from(relation));
        eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_DELETED));
        return result;
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityRelation relation) {
        log.trace("Executing deleteRelationAsync [{}]", relation);
        validate(relation);
        var future = relationDao.deleteRelationAsync(tenantId, relation);
        future.addListener(() -> {
            handleEvictEvent(EntityRelationEvent.from(relation));
            eventPublisher.publishEvent(new RelationActionEvent(tenantId, relation, ActionType.RELATION_DELETED));
        }, MoreExecutors.directExecutor());
        return future;
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    @Override
    public boolean deleteRelation(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelation [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var result = relationDao.deleteRelation(tenantId, from, to, relationType, typeGroup);
        //TODO: evict cache only if the relation was deleted. Note: relationDao.deleteRelation requires improvement.
        EntityRelation entityRelation = new EntityRelation(from, to, relationType, typeGroup);
        publishEvictEvent(EntityRelationEvent.from(entityRelation));
        eventPublisher.publishEvent(new RelationActionEvent(tenantId, entityRelation, ActionType.RELATION_DELETED));
        return result;
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> deleteRelationAsync(TenantId tenantId, EntityId from, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing deleteRelationAsync [{}][{}][{}][{}]", from, to, relationType, typeGroup);
        validate(from, to, relationType, typeGroup);
        var future = relationDao.deleteRelationAsync(tenantId, from, to, relationType, typeGroup);
        EntityRelationEvent event = new EntityRelationEvent(from, to, relationType, typeGroup);
        future.addListener(() -> handleEvictEvent(event), MoreExecutors.directExecutor());
        return future;
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Transactional
    @Override
    public void deleteEntityCommonRelations(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId, RelationTypeGroup.COMMON);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Transactional
    @Override
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        deleteEntityRelations(tenantId, entityId, null);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `relationTypeGroup`：类型。
     * 返回：无。
     */
    @Transactional
    public void deleteEntityRelations(TenantId tenantId, EntityId entityId, RelationTypeGroup relationTypeGroup) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        validate(entityId);
        List<EntityRelation> inboundRelations = relationTypeGroup == null
                    ? relationDao.findAllByTo(tenantId, entityId)
                    : relationDao.findAllByTo(tenantId, entityId, relationTypeGroup);
        List<EntityRelation> outboundRelations = relationTypeGroup == null
                    ? relationDao.findAllByFrom(tenantId, entityId)
                    : relationDao.findAllByFrom(tenantId, entityId, relationTypeGroup);

        if (!inboundRelations.isEmpty()) {
            try {
                if (relationTypeGroup == null) {
                    relationDao.deleteInboundRelations(tenantId, entityId);
                } else {
                    relationDao.deleteInboundRelations(tenantId, entityId, relationTypeGroup);
                }
            } catch (ConcurrencyFailureException e) {
                log.debug("Concurrency exception while deleting relations [{}]", inboundRelations, e);
            }

            for (EntityRelation relation : inboundRelations) {
                eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            }
        }

        if (!outboundRelations.isEmpty()) {
            if (relationTypeGroup == null) {
                relationDao.deleteOutboundRelations(tenantId, entityId);
            } else {
                relationDao.deleteOutboundRelations(tenantId, entityId, relationTypeGroup);
            }

            for (EntityRelation relation : outboundRelations) {
                eventPublisher.publishEvent(EntityRelationEvent.from(relation));
            }
        }
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relations`：数据列表。
     * - `deleteFromDb`：`deleteFromDb` 参数。
     * 返回：匹配的数据集合。
     */
    private List<ListenableFuture<Boolean>> deleteRelationGroupsAsync(TenantId tenantId, List<List<EntityRelation>> relations, boolean deleteFromDb) {
        List<ListenableFuture<Boolean>> results = new ArrayList<>();
        for (List<EntityRelation> relationList : relations) {
            relationList.forEach(relation -> results.add(deleteAsync(tenantId, relation, deleteFromDb)));
        }
        return results;
    }

    /**
     * 功能：删除或清理`Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * - `deleteFromDb`：`deleteFromDb` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Boolean> deleteAsync(TenantId tenantId, EntityRelation relation, boolean deleteFromDb) {
        if (deleteFromDb) {
            return Futures.transform(relationDao.deleteRelationAsync(tenantId, relation),
                    bool -> {
                        handleEvictEvent(EntityRelationEvent.from(relation));
                        return bool;
                    }, MoreExecutors.directExecutor());
        } else {
            handleEvictEvent(EntityRelationEvent.from(relation));
            return Futures.immediateFuture(false);
        }
    }

    /**
     * 功能：获取`By From`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        validate(from);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFrom(tenantId, from, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    /**
     * 功能：获取`By From Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAsync(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);

        var cacheValue = cache.get(RelationCacheKey.builder().from(from).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build());

        if (cacheValue != null && cacheValue.get() != null) {
            return Futures.immediateFuture(cacheValue.get().getRelations());
        } else {
            //Disabled cache put for the async requests due to limitations of the cache implementation (Redis lib does not support thread-safe transactions)
            return executor.submit(() -> findByFrom(tenantId, from, typeGroup));
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByFrom(TenantId tenantId, EntityId from, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByFrom [{}][{}]", from, typeGroup);
        validate(from);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = executor.submit(() -> relationDao.findAllByFrom(tenantId, from, typeGroup));
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getTo,
                                    EntityRelationInfo::setToName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findByFromAndType(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        RelationCacheKey cacheKey = RelationCacheKey.builder().from(from).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.FROM).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByFromAndType(tenantId, from, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `from`：`from` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelation>> findByFromAndTypeAsync(TenantId tenantId, EntityId from, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByFromAndType [{}][{}][{}]", from, relationType, typeGroup);
        validate(from);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByFromAndType(tenantId, from, relationType, typeGroup));
    }

    /**
     * 功能：获取`By To`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        validate(to);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByTo(tenantId, to, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    /**
     * 功能：获取`By To Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelation>> findByToAsync(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAsync [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByTo(tenantId, to, typeGroup));
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByTo(TenantId tenantId, EntityId to, RelationTypeGroup typeGroup) {
        log.trace("Executing findInfoByTo [{}][{}]", to, typeGroup);
        validate(to);
        validateTypeGroup(typeGroup);
        ListenableFuture<List<EntityRelation>> relations = findByToAsync(tenantId, to, typeGroup);
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    EntityRelation::getFrom,
                                    EntityRelationInfo::setFromName))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * - `entityIdGetter`：实体对象。
     * - `entityNameSetter`：实体对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<EntityRelationInfo> fetchRelationInfoAsync(TenantId tenantId, EntityRelation relation,
                                                                        Function<EntityRelation, EntityId> entityIdGetter,
                                                                        BiConsumer<EntityRelationInfo, String> entityNameSetter) {
        EntityRelationInfo relationInfo = new EntityRelationInfo(relation);
        entityNameSetter.accept(relationInfo,
                entityService.fetchEntityName(tenantId, entityIdGetter.apply(relation)).orElse("N/A"));
        return Futures.immediateFuture(relationInfo);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findByToAndType(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndType [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        RelationCacheKey cacheKey = RelationCacheKey.builder().to(to).type(relationType).typeGroup(typeGroup).direction(EntitySearchDirection.TO).build();
        return cache.getAndPutInTransaction(cacheKey,
                () -> relationDao.findAllByToAndType(tenantId, to, relationType, typeGroup),
                RelationCacheValue::getRelations,
                relations -> RelationCacheValue.builder().relations(relations).build(), false);

    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `to`：`to` 参数。
     * - `relationType`：类型。
     * - `typeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelation>> findByToAndTypeAsync(TenantId tenantId, EntityId to, String relationType, RelationTypeGroup typeGroup) {
        log.trace("Executing findByToAndTypeAsync [{}][{}][{}]", to, relationType, typeGroup);
        validate(to);
        validateType(relationType);
        validateTypeGroup(typeGroup);
        return executor.submit(() -> findByToAndType(tenantId, to, relationType, typeGroup));
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelation>> findByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findByQuery [{}]", query);
        RelationsSearchParameters params = query.getParameters();
        final List<RelationEntityTypeFilter> filters = query.getFilters();
        if (filters == null || filters.isEmpty()) {
            log.debug("Filters are not set [{}]", query);
        }

        int maxLvl = params.getMaxLevel() > 0 ? params.getMaxLevel() : Integer.MAX_VALUE;

        try {
            ListenableFuture<Set<EntityRelation>> relationSet = findRelationsRecursively(tenantId, params.getEntityId(), params.getDirection(),
                    params.getRelationTypeGroup(), maxLvl, params.isFetchLastLevelOnly(), new ConcurrentHashMap<>());
            return Futures.transform(relationSet, input -> {
                List<EntityRelation> relations = new ArrayList<>();
                if (filters == null || filters.isEmpty()) {
                    relations.addAll(input);
                    return relations;
                }
                for (EntityRelation relation : input) {
                    if (matchFilters(filters, relation, params.getDirection())) {
                        relations.add(relation);
                    }
                }
                return relations;
            }, MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.warn("Failed to query relations: [{}]", query, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntityRelationInfo>> findInfoByQuery(TenantId tenantId, EntityRelationsQuery query) {
        log.trace("Executing findInfoByQuery [{}]", query);
        ListenableFuture<List<EntityRelation>> relations = findByQuery(tenantId, query);
        EntitySearchDirection direction = query.getParameters().getDirection();
        return Futures.transformAsync(relations,
                relations1 -> {
                    List<ListenableFuture<EntityRelationInfo>> futures = new ArrayList<>();
                    relations1.forEach(relation ->
                            futures.add(fetchRelationInfoAsync(tenantId, relation,
                                    relation2 -> direction == EntitySearchDirection.FROM ? relation2.getTo() : relation2.getFrom(),
                                    (EntityRelationInfo relationInfo, String entityName) -> {
                                        if (direction == EntitySearchDirection.FROM) {
                                            relationInfo.setToName(entityName);
                                        } else {
                                            relationInfo.setFromName(entityName);
                                        }
                                    }))
                    );
                    return Futures.successfulAsList(futures);
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：删除或清理`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void removeRelations(TenantId tenantId, EntityId entityId) {
        log.trace("removeRelations {}", entityId);

        List<EntityRelation> relations = new ArrayList<>();
        for (RelationTypeGroup relationTypeGroup : RelationTypeGroup.values()) {
            relations.addAll(findByFrom(tenantId, entityId, relationTypeGroup));
            relations.addAll(findByTo(tenantId, entityId, relationTypeGroup));
        }

        for (EntityRelation relation : relations) {
            deleteRelation(tenantId, relation);
        }
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ruleChainType`：类型。
     * - `limit`：数量限制。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityRelation> findRuleNodeToRuleChainRelations(TenantId tenantId, RuleChainType ruleChainType, int limit) {
        log.trace("Executing findRuleNodeToRuleChainRelations, tenantId [{}], ruleChainType {} and limit {}", tenantId, ruleChainType, limit);
        validateId(tenantId, "Invalid tenant id: " + tenantId);
        return relationDao.findRuleNodeToRuleChainRelations(ruleChainType, limit);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    protected void validate(EntityRelation relation) {
        if (relation == null) {
            throw new DataValidationException("Relation type should be specified!");
        }
        ConstraintValidator.validateFields(relation);
        validate(relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `from`：`from` 参数。
     * - `to`：`to` 参数。
     * - `type`：类型。
     * - `typeGroup`：类型。
     * 返回：无。
     */
    protected void validate(EntityId from, EntityId to, String type, RelationTypeGroup typeGroup) {
        validateType(type);
        validateTypeGroup(typeGroup);
        if (from == null) {
            throw new DataValidationException("Relation should contain from entity!");
        }
        if (to == null) {
            throw new DataValidationException("Relation should contain to entity!");
        }
    }

    /**
     * 功能：校验类型。
     * 参数：
     * - `type`：类型。
     * 返回：无。
     */
    private void validateType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new DataValidationException("Relation type should be specified!");
        }
    }

    /**
     * 功能：校验类型。
     * 参数：
     * - `typeGroup`：类型。
     * 返回：无。
     */
    private void validateTypeGroup(RelationTypeGroup typeGroup) {
        if (typeGroup == null) {
            throw new DataValidationException("Relation type group should be specified!");
        }
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    protected void validate(EntityId entity) {
        if (entity == null) {
            throw new DataValidationException("Entity should be specified!");
        }
    }

    /**
     * 功能：执行 `matchFilters` 对应的处理。
     * 参数：
     * - `filters`：数据列表。
     * - `relation`：`relation` 参数。
     * - `direction`：`direction` 参数。
     * 返回：判断结果。
     */
    private boolean matchFilters(List<RelationEntityTypeFilter> filters, EntityRelation relation, EntitySearchDirection direction) {
        for (RelationEntityTypeFilter filter : filters) {
            if (match(filter, relation, direction)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `match` 对应的处理。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `relation`：`relation` 参数。
     * - `direction`：`direction` 参数。
     * 返回：判断结果。
     */
    private boolean match(RelationEntityTypeFilter filter, EntityRelation relation, EntitySearchDirection direction) {
        if (StringUtils.isEmpty(filter.getRelationType()) || filter.getRelationType().equals(relation.getType())) {
            if (filter.getEntityTypes() == null || filter.getEntityTypes().isEmpty()) {
                return true;
            } else {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                return filter.getEntityTypes().contains(entityId.getEntityType());
            }
        } else {
            return false;
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`RelationQueueCtx` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
     * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Service / Repository / Graph Query。
     */
    @RequiredArgsConstructor
    private static class RelationQueueCtx {
        final SettableFuture<Set<EntityRelation>> future = SettableFuture.create();
        final Set<EntityRelation> result = ConcurrentHashMap.newKeySet();
        final Queue<RelationTask> tasks = new ConcurrentLinkedQueue<>();

        /**
         * 租户ID，用于定位对应业务对象。
         */
        final TenantId tenantId;
        final EntitySearchDirection direction;
        /**
         * 关系，用于区分不同处理分支。
         */
        final RelationTypeGroup relationTypeGroup;
        final boolean fetchLastLevelOnly;
        /**
         * `maxLvl` 字段，保存当前对象的对应属性。
         */
        final int maxLvl;
        final ConcurrentHashMap<EntityId, Boolean> uniqueMap;

    }

    /**
     * 中文说明：
     * 1. 类目的：`RelationTask` 是 ThingsBoard DAO 模块 中的实体与关系持久化服务类型，用于维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
     * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
     * 3. 协作对象：主要协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
     * 4. 生命周期：由实体管理、关系维护、规则链元数据读取或查询 API 调用，随单次业务事务完成。
     * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
     * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
     * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
     * 8. 设计模式：主要体现 Service / Repository / Graph Query。
     */
    @RequiredArgsConstructor
    private static class RelationTask {
        /**
         * `currentLvl` 字段，保存当前对象的对应属性。
         */
        private final int currentLvl;
        private final EntityId root;
        /**
         * `prevRelations`列表，用于保存一组待处理对象。
         */
        private final List<EntityRelation> prevRelations;
    }

    /**
     * 功能：处理队列。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void processQueue(RelationQueueCtx ctx) {
        RelationTask task = ctx.tasks.poll();
        while (task != null) {
            List<EntityRelation> relations = findRelations(ctx.tenantId, task.root, ctx.direction, ctx.relationTypeGroup);
            Map<EntityId, List<EntityRelation>> newChildrenRelations = new HashMap<>();
            for (EntityRelation childRelation : relations) {
                log.trace("Found Relation: {}", childRelation);
                EntityId childId = ctx.direction == EntitySearchDirection.FROM ? childRelation.getTo() : childRelation.getFrom();
                if (ctx.uniqueMap.putIfAbsent(childId, Boolean.TRUE) == null) {
                    log.trace("Adding Relation: {}", childId);
                    newChildrenRelations.put(childId, new ArrayList<>());
                }
                if (ctx.fetchLastLevelOnly) {
                    var list = newChildrenRelations.get(childId);
                    if (list != null) {
                        list.add(childRelation);
                    }
                }
            }
            if (ctx.fetchLastLevelOnly) {
                if (relations.isEmpty()) {
                    ctx.result.addAll(task.prevRelations);
                } else if (task.currentLvl == ctx.maxLvl) {
                    ctx.result.addAll(relations);
                }
            } else {
                ctx.result.addAll(relations);
            }
            var finalTask = task;
            newChildrenRelations.forEach((child, childRelations) -> {
                var newLvl = finalTask.currentLvl + 1;
                if (newLvl <= ctx.maxLvl)
                    ctx.tasks.add(new RelationTask(newLvl, child, childRelations));
            });
            task = ctx.tasks.poll();
        }
        ctx.future.set(ctx.result);
    }

    /**
     * 功能：获取`Relations Recursively`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rootId`：`rootId`ID。
     * - `direction`：`direction` 参数。
     * - `relationTypeGroup`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Set<EntityRelation>> findRelationsRecursively(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction,
                                                                           RelationTypeGroup relationTypeGroup, int lvl, boolean fetchLastLevelOnly,
                                                                           final ConcurrentHashMap<EntityId, Boolean> uniqueMap) {
        if (lvl == 0) {
            return Futures.immediateFuture(Collections.emptySet());
        }
        var relationQueueCtx = new RelationQueueCtx(tenantId, direction, relationTypeGroup, fetchLastLevelOnly, lvl, uniqueMap);
        relationQueueCtx.tasks.add(new RelationTask(1, rootId, Collections.emptyList()));
        relationsExecutor.submit(() -> processQueue(relationQueueCtx));
        return Futures.withTimeout(relationQueueCtx.future, relationQueryTimeout, TimeUnit.SECONDS, timeoutExecutorService);
    }


    /**
     * 功能：获取`Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rootId`：`rootId`ID。
     * - `direction`：`direction` 参数。
     * - `relationTypeGroup`：类型。
     * 返回：匹配的数据集合。
     */
    private List<EntityRelation> findRelations(final TenantId tenantId, final EntityId rootId, final EntitySearchDirection direction, RelationTypeGroup relationTypeGroup) {
        List<EntityRelation> relations;
        if (relationTypeGroup == null) {
            relationTypeGroup = RelationTypeGroup.COMMON;
        }
        if (direction == EntitySearchDirection.FROM) {
            relations = findByFrom(tenantId, rootId, relationTypeGroup);
        } else {
            relations = findByTo(tenantId, rootId, relationTypeGroup);
        }
        return relations;
    }

    /**
     * 功能：发送或提交事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    private void publishEvictEvent(EntityRelationEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseRelationService` 在 ThingsBoard DAO 模块 中承担实体与关系持久化服务类型职责，核心目的是维护资产、实体视图、实体索引、关系图和实体查询的持久化访问路径。
 * 2. 核心流程：解析实体范围和关系方向，调用 DAO 查询或更新数据库，并在变更后同步缓存和事件。
 * 3. 关键依赖：主要依赖或协作对象包括EntityService、RelationService、AssetService、Rule Engine、缓存和查询服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
