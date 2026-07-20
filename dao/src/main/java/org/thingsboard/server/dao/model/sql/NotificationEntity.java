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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.NotificationId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
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
 * 1. `NotificationEntity` 是 ThingsBoard DAO 中表示通知持久化结构的实体类型。
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
@Table(name = ModelConstants.NOTIFICATION_TABLE_NAME)
public class NotificationEntity extends BaseSqlEntity<Notification> {

    /**
     * 请求ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_REQUEST_ID_PROPERTY, nullable = false)
    private UUID requestId;

    /**
     * `recipientId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.NOTIFICATION_RECIPIENT_ID_PROPERTY, nullable = false)
    private UUID recipientId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_TYPE_PROPERTY, nullable = false)
    private NotificationType type;

    /**
     * `deliveryMethod` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_DELIVERY_METHOD_PROPERTY, nullable = false)
    private NotificationDeliveryMethod deliveryMethod;

    /**
     * `subject` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.NOTIFICATION_SUBJECT_PROPERTY)
    private String subject;

    /**
     * `text` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.NOTIFICATION_TEXT_PROPERTY, nullable = false)
    private String text;

    /**
     * 配置，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.NOTIFICATION_ADDITIONAL_CONFIG_PROPERTY)
    private JsonNode additionalConfig;

    /**
     * 信息对象，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Formula("(SELECT r.info FROM notification_request r WHERE r.id = request_id)")
    private JsonNode info;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.NOTIFICATION_STATUS_PROPERTY)
    private NotificationStatus status;

    /**
     * 功能：创建 `NotificationEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NotificationEntity() {}

    /**
     * 功能：创建 `NotificationEntity` 实例，并初始化必要字段。
     * 参数：
     * - `notification`：`notification` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationEntity(Notification notification) {
        setId(notification.getUuidId());
        setCreatedTime(notification.getCreatedTime());
        setRequestId(getUuid(notification.getRequestId()));
        setRecipientId(getUuid(notification.getRecipientId()));
        setType(notification.getType());
        setDeliveryMethod(notification.getDeliveryMethod());
        setSubject(notification.getSubject());
        setText(notification.getText());
        setAdditionalConfig(notification.getAdditionalConfig());
        setInfo(toJson(notification.getInfo()));
        setStatus(notification.getStatus());
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Notification toData() {
        Notification notification = new Notification();
        notification.setId(new NotificationId(id));
        notification.setCreatedTime(createdTime);
        notification.setRequestId(getEntityId(requestId, NotificationRequestId::new));
        notification.setRecipientId(getEntityId(recipientId, UserId::new));
        notification.setType(type);
        notification.setDeliveryMethod(deliveryMethod);
        notification.setSubject(subject);
        notification.setText(text);
        notification.setAdditionalConfig(additionalConfig);
        notification.setInfo(fromJson(info, NotificationInfo.class));
        notification.setStatus(status);
        return notification;
    }

}
