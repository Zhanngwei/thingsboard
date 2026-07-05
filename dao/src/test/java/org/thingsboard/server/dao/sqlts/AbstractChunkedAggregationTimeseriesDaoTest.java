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
package org.thingsboard.server.dao.sqlts;

import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.common.data.id.TenantId.SYS_TENANT_ID;
import static org.thingsboard.server.common.data.kv.Aggregation.COUNT;

/**
 * 中文说明：
 * 1. 类目的：`AbstractChunkedAggregationTimeseriesDaoTest` 是 ThingsBoard DAO 测试模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
public class AbstractChunkedAggregationTimeseriesDaoTest {

    /**
     * 数量限制常量，用于统一引用固定值。
     */
    final int LIMIT = 1;
    final String TEMP = "temp";
    /**
     * `DESC`常量，用于统一引用固定值。
     */
    final String DESC = "DESC";
    private AbstractChunkedAggregationTimeseriesDao tsDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {
        tsDao = spy(AbstractChunkedAggregationTimeseriesDao.class);
        Optional<TsKvEntry> optionalListenableFuture = Optional.of(mock(TsKvEntry.class));
        willReturn(Futures.immediateFuture(optionalListenableFuture)).given(tsDao).findAndAggregateAsync(any(), anyString(), anyLong(), anyLong(), anyLong(), any());
        willReturn(Futures.immediateFuture(mock(ReadTsKvQueryResult.class))).given(tsDao).getReadTsKvQueryResultFuture(any(), any());
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenLastIntervalShorterThanOthersAndEqualsEndTs() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 2001, 1001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuerySecond = new BaseReadTsKvQuery(TEMP, 2001, 3000, 2501, LIMIT, COUNT, DESC);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(2)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 2001, getTsForReadTsKvQuery(1, 2001), COUNT);
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuerySecond.getKey(), 2001, 3000, getTsForReadTsKvQuery(2001, 3000), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriod() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3000, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        assertThat(tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query)).isNotNull();
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodMinusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 2999, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3000, 1500, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);

    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsPeriodPlusOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3001, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsZero() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 0, 0, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 0, 1, 0, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 0, 1, getTsForReadTsKvQuery(0, 1), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsOne() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 1, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQuery = new BaseReadTsKvQuery(TEMP, 1, 2, 1, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQuery.getKey(), 1, 2, getTsForReadTsKvQuery(1, 2), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsOneMillisecondAndStartTsIsIntegerMax() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE, 1, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), Integer.MAX_VALUE, 1L + Integer.MAX_VALUE, getTsForReadTsKvQuery(Integer.MAX_VALUE, 1L + Integer.MAX_VALUE), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenIntervalEqualsBigNumber() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, Integer.MAX_VALUE, LIMIT, COUNT, DESC);
        ReadTsKvQuery subQueryFirst = new BaseReadTsKvQuery(TEMP, 1, 3001, 1501, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, subQueryFirst.getKey(), 1, 3000, getTsForReadTsKvQuery(1, 3000), COUNT);
    }

    /**
     * 功能：验证 `givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenIntervalNotMultiplePeriod_whenAggregateCount_thenCountIntervalEqualsPeriodSize() {
        ReadTsKvQuery query = new BaseReadTsKvQuery(TEMP, 1, 3000, 3, LIMIT, COUNT, DESC);
        willCallRealMethod().given(tsDao).findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        tsDao.findAllAsync(SYS_TENANT_ID, SYS_TENANT_ID, query);
        verify(tsDao, times(1000)).findAndAggregateAsync(any(), any(), anyLong(), anyLong(), anyLong(), any());
        for (long i = 1; i <= 3000; i += 3) {
            verify(tsDao, times(1)).findAndAggregateAsync(SYS_TENANT_ID, TEMP, i, Math.min(i + 3, 3000), getTsForReadTsKvQuery(i, i + 3), COUNT);
        }
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：数值结果。
     */
    long getTsForReadTsKvQuery(long startTs, long endTs) {
        return startTs + (endTs - startTs) / 2L;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractChunkedAggregationTimeseriesDaoTest` 在 ThingsBoard DAO 测试模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
