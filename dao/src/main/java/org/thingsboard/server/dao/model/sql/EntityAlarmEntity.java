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

import lombok.Data;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.CREATED_TIME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ALARM_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TENANT_ID_COLUMN;

/**
 * 中文说明：
 * 1. `EntityAlarmEntity` 是 ThingsBoard DAO 中表示告警持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `ToData`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Entity
@Table(name = ENTITY_ALARM_TABLE_NAME)
@IdClass(EntityAlarmCompositeKey.class)
public final class EntityAlarmEntity implements ToData<EntityAlarm> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = TENANT_ID_COLUMN, columnDefinition = "uuid")
    private UUID tenantId;

    /**
     * 实体，用于区分不同处理分支。
     */
    @Column(name = ENTITY_TYPE_COLUMN)
    private String entityType;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    private UUID entityId;

    /**
     * 告警ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = "alarm_id", columnDefinition = "uuid")
    private UUID alarmId;

    /**
     * 创建时间，用于记录当前对象首次生成的时间。
     */
    @Column(name = CREATED_TIME_PROPERTY)
    private long createdTime;

    /**
     * 告警，用于区分不同处理分支。
     */
    @Column(name = "alarm_type")
    private String alarmType;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    /**
     * 功能：创建 `EntityAlarmEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EntityAlarmEntity() {
        super();
    }

    /**
     * 功能：创建 `EntityAlarmEntity` 实例，并初始化必要字段。
     * 参数：
     * - `entityAlarm`：实体对象。
     * 返回：新创建的对象实例。
     */
    public EntityAlarmEntity(EntityAlarm entityAlarm) {
        tenantId = entityAlarm.getTenantId().getId();
        entityId = entityAlarm.getEntityId().getId();
        entityType = entityAlarm.getEntityId().getEntityType().name();
        alarmId = entityAlarm.getAlarmId().getId();
        alarmType = entityAlarm.getAlarmType();
        createdTime = entityAlarm.getCreatedTime();
        if (entityAlarm.getCustomerId() != null) {
            customerId = entityAlarm.getCustomerId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityAlarm toData() {
        EntityAlarm result = new EntityAlarm();
        result.setTenantId(TenantId.fromUUID(tenantId));
        result.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        result.setAlarmId(new AlarmId(alarmId));
        result.setAlarmType(alarmType);
        result.setCreatedTime(createdTime);
        if (customerId != null) {
            result.setCustomerId(new CustomerId(customerId));
        }
        return result;
    }
}