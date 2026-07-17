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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_DATA_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_STATUS_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_USER_NAME_PROPERTY;

/**
 * 中文说明：
 * 1. `AuditLogEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.AUDIT_LOG_TABLE_NAME)
public class AuditLogEntity extends BaseSqlEntity<AuditLog> implements BaseEntity<AuditLog> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = AUDIT_LOG_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = AUDIT_LOG_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    /**
     * 实体，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ENTITY_TYPE_PROPERTY)
    private EntityType entityType;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = AUDIT_LOG_ENTITY_ID_PROPERTY)
    private UUID entityId;

    /**
     * 实体，用于标识或展示当前对象。
     */
    @Column(name = AUDIT_LOG_ENTITY_NAME_PROPERTY)
    private String entityName;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    @Column(name = AUDIT_LOG_USER_ID_PROPERTY)
    private UUID userId;

    /**
     * 用户，用于标识或展示当前对象。
     */
    @Column(name = AUDIT_LOG_USER_NAME_PROPERTY)
    private String userName;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_TYPE_PROPERTY)
    private ActionType actionType;

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Type(type = "json")
    @Column(name = AUDIT_LOG_ACTION_DATA_PROPERTY)
    private JsonNode actionData;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = AUDIT_LOG_ACTION_STATUS_PROPERTY)
    private ActionStatus actionStatus;

    /**
     * 失败信息，表示当前对象的对应属性。
     */
    @Column(name = AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY)
    private String actionFailureDetails;

    /**
     * 功能：创建 `AuditLogEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AuditLogEntity() {
        super();
    }

    /**
     * 功能：创建 `AuditLogEntity` 实例，并初始化必要字段。
     * 参数：
     * - `auditLog`：`auditLog` 参数。
     * 返回：新创建的对象实例。
     */
    public AuditLogEntity(AuditLog auditLog) {
        if (auditLog.getId() != null) {
            this.setUuid(auditLog.getId().getId());
        }
        this.setCreatedTime(auditLog.getCreatedTime());
        if (auditLog.getTenantId() != null) {
            this.tenantId = auditLog.getTenantId().getId();
        }
        if (auditLog.getCustomerId() != null) {
            this.customerId = auditLog.getCustomerId().getId();
        }
        if (auditLog.getEntityId() != null) {
            this.entityId = auditLog.getEntityId().getId();
            this.entityType = auditLog.getEntityId().getEntityType();
        }
        if (auditLog.getUserId() != null) {
            this.userId = auditLog.getUserId().getId();
        }
        this.entityName = auditLog.getEntityName();
        this.userName = auditLog.getUserName();
        this.actionType = auditLog.getActionType();
        this.actionData = auditLog.getActionData();
        this.actionStatus = auditLog.getActionStatus();
        this.actionFailureDetails = auditLog.getActionFailureDetails();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public AuditLog toData() {
        AuditLog auditLog = new AuditLog(new AuditLogId(this.getUuid()));
        auditLog.setCreatedTime(createdTime);
        if (tenantId != null) {
            auditLog.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            auditLog.setCustomerId(new CustomerId(customerId));
        }
        if (entityId != null) {
            auditLog.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType.name(), entityId));
        }
        if (userId != null) {
            auditLog.setUserId(new UserId(userId));
        }
        auditLog.setEntityName(this.entityName);
        auditLog.setUserName(this.userName);
        auditLog.setActionType(this.actionType);
        auditLog.setActionData(this.actionData);
        auditLog.setActionStatus(this.actionStatus);
        auditLog.setActionFailureDetails(this.actionFailureDetails);
        return auditLog;
    }
}
