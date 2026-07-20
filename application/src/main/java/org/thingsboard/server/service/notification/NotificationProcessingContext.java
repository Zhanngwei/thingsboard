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
 * 1. `NotificationProcessingContext` 是 ThingsBoard Application 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
