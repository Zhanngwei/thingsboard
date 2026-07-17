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
 * 1. `TimeseriesServiceNoSqlTest` 是 ThingsBoard DAO 中验证 `TimeseriesServiceNoSql` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `BaseTimeseriesServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
