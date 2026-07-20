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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_DATA_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ERROR_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_METADATA_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_MSG_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_RELATION_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TYPE_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.RULE_NODE_DEBUG_EVENT_TABLE_NAME;

/**
 * 中文说明：
 * 1. `RuleNodeDebugEventEntity` 是 ThingsBoard DAO 中表示事件持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `EventEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = RULE_NODE_DEBUG_EVENT_TABLE_NAME)
@NoArgsConstructor
public class RuleNodeDebugEventEntity extends EventEntity<RuleNodeDebugEvent> implements BaseEntity<RuleNodeDebugEvent> {

    /**
     * 事件，用于区分不同处理分支。
     */
    @Column(name = EVENT_TYPE_COLUMN_NAME)
    private String eventType;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = EVENT_ENTITY_ID_COLUMN_NAME)
    private UUID eventEntityId;
    /**
     * 实体，用于区分不同处理分支。
     */
    @Column(name = EVENT_ENTITY_TYPE_COLUMN_NAME)
    private String eventEntityType;
    /**
     * 消息ID，用于定位对应业务对象。
     */
    @Column(name = EVENT_MSG_ID_COLUMN_NAME)
    private UUID msgId;
    /**
     * 消息，用于区分不同处理分支。
     */
    @Column(name = EVENT_MSG_TYPE_COLUMN_NAME)
    private String msgType;
    /**
     * 数据，用于区分不同处理分支。
     */
    @Column(name = EVENT_DATA_TYPE_COLUMN_NAME)
    private String dataType;
    /**
     * 关系，用于区分不同处理分支。
     */
    @Column(name = EVENT_RELATION_TYPE_COLUMN_NAME)
    private String relationType;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Column(name = EVENT_DATA_COLUMN_NAME)
    private String data;
    /**
     * `metadata` 字段，保存当前对象的对应属性。
     */
    @Column(name = EVENT_METADATA_COLUMN_NAME)
    private String metadata;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @Column(name = EVENT_ERROR_COLUMN_NAME)
    private String error;

    /**
     * 功能：创建 `RuleNodeDebugEventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleNodeDebugEventEntity(RuleNodeDebugEvent event) {
        super(event);
        this.eventType = event.getEventType();
        if (event.getEventEntity() != null) {
            this.eventEntityId = event.getEventEntity().getId();
            this.eventEntityType = event.getEventEntity().getEntityType().name();
        }
        this.msgId = event.getMsgId();
        this.msgType = event.getMsgType();
        this.dataType = event.getDataType();
        this.relationType = event.getRelationType();
        this.data = event.getData();
        this.metadata = event.getMetadata();
        this.error = event.getError();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeDebugEvent toData() {
        var builder = RuleNodeDebugEvent.builder()
                .tenantId(TenantId.fromUUID(tenantId))
                .entityId(entityId)
                .serviceId(serviceId)
                .id(id)
                .ts(ts)
                .eventType(eventType)
                .msgId(msgId)
                .msgType(msgType)
                .dataType(dataType)
                .relationType(relationType)
                .data(data)
                .metadata(metadata)
                .error(error);
        if (eventEntityId != null) {
            builder.eventEntity(EntityIdFactory.getByTypeAndUuid(eventEntityType, eventEntityId));
        }
        return builder.build();
    }

}
