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
package org.thingsboard.server.dao.model.sqlts.ts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * 中文说明：
 * 1. 类目的：`TsKvEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "ts_kv")
@IdClass(TsKvCompositeKey.class)
public final class TsKvEntity extends AbstractTsKvEntity {

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity() {
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `strValue`：值。
     * - `aggValuesLastTs`：时间戳。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(String strValue, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        this.strValue = strValue;
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `longValue`：值。
     * - `doubleValue`：值。
     * - `longCountValue`：值。
     * - `doubleCountValue`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(Long longValue, Double doubleValue, Long longCountValue, Long doubleCountValue, String aggType, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!isAllNull(longValue, doubleValue, longCountValue, doubleCountValue)) {
            switch (aggType) {
                case AVG:
                    double sum = 0.0;
                    if (longValue != null) {
                        sum += longValue;
                    }
                    if (doubleValue != null) {
                        sum += doubleValue;
                    }
                    long totalCount = longCountValue + doubleCountValue;
                    if (totalCount > 0) {
                        this.doubleValue = sum / (longCountValue + doubleCountValue);
                    } else {
                        this.doubleValue = 0.0;
                    }
                    this.aggValuesCount = totalCount;
                    break;
                case SUM:
                    if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue + (longValue != null ? longValue.doubleValue() : 0.0);
                    } else {
                        this.longValue = longValue;
                    }
                    break;
                case MIN:
                case MAX:
                    if (longCountValue > 0 && doubleCountValue > 0) {
                        this.doubleValue = MAX.equals(aggType) ? Math.max(doubleValue, longValue.doubleValue()) : Math.min(doubleValue, longValue.doubleValue());
                    } else if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue;
                    } else if (longCountValue > 0) {
                        this.longValue = longValue;
                    }
                    break;
            }
        }
    }

    /**
     * 功能：创建 `TsKvEntity` 实例，并初始化必要字段。
     * 参数：
     * - `booleanValueCount`：值。
     * - `strValueCount`：值。
     * - `longValueCount`：值。
     * - `doubleValueCount`：值。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TsKvEntity(Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount, Long jsonValueCount, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        if (!isAllNull(booleanValueCount, strValueCount, longValueCount, doubleValueCount)) {
            if (booleanValueCount != 0) {
                this.longValue = booleanValueCount;
            } else if (strValueCount != 0) {
                this.longValue = strValueCount;
            } else if (jsonValueCount != 0) {
                this.longValue = jsonValueCount;
            } else {
                this.longValue = longValueCount + doubleValueCount;
            }
        }
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isNotEmpty() {
        return strValue != null || longValue != null || doubleValue != null || booleanValue != null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TsKvEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
