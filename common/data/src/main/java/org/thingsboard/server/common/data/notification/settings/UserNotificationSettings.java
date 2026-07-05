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
 * 1. 类目的：`UserNotificationSettings` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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
     * 1. 类目的：`NotificationPref` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
     * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`UserNotificationSettings` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
