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
package org.thingsboard.server.service.notification;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStats;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.settings.NotificationDeliveryMethodConfig;
import org.thingsboard.server.common.data.notification.settings.NotificationSettings;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;
import org.thingsboard.server.common.data.notification.template.DeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplate;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;
import org.thingsboard.server.common.data.util.TemplateUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`NotificationProcessingContext` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@SuppressWarnings("unchecked")
public class NotificationProcessingContext {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private final TenantId tenantId;
    private final NotificationSettings settings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final NotificationSettings systemSettings;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    private final NotificationRequest request;
    /**
     * `deliveryMethods`集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final Set<NotificationDeliveryMethod> deliveryMethods;
    /**
     * 通知，表示当前对象的对应属性。
     */
    @Getter
    private final NotificationTemplate notificationTemplate;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private final NotificationType notificationType;

    /**
     * `templates`映射关系，用于按键查找对应值。
     */
    private final Map<NotificationDeliveryMethod, DeliveryMethodNotificationTemplate> templates;
    /**
     * `stats` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final NotificationRequestStats stats;

    /**
     * 功能：创建 `NotificationProcessingContext` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `request`：请求对象。
     * - `deliveryMethods`：`deliveryMethods` 参数。
     * - `template`：`template` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    public NotificationProcessingContext(TenantId tenantId, NotificationRequest request, Set<NotificationDeliveryMethod> deliveryMethods,
                                         NotificationTemplate template, NotificationSettings settings, NotificationSettings systemSettings) {
        this.tenantId = tenantId;
        this.request = request;
        this.deliveryMethods = deliveryMethods;
        this.settings = settings;
        this.systemSettings = systemSettings;
        this.notificationTemplate = template;
        this.notificationType = template.getNotificationType();
        this.templates = new EnumMap<>(NotificationDeliveryMethod.class);
        this.stats = new NotificationRequestStats();
        init();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void init() {
        NotificationTemplateConfig templateConfig = notificationTemplate.getConfiguration();
        templateConfig.getDeliveryMethodsTemplates().forEach((deliveryMethod, template) -> {
            if (template.isEnabled()) {
                template = processTemplate(template, null); // processing template with immutable params
                templates.put(deliveryMethod, template);
            }
        });
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * 返回：处理结果。
     */
    public <C extends NotificationDeliveryMethodConfig> C getDeliveryMethodConfig(NotificationDeliveryMethod deliveryMethod) {
        NotificationSettings settings;
        if (deliveryMethod == NotificationDeliveryMethod.MOBILE_APP) {
            settings = this.systemSettings;
        } else {
            settings = this.settings;
        }
        return (C) settings.getDeliveryMethodsConfigs().get(deliveryMethod);
    }

    /**
     * 功能：获取`Processed Template`。
     * 参数：
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * - `recipient`：`recipient` 参数。
     * 返回：处理结果。
     */
    public <T extends DeliveryMethodNotificationTemplate> T getProcessedTemplate(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        T template = (T) templates.get(deliveryMethod);
        if (recipient != null) {
            Map<String, String> additionalTemplateContext = createTemplateContextForRecipient(recipient);
            if (template.getTemplatableValues().stream().anyMatch(value -> value.containsParams(additionalTemplateContext.keySet()))) {
                template = processTemplate(template, additionalTemplateContext);
            }
        }
        return template;
    }

    /**
     * 功能：处理`Template`。
     * 参数：
     * - `template`：`template` 参数。
     * - `additionalTemplateContext`：处理上下文。
     * 返回：处理结果。
     */
    private <T extends DeliveryMethodNotificationTemplate> T processTemplate(T template, Map<String, String> additionalTemplateContext) {
        Map<String, String> templateContext = new HashMap<>();
        if (request.getInfo() != null) {
            templateContext.putAll(request.getInfo().getTemplateData());
        }
        if (additionalTemplateContext != null) {
            templateContext.putAll(additionalTemplateContext);
        }
        if (templateContext.isEmpty()) return template;

        template = (T) template.copy();
        template.getTemplatableValues().forEach(templatableValue -> {
            String value = templatableValue.get();
            if (StringUtils.isNotEmpty(value)) {
                value = TemplateUtils.processTemplate(value, templateContext);
                templatableValue.set(value);
            }
        });
        return template;
    }

    /**
     * 功能：保存或创建上下文。
     * 参数：
     * - `recipient`：`recipient` 参数。
     * 返回：处理结果。
     */
    private Map<String, String> createTemplateContextForRecipient(NotificationRecipient recipient) {
        return Map.of(
                "recipientTitle", recipient.getTitle(),
                "recipientEmail", Strings.nullToEmpty(recipient.getEmail()),
                "recipientFirstName", Strings.nullToEmpty(recipient.getFirstName()),
                "recipientLastName", Strings.nullToEmpty(recipient.getLastName())
        );
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`NotificationProcessingContext` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
