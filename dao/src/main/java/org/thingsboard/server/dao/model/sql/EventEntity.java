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
 * 1. 类目的：`EventEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
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

/*
 * 本类总结：
 * 1. 核心职责：`EventEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
