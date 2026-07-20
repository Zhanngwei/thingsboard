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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ACTION_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_BODY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_EDGE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_SEQUENTIAL_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_TYPE_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_EVENT_UID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EPOCH_DIFF;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

/**
 * 中文说明：
 * 1. `EdgeEventEntity` 是 ThingsBoard DAO 中表示事件持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = EDGE_EVENT_TABLE_NAME)
@NoArgsConstructor
public class EdgeEventEntity extends BaseSqlEntity<EdgeEvent> implements BaseEntity<EdgeEvent> {

    /**
     * 序号ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_EVENT_SEQUENTIAL_ID_PROPERTY)
    protected long seqId;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_EVENT_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_EVENT_EDGE_ID_PROPERTY)
    private UUID edgeId;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_EVENT_ENTITY_ID_PROPERTY)
    private UUID entityId;

    /**
     * 边缘节点，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_TYPE_PROPERTY)
    private EdgeEventType edgeEventType;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = EDGE_EVENT_ACTION_PROPERTY)
    private EdgeEventActionType edgeEventAction;

    /**
     * 实体对象，用于描述当前业务场景。
     */
    @Type(type = "json")
    @Column(name = EDGE_EVENT_BODY_PROPERTY)
    private JsonNode entityBody;

    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_EVENT_UID_PROPERTY)
    private String edgeEventUid;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = TS_COLUMN)
    private long ts;

    /**
     * 功能：创建 `EdgeEventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：新创建的对象实例。
     */
    public EdgeEventEntity(EdgeEvent edgeEvent) {
        if (edgeEvent.getId() != null) {
            this.setUuid(edgeEvent.getId().getId());
            this.ts = getTs(edgeEvent.getId().getId());
        } else {
            this.ts = System.currentTimeMillis();
        }
        this.setCreatedTime(edgeEvent.getCreatedTime());
        if (edgeEvent.getTenantId() != null) {
            this.tenantId = edgeEvent.getTenantId().getId();
        }
        if (edgeEvent.getEdgeId() != null) {
            this.edgeId = edgeEvent.getEdgeId().getId();
        }
        if (edgeEvent.getEntityId() != null) {
            this.entityId = edgeEvent.getEntityId();
        }
        this.edgeEventType = edgeEvent.getType();
        this.edgeEventAction = edgeEvent.getAction();
        this.entityBody = edgeEvent.getBody();
        this.edgeEventUid = edgeEvent.getUid();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EdgeEvent toData() {
        EdgeEvent edgeEvent = new EdgeEvent(new EdgeEventId(this.getUuid()));
        edgeEvent.setCreatedTime(createdTime);
        edgeEvent.setTenantId(TenantId.fromUUID(tenantId));
        edgeEvent.setEdgeId(new EdgeId(edgeId));
        if (entityId != null) {
            edgeEvent.setEntityId(entityId);
        }
        edgeEvent.setType(edgeEventType);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setBody(entityBody);
        edgeEvent.setUid(edgeEventUid);
        edgeEvent.setSeqId(seqId);
        return edgeEvent;
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：
     * - `uuid`：`uuid`ID。
     * 返回：数值结果。
     */
    private static long getTs(UUID uuid) {
        return (uuid.timestamp() - EPOCH_DIFF) / 10000;
    }
}
