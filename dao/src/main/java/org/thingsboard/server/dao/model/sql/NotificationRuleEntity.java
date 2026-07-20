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
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleConfig;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleRecipientsConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
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
 * 1. `NotificationRuleEntity` 是 ThingsBoard DAO 中表示通知持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data @EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.NOTIFICATION_RULE_TABLE_NAME)
public class NotificationRuleEntity extends BaseSqlEntity<NotificationRule> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.TENANT_ID_PROPERTY, nullable = false)
    private UUID tenantId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.NAME_PROPERTY, nullable = false)
    private String name;

    /**
     * 是否启用`enabled`。
     */
    @Column(name = ModelConstants.NOTIFICATION_RULE_ENABLED_PROPERTY, nullable = false)
    private boolean enabled;

    /**
     * `templateId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_RULE_TEMPLATE_ID_PROPERTY, nullable = false)
    private UUID templateId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_RULE_TRIGGER_TYPE_PROPERTY, nullable = false)
    private NotificationRuleTriggerType triggerType;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_RULE_TRIGGER_CONFIG_PROPERTY, nullable = false)
    private JsonNode triggerConfig;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_RULE_RECIPIENTS_CONFIG_PROPERTY, nullable = false)
    private JsonNode recipientsConfig;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_RULE_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `NotificationRuleEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NotificationRuleEntity() {}

    /**
     * 功能：创建 `NotificationRuleEntity` 实例，并初始化必要字段。
     * 参数：
     * - `notificationRule`：`notificationRule` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationRuleEntity(NotificationRule notificationRule) {
        setId(notificationRule.getUuidId());
        setCreatedTime(notificationRule.getCreatedTime());
        setTenantId(getTenantUuid(notificationRule.getTenantId()));
        setName(notificationRule.getName());
        setEnabled(notificationRule.isEnabled());
        setTemplateId(getUuid(notificationRule.getTemplateId()));
        setTriggerType(notificationRule.getTriggerType());
        setTriggerConfig(toJson(notificationRule.getTriggerConfig()));
        setRecipientsConfig(toJson(notificationRule.getRecipientsConfig()));
        setAdditionalConfig(toJson(notificationRule.getAdditionalConfig()));
        setExternalId(getUuid(notificationRule.getExternalId()));
    }

    /**
     * 功能：创建 `NotificationRuleEntity` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationRuleEntity(NotificationRuleEntity other) {
        this.id = other.id;
        this.createdTime = other.createdTime;
        this.tenantId = other.tenantId;
        this.name = other.name;
        this.enabled = other.enabled;
        this.templateId = other.templateId;
        this.triggerType = other.triggerType;
        this.triggerConfig = other.triggerConfig;
        this.recipientsConfig = other.recipientsConfig;
        this.additionalConfig = other.additionalConfig;
        this.externalId = other.externalId;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRule toData() {
        NotificationRule notificationRule = new NotificationRule();
        notificationRule.setId(new NotificationRuleId(id));
        notificationRule.setCreatedTime(createdTime);
        notificationRule.setTenantId(getTenantId(tenantId));
        notificationRule.setName(name);
        notificationRule.setEnabled(enabled);
        notificationRule.setTemplateId(getEntityId(templateId, NotificationTemplateId::new));
        notificationRule.setTriggerType(triggerType);
        notificationRule.setTriggerConfig(fromJson(triggerConfig, NotificationRuleTriggerConfig.class));
        notificationRule.setRecipientsConfig(fromJson(recipientsConfig, NotificationRuleRecipientsConfig.class));
        notificationRule.setAdditionalConfig(fromJson(additionalConfig, NotificationRuleConfig.class));
        notificationRule.setExternalId(getEntityId(externalId, NotificationRuleId::new));
        return notificationRule;
    }

}
