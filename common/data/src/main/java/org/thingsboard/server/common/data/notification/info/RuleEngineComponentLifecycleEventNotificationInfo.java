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
package org.thingsboard.server.common.data.notification.info;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Map;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

/**
 * 中文说明：
 * 1. `RuleEngineComponentLifecycleEventNotificationInfo` 是 ThingsBoard Common Data 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `RuleOriginatedNotificationInfo`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleEngineComponentLifecycleEventNotificationInfo implements RuleOriginatedNotificationInfo {

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    private RuleChainId ruleChainId;
    private String ruleChainName;
    /**
     * `componentId`ID，用于定位对应业务对象。
     */
    private EntityId componentId;
    private String componentName;
    /**
     * `action` 字段，保存当前对象的对应属性。
     */
    private String action;
    private ComponentLifecycleEvent eventType;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    private String error;

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "ruleChainId", ruleChainId.toString(),
                "ruleChainName", ruleChainName,
                "componentId", componentId.toString(),
                "componentType", componentId.getEntityType().getNormalName(),
                "componentName", componentName,
                "action", action,
                "eventType", eventType.name().toLowerCase(),
                "error", error
        );
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityId getStateEntityId() {
        return ruleChainId;
    }

}
