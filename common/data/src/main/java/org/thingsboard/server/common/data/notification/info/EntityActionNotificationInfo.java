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
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.common.data.util.CollectionsUtil.mapOf;

/**
 * 中文说明：
 * 1. `EntityActionNotificationInfo` 是 ThingsBoard Common Data 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `RuleOriginatedNotificationInfo`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityActionNotificationInfo implements RuleOriginatedNotificationInfo {

    /**
     * 实体ID，用于定位对应业务对象。
     */
    private EntityId entityId;
    private String entityName;
    /**
     * 类型，用于区分不同处理分支。
     */
    private ActionType actionType;
    private CustomerId entityCustomerId;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    private UUID userId;
    private String userTitle;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private String userEmail;
    private String userFirstName;
    /**
     * 用户，用于标识或展示当前对象。
     */
    private String userLastName;

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Map<String, String> getTemplateData() {
        return mapOf(
                "entityType", entityId.getEntityType().getNormalName(),
                "entityId", entityId.toString(),
                "entityName", entityName,
                "actionType", actionType.name().toLowerCase(),
                "userId", userId.toString(),
                "userTitle", userTitle,
                "userEmail", userEmail,
                "userFirstName", userFirstName,
                "userLastName", userLastName
        );
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CustomerId getAffectedCustomerId() {
        return entityCustomerId;
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityId getStateEntityId() {
        return entityId;
    }

}
