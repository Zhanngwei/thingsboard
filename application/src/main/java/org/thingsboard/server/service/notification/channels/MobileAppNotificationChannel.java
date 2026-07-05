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
package org.thingsboard.server.service.notification.channels;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.Notification;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationStatus;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.settings.MobileAppNotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.template.MobileAppDeliveryMethodNotificationTemplate;
import org.thingsboard.server.dao.notification.NotificationService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.notification.NotificationProcessingContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.thingsboard.server.common.data.notification.NotificationDeliveryMethod.MOBILE_APP;

/**
 * 中文说明：
 * 1. 类目的：`MobileAppNotificationChannel` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MobileAppNotificationChannel implements NotificationChannel<User, MobileAppDeliveryMethodNotificationTemplate> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final FirebaseService firebaseService;
    private final UserService userService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final NotificationService notificationService;
    private final NotificationSettingsService notificationSettingsService;

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `recipient`：`recipient` 参数。
     * - `processedTemplate`：`processedTemplate` 参数。
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void sendNotification(User recipient, MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        NotificationRequest request = ctx.getRequest();
        NotificationInfo info = request.getInfo();
        if (info != null && info.getDashboardId() != null) {
            ObjectNode additionalConfig = JacksonUtil.asObject(processedTemplate.getAdditionalConfig());
            ObjectNode onClick = JacksonUtil.asObject(additionalConfig.get("onClick"));
            if (onClick.get("enabled") == null || !Boolean.parseBoolean(onClick.get("enabled").asText())) {
                onClick.put("enabled", true);
                onClick.put("linkType", "DASHBOARD");
                onClick.put("setEntityIdInState", true);
                onClick.put("dashboardId", info.getDashboardId().toString());
                additionalConfig.set("onClick", onClick);
            }
            processedTemplate.setAdditionalConfig(additionalConfig);
        }
        Notification notification = Notification.builder()
                .requestId(request.getId())
                .recipientId(recipient.getId())
                .type(ctx.getNotificationType())
                .deliveryMethod(MOBILE_APP)
                .subject(processedTemplate.getSubject())
                .text(processedTemplate.getBody())
                .additionalConfig(processedTemplate.getAdditionalConfig())
                .info(info)
                .status(NotificationStatus.SENT)
                .build();
        notificationService.saveNotification(recipient.getTenantId(), notification);

        var mobileSessions = userService.findMobileSessions(recipient.getTenantId(), recipient.getId());
        if (mobileSessions.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }

        MobileAppNotificationDeliveryMethodConfig config = ctx.getDeliveryMethodConfig(MOBILE_APP);
        String credentials = config.getFirebaseServiceAccountCredentials();
        Set<String> validTokens = new HashSet<>(mobileSessions.keySet());

        String subject = processedTemplate.getSubject();
        String body = processedTemplate.getBody();
        Map<String, String> data = getNotificationData(processedTemplate, ctx);
        int unreadCount = notificationService.countUnreadNotificationsByRecipientId(ctx.getTenantId(), MOBILE_APP, recipient.getId());
        for (String token : mobileSessions.keySet()) {
            try {
                firebaseService.sendMessage(ctx.getTenantId(), credentials, token, subject, body, data, unreadCount);
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode errorCode = e.getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    validTokens.remove(token);
                    userService.removeMobileSession(recipient.getTenantId(), token);
                    continue;
                }
                throw new RuntimeException("Failed to send message via FCM: " + e.getMessage(), e);
            }
        }
        if (validTokens.isEmpty()) {
            throw new IllegalArgumentException("User doesn't use the mobile app");
        }
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `processedTemplate`：`processedTemplate` 参数。
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    private Map<String, String> getNotificationData(MobileAppDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) {
        Map<String, String> data = Optional.ofNullable(processedTemplate.getAdditionalConfig())
                .filter(JsonNode::isObject).map(JacksonUtil::toFlatMap).orElseGet(HashMap::new);
        NotificationInfo info = ctx.getRequest().getInfo();
        if (info == null) {
            return data;
        }
        Optional.ofNullable(info.getStateEntityId()).ifPresent(stateEntityId -> {
            data.put("stateEntityId", stateEntityId.getId().toString());
            data.put("stateEntityType", stateEntityId.getEntityType().name());
        });
        data.put("notificationType", ctx.getNotificationType().name());
        switch (ctx.getNotificationType()) {
            case ALARM:
            case ALARM_ASSIGNMENT:
            case ALARM_COMMENT:
                info.getTemplateData().forEach((key, value) -> {
                    data.put("info." + key, value);
                });
                break;
        }
        data.replaceAll((key, value) -> Strings.nullToEmpty(value));
        return data;
    }

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void check(TenantId tenantId) throws Exception {
        NotificationSettings systemSettings = notificationSettingsService.findNotificationSettings(TenantId.SYS_TENANT_ID);
        if (!systemSettings.getDeliveryMethodsConfigs().containsKey(MOBILE_APP)) {
            throw new RuntimeException("Push-notifications to mobile are not configured");
        }
    }

    /**
     * 功能：获取`Delivery Method`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return MOBILE_APP;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`MobileAppNotificationChannel` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
