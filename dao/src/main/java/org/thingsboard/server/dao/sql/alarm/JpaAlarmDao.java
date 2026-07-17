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
package org.thingsboard.server.dao.sql.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmPropagationInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.EntityAlarmEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.query.AlarmQueryRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityTypesToDto;
import static org.thingsboard.server.dao.DaoUtil.toPageable;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
/**
 * 中文说明：
 * 1. `JpaAlarmDao` 是 ThingsBoard DAO 中负责告警存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`AlarmDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao {

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmRepository alarmRepository;

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmQueryRepository alarmQueryRepository;

    /**
     * 实体，用于读取或保存对应领域对象。
     */
    @Autowired
    private EntityAlarmRepository entityAlarmRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<AlarmEntity, UUID> getRepository() {
        return alarmRepository;
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
    public Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                originator.getId(),
                type,
                PageRequest.of(0, 1));
        return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
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
        List<AlarmEntity> latest = alarmRepository.findLatestActiveByOriginatorAndType(
                originator.getId(),
                type,
                PageRequest.of(0, 1));
        return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `originator`：`originator` 参数。
     * - `type`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> findLatestByOriginatorAndType(tenantId, originator, type));
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public Alarm findAlarmById(TenantId tenantId, UUID key) {
        return findById(tenantId, key);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, UUID key) {
        return DaoUtil.getData(alarmRepository.findAlarmInfoById(tenantId.getId(), key));
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
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
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        AlarmStatusFilter asf = AlarmStatusFilter.from(query);
        if (affectedEntity != null) {
            return DaoUtil.toPageData(
                    alarmRepository.findAlarms(
                            tenantId.getId(),
                            affectedEntity.getId(),
                            affectedEntity.getEntityType().name(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getStringId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        } else {
            return DaoUtil.toPageData(
                    alarmRepository.findAllAlarms(
                            tenantId.getId(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getStringId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        }
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
        log.trace("Try to find customer alarms by status [{}] and pageLink [{}]", query.getStatus(), query.getPageLink());
        AlarmStatusFilter asf = AlarmStatusFilter.from(query);
        return DaoUtil.toPageData(
                alarmRepository.findCustomerAlarms(
                        tenantId.getId(),
                        customerId.getId(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        asf.hasClearFilter(),
                        asf.hasClearFilter() && asf.getClearFilter(),
                        asf.hasAckFilter(),
                        asf.hasAckFilter() && asf.getAckFilter(),
                        DaoUtil.getStringId(query.getAssigneeId()),
                        query.getPageLink().getTextSearch(),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
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
        log.trace("Try to find alarms by entity [{}], query [{}] and pageLink [{}]", query.getAffectedEntityId(), query, query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        List<String> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<AlarmSeverity> severityList = query.getSeverityList() != null && !query.getSeverityList().isEmpty() ? query.getSeverityList() : null;
        AlarmStatusFilter asf = AlarmStatusFilter.from(query.getStatusList());
        if (affectedEntity != null) {
            return DaoUtil.toPageData(
                    alarmRepository.findAlarmsV2(
                            tenantId.getId(),
                            affectedEntity.getId(),
                            affectedEntity.getEntityType().name(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            typeList,
                            severityList,
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getStringId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        } else {
            return DaoUtil.toPageData(
                    alarmRepository.findAllAlarmsV2(
                            tenantId.getId(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            typeList,
                            severityList,
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getStringId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        }
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
        log.trace("Try to find customer alarms by query [{}] and pageLink [{}]", query, query.getPageLink());
        List<String> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<AlarmSeverity> severityList = query.getSeverityList() != null && !query.getSeverityList().isEmpty() ? query.getSeverityList() : null;
        AlarmStatusFilter asf = AlarmStatusFilter.from(query.getStatusList());
        return DaoUtil.toPageData(
                alarmRepository.findCustomerAlarmsV2(
                        tenantId.getId(),
                        customerId.getId(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        typeList,
                        severityList,
                        asf.hasClearFilter(),
                        asf.hasClearFilter() && asf.getClearFilter(),
                        asf.hasAckFilter(),
                        asf.hasAckFilter() && asf.getAckFilter(),
                        DaoUtil.getStringId(query.getAssigneeId()),
                        query.getPageLink().getTextSearch(),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
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
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `asf`：`asf` 参数。
     * - `assigneeId`：`assigneeId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, AlarmStatusFilter asf, String assigneeId) {
        return alarmRepository.findAlarmSeverities(tenantId.getId(), entityId.getId(), entityId.getEntityType().name(),
                asf.hasClearFilter(),
                asf.hasClearFilter() && asf.getClearFilter(),
                asf.hasAckFilter(),
                asf.hasAckFilter() && asf.getAckFilter(),
                assigneeId);
    }

    /**
     * 功能：获取结束时间戳。
     * 参数：
     * - `time`：`time` 参数。
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmId> findAlarmsIdsByEndTsBeforeAndTenantId(Long time, TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(alarmRepository.findAlarmsIdsByEndTsBeforeAndTenantId(time, tenantId.getId(), DaoUtil.toPageable(pageLink)))
                .mapData(AlarmId::new);
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
    public PageData<AlarmId> findAlarmIdsByAssigneeId(TenantId tenantId, UUID userId, PageLink pageLink) {
        return DaoUtil.pageToPageData(alarmRepository.findAlarmIdsByAssigneeId(tenantId.getId(), userId, DaoUtil.toPageable(pageLink)))
                .mapData(AlarmId::new);
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
        return DaoUtil.pageToPageData(alarmRepository.findAlarmIdsByOriginatorId(tenantId.getId(), originatorId.getId(), DaoUtil.toPageable(pageLink)))
                .mapData(AlarmId::new);
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `entityAlarm`：实体对象。
     * 返回：无。
     */
    @Override
    public void createEntityAlarmRecord(EntityAlarm entityAlarm) {
        log.debug("Saving entity {}", entityAlarm);
        entityAlarmRepository.save(new EntityAlarmEntity(entityAlarm));
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EntityAlarm> findEntityAlarmRecords(TenantId tenantId, AlarmId id) {
        log.trace("[{}] Try to find entity alarm records using [{}]", tenantId, id);
        return DaoUtil.convertDataList(entityAlarmRepository.findAllByAlarmId(id.getId()));
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId) {
        entityAlarmRepository.deleteByEntityId(entityId.getId());
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteEntityAlarmRecordsByTenantId(TenantId tenantId) {
        entityAlarmRepository.deleteByTenantId(tenantId.getId());
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `request`：请求对象。
     * - `alarmCreationEnabled`：`alarmCreationEnabled` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult createOrUpdateActiveAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled) {
        AlarmPropagationInfo ap = getSafePropagationInfo(request.getPropagation());
        return toAlarmApiResult(alarmRepository.createOrUpdateActiveAlarm(
                request.getTenantId().getId(),
                request.getCustomerId() != null ? request.getCustomerId().getId() : CustomerId.NULL_UUID,
                request.getEdgeAlarmId() != null ? request.getEdgeAlarmId().getId() : UUID.randomUUID(),
                System.currentTimeMillis(),
                request.getOriginator().getId(),
                request.getOriginator().getEntityType().ordinal(),
                request.getType(),
                request.getSeverity().name(),
                request.getStartTs(), request.getEndTs(),
                getDetailsAsString(request.getDetails()),
                ap.isPropagate(),
                ap.isPropagateToOwner(),
                ap.isPropagateToTenant(),
                getPropagationTypes(ap),
                alarmCreationEnabled
        ));
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `request`：请求对象。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        AlarmPropagationInfo ap = getSafePropagationInfo(request.getPropagation());
        return toAlarmApiResult(alarmRepository.updateAlarm(
                request.getTenantId().getId(),
                request.getAlarmId().getId(),
                request.getSeverity().name(),
                request.getStartTs(), request.getEndTs(),
                getDetailsAsString(request.getDetails()),
                ap.isPropagate(),
                ap.isPropagateToOwner(),
                ap.isPropagateToTenant(),
                getPropagationTypes(ap)
        ));
    }

    /**
     * 功能：执行 `acknowledgeAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `ackTs`：时间戳。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId id, long ackTs) {
        return toAlarmApiResult(alarmRepository.acknowledgeAlarm(tenantId.getId(), id.getId(), ackTs));
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `clearTs`：时间戳。
     * - `details`：`details` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId id, long clearTs, JsonNode details) {
        return toAlarmApiResult(alarmRepository.clearAlarm(tenantId.getId(), id.getId(), clearTs, details != null ? getDetailsAsString(details) : null));
    }

    /**
     * 功能：执行 `assignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `assigneeId`：`assigneeId`ID。
     * - `assignTime`：`assignTime` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId id, UserId assigneeId, long assignTime) {
        return toAlarmApiResult(alarmRepository.assignAlarm(tenantId.getId(), id.getId(), assigneeId.getId(), assignTime));
    }

    /**
     * 功能：执行 `unassignAlarm` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `unassignTime`：`unassignTime` 参数。
     * 返回：键值映射结果。
     */
    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId id, long unassignTime) {
        return toAlarmApiResult(alarmRepository.unassignAlarm(tenantId.getId(), id.getId(), unassignTime));
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
        return alarmQueryRepository.countAlarmsByQuery(tenantId, customerId, query);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntitySubtype> findTenantAlarmTypes(UUID tenantId, PageLink pageLink) {
        Page<String> page = alarmRepository.findTenantAlarmTypes(tenantId, Objects.toString(pageLink.getTextSearch(), ""), toPageable(pageLink));
        if (page.isEmpty()) {
            return PageData.emptyPageData();
        }

        List<EntitySubtype> data = convertTenantEntityTypesToDto(tenantId, EntityType.ALARM, page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * 功能：删除或清理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `types`：类型。
     * 返回：判断结果。
     */
    @Override
    public boolean removeAlarmTypesIfNoAlarmsPresent(UUID tenantId, Set<String> types) {
        return alarmRepository.deleteTypeIfNoAlarmsExist(tenantId, types) > 0;
    }

    /**
     * 功能：获取`Propagation Types`。
     * 参数：
     * - `ap`：`ap` 参数。
     * 返回：文本结果。
     */
    private static String getPropagationTypes(AlarmPropagationInfo ap) {
        String propagateRelationTypes;
        if (!CollectionUtils.isEmpty(ap.getPropagateRelationTypes())) {
            propagateRelationTypes = String.join(",", ap.getPropagateRelationTypes());
        } else {
            propagateRelationTypes = "";
        }
        return propagateRelationTypes;
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `ap`：`ap` 参数。
     * 返回：处理结果。
     */
    private static AlarmPropagationInfo getSafePropagationInfo(AlarmPropagationInfo ap) {
        return ap != null ? ap : AlarmPropagationInfo.EMPTY;
    }

    /**
     * 功能：获取`Details As String`。
     * 参数：
     * - `details`：`details` 参数。
     * 返回：文本结果。
     */
    private static String getDetailsAsString(JsonNode details) {
        var detailsStr = JacksonUtil.toString(details);
        if (StringUtils.isEmpty(detailsStr)) {
            detailsStr = "{}";
        }
        return detailsStr;
    }

    /**
     * 功能：执行 `toAlarmApiResult` 对应的处理。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：键值映射结果。
     */
    private AlarmApiCallResult toAlarmApiResult(String str) {
        var json = JacksonUtil.toJsonNode(str);
        var result = AlarmApiCallResult.builder();
        boolean success = json.get("success").asBoolean();
        result.successful(success);
        if (success) {
            boolean modified = false;
            boolean created = false;
            boolean cleared = false;
            if (json.has("modified")) {
                modified = json.get("modified").asBoolean();
            }

            if (json.has("created")) {
                created = json.get("created").asBoolean();
            }

            if (json.has("cleared")) {
                cleared = json.get("cleared").asBoolean();
            }
            result.created(created);
            result.cleared(cleared);
            result.modified(created || cleared || modified);
            if (json.has("alarm") && !json.get("alarm").isNull()) {
                result.alarm(toAlarmInfo(json.get("alarm")));
            }
            if (json.has("old") && !json.get("old").isNull()) {
                result.old(toAlarm(json.get("old")));
            }
        }
        return result.build();
    }

    /**
     * 功能：执行 `toAlarmInfo` 对应的处理。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：处理结果。
     */
    private AlarmInfo toAlarmInfo(JsonNode json) {
        AlarmInfo alarmInfo = new AlarmInfo(toAlarm(json));
        getSafe(json, ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY).ifPresent(alarmInfo::setOriginatorName);
        getSafe(json, ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY).ifPresent(alarmInfo::setOriginatorLabel);
        if (alarmInfo.getAssigneeId() != null) {
            var assigneeBuilder = AlarmAssignee.builder().id(alarmInfo.getAssigneeId());
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY).ifPresent(assigneeBuilder::firstName);
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY).ifPresent(assigneeBuilder::lastName);
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY).ifPresent(assigneeBuilder::email);
            alarmInfo.setAssignee(assigneeBuilder.build());
        }
        return alarmInfo;
    }

    /**
     * 功能：执行 `toAlarm` 对应的处理。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：处理结果。
     */
    private Alarm toAlarm(JsonNode json) {
        Alarm alarm = new Alarm(new AlarmId(UUID.fromString(json.get(ModelConstants.ID_PROPERTY).asText())));
        alarm.setCreatedTime(json.get(ModelConstants.CREATED_TIME_PROPERTY).asLong());
        getSafe(json, ModelConstants.TENANT_ID_COLUMN).ifPresent(s -> alarm.setTenantId(TenantId.fromUUID(UUID.fromString(s))));
        getSafe(json, ModelConstants.CUSTOMER_ID_PROPERTY).ifPresent(s -> alarm.setCustomerId(new CustomerId(UUID.fromString(s))));
        getSafe(json, ModelConstants.ASSIGNEE_ID_PROPERTY).ifPresent(s -> alarm.setAssigneeId(new UserId(UUID.fromString(s))));
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(
                json.get(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY).asInt(),
                json.get(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY).asText()));
        getSafe(json, ModelConstants.ALARM_TYPE_PROPERTY).ifPresent(alarm::setType);
        getSafe(json, ModelConstants.ALARM_SEVERITY_PROPERTY).map(AlarmSeverity::valueOf).ifPresent(alarm::setSeverity);
        alarm.setAcknowledged(json.get(ModelConstants.ALARM_ACKNOWLEDGED_PROPERTY).asBoolean());
        alarm.setCleared(json.get(ModelConstants.ALARM_CLEARED_PROPERTY).asBoolean());
        alarm.setPropagate(json.get(ModelConstants.ALARM_PROPAGATE_PROPERTY).asBoolean());
        alarm.setPropagateToOwner(json.get(ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY).asBoolean());
        alarm.setPropagateToTenant(json.get(ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY).asBoolean());
        alarm.setStartTs(json.get(ModelConstants.ALARM_START_TS_PROPERTY).asLong());
        alarm.setEndTs(json.get(ModelConstants.ALARM_END_TS_PROPERTY).asLong());
        alarm.setAckTs(json.get(ModelConstants.ALARM_ACK_TS_PROPERTY).asLong());
        alarm.setClearTs(json.get(ModelConstants.ALARM_CLEAR_TS_PROPERTY).asLong());
        alarm.setAssignTs(json.get(ModelConstants.ALARM_ASSIGN_TS_PROPERTY).asLong());
        getSafe(json, ModelConstants.ALARM_DETAILS_PROPERTY).map(JacksonUtil::toJsonNode).ifPresent(alarm::setDetails);
        alarm.setPropagateRelationTypes(getSafe(json, ModelConstants.ALARM_PROPAGATE_RELATION_TYPES).filter(StringUtils::isNoneEmpty)
                .map(s -> Arrays.asList(s.split(","))).orElse(Collections.emptyList()));
        return alarm;
    }

    /**
     * 功能：获取`Safe`。
     * 参数：
     * - `json`：`json` 参数。
     * - `fieldName`：名称。
     * 返回：可能存在的结果。
     */
    private static Optional<String> getSafe(JsonNode json, String fieldName) {
        if (json.has(fieldName)) {
            var element = json.get(fieldName);
            if (element.isNull() || !element.isTextual()) {
                return Optional.empty();
            } else {
                return Optional.of(element.asText());
            }
        } else {
            return Optional.empty();
        }
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

}
