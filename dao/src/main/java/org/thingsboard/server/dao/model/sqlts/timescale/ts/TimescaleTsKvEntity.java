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
package org.thingsboard.server.dao.model.sqlts.timescale.ts;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.model.sql.AbstractTsKvEntity;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_AVG;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_AVG_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_COUNT;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_COUNT_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MAX;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MAX_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MIN;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_MIN_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_SUM;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FIND_SUM_QUERY;
import static org.thingsboard.server.dao.sqlts.timescale.AggregationRepository.FROM_WHERE_CLAUSE;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ts_kv")
@IdClass(TimescaleTsKvCompositeKey.class)
@SqlResultSetMappings({
        @SqlResultSetMapping(
                name = "timescaleAggregationMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TimescaleTsKvEntity.class,
                                columns = {
                                        @ColumnResult(name = "tsBucket", type = Long.class),
                                        @ColumnResult(name = "interval", type = Long.class),
                                        @ColumnResult(name = "longValue", type = Long.class),
                                        @ColumnResult(name = "doubleValue", type = Double.class),
                                        @ColumnResult(name = "longCountValue", type = Long.class),
                                        @ColumnResult(name = "doubleCountValue", type = Long.class),
                                        @ColumnResult(name = "strValue", type = String.class),
                                        @ColumnResult(name = "aggType", type = String.class),
                                        @ColumnResult(name = "maxAggTs", type = Long.class),
                                }
                        ),
                }),
        @SqlResultSetMapping(
                name = "timescaleCountMapping",
                classes = {
                        @ConstructorResult(
                                targetClass = TimescaleTsKvEntity.class,
                                columns = {
                                        @ColumnResult(name = "tsBucket", type = Long.class),
                                        @ColumnResult(name = "interval", type = Long.class),
                                        @ColumnResult(name = "booleanValueCount", type = Long.class),
                                        @ColumnResult(name = "strValueCount", type = Long.class),
                                        @ColumnResult(name = "longValueCount", type = Long.class),
                                        @ColumnResult(name = "doubleValueCount", type = Long.class),
                                        @ColumnResult(name = "jsonValueCount", type = Long.class),
                                        @ColumnResult(name = "maxAggTs", type = Long.class),
                                }
                        )
                }),
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = FIND_AVG,
                query = FIND_AVG_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_MAX,
                query = FIND_MAX_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_MIN,
                query = FIND_MIN_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_SUM,
                query = FIND_SUM_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleAggregationMapping"
        ),
        @NamedNativeQuery(
                name = FIND_COUNT,
                query = FIND_COUNT_QUERY + FROM_WHERE_CLAUSE,
                resultSetMapping = "timescaleCountMapping"
        )
})
/**
 * 中文说明：
 * 1. 类目的：`TimescaleTsKvEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
public final class TimescaleTsKvEntity extends AbstractTsKvEntity {

    /**
     * 方法说明：
     * 1. 职责：执行 `TimescaleTsKvEntity` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TimescaleTsKvEntity() {
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `TimescaleTsKvEntity` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TimescaleTsKvEntity(Long tsBucket, Long interval, Long longValue, Double doubleValue, Long longCountValue, Long doubleCountValue, String strValue, String aggType, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (!StringUtils.isEmpty(strValue)) {
            this.strValue = strValue;
        }
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (!isAllNull(tsBucket, interval, longValue, doubleValue, longCountValue, doubleCountValue)) {
            this.ts = tsBucket + interval / 2;
            // 根据实体类型、查询类型或数据库方言分支，保持不同持久化路径的语义隔离。
            switch (aggType) {
                case AVG:
                    double sum = 0.0;
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (longValue != null) {
                        sum += longValue;
                    }
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (doubleValue != null) {
                        sum += doubleValue;
                    }
                    long totalCount = longCountValue + doubleCountValue;
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (totalCount > 0) {
                        this.doubleValue = sum / (longCountValue + doubleCountValue);
                    } else {
                        this.doubleValue = 0.0;
                    }
                    this.aggValuesCount = totalCount;
                    break;
                case SUM:
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue + (longValue != null ? longValue.doubleValue() : 0.0);
                    } else {
                        this.longValue = longValue;
                    }
                    break;
                case MIN:
                case MAX:
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    if (longCountValue > 0 && doubleCountValue > 0) {
                        this.doubleValue = MAX.equals(aggType) ? Math.max(doubleValue, longValue.doubleValue()) : Math.min(doubleValue, longValue.doubleValue());
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    } else if (doubleCountValue > 0) {
                        this.doubleValue = doubleValue;
                    // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
                    } else if (longCountValue > 0) {
                        this.longValue = longValue;
                    }
                    break;
            }
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `TimescaleTsKvEntity` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TimescaleTsKvEntity(Long tsBucket, Long interval, Long booleanValueCount, Long strValueCount, Long longValueCount, Long doubleValueCount, Long jsonValueCount, Long aggValuesLastTs) {
        super(aggValuesLastTs);
        // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
        if (!isAllNull(tsBucket, interval, booleanValueCount, strValueCount, longValueCount, doubleValueCount, jsonValueCount)) {
            this.ts = tsBucket + interval / 2;
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (booleanValueCount != 0) {
                this.longValue = booleanValueCount;
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            } else if (strValueCount != 0) {
                this.longValue = strValueCount;
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            } else if (jsonValueCount != 0) {
                this.longValue = jsonValueCount;
            } else {
                this.longValue = longValueCount + doubleValueCount;
            }
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `isNotEmpty` 对应的持久化实体映射类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public boolean isNotEmpty() {
        return ts != null && (strValue != null || longValue != null || doubleValue != null || booleanValue != null || jsonValue != null);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TimescaleTsKvEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
