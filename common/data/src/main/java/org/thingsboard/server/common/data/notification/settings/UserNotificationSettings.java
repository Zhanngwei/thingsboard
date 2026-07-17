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
package org.thingsboard.server.common.data.notification.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.NotificationType;
import org.thingsboard.server.common.data.notification.targets.NotificationTargetType;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `UserNotificationSettings` 是 ThingsBoard Common Data 中描述通知行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class UserNotificationSettings {

    /**
     * `prefs`映射关系，用于按键查找对应值。
     */
    @NotNull
    @Valid
    private final Map<NotificationType, NotificationPref> prefs;

    public static final UserNotificationSettings DEFAULT = new UserNotificationSettings(Collections.emptyMap());

    public static final Set<NotificationDeliveryMethod> deliveryMethods = NotificationTargetType.PLATFORM_USERS.getSupportedDeliveryMethods();

    /**
     * 功能：创建 `UserNotificationSettings` 实例，并初始化必要字段。
     * 参数：
     * - `prefs`：键值映射。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public UserNotificationSettings(@JsonProperty("prefs") Map<NotificationType, NotificationPref> prefs) {
        this.prefs = prefs;
    }

    /**
     * 功能：判断`Enabled`。
     * 参数：
     * - `notificationType`：类型。
     * - `deliveryMethod`：`deliveryMethod` 参数。
     * 返回：判断结果。
     */
    public boolean isEnabled(NotificationType notificationType, NotificationDeliveryMethod deliveryMethod) {
        NotificationPref pref = prefs.get(notificationType);
        if (pref == null) {
            return true;
        }
        if (!pref.isEnabled()) {
            return false;
        }
        return pref.getEnabledDeliveryMethods().getOrDefault(deliveryMethod, true);
    }

    /**
     * 中文说明：
     * 1. `NotificationPref` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    public static class NotificationPref {
        /**
         * 是否启用`enabled`。
         */
        private boolean enabled;
        /**
         * `enabledDeliveryMethods`映射关系，用于按键查找对应值。
         */
        @NotNull
        private Map<NotificationDeliveryMethod, Boolean> enabledDeliveryMethods;

        /**
         * 功能：保存或创建`Default`。
         * 参数：无。
         * 返回：处理结果。
         */
        public static NotificationPref createDefault() {
            NotificationPref pref = new NotificationPref();
            pref.setEnabled(true);
            pref.setEnabledDeliveryMethods(deliveryMethods.stream().collect(Collectors.toMap(v -> v, v -> true)));
            return pref;
        }

        /**
         * 功能：判断`Valid`。
         * 参数：无。
         * 返回：判断结果。
         */
        @JsonIgnore
        @AssertTrue(message = "Only email, Web and SMS delivery methods are allowed")
        public boolean isValid() {
            return enabledDeliveryMethods.entrySet().stream()
                    .allMatch(entry -> deliveryMethods.contains(entry.getKey()) && entry.getValue() != null);
        }
    }

}
