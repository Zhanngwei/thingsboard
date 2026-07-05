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
package org.thingsboard.server.dao.service.attributes;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.attributes.AttributeCacheKey;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`BaseAttributesServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Slf4j
public abstract class BaseAttributesServiceTest extends AbstractServiceTest {

    /**
     * 值常量，用于统一引用固定值。
     */
    private static final String OLD_VALUE = "OLD VALUE";
    private static final String NEW_VALUE = "NEW VALUE";

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    @Autowired
    private TbTransactionalCache<AttributeCacheKey, AttributeKvEntry> cache;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
    }

    /**
     * 功能：保存或创建`And Fetch`。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        KvEntry attrValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attr = new BaseAttributeKvEntry(attrValue, 42L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attr)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attr.getKey()).get();
        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attr, saved.get());
    }

    /**
     * 功能：保存或创建类型。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void saveMultipleTypeAndFetch() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());
        KvEntry attrOldValue = new StringDataEntry("attribute1", "value1");
        AttributeKvEntry attrOld = new BaseAttributeKvEntry(attrOldValue, 42L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrOld)).get();
        Optional<AttributeKvEntry> saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();

        Assert.assertTrue(saved.isPresent());
        Assert.assertEquals(attrOld, saved.get());

        KvEntry attrNewValue = new StringDataEntry("attribute1", "value2");
        AttributeKvEntry attrNew = new BaseAttributeKvEntry(attrNewValue, 73L);
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrNew)).get();

        saved = attributesService.find(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, attrOld.getKey()).get();
        Assert.assertEquals(attrNew, saved.get());
    }

    /**
     * 功能：获取`All`。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findAll() throws Exception {
        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        KvEntry attrAOldValue = new StringDataEntry("A", "value1");
        AttributeKvEntry attrAOld = new BaseAttributeKvEntry(attrAOldValue, 42L);
        KvEntry attrANewValue = new StringDataEntry("A", "value2");
        AttributeKvEntry attrANew = new BaseAttributeKvEntry(attrANewValue, 73L);
        KvEntry attrBNewValue = new StringDataEntry("B", "value3");
        AttributeKvEntry attrBNew = new BaseAttributeKvEntry(attrBNewValue, 73L);

        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrAOld)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrANew)).get();
        attributesService.save(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE, Collections.singletonList(attrBNew)).get();

        List<AttributeKvEntry> saved = attributesService.findAll(SYSTEM_TENANT_ID, deviceId, DataConstants.CLIENT_SCOPE).get();

        Assert.assertNotNull(saved);
        Assert.assertEquals(2, saved.size());

        Assert.assertEquals(attrANew, saved.get(0));
        Assert.assertEquals(attrBNew, saved.get(1));
    }

    /**
     * 功能：验证请求相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDummyRequestWithEmptyResult() throws Exception {
        var future = attributesService.find(new TenantId(UUID.randomUUID()), new DeviceId(UUID.randomUUID()), DataConstants.SERVER_SCOPE, "TEST");
        Assert.assertNotNull(future);
        var result = future.get(10, TimeUnit.SECONDS);
        Assert.assertTrue(result.isEmpty());
    }

    /**
     * 功能：验证`Concurrent Transaction`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConcurrentTransaction() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var scope = DataConstants.SERVER_SCOPE;
        var key = "TEST";

        var attrKey = new AttributeCacheKey(scope, deviceId, "TEST");
        var oldValue = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, OLD_VALUE));
        var newValue = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, NEW_VALUE));

        var trx = cache.newTransactionForKey(attrKey);
        cache.putIfAbsent(attrKey, newValue);
        trx.putIfAbsent(attrKey, oldValue);
        Assert.assertFalse(trx.commit());
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    /**
     * 功能：验证`Concurrent Fetch And Update`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConcurrentFetchAndUpdate() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdate(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 功能：验证`Concurrent Fetch And Update Multi`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConcurrentFetchAndUpdateMulti() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));
        try {
            for (int i = 0; i < 100; i++) {
                var deviceId = new DeviceId(UUID.randomUUID());
                testConcurrentFetchAndUpdateMulti(tenantId, deviceId, pool);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 功能：验证`Fetch And Update Empty`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFetchAndUpdateEmpty() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var scope = DataConstants.SERVER_SCOPE;
        var key = "TEST";

        Optional<AttributeKvEntry> emptyValue = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
        Assert.assertTrue(emptyValue.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE);
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    /**
     * 功能：验证`Fetch And Update Multi`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFetchAndUpdateMulti() throws Exception {
        var tenantId = new TenantId(UUID.randomUUID());
        var deviceId = new DeviceId(UUID.randomUUID());
        var scope = DataConstants.SERVER_SCOPE;
        var key1 = "TEST1";
        var key2 = "TEST2";

        var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertTrue(value.isEmpty());

        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(1, value.size());
        Assert.assertEquals(OLD_VALUE, value.get(0));

        saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertTrue(value.contains(OLD_VALUE));
        Assert.assertTrue(value.contains(NEW_VALUE));

        saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);

        value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, value.size());
        Assert.assertEquals(NEW_VALUE, value.get(0));
        Assert.assertEquals(NEW_VALUE, value.get(1));
    }

    /**
     * 功能：验证`Concurrent Fetch And Update`相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `pool`：数据列表。
     * 返回：无。
     */
    private void testConcurrentFetchAndUpdate(TenantId tenantId, DeviceId deviceId, ListeningExecutorService pool) throws Exception {
        var scope = DataConstants.SERVER_SCOPE;
        var key = "TEST";
        saveAttribute(tenantId, deviceId, scope, key, OLD_VALUE);
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            var value = getAttributeValue(tenantId, deviceId, scope, key);
            Assert.assertTrue(value.equals(OLD_VALUE) || value.equals(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> saveAttribute(tenantId, deviceId, scope, key, NEW_VALUE)));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);
        Assert.assertEquals(NEW_VALUE, getAttributeValue(tenantId, deviceId, scope, key));
    }

    /**
     * 功能：验证`Concurrent Fetch And Update Multi`相关场景。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `pool`：数据列表。
     * 返回：无。
     */
    private void testConcurrentFetchAndUpdateMulti(TenantId tenantId, DeviceId deviceId, ListeningExecutorService pool) throws Exception {
        var scope = DataConstants.SERVER_SCOPE;
        var key1 = "TEST1";
        var key2 = "TEST2";
        saveAttribute(tenantId, deviceId, scope, key1, OLD_VALUE);
        saveAttribute(tenantId, deviceId, scope, key2, OLD_VALUE);
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(pool.submit(() -> {
            var value = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
            Assert.assertEquals(2, value.size());
            Assert.assertTrue(value.contains(OLD_VALUE) || value.contains(NEW_VALUE));
        }));
        futures.add(pool.submit(() -> {
            saveAttribute(tenantId, deviceId, scope, key1, NEW_VALUE);
            saveAttribute(tenantId, deviceId, scope, key2, NEW_VALUE);
        }));
        Futures.allAsList(futures).get(10, TimeUnit.SECONDS);
        var newResult = getAttributeValues(tenantId, deviceId, scope, Arrays.asList(key1, key2));
        Assert.assertEquals(2, newResult.size());
        Assert.assertEquals(NEW_VALUE, newResult.get(0));
        Assert.assertEquals(NEW_VALUE, newResult.get(1));
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * 返回：文本结果。
     */
    private String getAttributeValue(TenantId tenantId, DeviceId deviceId, String scope, String key) {
        try {
            Optional<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, key).get(10, TimeUnit.SECONDS);
            return entry.orElseThrow(RuntimeException::new).getStrValue().orElse("Unknown");
        } catch (Exception e) {
            log.warn("Failed to get attribute", e.getCause());
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    private List<String> getAttributeValues(TenantId tenantId, DeviceId deviceId, String scope, List<String> keys) {
        try {
            List<AttributeKvEntry> entry = attributesService.find(tenantId, deviceId, scope, keys).get(10, TimeUnit.SECONDS);
            return entry.stream().map(e -> e.getStrValue().orElse(null)).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to get attributes", e.getCause());
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `scope`：`scope` 参数。
     * - `key`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void saveAttribute(TenantId tenantId, DeviceId deviceId, String scope, String key, String s) {
        try {
            AttributeKvEntry newEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), new StringDataEntry(key, s));
            attributesService.save(tenantId, deviceId, scope, Collections.singletonList(newEntry)).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to save attribute", e.getCause());
            Assert.assertNull(e);
        }
    }


}

/*
 * 本类总结：
 * 1. 核心职责：`BaseAttributesServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
