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

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.info.EdgeCommunicationFailureNotificationInfo;
import org.thingsboard.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.thingsboard.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.thingsboard.server.common.data.notification.rule.trigger.config.EdgeCommunicationFailureNotificationRuleTriggerConfig;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

/**
 * 中文说明：
 * 1. `EdgeCommunicationFailureTriggerProcessor` 是 ThingsBoard Application 中处理边缘节点的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTriggerProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Service
@RequiredArgsConstructor
public class EdgeCommunicationFailureTriggerProcessor implements NotificationRuleTriggerProcessor<EdgeCommunicationFailureTrigger, EdgeCommunicationFailureNotificationRuleTriggerConfig> {

    /**
     * 功能：执行 `matchesFilter` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * - `triggerConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean matchesFilter(EdgeCommunicationFailureTrigger trigger, EdgeCommunicationFailureNotificationRuleTriggerConfig triggerConfig) {
        if (CollectionUtils.isNotEmpty(triggerConfig.getEdges())) {
            return !triggerConfig.getEdges().contains(trigger.getEdgeId().getId());
        }
        return true;
    }

    /**
     * 功能：执行 `constructNotificationInfo` 对应的处理。
     * 参数：
     * - `trigger`：`trigger` 参数。
     * 返回：处理结果。
     */
    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(EdgeCommunicationFailureTrigger trigger) {
        return EdgeCommunicationFailureNotificationInfo.builder()
                .tenantId(trigger.getTenantId())
                .edgeId(trigger.getEdgeId())
                .customerId(trigger.getCustomerId())
                .edgeName(trigger.getEdgeName())
                .failureMsg(truncateFailureMsg(trigger.getFailureMsg()))
                .build();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
    }

    /**
     * 功能：执行 `truncateFailureMsg` 对应的处理。
     * 参数：
     * - `input`：`input` 参数。
     * 返回：文本结果。
     */
    private String truncateFailureMsg(String input) {
        int maxLength = 500;
        if (input != null && input.length() > maxLength) {
            return input.substring(0, maxLength);
        }
        return input;
    }
}
