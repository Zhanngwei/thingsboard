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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.info.NotificationInfo;
import org.thingsboard.server.common.data.notification.targets.MicrosoftTeamsNotificationTargetConfig;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate;
import org.thingsboard.server.common.data.notification.template.MicrosoftTeamsDeliveryMethodNotificationTemplate.Button.LinkType;
import org.thingsboard.server.service.notification.NotificationProcessingContext;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `MicrosoftTeamsNotificationChannel` 是 ThingsBoard Application 中处理通知的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationChannel`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@RequiredArgsConstructor
public class MicrosoftTeamsNotificationChannel implements NotificationChannel<MicrosoftTeamsNotificationTargetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final SystemSecurityService systemSecurityService;

    @Setter
    private RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .setReadTimeout(Duration.of(15, ChronoUnit.SECONDS))
            .build();

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `targetConfig`：配置对象。
     * - `processedTemplate`：`processedTemplate` 参数。
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void sendNotification(MicrosoftTeamsNotificationTargetConfig targetConfig, MicrosoftTeamsDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        Message message = new Message();
        message.setThemeColor(Strings.emptyToNull(processedTemplate.getThemeColor()));
        if (StringUtils.isEmpty(processedTemplate.getSubject())) {
            message.setText(processedTemplate.getBody());
        } else {
            message.setSummary(processedTemplate.getSubject());
            Message.Section section = new Message.Section();
            section.setActivityTitle(processedTemplate.getSubject());
            section.setActivitySubtitle(processedTemplate.getBody());
            message.setSections(List.of(section));
        }
        var button = processedTemplate.getButton();
        if (button != null && button.isEnabled()) {
            String uri;
            if (button.getLinkType() == LinkType.DASHBOARD) {
                String state = null;
                if (button.isSetEntityIdInState() || StringUtils.isNotEmpty(button.getDashboardState())) {
                    ObjectNode stateObject = JacksonUtil.newObjectNode();
                    if (button.isSetEntityIdInState()) {
                        stateObject.putObject("params")
                                .set("entityId", Optional.ofNullable(ctx.getRequest().getInfo())
                                        .map(NotificationInfo::getStateEntityId)
                                        .map(JacksonUtil::valueToTree)
                                        .orElse(null));
                    } else {
                        stateObject.putObject("params");
                    }
                    if (StringUtils.isNotEmpty(button.getDashboardState())) {
                        stateObject.put("id", button.getDashboardState());
                    }
                    state = Base64.encodeBase64String(JacksonUtil.OBJECT_MAPPER.writeValueAsBytes(List.of(stateObject)));
                }
                String baseUrl = systemSecurityService.getBaseUrl(ctx.getTenantId(), null, null);
                if (StringUtils.isEmpty(baseUrl)) {
                    throw new IllegalStateException("Failed to determine base url to construct dashboard link");
                }
                uri = baseUrl + "/dashboards/" + button.getDashboardId();
                if (state != null) {
                    uri += "?state=" + state;
                }
            } else {
                uri = button.getLink();
            }
            if (StringUtils.isNotBlank(uri) && button.getText() != null) {
                Message.ActionCard actionCard = new Message.ActionCard();
                actionCard.setType("OpenUri");
                actionCard.setName(button.getText());
                var target = new Message.ActionCard.Target("default", uri);
                actionCard.setTargets(List.of(target));
                message.setPotentialAction(List.of(actionCard));
            }
        }

        restTemplate.postForEntity(targetConfig.getWebhookUrl(), message, String.class);
    }

    /**
     * 功能：执行 `check` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void check(TenantId tenantId) throws Exception {
    }

    /**
     * 功能：获取`Delivery Method`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.MICROSOFT_TEAMS;
    }

    /**
     * 中文说明：
     * 1. `Message` 是 ThingsBoard Application 中承载消息信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    public static class Message {
        /**
         * 类型，用于区分不同处理分支。
         */
        @JsonProperty("@type")
        private final String type = "MessageCard";
        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        @JsonProperty("@context")
        private final String context = "http://schema.org/extensions";
        private String themeColor;
        /**
         * `summary` 字段，保存当前对象的对应属性。
         */
        private String summary;
        private String text;
        /**
         * `sections`列表，用于保存一组待处理对象。
         */
        private List<Section> sections;
        private List<ActionCard> potentialAction;

        /**
         * 中文说明：
         * 1. `Section` 是 ThingsBoard Application 中承载 `Section` 信息的数据类型。
         * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
         * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
         * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
         * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
         * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
         */
        @Data
        public static class Section {
            /**
             * `activityTitle` 字段，保存当前对象的对应属性。
             */
            private String activityTitle;
            private String activitySubtitle;
            /**
             * 图片资源，表示当前对象的对应属性。
             */
            private String activityImage;
            private List<Fact> facts;
            /**
             * 是否满足Markdown条件。
             */
            private boolean markdown;

            /**
             * 中文说明：
             * 1. `Fact` 是 ThingsBoard Application 中承载 `Fact` 信息的数据类型。
             * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
             * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
             * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
             * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
             * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
             */
            @Data
            public static class Fact {
                /**
                 * 名称，用于标识或展示当前对象。
                 */
                private final String name;
                private final String value;
            }
        }

        /**
         * 中文说明：
         * 1. `ActionCard` 是 ThingsBoard Application 中承载 `Action Card` 信息的数据类型。
         * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
         * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
         * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
         * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
         * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
         */
        @Data
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class ActionCard {
            /**
             * 类型，用于区分不同处理分支。
             */
            @JsonProperty("@type")
            private String type; // ActionCard, OpenUri
            private String name;
            /**
             * `inputs`列表，用于保存一组待处理对象。
             */
            private List<Input> inputs; // for ActionCard
            private List<Action> actions; // for ActionCard
            /**
             * `targets`列表，用于保存一组待处理对象。
             */
            private List<Target> targets;

            /**
             * 中文说明：
             * 1. `Input` 是 ThingsBoard Application 中承载 `Input` 信息的数据类型。
             * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
             * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
             * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
             * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
             * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
             */
            @Data
            public static class Input {
                /**
                 * 类型，用于区分不同处理分支。
                 */
                @JsonProperty("@type")
                private String type; // TextInput, DateInput, MultichoiceInput
                private String id;
                /**
                 * 是否为`multiple`。
                 */
                private boolean isMultiple;
                private String title;
                /**
                 * 是否为`multi select`。
                 */
                private boolean isMultiSelect;

                /**
                 * 中文说明：
                 * 1. `Choice` 是 ThingsBoard Application 中承载 `Choice` 信息的数据类型。
                 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
                 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
                 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
                 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
                 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
                 */
                @Data
                public static class Choice {
                    /**
                     * `display` 字段，保存当前对象的对应属性。
                     */
                    private final String display;
                    private final String value;
                }
            }

            /**
             * 中文说明：
             * 1. `Action` 是 ThingsBoard Application 中承载 `Action` 信息的数据类型。
             * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
             * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
             * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
             * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
             * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
             */
            @Data
            public static class Action {
                /**
                 * 类型，用于区分不同处理分支。
                 */
                @JsonProperty("@type")
                private final String type; // HttpPOST
                private final String name;
                /**
                 * 目标对象，表示当前对象的对应属性。
                 */
                private final String target; // url
            }

            /**
             * 中文说明：
             * 1. `Target` 是 ThingsBoard Application 中承载 `Target` 信息的数据类型。
             * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
             * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
             * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
             * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
             * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
             */
            @Data
            public static class Target {
                /**
                 * `os` 字段，保存当前对象的对应属性。
                 */
                private final String os;
                private final String uri;
            }
        }

    }

}
