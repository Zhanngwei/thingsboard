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

/**
 * 中文说明：
 * 1. 类目的：`CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest` 是 ThingsBoard DAO 测试模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CassandraBaseTimeseriesDao.class)
@TestPropertySource(properties = {
        "database.ts.type=cassandra",
        "cassandra.query.ts_key_value_partitioning=YEARS",
        "cassandra.query.use_ts_key_value_partitioning_on_read=false",
        "cassandra.query.ts_key_value_partitions_max_cache_size=100000",
        "cassandra.query.ts_key_value_partitions_cache_stats_enabled=true",
        "cassandra.query.ts_key_value_partitions_cache_stats_interval=60",
        "cassandra.query.ts_key_value_ttl=0",
        "cassandra.query.set_null_values_enabled=false",
})
@Slf4j
public class CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest {

    /**
     * 时间戳集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    CassandraBaseTimeseriesDao tsDao;

    /**
     * Cassandra 集群，用于支撑当前网络或外部服务交互。
     */
    @MockBean(answer = Answers.RETURNS_MOCKS)
    @Qualifier("CassandraCluster")
    CassandraCluster cassandraCluster;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    @MockBean
    CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @MockBean
    CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    /**
     * 功能：验证`To Partitions Years`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testToPartitionsYears() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("YEARS");
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime());
    }

    /**
     * 功能：验证`Calculate Partitions Years`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCalculatePartitionsYears() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-10-12T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2025-07-15T00:00:00Z").getTime());
        log.info("startTs {}, nextTs {}, endTs {}", startTs, nextTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime()));
        assertThat(tsDao.calculatePartitions(startTs, nextTs)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime()));

        assertThat(tsDao.calculatePartitions(startTs, endTs)).hasSize(7).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2024-01-01T00:00:00Z").getTime(),
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2025-01-01T00:00:00Z").getTime()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraBaseTimeseriesDaoPartitioningYearsAlwaysExistsTest` 在 ThingsBoard DAO 测试模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
