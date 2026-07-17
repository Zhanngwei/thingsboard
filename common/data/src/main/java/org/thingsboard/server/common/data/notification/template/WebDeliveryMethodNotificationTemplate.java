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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `WebDeliveryMethodNotificationTemplate` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DeliveryMethodNotificationTemplate`、`HasSubject`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WebDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    /**
     * `subject` 字段，保存当前对象的对应属性。
     */
    @NoXss(fieldName = "web notification subject")
    @Length(fieldName = "web notification subject", max = 150, message = "cannot be longer than 150 chars")
    @NotEmpty
    private String subject;
    private JsonNode additionalConfig;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject),
            TemplatableValue.of(this::getButtonText, this::setButtonText),
            TemplatableValue.of(this::getButtonLink, this::setButtonLink)
    );

    /**
     * 功能：创建 `WebDeliveryMethodNotificationTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public WebDeliveryMethodNotificationTemplate(WebDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.additionalConfig = other.additionalConfig != null ? other.additionalConfig.deepCopy() : null;
    }

    /**
     * 功能：获取`Body`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Length(fieldName = "web notification message", max = 250, message = "cannot be longer than 250 chars")
    @Override
    public String getBody() {
        return super.getBody();
    }

    /**
     * 功能：获取`Button Text`。
     * 参数：无。
     * 返回：文本结果。
     */
    @NoXss(fieldName = "web notification button text")
    @Length(fieldName = "web notification button text", max = 50, message = "cannot be longer than 50 chars")
    @JsonIgnore
    public String getButtonText() {
        return getButtonConfigProperty("text");
    }

    /**
     * 功能：更新`Button Text`。
     * 参数：
     * - `buttonText`：`buttonText` 参数。
     * 返回：无。
     */
    @JsonIgnore
    public void setButtonText(String buttonText) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("text", new TextNode(buttonText));
        });
    }

    /**
     * 功能：获取`Button Link`。
     * 参数：无。
     * 返回：文本结果。
     */
    @NoXss(fieldName = "web notification button link")
    @Length(fieldName = "web notification button link", max = 300, message = "cannot be longer than 300 chars")
    @JsonIgnore
    public String getButtonLink() {
        return getButtonConfigProperty("link");
    }

    /**
     * 功能：更新`Button Link`。
     * 参数：
     * - `buttonLink`：`buttonLink` 参数。
     * 返回：无。
     */
    @JsonIgnore
    public void setButtonLink(String buttonLink) {
        getButtonConfig().ifPresent(buttonConfig -> {
            buttonConfig.set("link", new TextNode(buttonLink));
        });
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `property`：`property` 参数。
     * 返回：文本结果。
     */
    private String getButtonConfigProperty(String property) {
        return getButtonConfig()
                .map(buttonConfig -> buttonConfig.get(property))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText).orElse(null);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    private Optional<ObjectNode> getButtonConfig() {
        return Optional.ofNullable(additionalConfig)
                .map(config -> config.get("actionButtonConfig")).filter(JsonNode::isObject)
                .map(config -> (ObjectNode) config);
    }

    /**
     * 功能：获取`Method`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.WEB;
    }

    /**
     * 功能：执行 `copy` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WebDeliveryMethodNotificationTemplate copy() {
        return new WebDeliveryMethodNotificationTemplate(this);
    }

}
