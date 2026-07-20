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
 * 1. `CassandraPartitionsCacheTest` 是 ThingsBoard DAO 中验证 `CassandraPartitionsCache` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
