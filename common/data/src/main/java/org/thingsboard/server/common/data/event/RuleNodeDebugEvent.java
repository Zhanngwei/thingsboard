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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleNodeDebugEvent` 是 ThingsBoard Common Data 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Event`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString
@EqualsAndHashCode(callSuper = true)
public class RuleNodeDebugEvent extends Event {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -6575797430064573984L;

    /**
     * 功能：创建 `RuleNodeDebugEvent` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `serviceId`：服务ID。
     * - `id`：`id`ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private RuleNodeDebugEvent(TenantId tenantId, UUID entityId, String serviceId, UUID id, long ts,
                               String eventType, EntityId eventEntity, UUID msgId,
                               String msgType, String dataType, String relationType,
                               String data, String metadata, String error) {
        super(tenantId, entityId, serviceId, id, ts);
        this.eventType = eventType;
        this.eventEntity = eventEntity;
        this.msgId = msgId;
        this.msgType = msgType;
        this.dataType = dataType;
        this.relationType = relationType;
        this.data = data;
        this.metadata = metadata;
        this.error = error;
    }

    /**
     * 事件，用于区分不同处理分支。
     */
    @Getter
    private final String eventType;
    /**
     * 实体对象，用于描述当前业务场景。
     */
    @Getter
    private final EntityId eventEntity;
    /**
     * 消息ID，用于定位对应业务对象。
     */
    @Getter
    private final UUID msgId;
    /**
     * 消息，用于区分不同处理分支。
     */
    @Getter
    private final String msgType;
    /**
     * 数据，用于区分不同处理分支。
     */
    @Getter
    private final String dataType;
    /**
     * 关系，用于区分不同处理分支。
     */
    @Getter
    private final String relationType;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Getter
    @Setter
    private String data;
    /**
     * `metadata` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    private String metadata;
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
        return EventType.DEBUG_RULE_NODE;
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
        json.put("type", eventType);
        if (eventEntity != null) {
            json.put("entityId", eventEntity.getId().toString())
                    .put("entityType", eventEntity.getEntityType().name());
        }
        if (msgId != null) {
            json.put("msgId", msgId.toString());
        }
        putNotNull(json, "msgType", msgType);
        putNotNull(json, "dataType", dataType);
        putNotNull(json, "relationType", relationType);
        putNotNull(json, "data", data);
        putNotNull(json, "metadata", metadata);
        putNotNull(json, "error", error);
        return eventInfo;
    }

}
