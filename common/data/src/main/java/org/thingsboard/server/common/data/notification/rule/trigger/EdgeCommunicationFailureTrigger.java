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
package org.thingsboard.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `EdgeCommunicationFailureTrigger` 是 ThingsBoard Common Data 中承载边缘节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `NotificationRuleTrigger`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Builder
public class EdgeCommunicationFailureTrigger implements NotificationRuleTrigger {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final CustomerId customerId;
    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    private final EdgeId edgeId;
    private final String edgeName;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final String failureMsg;
    private final String error;

    /**
     * 功能：执行 `deduplicate` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean deduplicate() {
        return true;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getDeduplicationKey() {
        return String.join(":", NotificationRuleTrigger.super.getDeduplicationKey(), error);
    }

    /**
     * 功能：获取持续时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getDefaultDeduplicationDuration() {
        return TimeUnit.MINUTES.toMillis(30);
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.EDGE_COMMUNICATION_FAILURE;
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityId getOriginatorEntityId() {
        return edgeId;
    }
}
