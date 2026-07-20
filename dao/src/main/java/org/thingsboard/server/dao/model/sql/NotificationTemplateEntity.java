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
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
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
 * 1. `NotificationTemplateEntity` 是 ThingsBoard DAO 中表示通知持久化结构的实体类型。
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
@Table(name = ModelConstants.NOTIFICATION_TEMPLATE_TABLE_NAME)
public class NotificationTemplateEntity extends BaseSqlEntity<NotificationTemplate> {

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
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_TEMPLATE_NOTIFICATION_TYPE_PROPERTY, nullable = false)
    private NotificationType notificationType;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_TEMPLATE_CONFIGURATION_PROPERTY, nullable = false)
    private JsonNode configuration;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `NotificationTemplateEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NotificationTemplateEntity() {}

    /**
     * 功能：创建 `NotificationTemplateEntity` 实例，并初始化必要字段。
     * 参数：
     * - `notificationTemplate`：`notificationTemplate` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationTemplateEntity(NotificationTemplate notificationTemplate) {
        setId(notificationTemplate.getUuidId());
        setCreatedTime(notificationTemplate.getCreatedTime());
        setTenantId(getTenantUuid(notificationTemplate.getTenantId()));
        setName(notificationTemplate.getName());
        setNotificationType(notificationTemplate.getNotificationType());
        setConfiguration(toJson(notificationTemplate.getConfiguration()));
        setExternalId(getUuid(notificationTemplate.getExternalId()));
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationTemplate toData() {
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setId(new NotificationTemplateId(id));
        notificationTemplate.setCreatedTime(createdTime);
        notificationTemplate.setTenantId(getTenantId(tenantId));
        notificationTemplate.setName(name);
        notificationTemplate.setNotificationType(notificationType);
        notificationTemplate.setConfiguration(fromJson(configuration, NotificationTemplateConfig.class));
        notificationTemplate.setExternalId(getEntityId(externalId, NotificationTemplateId::new));
        return notificationTemplate;
    }

}
