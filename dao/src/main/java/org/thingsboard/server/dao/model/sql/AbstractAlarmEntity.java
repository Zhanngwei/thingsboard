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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ACKNOWLEDGED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ACK_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGNEE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ASSIGN_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CLEARED_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CLEAR_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_END_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_RELATION_TYPES;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_SEVERITY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_START_TS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ALARM_TYPE_PROPERTY;

/**
 * 中文说明：
 * 1. 类目的：`AbstractAlarmEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractAlarmEntity<T extends Alarm> extends BaseSqlEntity<T> implements BaseEntity<T> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ALARM_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = ALARM_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    /**
     * `originatorId`ID，用于定位对应业务对象。
     */
    @Column(name = ALARM_ORIGINATOR_ID_PROPERTY)
    private UUID originatorId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ALARM_ORIGINATOR_TYPE_PROPERTY)
    private EntityType originatorType;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ALARM_TYPE_PROPERTY)
    private String type;

    /**
     * `severity` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ALARM_SEVERITY_PROPERTY)
    private AlarmSeverity severity;

    /**
     * `assigneeId`ID，用于定位对应业务对象。
     */
    @Type(type="pg-uuid")
    @Column(name = ALARM_ASSIGNEE_ID_PROPERTY)
    private UUID assigneeId;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ALARM_START_TS_PROPERTY)
    private Long startTs;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ALARM_END_TS_PROPERTY)
    private Long endTs;

    /**
     * 当前告警是否已经确认。
     */
    @Column(name = ALARM_ACKNOWLEDGED_PROPERTY)
    private boolean acknowledged;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ALARM_ACK_TS_PROPERTY)
    private Long ackTs;

    /**
     * 当前告警是否已经清除。
     */
    @Column(name = ALARM_CLEARED_PROPERTY)
    private boolean cleared;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ALARM_CLEAR_TS_PROPERTY)
    private Long clearTs;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ALARM_ASSIGN_TS_PROPERTY)
    private Long assignTs;

    /**
     * `details` 字段，保存当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.ALARM_DETAILS_PROPERTY)
    private JsonNode details;

    /**
     * 是否向关联对象传播当前状态。
     */
    @Column(name = ALARM_PROPAGATE_PROPERTY)
    private Boolean propagate;

    /**
     * 是否向关联对象传播当前状态。
     */
    @Column(name = ALARM_PROPAGATE_TO_OWNER_PROPERTY)
    private Boolean propagateToOwner;

    /**
     * 是否向关联对象传播当前状态。
     */
    @Column(name = ALARM_PROPAGATE_TO_TENANT_PROPERTY)
    private Boolean propagateToTenant;

    /**
     * 关系，用于区分不同处理分支。
     */
    @Column(name = ALARM_PROPAGATE_RELATION_TYPES)
    private String propagateRelationTypes;

    /**
     * 功能：创建 `AbstractAlarmEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractAlarmEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmEntity(Alarm alarm) {
        if (alarm.getId() != null) {
            this.setUuid(alarm.getUuidId());
        }
        this.setCreatedTime(alarm.getCreatedTime());
        if (alarm.getTenantId() != null) {
            this.tenantId = alarm.getTenantId().getId();
        }
        if (alarm.getCustomerId() != null) {
            this.customerId = alarm.getCustomerId().getId();
        }
        this.type = alarm.getType();
        this.originatorId = alarm.getOriginator().getId();
        this.originatorType = alarm.getOriginator().getEntityType();
        this.type = alarm.getType();
        this.severity = alarm.getSeverity();
        this.acknowledged = alarm.isAcknowledged();
        this.cleared = alarm.isCleared();
        if (alarm.getAssigneeId() != null) {
            this.assigneeId = alarm.getAssigneeId().getId();
        }
        this.propagate = alarm.isPropagate();
        this.propagateToOwner = alarm.isPropagateToOwner();
        this.propagateToTenant = alarm.isPropagateToTenant();
        this.startTs = alarm.getStartTs();
        this.endTs = alarm.getEndTs();
        this.ackTs = alarm.getAckTs();
        this.clearTs = alarm.getClearTs();
        this.assignTs = alarm.getAssignTs();
        this.details = alarm.getDetails();
        if (!CollectionUtils.isEmpty(alarm.getPropagateRelationTypes())) {
            this.propagateRelationTypes = String.join(",", alarm.getPropagateRelationTypes());
        } else {
            this.propagateRelationTypes = "";
        }
    }

    /**
     * 功能：创建 `AbstractAlarmEntity` 实例，并初始化必要字段。
     * 参数：
     * - `alarmEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractAlarmEntity(AlarmEntity alarmEntity) {
        this.setId(alarmEntity.getId());
        this.setCreatedTime(alarmEntity.getCreatedTime());
        this.tenantId = alarmEntity.getTenantId();
        this.customerId = alarmEntity.getCustomerId();
        this.type = alarmEntity.getType();
        this.originatorId = alarmEntity.getOriginatorId();
        this.originatorType = alarmEntity.getOriginatorType();
        this.type = alarmEntity.getType();
        this.severity = alarmEntity.getSeverity();
        this.acknowledged = alarmEntity.isAcknowledged();
        this.cleared = alarmEntity.isCleared();
        this.assigneeId = alarmEntity.getAssigneeId();
        this.propagate = alarmEntity.getPropagate();
        this.propagateToOwner = alarmEntity.getPropagateToOwner();
        this.propagateToTenant = alarmEntity.getPropagateToTenant();
        this.startTs = alarmEntity.getStartTs();
        this.endTs = alarmEntity.getEndTs();
        this.ackTs = alarmEntity.getAckTs();
        this.clearTs = alarmEntity.getClearTs();
        this.assignTs = alarmEntity.getAssignTs();
        this.details = alarmEntity.getDetails();
        this.propagateRelationTypes = alarmEntity.getPropagateRelationTypes();
    }

    /**
     * 功能：执行 `toAlarm` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Alarm toAlarm() {
        Alarm alarm = new Alarm(new AlarmId(id));
        alarm.setCreatedTime(createdTime);
        if (tenantId != null) {
            alarm.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            alarm.setCustomerId(new CustomerId(customerId));
        }
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        alarm.setType(type);
        alarm.setSeverity(severity);
        alarm.setAcknowledged(acknowledged);
        alarm.setCleared(cleared);
        if (assigneeId != null) {
            alarm.setAssigneeId(new UserId(assigneeId));
        }
        alarm.setPropagate(propagate);
        alarm.setPropagateToOwner(propagateToOwner);
        alarm.setPropagateToTenant(propagateToTenant);
        alarm.setStartTs(startTs);
        alarm.setEndTs(endTs);
        alarm.setAckTs(ackTs);
        alarm.setClearTs(clearTs);
        alarm.setAssignTs(assignTs);
        alarm.setDetails(details);
        if (!StringUtils.isEmpty(propagateRelationTypes)) {
            alarm.setPropagateRelationTypes(Arrays.asList(propagateRelationTypes.split(",")));
        } else {
            alarm.setPropagateRelationTypes(Collections.emptyList());
        }
        return alarm;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractAlarmEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
