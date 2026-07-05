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
package org.thingsboard.server.dao.service.timeseries.nosql;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.dao.service.DaoNoSqlTest;
import org.thingsboard.server.dao.service.timeseries.BaseTimeseriesServiceTest;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 中文说明：
 * 1. 类目的：`TimeseriesServiceNoSqlTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoNoSqlTest
public class TimeseriesServiceNoSqlTest extends BaseTimeseriesServiceTest {

    /**
     * 功能：执行 `shouldSaveEntryOfEachTypeWithTtl` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void shouldSaveEntryOfEachTypeWithTtl() throws ExecutionException, InterruptedException, TimeoutException {
        long ttlInSec = TimeUnit.SECONDS.toSeconds(3);
        List<TsKvEntry> timeseries = List.of(
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(1), new BooleanDataEntry("test", true)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(2), new StringDataEntry("test", "text")),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(3), new LongDataEntry("test", 15L)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(4), new DoubleDataEntry("test", 10.5)),
                new BasicTsKvEntry(TimeUnit.MINUTES.toMillis(5), new JsonDataEntry("test", "{\"test\":\"testValue\"}")));

        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, timeseries, ttlInSec);

        List<TsKvEntry> fullList = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(5, fullList.size());

        // check entries after ttl
        Thread.sleep(TimeUnit.SECONDS.toMillis(ttlInSec + 1));
        List<TsKvEntry> listAfterTtl = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("test", 0L,
                TimeUnit.MINUTES.toMillis(6), 1000, 10, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(0, listAfterTtl.size());
    }

    /**
     * 功能：验证目标对象相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testNullValuesOfNoneTargetColumn() throws ExecutionException, InterruptedException, TimeoutException {
        long ts = TimeUnit.MINUTES.toMillis(1);
        long longValue = 10L;
        TsKvEntry longEntry = new BasicTsKvEntry(ts, new LongDataEntry("temp", longValue));
        double doubleValue = 20.6;
        TsKvEntry doubleEntry = new BasicTsKvEntry(ts, new DoubleDataEntry("temp", doubleValue));
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        tsService.save(tenantId, deviceId, longEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        tsService.save(tenantId, deviceId, doubleEntry).get(MAX_TIMEOUT, TimeUnit.SECONDS);

        List<TsKvEntry> listWithoutAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1 , 1000, 3, Aggregation.NONE))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithoutAgg.size());
        assertTrue(listWithoutAgg.get(0).getLongValue().isPresent());
        assertFalse(listWithoutAgg.get(0).getDoubleValue().isPresent());
        assertThat(listWithoutAgg.get(0).getLongValue().get()).isEqualTo(longValue);

        // long value should not be reset to null, so avg = (doubleValue + longValue)/ 2
        List<TsKvEntry> listWithAgg = tsService.findAll(tenantId, deviceId, Collections.singletonList(new BaseReadTsKvQuery("temp", 0L,
                ts + 1, 200000, 3, Aggregation.AVG))).get(MAX_TIMEOUT, TimeUnit.SECONDS);
        assertEquals(1, listWithAgg.size());
        assertTrue(listWithAgg.get(0).getDoubleValue().isPresent());
        double expectedValue = (doubleValue + longValue)/ 2;
        assertThat(listWithAgg.get(0).getDoubleValue().get()).isEqualTo(expectedValue);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TimeseriesServiceNoSqlTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
