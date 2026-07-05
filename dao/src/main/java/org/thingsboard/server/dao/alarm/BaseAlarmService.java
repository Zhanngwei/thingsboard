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
package org.thingsboard.server.dao.alarm;


import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmModificationRequest;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ApiUsageLimitsExceededException;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`BaseAlarmService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service("AlarmDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseAlarmService extends AbstractCachedEntityService<TenantId, PageData<EntitySubtype>, AlarmTypesCacheEvictEvent> implements AlarmService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private static final PageLink DEFAULT_ALARM_TYPES_PAGE_LINK = new PageLink(25, 0, null, new SortOrder("type"));

    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    /**
     * 实体，提供当前类调用的业务操作。
     */
    private final EntityService entityService;
    private final DataValidator<Alarm> alarmDataValidator;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = AlarmTypesCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(AlarmTypesCacheEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        validateAlarmRequest(request);
        AlarmApiCallResult result = withPropagated(alarmDao.updateAlarm(request));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getAlarm().getTenantId()).entity(result)
                    .entityId(result.getAlarm().getId()).build());
        }
        return result;
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request) {
        return createAlarm(request, true);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * - `alarmCreationEnabled`：`alarmCreationEnabled` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult createAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled) {
        validateAlarmRequest(request);
        CustomerId customerId = entityService.fetchEntityCustomerId(request.getTenantId(), request.getOriginator()).orElse(null);
        if (customerId == null && request.getCustomerId() != null) {
            throw new DataValidationException("Can't assign alarm to customer. Originator is not assigned to customer!");
        } else if (customerId != null && request.getCustomerId() != null && !customerId.equals(request.getCustomerId())) {
            throw new DataValidationException("Can't assign alarm to customer. Originator belongs to different customer!");
        }
        request.setCustomerId(customerId);
        AlarmApiCallResult result = alarmDao.createOrUpdateActiveAlarm(request, alarmCreationEnabled);
        if (!result.isSuccessful() && !alarmCreationEnabled) {
            throw new ApiUsageLimitsExceededException("Alarms creation is disabled");
        }
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getAlarm().getTenantId())
                    .entityId(result.getAlarm().getId()).entity(result).created(true).build());
            publishEvictEvent(new AlarmTypesCacheEvictEvent(request.getTenantId()));
        }
        return withPropagated(result);
    }

    /**
     * 功能：执行 `acknowledgeAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `ackTs`：时间戳。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId alarmId, long ackTs) {
        var result = withPropagated(alarmDao.acknowledgeAlarm(tenantId, alarmId, ackTs));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_ACK).build());
        }
        return result;
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `clearTs`：时间戳。
     * - `details`：`details` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId alarmId, long clearTs, JsonNode details) {
        var result = withPropagated(alarmDao.clearAlarm(tenantId, alarmId, clearTs, details));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_CLEAR).build());
        }
        return result;
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        return alarmDao.findLatestActiveByOriginatorAndType(tenantId, originator, type);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * - `orderedEntityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId,
                                                               AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateEntityDataPageLink(query.getPageLink());
        return alarmDao.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    /**
     * 功能：执行 `delAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：键值映射结果。
     */
    @Override
    @Transactional
    public AlarmApiCallResult delAlarm(TenantId tenantId, AlarmId alarmId) {
        return delAlarm(tenantId, alarmId, true);
    }

    /**
     * 功能：执行 `delAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `checkAndDeleteAlarmType`：类型。
     * 返回：键值映射结果。
     */
    @Override
    @Transactional
    public AlarmApiCallResult delAlarm(TenantId tenantId, AlarmId alarmId, boolean checkAndDeleteAlarmType) {
        log.debug("Deleting Alarm Id: {}", alarmId);
        AlarmInfo alarm = alarmDao.findAlarmInfoById(tenantId, alarmId.getId());
        if (alarm == null) {
            return AlarmApiCallResult.builder().successful(false).build();
        } else {
            var propagationIds = getPropagationEntityIdsList(alarm);
            deleteEntityRelations(tenantId, alarm.getId());
            alarmDao.removeById(tenantId, alarm.getUuidId());
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId)
                    .entityId(alarmId).entity(alarm).build());
            if (checkAndDeleteAlarmType) {
                delAlarmTypes(tenantId, Collections.singleton(alarm.getType()));
            }
            return AlarmApiCallResult.builder().alarm(alarm).deleted(true).successful(true).propagatedEntitiesList(propagationIds).build();
        }
    }

    /**
     * 功能：执行 `delAlarmTypes` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `types`：类型。
     * 返回：无。
     */
    @Override
    @Transactional
    public void delAlarmTypes(TenantId tenantId, Set<String> types) {
        if (!types.isEmpty() && alarmDao.removeAlarmTypesIfNoAlarmsPresent(tenantId.getId(), types)) {
            publishEvictEvent(new AlarmTypesCacheEvictEvent(tenantId));
        }
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：匹配的数据集合。
     */
    private List<EntityId> createEntityAlarmRecords(Alarm alarm) throws ExecutionException, InterruptedException {
        Set<EntityId> propagatedEntitiesSet = new LinkedHashSet<>();
        propagatedEntitiesSet.add(alarm.getOriginator());
        if (alarm.isPropagate()) {
            propagatedEntitiesSet.addAll(getRelatedEntities(alarm));
        }
        if (alarm.isPropagateToOwner()) {
            propagatedEntitiesSet.add(alarm.getCustomerId() != null ? alarm.getCustomerId() : alarm.getTenantId());
        }
        if (alarm.isPropagateToTenant()) {
            propagatedEntitiesSet.add(alarm.getTenantId());
        }
        for (EntityId entityId : propagatedEntitiesSet) {
            createEntityAlarmRecord(alarm.getTenantId(), entityId, alarm);
        }
        return new ArrayList<>(propagatedEntitiesSet);
    }

    /**
     * 功能：获取`Related Entities`。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：匹配的数据集合。
     */
    private Set<EntityId> getRelatedEntities(Alarm alarm) throws InterruptedException, ExecutionException {
        EntityRelationsQuery query = new EntityRelationsQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(alarm.getOriginator(), EntitySearchDirection.TO, Integer.MAX_VALUE, false);
        query.setParameters(parameters);
        List<String> propagateRelationTypes = alarm.getPropagateRelationTypes();
        Stream<EntityRelation> relations = relationService.findByQuery(alarm.getTenantId(), query).get().stream();
        if (!CollectionUtils.isEmpty(propagateRelationTypes)) {
            relations = relations.filter(entityRelation -> propagateRelationTypes.contains(entityRelation.getType()));
        }
        return relations.map(EntityRelation::getFrom).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `assigneeId`：`assigneeId`ID。
     * - `assignTime`：`assignTime` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId alarmId, UserId assigneeId, long assignTime) {
        var result = withPropagated(alarmDao.assignAlarm(tenantId, alarmId, assigneeId, assignTime));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_ASSIGNED).build());
        }
        return result;
    }

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `unassignTime`：`unassignTime` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId alarmId, long unassignTime) {
        var result = withPropagated(alarmDao.unassignAlarm(tenantId, alarmId, unassignTime));
        if (result.getAlarm() != null) {
            eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(result.getAlarm().getId())
                    .actionType(ActionType.ALARM_UNASSIGNED).build());
        }
        return result;
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public Alarm findAlarmById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmById [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmById(tenantId, alarmId.getId());
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmByIdAsync(tenantId, alarmId.getId());
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, AlarmId alarmId) {
        log.trace("Executing findAlarmInfoByIdAsync [{}]", alarmId);
        validateId(alarmId, "Incorrect alarmId " + alarmId);
        return alarmDao.findAlarmInfoById(tenantId, alarmId.getId());
    }

    /**
     * 功能：获取`Alarms`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        return alarmDao.findAlarms(tenantId, query);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        return alarmDao.findCustomerAlarms(tenantId, customerId, query);
    }

    /**
     * 功能：获取`Alarms V2`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        return alarmDao.findAlarmsV2(tenantId, query);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        return alarmDao.findCustomerAlarmsV2(tenantId, customerId, query);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmId> findAlarmIdsByAssigneeId(TenantId tenantId, UserId userId, PageLink pageLink) {
        log.trace("[{}] Executing findAlarmIdsByAssigneeId [{}]", tenantId, userId);
        validateId(userId, "Incorrect userId " + userId);
        return alarmDao.findAlarmIdsByAssigneeId(tenantId, userId.getId(), pageLink);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originatorId`：`originatorId`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmId> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, PageLink pageLink) {
        log.trace("[{}] Executing findAlarmsByOriginatorId [{}]", tenantId, originatorId);
        return alarmDao.findAlarmIdsByOriginatorId(tenantId, originatorId, pageLink);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarmSearchStatus`：`alarmSearchStatus` 参数。
     * - `alarmStatus`：`alarmStatus` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public AlarmSeverity findHighestAlarmSeverity(TenantId tenantId, EntityId entityId, AlarmSearchStatus alarmSearchStatus,
                                                  AlarmStatus alarmStatus, String assigneeId) {
        AlarmStatusFilter asf;
        if (alarmSearchStatus != null) {
            asf = AlarmStatusFilter.from(alarmSearchStatus);
        } else if (alarmStatus != null) {
            asf = AlarmStatusFilter.from(alarmStatus);
        } else {
            asf = AlarmStatusFilter.empty();
        }

        Set<AlarmSeverity> alarmSeverities = alarmDao.findAlarmSeverities(tenantId, entityId, asf, assigneeId);
        return alarmSeverities.stream().min(AlarmSeverity::compareTo).orElse(null);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void deleteEntityAlarmRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityAlarms [{}]", entityId);
        alarmDao.deleteEntityAlarmRecords(tenantId, entityId);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteEntityAlarmRecordsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEntityAlarmRecordsByTenantId [{}]", tenantId);
        alarmDao.deleteEntityAlarmRecordsByTenantId(tenantId);
    }

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, AlarmCountQuery query) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return alarmDao.countAlarmsByQuery(tenantId, customerId, query);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntitySubtype> findAlarmTypesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAlarmTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        if (DEFAULT_ALARM_TYPES_PAGE_LINK.equals(pageLink)) {
            return cache.getAndPutInTransaction(tenantId, () ->
                    alarmDao.findTenantAlarmTypes(tenantId.getId(), pageLink), false);
        }
        return alarmDao.findTenantAlarmTypes(tenantId.getId(), pageLink);
    }

    /**
     * 功能：执行 `merge` 对应的处理。
     * 参数：
     * - `existing`：`existing` 参数。
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    private Alarm merge(Alarm existing, Alarm alarm) {
        if (alarm.getStartTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getStartTs());
        }
        if (alarm.getEndTs() > existing.getEndTs()) {
            existing.setEndTs(alarm.getEndTs());
        }
        if (alarm.getClearTs() > existing.getClearTs()) {
            existing.setClearTs(alarm.getClearTs());
        }
        if (alarm.getAckTs() > existing.getAckTs()) {
            existing.setAckTs(alarm.getAckTs());
        }
        if (alarm.getAssignTs() > existing.getAssignTs()) {
            existing.setAssignTs(alarm.getAssignTs());
        }
        existing.setAcknowledged(alarm.isAcknowledged());
        existing.setCleared(alarm.isCleared());
        existing.setSeverity(alarm.getSeverity());
        existing.setDetails(alarm.getDetails());
        existing.setCustomerId(alarm.getCustomerId());
        existing.setAssigneeId(alarm.getAssigneeId());
        existing.setPropagate(existing.isPropagate() || alarm.isPropagate());
        existing.setPropagateToOwner(existing.isPropagateToOwner() || alarm.isPropagateToOwner());
        existing.setPropagateToTenant(existing.isPropagateToTenant() || alarm.isPropagateToTenant());
        List<String> existingPropagateRelationTypes = existing.getPropagateRelationTypes();
        List<String> newRelationTypes = alarm.getPropagateRelationTypes();
        if (!CollectionUtils.isEmpty(newRelationTypes)) {
            if (!CollectionUtils.isEmpty(existingPropagateRelationTypes)) {
                existing.setPropagateRelationTypes(Stream.concat(existingPropagateRelationTypes.stream(), newRelationTypes.stream())
                        .distinct()
                        .collect(Collectors.toList()));
            } else {
                existing.setPropagateRelationTypes(newRelationTypes);
            }
        }
        return existing;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：匹配的数据集合。
     */
    private List<EntityId> getPropagationEntityIdsList(Alarm alarm) {
        return new ArrayList<>(getPropagationEntityIds(alarm));
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：匹配的数据集合。
     */
    private Set<EntityId> getPropagationEntityIds(Alarm alarm) {
        if (alarm.isPropagate() || alarm.isPropagateToOwner() || alarm.isPropagateToTenant()) {
            List<EntityAlarm> entityAlarms = alarmDao.findEntityAlarmRecords(alarm.getTenantId(), alarm.getId());
            return entityAlarms.stream().map(EntityAlarm::getEntityId).collect(Collectors.toSet());
        } else {
            return Collections.singleton(alarm.getOriginator());
        }
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * 返回：无。
     */
    private void createEntityAlarmRecord(TenantId tenantId, EntityId entityId, Alarm alarm) {
        EntityAlarm entityAlarm = new EntityAlarm(tenantId, entityId, alarm.getCreatedTime(), alarm.getType(), alarm.getCustomerId(), null, alarm.getId());
        try {
            alarmDao.createEntityAlarmRecord(entityAlarm);
        } catch (Exception e) {
            log.warn("[{}] Failed to create entity alarm record: {}", tenantId, entityAlarm, e);
        }
    }

    /**
     * 功能：获取`And Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `function`：`function` 参数。
     * 返回：处理结果。
     */
    private <T> T getAndUpdate(TenantId tenantId, AlarmId alarmId, Function<Alarm, T> function) {
        validateId(alarmId, "Alarm id should be specified!");
        Alarm entity = alarmDao.findAlarmById(tenantId, alarmId.getId());
        return function.apply(entity);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAlarmById(tenantId, new AlarmId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

    //TODO: refactor to use efficient caching.
    /**
     * 功能：执行 `withPropagated` 对应的处理。
     * 参数：
     * - `result`：键值映射。
     * 返回：键值映射结果。
     */
    private AlarmApiCallResult withPropagated(AlarmApiCallResult result) {
        if (result.isSuccessful() && result.getAlarm() != null) {
            List<EntityId> propagationEntities;
            if (result.isPropagationChanged()) {
                try {
                    propagationEntities = createEntityAlarmRecords(result.getAlarm());
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                propagationEntities = getPropagationEntityIdsList(result.getAlarm());
            }
            return new AlarmApiCallResult(result, propagationEntities);
        } else {
            return result;
        }
    }

    /**
     * 功能：校验告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：无。
     */
    private void validateAlarmRequest(AlarmModificationRequest request) {
        ConstraintValidator.validateFields(request);
        if (request.getEndTs() > 0 && request.getStartTs() > request.getEndTs()) {
            throw new DataValidationException("Alarm start ts can't be greater then alarm end ts!");
        }
        if (!tenantService.tenantExists(request.getTenantId())) {
            throw new DataValidationException("Alarm is referencing to non-existent tenant!");
        }
        if (request.getStartTs() == 0L) {
            request.setStartTs(System.currentTimeMillis());
        }
        if (request.getEndTs() == 0L) {
            request.setEndTs(request.getStartTs());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseAlarmService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
