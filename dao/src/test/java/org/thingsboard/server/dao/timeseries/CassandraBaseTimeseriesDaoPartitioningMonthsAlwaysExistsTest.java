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
 * 1. `CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest` 是 ThingsBoard DAO 中验证 `CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExists` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
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
public class CassandraBaseTimeseriesDaoPartitioningMonthsAlwaysExistsTest {

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
     * 功能：验证`To Partitions Months`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testToPartitionsMonths() throws ParseException {
        assertThat(tsDao.getPartitioning()).isEqualTo("MONTHS");
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime())).isEqualTo(1640995200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-01-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:01Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-31T23:59:59Z").getTime())).isEqualTo(1651363200000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2022-05-01T00:00:00Z").getTime());
        assertThat(tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-31T23:59:59Z").getTime())).isEqualTo(1701388800000L).isEqualTo(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2023-12-01T00:00:00Z").getTime());
    }

    /**
     * 功能：验证`Calculate Partitions Months`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCalculatePartitionsMonths() throws ParseException {
        long startTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-12T00:00:00Z").getTime());
        long nextTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-01-31T23:59:59Z").getTime());
        long leapTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2020-02-29T23:59:59Z").getTime());
        long endTs = tsDao.toPartitionTs(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2021-01-31T23:59:59Z").getTime());
        log.info("startTs {}, nextTs {}, leapTs {}, endTs {}", startTs, nextTs, leapTs, endTs);

        assertThat(tsDao.calculatePartitions(0, 0)).isEqualTo(List.of(0L));
        assertThat(tsDao.calculatePartitions(0, 1)).isEqualTo(List.of(0L, 1L));

        assertThat(tsDao.calculatePartitions(startTs, startTs)).isEqualTo(List.of(1575158400000L)).isEqualTo(List.of(
                ISO_DATETIME_TIME_ZONE_FORMAT.parse("2019-12-01T00:00:00Z").getTime()));
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
