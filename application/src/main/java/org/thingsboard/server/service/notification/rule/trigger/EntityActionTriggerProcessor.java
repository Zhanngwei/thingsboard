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
package org.thingsboard.server.service.notification.rule.trigger;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.notification.info.EntityActionNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EntityActionNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.thingsboard.server.common.data.notification.rule.trigger.EntityActionTrigger;

import static org.thingsboard.server.common.data.util.CollectionsUtil.emptyOrContains;

/**
 * 中文说明：
 * 1. `EntityActionTriggerProcessor` 是 ThingsBoard Application 中处理实体的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
public class EntityActionTriggerProcessor implements NotificationRuleTriggerProcessor<EntityActionTrigger, EntityActionNotificationRuleTriggerConfig> {

    /**
     * 功能：执行 `matchesFilter` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesFilter(EntityActionTrigger trigger, EntityActionNotificationRuleTriggerConfig triggerConfig) {
        return ((trigger.getActionType() == ActionType.ADDED && triggerConfig.isCreated())
                || (trigger.getActionType() == ActionType.UPDATED && triggerConfig.isUpdated())
                || (trigger.getActionType() == ActionType.DELETED && triggerConfig.isDeleted()))
                && emptyOrContains(triggerConfig.getEntityTypes(), trigger.getEntityId().getEntityType());
    }

    /**
     * 功能：执行 `constructNotificationInfo` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EntityActionTrigger trigger) {
        return EntityActionNotificationInfo.builder()
                .entityId(trigger.getEntityId())
                .entityName(trigger.getEntity().getName())
                .actionType(trigger.getActionType())
                .userId(trigger.getUser().getUuidId())
                .userTitle(trigger.getUser().getTitle())
                .userEmail(trigger.getUser().getEmail())
                .userFirstName(trigger.getUser().getFirstName())
                .userLastName(trigger.getUser().getLastName())
                .entityCustomerId(trigger.getEntity() instanceof HasCustomerId ?
                        ((HasCustomerId) trigger.getEntity()).getCustomerId() :
                        trigger.getUser().getCustomerId())
                .build();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

}
