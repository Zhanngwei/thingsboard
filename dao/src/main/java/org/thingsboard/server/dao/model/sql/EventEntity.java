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
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EVENT_ENTITY_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_SERVICE_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EVENT_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

/**
 * 中文说明：
 * 1. `EventEntity` 是 ThingsBoard DAO 中表示事件持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Event`、`BaseEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@NoArgsConstructor
@MappedSuperclass
public abstract class EventEntity<T extends Event> implements BaseEntity<T> {

    public static final Map<String, String> eventColumnMap = new HashMap<>();

    static {
        eventColumnMap.put("createdTime", "ts");
    }

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = EVENT_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    protected UUID tenantId;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = EVENT_ENTITY_ID_PROPERTY, columnDefinition = "uuid")
    protected UUID entityId;

    /**
     * 服务ID，用于定位对应业务对象。
     */
    @Column(name = EVENT_SERVICE_ID_PROPERTY)
    protected String serviceId;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Column(name = TS_COLUMN)
    protected long ts;

    /**
     * 功能：创建 `EventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `serviceId`：服务ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public EventEntity(UUID id, UUID tenantId, UUID entityId, String serviceId, long ts) {
        this.id = id;
        this.tenantId = tenantId;
        this.entityId = entityId;
        this.serviceId = serviceId;
        this.ts = ts;
    }

    /**
     * 功能：创建 `EventEntity` 实例，并初始化必要字段。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：新创建的对象实例。
     */
    public EventEntity(Event event) {
        this.id = event.getId().getId();
        this.tenantId = event.getTenantId().getId();
        this.entityId = event.getEntityId();
        this.serviceId = event.getServiceId();
        this.ts = event.getCreatedTime();
    }

    /**
     * 功能：获取`Uuid`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UUID getUuid() {
        return id;
    }

    /**
     * 功能：更新`Uuid`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void setUuid(UUID id) {
        this.id = id;
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public long getCreatedTime() {
        return ts;
    }

    /**
     * 功能：更新创建时间。
     * 参数：
     * - `createdTime`：`createdTime` 参数。
     * 返回：无。
     */
    @Override
    public void setCreatedTime(long createdTime) {
        ts = createdTime;
    }

}
