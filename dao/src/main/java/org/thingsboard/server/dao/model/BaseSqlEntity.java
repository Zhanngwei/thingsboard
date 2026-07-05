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
package org.thingsboard.server.dao.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.dao.DaoUtil;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 13.07.17.
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseSqlEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@Data
@MappedSuperclass
public abstract class BaseSqlEntity<D> implements BaseEntity<D> {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = ModelConstants.ID_PROPERTY, columnDefinition = "uuid")
    protected UUID id;

    /**
     * 创建时间，用于记录当前对象首次生成的时间。
     */
    @Column(name = ModelConstants.CREATED_TIME_PROPERTY, updatable = false)
    protected long createdTime;

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
        return createdTime;
    }

    /**
     * 功能：更新创建时间。
     * 参数：
     * - `createdTime`：`createdTime` 参数。
     * 返回：无。
     */
    public void setCreatedTime(long createdTime) {
        if (createdTime > 0) {
            this.createdTime = createdTime;
        }
    }

    /**
     * 功能：获取`Uuid`。
     * 参数：
     * - `uuidBased`：`uuidBased` 参数。
     * 返回：处理结果。
     */
    protected static UUID getUuid(UUIDBased uuidBased) {
        if (uuidBased != null) {
            return uuidBased.getId();
        } else {
            return null;
        }
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    protected static UUID getTenantUuid(TenantId tenantId) {
        if (tenantId != null) {
            return tenantId.getId();
        } else {
            return EntityId.NULL_UUID;
        }
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `uuid`：`uuid`ID。
     * - `creator`：`creator` 参数。
     * 返回：处理结果。
     */
    protected static <I> I getEntityId(UUID uuid, Function<UUID, I> creator) {
        return DaoUtil.toEntityId(uuid, creator);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    protected static TenantId getTenantId(UUID uuid) {
        if (uuid != null && !uuid.equals(EntityId.NULL_UUID)) {
            return TenantId.fromUUID(uuid);
        } else {
            return TenantId.SYS_TENANT_ID;
        }
    }

    /**
     * 功能：执行 `toJson` 对应的处理。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    protected JsonNode toJson(Object value) {
        if (value != null) {
            return JacksonUtil.valueToTree(value);
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `fromJson` 对应的处理。
     * 参数：
     * - `json`：`json` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected <T> T fromJson(JsonNode json, Class<T> type) {
        return JacksonUtil.convertValue(json, type);
    }

    /**
     * 功能：获取`To String`。
     * 参数：
     * - `list`：数据列表。
     * 返回：文本结果。
     */
    protected String listToString(List<?> list) {
        if (list != null) {
            return StringUtils.join(list, ',');
        } else {
            return "";
        }
    }

    /**
     * 功能：获取`From String`。
     * 参数：
     * - `string`：`string` 参数。
     * - `mappingFunction`：`mappingFunction` 参数。
     * 返回：匹配的数据集合。
     */
    protected <E> List<E> listFromString(String string, Function<String, E> mappingFunction) {
        if (string != null) {
            return Arrays.stream(StringUtils.split(string, ','))
                    .filter(StringUtils::isNotBlank)
                    .map(mappingFunction).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseSqlEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
