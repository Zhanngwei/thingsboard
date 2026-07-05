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
 * 1. 类目的：`NotificationRuleEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
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

/*
 * 本类总结：
 * 1. 核心职责：`NotificationRuleEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
