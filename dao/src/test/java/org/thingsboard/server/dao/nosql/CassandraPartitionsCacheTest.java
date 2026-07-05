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
package org.thingsboard.server.dao.nosql;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.timeseries.CassandraBaseTimeseriesDao;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`CassandraPartitionsCacheTest` 是 ThingsBoard DAO 测试模块 中的NoSQL/Cassandra 持久化类型，用于封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 生命周期：由 Spring 容器创建并在 Cassandra 查询生命周期内处理异步结果、分页和异常转换。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Async Callback / Template。
 */
@RunWith(MockitoJUnitRunner.class)
public class CassandraPartitionsCacheTest {

    /**
     * 时序数据集合，用于去重保存或快速判断对象是否存在。
     */
    @Spy
    private CassandraBaseTimeseriesDao cassandraBaseTimeseriesDao;

    /**
     * 语句，表示当前对象所处状态。
     */
    @Mock
    private PreparedStatement preparedStatement;

    /**
     * 语句，表示当前对象所处状态。
     */
    @Mock
    private BoundStatement boundStatement;

    /**
     * 环境配置，保存当前对象的配置选项。
     */
    @Mock
    private Environment environment;

    /**
     * 集群，用于支撑当前网络或外部服务交互。
     */
    @Mock
    private CassandraCluster cluster;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Mock
    private GuavaSession session;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitioning", "MONTHS");
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "partitionsCacheSize", 100000);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "systemTtl", 0);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "setNullValuesEnabled", false);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "environment", environment);
        ReflectionTestUtils.setField(cassandraBaseTimeseriesDao, "cluster", cluster);

        when(cluster.getDefaultReadConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getDefaultWriteConsistencyLevel()).thenReturn(ConsistencyLevel.ONE);
        when(cluster.getSession()).thenReturn(session);
        when(session.prepare(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.bind()).thenReturn(boundStatement);

        when(boundStatement.setString(anyInt(), anyString())).thenReturn(boundStatement);
        when(boundStatement.setUuid(anyInt(), any(UUID.class))).thenReturn(boundStatement);
        when(boundStatement.setLong(anyInt(), any(Long.class))).thenReturn(boundStatement);

        willReturn(new TbResultSetFuture(SettableFuture.create())).given(cassandraBaseTimeseriesDao).executeAsyncWrite(any(), any());

        doReturn(Futures.immediateFuture(0)).when(cassandraBaseTimeseriesDao).getFuture(any(), any());
    }

    /**
     * 功能：验证分区相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testPartitionSave() throws Exception {
        cassandraBaseTimeseriesDao.init();

        UUID id = UUID.randomUUID();
        TenantId tenantId = TenantId.fromUUID(id);
        long tsKvEntryTs = System.currentTimeMillis();

        for (int i = 0; i < 50000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i);
        }
        for (int i = 0; i < 60000; i++) {
            cassandraBaseTimeseriesDao.savePartition(tenantId, tenantId, tsKvEntryTs, "test" + i);
        }
        verify(cassandraBaseTimeseriesDao, times(60000)).executeAsyncWrite(any(TenantId.class), any(Statement.class));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`CassandraPartitionsCacheTest` 在 ThingsBoard DAO 测试模块 中承担NoSQL/Cassandra 持久化类型职责，核心目的是封装 Cassandra 异步查询、分页读取、语句构造和 NoSQL 时序/事件访问边界。
 * 2. 核心流程：构造 Cassandra 语句并异步执行，随后把 ResultSet 转换为 DAO API 需要的领域结果。
 * 3. 关键依赖：主要依赖或协作对象包括Cassandra Session、Guava Future、NoSQL DAO API、TimeseriesService 和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
