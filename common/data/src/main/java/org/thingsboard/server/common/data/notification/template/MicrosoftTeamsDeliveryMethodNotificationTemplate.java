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
package org.thingsboard.server.common.data.notification.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `MicrosoftTeamsDeliveryMethodNotificationTemplate` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DeliveryMethodNotificationTemplate`、`HasSubject`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class MicrosoftTeamsDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    /**
     * `subject` 字段，保存当前对象的对应属性。
     */
    private String subject;
    private String themeColor;
    /**
     * `button` 字段，保存当前对象的对应属性。
     */
    private Button button;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject),
            TemplatableValue.of(() -> button != null ? button.getText() : null,
                    processed -> { if (button != null) button.setText(processed); }),
            TemplatableValue.of(() -> button != null ? button.getLink() : null,
                    processed -> { if (button != null) button.setLink(processed); })
    );

    /**
     * 功能：创建 `MicrosoftTeamsDeliveryMethodNotificationTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public MicrosoftTeamsDeliveryMethodNotificationTemplate(MicrosoftTeamsDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.themeColor = other.themeColor;
        this.button = other.button != null ? new Button(other.button) : null;
    }

    /**
     * 功能：获取`Method`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

    /**
     * 功能：执行 `copy` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public MicrosoftTeamsDeliveryMethodNotificationTemplate copy() {
        return new MicrosoftTeamsDeliveryMethodNotificationTemplate(this);
    }

    /**
     * 中文说明：
     * 1. `Button` 是 ThingsBoard Common Data 中承载 `Button` 信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    @NoArgsConstructor
    public static class Button {
        /**
         * 是否启用`enabled`。
         */
        private boolean enabled;
        private String text;
        /**
         * 类型，用于区分不同处理分支。
         */
        private LinkType linkType;
        private String link;

        /**
         * 仪表盘ID，用于定位对应业务对象。
         */
        private UUID dashboardId;
        private String dashboardState;
        /**
         * 是否满足实体ID条件。
         */
        private boolean setEntityIdInState;

        /**
         * 功能：创建 `MicrosoftTeamsDeliveryMethodNotificationTemplate` 实例，并初始化必要字段。
         * 参数：
         * - `other`：`other` 参数。
         * 返回：新创建的对象实例。
         */
        public Button(Button other) {
            this.enabled = other.enabled;
            this.text = other.text;
            this.linkType = other.linkType;
            this.link = other.link;
            this.dashboardId = other.dashboardId;
            this.dashboardState = other.dashboardState;
            this.setEntityIdInState = other.setEntityIdInState;
        }

        /**
         * 中文说明：
         * 1. `LinkType` 是 ThingsBoard Common Data 中定义 `Link Type` 固定取值的枚举类型。
         * 2. 它列出当前流程允许使用的有限状态、模式或类别。
         * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
         * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
         * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
         * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
         */
        public enum LinkType {
            LINK, DASHBOARD
        }
    }

}
