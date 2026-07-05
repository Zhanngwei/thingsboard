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
 * 1. 类目的：`WebDeliveryMethodNotificationTemplate` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`WebDeliveryMethodNotificationTemplate` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
