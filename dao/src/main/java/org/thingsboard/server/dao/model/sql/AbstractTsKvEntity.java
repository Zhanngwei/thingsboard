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
import org.thingsboard.server.common.data.kv.AggTsKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.BOOLEAN_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.DOUBLE_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.ENTITY_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.JSON_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.KEY_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.LONG_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.STRING_VALUE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.TS_COLUMN;

/**
 * 中文说明：
 * 1. 类目的：`AbstractTsKvEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
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
public abstract class AbstractTsKvEntity implements ToData<TsKvEntry> {

    /**
     * `SUM`常量，用于统一引用固定值。
     */
    protected static final String SUM = "SUM";
    protected static final String AVG = "AVG";
    /**
     * `MIN`常量，用于统一引用固定值。
     */
    protected static final String MIN = "MIN";
    protected static final String MAX = "MAX";

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = ENTITY_ID_COLUMN, columnDefinition = "uuid")
    protected UUID entityId;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Id
    @Column(name = KEY_COLUMN)
    protected int key;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Id
    @Column(name = TS_COLUMN)
    protected Long ts;

    /**
     * 是否满足值条件。
     */
    @Column(name = BOOLEAN_VALUE_COLUMN)
    protected Boolean booleanValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = STRING_VALUE_COLUMN)
    protected String strValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = LONG_VALUE_COLUMN)
    protected Long longValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = DOUBLE_VALUE_COLUMN)
    protected Double doubleValue;

    /**
     * 值，保存当前处理得到的具体内容。
     */
    @Column(name = JSON_VALUE_COLUMN)
    protected String jsonValue;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Transient
    protected String strKey;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Transient
    protected Long aggValuesLastTs;
    /**
     * 数量，保存当前处理得到的具体内容。
     */
    @Transient
    protected Long aggValuesCount;

    /**
     * 功能：创建 `AbstractTsKvEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractTsKvEntity() {
    }

    /**
     * 功能：创建 `AbstractTsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `aggValuesLastTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public AbstractTsKvEntity(Long aggValuesLastTs) {
        this.aggValuesLastTs = aggValuesLastTs;
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    public abstract boolean isNotEmpty();

    /**
     * 功能：判断`All Null`。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：判断结果。
     */
    protected static boolean isAllNull(Object... args) {
        for (Object arg : args) {
            if (arg != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TsKvEntry toData() {
        KvEntry kvEntry = null;
        if (strValue != null) {
            kvEntry = new StringDataEntry(strKey, strValue);
        } else if (longValue != null) {
            kvEntry = new LongDataEntry(strKey, longValue);
        } else if (doubleValue != null) {
            kvEntry = new DoubleDataEntry(strKey, doubleValue);
        } else if (booleanValue != null) {
            kvEntry = new BooleanDataEntry(strKey, booleanValue);
        } else if (jsonValue != null) {
            kvEntry = new JsonDataEntry(strKey, jsonValue);
        }

        if (aggValuesCount == null) {
            return new BasicTsKvEntry(ts, kvEntry);
        } else {
            return new AggTsKvEntry(ts, kvEntry, aggValuesCount);
        }
    }


/*
 * 本类总结：
 * 1. 核心职责：`AbstractTsKvEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
}