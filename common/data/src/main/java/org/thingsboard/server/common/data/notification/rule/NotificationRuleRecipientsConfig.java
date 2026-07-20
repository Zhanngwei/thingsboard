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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `NotificationRuleRecipientsConfig` 是 ThingsBoard Common Data 中描述通知行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@JsonIgnoreProperties
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "triggerType", visible = true, include = JsonTypeInfo.As.EXISTING_PROPERTY, defaultImpl = DefaultNotificationRuleRecipientsConfig.class)
@JsonSubTypes({
        @Type(name = "ALARM", value = EscalatedNotificationRuleRecipientsConfig.class),
})
@Data
public abstract class NotificationRuleRecipientsConfig implements Serializable {

    /**
     * 类型，用于区分不同处理分支。
     */
    @NotNull
    private NotificationRuleTriggerType triggerType;

    /**
     * 功能：获取`Targets Table`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public abstract Map<Integer, List<UUID>> getTargetsTable();

}
