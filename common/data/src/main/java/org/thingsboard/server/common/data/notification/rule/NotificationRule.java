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
package org.thingsboard.server.common.data.notification.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.NotificationRuleId;
import org.thingsboard.server.common.data.id.NotificationTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `NotificationRule` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasTenantId`、`HasName`、`ExportableEntity`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationRule extends BaseData<NotificationRuleId> implements HasTenantId, HasName, ExportableEntity<NotificationRuleId>, Serializable {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NotBlank
    @NoXss
    @Length(max = 255, message = "cannot be longer than 255 chars")
    private String name;
    private boolean enabled;
    /**
     * `templateId`ID，用于定位对应业务对象。
     */
    @NotNull
    private NotificationTemplateId templateId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @NotNull
    private NotificationRuleTriggerType triggerType;
    /**
     * 配置，保存当前对象的配置选项。
     */
    @NotNull
    @Valid
    private NotificationRuleTriggerConfig triggerConfig;
    /**
     * 配置，保存当前对象的配置选项。
     */
    @NotNull
    @Valid
    private NotificationRuleRecipientsConfig recipientsConfig;

    /**
     * 配置，保存当前对象的配置选项。
     */
    private NotificationRuleConfig additionalConfig;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private NotificationRuleId externalId;

    /**
     * 功能：创建 `NotificationRule` 实例，并初始化必要字段。
     * 参数：
     * - `other`：`other` 参数。
     * 返回：新创建的对象实例。
     */
    public NotificationRule(NotificationRule other) {
        super(other);
        this.tenantId = other.tenantId;
        this.name = other.name;
        this.enabled = other.enabled;
        this.templateId = other.templateId;
        this.triggerType = other.triggerType;
        this.triggerConfig = other.triggerConfig;
        this.recipientsConfig = other.recipientsConfig;
        this.additionalConfig = other.additionalConfig;
        this.externalId = other.externalId;
    }

    /**
     * 功能：判断`Valid`。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    @AssertTrue(message = "trigger type not matching")
    public boolean isValid() {
        return triggerType == triggerConfig.getTriggerType() &&
                triggerType == recipientsConfig.getTriggerType();
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getDeduplicationKey() {
        String targets = recipientsConfig.getTargetsTable().values().stream()
                .flatMap(List::stream).sorted().map(Object::toString)
                .collect(Collectors.joining(","));
        return String.join(":", targets, triggerConfig.getDeduplicationKey());
    }

}
