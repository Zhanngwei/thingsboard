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
package org.thingsboard.server.common.data.notification.targets;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 * 中文说明：
 * 1. `MicrosoftTeamsNotificationTargetConfig` 是 ThingsBoard Common Data 中描述通知行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NotificationTargetConfig`、`NotificationRecipient`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MicrosoftTeamsNotificationTargetConfig extends NotificationTargetConfig implements NotificationRecipient {

    /**
     * Webhook 地址，用于定位外部资源或本地资源。
     */
    @NotBlank
    private String webhookUrl;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NotEmpty
    private String channelName;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationTargetType getType() {
        return NotificationTargetType.MICROSOFT_TEAMS;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Object getId() {
        return webhookUrl;
    }

    /**
     * 功能：获取`Title`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getTitle() {
        return channelName;
    }

}
