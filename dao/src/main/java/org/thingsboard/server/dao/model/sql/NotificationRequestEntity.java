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
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `NotificationRequestEntity` 是 ThingsBoard DAO 中表示请求持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_REQUEST_TABLE_NAME)
public class NotificationRequestEntity extends BaseSqlEntity<NotificationRequest> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    /**
     * `targets` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TARGETS_PROPERTY, nullable = false)
    private String targets;

    /**
     * `templateId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TEMPLATE_ID_PROPERTY)
    private UUID templateId;

    /**
     * `template` 字段，保存当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_TEMPLATE_PROPERTY)
    private JsonNode template;

    /**
     * 信息对象，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_INFO_PROPERTY)
    private JsonNode info;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_ID_PROPERTY)
    private UUID originatorEntityId;

    /**
     * 实体，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_TYPE_PROPERTY)
    private EntityType originatorEntityType;

    /**
     * `ruleId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_RULE_ID_PROPERTY)
    private UUID ruleId;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_STATUS_PROPERTY)
    private NotificationRequestStatus status;

    /**
     * `stats` 字段，保存当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_STATS_PROPERTY)
    private JsonNode stats;

    /**
     * 功能：创建 `NotificationRequestEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NotificationRequestEntity() {}

    /**
     * 功能：创建 `NotificationRequestEntity` 实例，并初始化必要字段。
     * 参数：
     * - `notificationRequest`：请求对象。
     * 返回：新创建的对象实例。
     */
    public NotificationRequestEntity(NotificationRequest notificationRequest) {
        setId(notificationRequest.getUuidId());
        setCreatedTime(notificationRequest.getCreatedTime());
        setTenantId(getTenantUuid(notificationRequest.getTenantId()));
        setTargets(listToString(notificationRequest.getTargets()));
        setTemplateId(getUuid(notificationRequest.getTemplateId()));
        setTemplate(toJson(notificationRequest.getTemplate()));
        setInfo(toJson(notificationRequest.getInfo()));
        setAdditionalConfig(toJson(notificationRequest.getAdditionalConfig()));
        if (notificationRequest.getOriginatorEntityId() != null) {
            setOriginatorEntityId(notificationRequest.getOriginatorEntityId().getId());
            setOriginatorEntityType(notificationRequest.getOriginatorEntityId().getEntityType());
        }
        setRuleId(getUuid(notificationRequest.getRuleId()));
        setStatus(notificationRequest.getStatus());
        setStats(toJson(notificationRequest.getStats()));
    }

    /**
     * 功能：创建 `NotificationRequestEntity` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationRequestEntity(NotificationRequestEntity other) {
        this.id = other.id;
        this.createdTime = other.createdTime;
        this.tenantId = other.tenantId;
        this.targets = other.targets;
        this.templateId = other.templateId;
        this.template = other.template;
        this.info = other.info;
        this.additionalConfig = other.additionalConfig;
        this.originatorEntityId = other.originatorEntityId;
        this.originatorEntityType = other.originatorEntityType;
        this.ruleId = other.ruleId;
        this.status = other.status;
        this.stats = other.stats;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRequest toData() {
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setId(new NotificationRequestId(id));
        notificationRequest.setCreatedTime(createdTime);
        notificationRequest.setTenantId(getTenantId(tenantId));
        notificationRequest.setTargets(listFromString(targets, UUID::fromString));
        notificationRequest.setTemplateId(getEntityId(templateId, NotificationTemplateId::new));
        notificationRequest.setTemplate(fromJson(template, NotificationTemplate.class));
        notificationRequest.setInfo(fromJson(info, NotificationInfo.class));
        notificationRequest.setAdditionalConfig(fromJson(additionalConfig, NotificationRequestConfig.class));
        if (originatorEntityId != null) {
            notificationRequest.setOriginatorEntityId(EntityIdFactory.getByTypeAndUuid(originatorEntityType, originatorEntityId));
        }
        notificationRequest.setRuleId(getEntityId(ruleId, NotificationRuleId::new));
        notificationRequest.setStatus(status);
        notificationRequest.setStats(fromJson(stats, NotificationRequestStats.class));
        return notificationRequest;
    }

}
