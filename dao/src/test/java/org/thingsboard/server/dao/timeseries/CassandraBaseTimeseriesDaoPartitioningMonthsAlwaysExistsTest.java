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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;

import java.text.ParseException;
import java.util.List;

import static org.apache.commons.lang3.time.DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=MONTHS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest` 是 ThingsBoard DAO 测试模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
public class CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tsDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    CassandraBaseTimeseriesDao tsDao;

    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    /**
     * 字段说明：
     * 1. 保存 `cassandraCluster` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    CassandraCluster cassandraCluster;

    @MockBean
    /**
     * 字段说明：
     * 1. 保存 `cassandraBufferedRateReadExecutor` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    @MockBean
    /**
     * 字段说明：
     * 1. 保存 `cassandraBufferedRateWriteExecutor` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testToPartitionsMonths` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testToPartitionsMonths() throws ParseException {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.getPartitioning()).isEqualTo("MONTHS");
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(1640995200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(1701388800000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-01T00:00:00Z").getTime());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCalculatePartitionsMonths` 对应的时序数据持久化类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void testCalculatePartitionsMonths() throws ParseException {
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        long startTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        long nextTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-31T23:59:59Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        long leapTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-29T23:59:59Z").getTime());
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        long endTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        log.info("startTs {}, nextTs {}, leapTs {}, endTs {}", startTs, nextTs, leapTs, endTs);

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(1575158400000L)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime()));
        // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(1575158400000L, 1577836800000L)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, leapTs)).isEqualTo(List.of(1575158400000L, 1577836800000L, 1580515200000L)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(14).isEqualTo(List.of(
                1575158400000L,
                1577836800000L, 1580515200000L, 1583020800000L,
                1585699200000L, 1588291200000L, 1590969600000L,
                1593561600000L, 1596240000000L, 1598918400000L,
                1601510400000L, 1604188800000L, 1606780800000L,
                1609459200000L)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-03-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-04-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-05-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-06-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-07-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-08-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-09-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-10-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-11-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-12-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest` 在 ThingsBoard DAO 测试模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
