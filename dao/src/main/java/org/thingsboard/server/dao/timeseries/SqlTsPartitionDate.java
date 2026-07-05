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
package org.thingsboard.server.dao.timeseries;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`SqlTsPartitionDate` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
public enum SqlTsPartitionDate {

    DAYS("yyyy_MM_dd", ChronoUnit.DAYS), MONTHS("yyyy_MM", ChronoUnit.MONTHS), YEARS("yyyy", ChronoUnit.YEARS), INDEFINITE("indefinite", ChronoUnit.FOREVER);

    /**
     * `pattern` 字段，保存当前对象的对应属性。
     */
    private final String pattern;
    private final transient TemporalUnit truncateUnit;
    public final static LocalDateTime EPOCH_START = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);

    /**
     * 功能：创建 `SqlTsPartitionDate` 实例，并初始化必要字段。
     * 参数：
     * - `pattern`：`pattern` 参数。
     * - `truncateUnit`：`truncateUnit` 参数。
     * 返回：新创建的对象实例。
     */
    SqlTsPartitionDate(String pattern, TemporalUnit truncateUnit) {
        this.pattern = pattern;
        this.truncateUnit = truncateUnit;
    }

    /**
     * 功能：获取`Pattern`。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * 功能：获取单位。
     * 参数：无。
     * 返回：处理结果。
     */
    public TemporalUnit getTruncateUnit() {
        return truncateUnit;
    }

    /**
     * 功能：执行 `trancateTo` 对应的处理。
     * 参数：
     * - `time`：`time` 参数。
     * 返回：处理结果。
     */
    public LocalDateTime trancateTo(LocalDateTime time) {
        switch (this) {
            case DAYS:
                return time.truncatedTo(ChronoUnit.DAYS);
            case MONTHS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
            case YEARS:
                return time.truncatedTo(ChronoUnit.DAYS).withDayOfYear(1);
            case INDEFINITE:
                return EPOCH_START;
            default:
                throw new RuntimeException("Failed to parse partitioning property!");
        }
    }

    /**
     * 功能：执行 `plusTo` 对应的处理。
     * 参数：
     * - `time`：`time` 参数。
     * 返回：处理结果。
     */
    public LocalDateTime plusTo(LocalDateTime time) {
        switch (this) {
            case DAYS:
                return time.plusDays(1);
            case MONTHS:
                return time.plusMonths(1);
            case YEARS:
                return time.plusYears(1);
            default:
                throw new RuntimeException("Failed to parse partitioning property!");
        }
    }

    /**
     * 功能：执行 `parse` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    public static Optional<SqlTsPartitionDate> parse(String name) {
        SqlTsPartitionDate partition = null;
        if (name != null) {
            for (SqlTsPartitionDate partitionDate : SqlTsPartitionDate.values()) {
                if (partitionDate.name().equalsIgnoreCase(name)) {
                    partition = partitionDate;
                    break;
                }
            }
        }
        return Optional.ofNullable(partition);
    }

/*
 * 本类总结：
 * 1. 核心职责：`SqlTsPartitionDate` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
}