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
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `LifecycleEvent` 是 ThingsBoard Common Data 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Event`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class LifecycleEvent extends Event {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -3247420461850911549L;

    /**
     * 功能：创建 `LifecycleEvent` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `serviceId`：服务ID。
     * - `id`：`id`ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private LifecycleEvent(TenantId tenantId, UUID entityId, String serviceId,
                           UUID id, long ts,
                           String lcEventType, boolean success, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.lcEventType = lcEventType;
        this.success = success;
        this.error = error;
    }

    /**
     * 事件，用于区分不同处理分支。
     */
    @Getter
    private final String lcEventType;
    /**
     * 当前操作是否成功。
     */
    @Getter
    private final boolean success;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @Getter
    @Setter
    private String error;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventType getType() {
        return EventType.LC_EVENT;
    }

    /**
     * 功能：执行 `toInfo` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public EventInfo toInfo(EntityType entityType) {
        EventInfo eventInfo = super.toInfo(entityType);
        var json = (ObjectNode) eventInfo.getBody();
        json.put("event", lcEventType)
                .put("success", success);
        if (error != null) {
            json.put("error", error);
        }
        return eventInfo;
    }

}
