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
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_TYPE_PROPERTY;

/**
 * Created by Victor Basanets on 8/30/2017.
 */

/**
 * 中文说明：
 * 1. `AbstractEntityViewEntity` 是 ThingsBoard DAO 中表示实体视图持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `EntityView`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
@Slf4j
public abstract class AbstractEntityViewEntity<T extends EntityView> extends BaseSqlEntity<T> {

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_ENTITY_ID_PROPERTY)
    private UUID entityId;

    /**
     * 实体，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ModelConstants.DEVICE_TYPE_PROPERTY)
    private String type;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_NAME_PROPERTY)
    private String name;

    /**
     * `keys` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_KEYS_PROPERTY)
    private String keys;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_START_TS_PROPERTY)
    private long startTs;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = ModelConstants.ENTITY_VIEW_END_TS_PROPERTY)
    private long endTs;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `AbstractEntityViewEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractEntityViewEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractEntityViewEntity` 实例，并初始化必要字段。
     * 参数：
     * - `entityView`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractEntityViewEntity(EntityView entityView) {
        if (entityView.getId() != null) {
            this.setUuid(entityView.getId().getId());
        }
        this.setCreatedTime(entityView.getCreatedTime());
        if (entityView.getEntityId() != null) {
            this.entityId = entityView.getEntityId().getId();
            this.entityType = entityView.getEntityId().getEntityType();
        }
        if (entityView.getTenantId() != null) {
            this.tenantId = entityView.getTenantId().getId();
        }
        if (entityView.getCustomerId() != null) {
            this.customerId = entityView.getCustomerId().getId();
        }
        this.type = entityView.getType();
        this.name = entityView.getName();
        try {
            this.keys = JacksonUtil.toString(entityView.getKeys());
        } catch (IllegalArgumentException e) {
            log.error("Unable to serialize entity view keys!", e);
        }
        this.startTs = entityView.getStartTimeMs();
        this.endTs = entityView.getEndTimeMs();
        this.additionalInfo = entityView.getAdditionalInfo();
        if (entityView.getExternalId() != null) {
            this.externalId = entityView.getExternalId().getId();
        }
    }

    /**
     * 功能：创建 `AbstractEntityViewEntity` 实例，并初始化必要字段。
     * 参数：
     * - `entityViewEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractEntityViewEntity(EntityViewEntity entityViewEntity) {
        this.setId(entityViewEntity.getId());
        this.setCreatedTime(entityViewEntity.getCreatedTime());
        this.entityId = entityViewEntity.getEntityId();
        this.entityType = entityViewEntity.getEntityType();
        this.tenantId = entityViewEntity.getTenantId();
        this.customerId = entityViewEntity.getCustomerId();
        this.type = entityViewEntity.getType();
        this.name = entityViewEntity.getName();
        this.keys = entityViewEntity.getKeys();
        this.startTs = entityViewEntity.getStartTs();
        this.endTs = entityViewEntity.getEndTs();
        this.additionalInfo = entityViewEntity.getAdditionalInfo();
        this.externalId = entityViewEntity.getExternalId();
    }

    /**
     * 功能：执行 `toEntityView` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected EntityView toEntityView() {
        EntityView entityView = new EntityView(new EntityViewId(getUuid()));
        entityView.setCreatedTime(createdTime);

        if (entityId != null) {
            entityView.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (tenantId != null) {
            entityView.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            entityView.setCustomerId(new CustomerId(customerId));
        }
        entityView.setType(type);
        entityView.setName(name);
        try {
            entityView.setKeys(JacksonUtil.fromString(keys, TelemetryEntityView.class));
        } catch (IllegalArgumentException e) {
            log.error("Unable to read entity view keys!", e);
        }
        entityView.setStartTimeMs(startTs);
        entityView.setEndTimeMs(endTs);
        entityView.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            entityView.setExternalId(new EntityViewId(externalId));
        }
        return entityView;
    }
}
