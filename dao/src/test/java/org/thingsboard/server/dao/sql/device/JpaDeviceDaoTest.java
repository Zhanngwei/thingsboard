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
package org.thingsboard.server.dao.sql.device;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. 类目的：`JpaDeviceDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public class JpaDeviceDaoTest extends AbstractJpaDaoTest {

    /**
     * 数量常量，用于统一引用固定值。
     */
    public static final int COUNT_DEVICES = 40;
    public static final String PREFIX_FOR_DEVICE_NAME = "SEARCH_TEXT_";
    /**
     * 设备列表，用于保存一组待处理对象。
     */
    List<UUID> deviceIds;
    UUID tenantId1;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    UUID tenantId2;
    UUID customerId1;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    UUID customerId2;
    /**
     * 设备，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceDao deviceDao;

    /**
     * 设备配置，用于读取或保存对应领域对象。
     */
    @Autowired
    private DeviceProfileDao deviceProfileDao;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private DeviceProfile savedDeviceProfile;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService executor;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        createDeviceProfile();

        tenantId1 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();

        deviceIds = createDevices(tenantId1, tenantId2, customerId1, customerId2, COUNT_DEVICES);
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：无。
     */
    private void createDeviceProfile() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("TEST");
        deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription("Test");
        savedDeviceProfile = deviceProfileDao.save(TenantId.SYS_TENANT_ID, deviceProfile);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() throws Exception {
        deviceDao.removeAllByIds(deviceIds);
        deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, savedDeviceProfile.getUuidId());
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceName0x00_thenSomeDatabaseException() {
        Device device = getDevice(tenantId1, customerId1, "\u0000");
        assertThatThrownBy(() -> deviceIds.add(deviceDao.save(TenantId.fromUUID(tenantId1), device).getUuidId()));
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantId() {
        PageLink pageLink = new PageLink(15, 0, PREFIX_FOR_DEVICE_NAME);
        PageData<Device> devices1 = deviceDao.findDevicesByTenantId(tenantId1, pageLink);
        assertEquals(15, devices1.getData().size());

        pageLink = pageLink.nextPageLink();

        PageData<Device> devices2 = deviceDao.findDevicesByTenantId(tenantId1, pageLink);
        assertEquals(5, devices2.getData().size());
    }

    /**
     * 功能：验证`Find Async`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAsync() throws ExecutionException, InterruptedException, TimeoutException {
        UUID tenantId = Uuids.timeBased();
        UUID customerId = Uuids.timeBased();
        // send to method getDevice() number = 40, because make random name is bad and name "SEARCH_TEXT_40" don't used
        Device device = getDevice(tenantId, customerId, 40);
        deviceIds.add(deviceDao.save(TenantId.fromUUID(tenantId), device).getUuidId());

        UUID uuid = device.getId().getId();
        Device entity = deviceDao.findById(TenantId.fromUUID(tenantId), uuid);
        assertNotNull(entity);
        assertEquals(uuid, entity.getId().getId());

        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        ListenableFuture<Device> future = executor.submit(() -> deviceDao.findById(TenantId.fromUUID(tenantId), uuid));
        Device asyncDevice = future.get(30, TimeUnit.SECONDS);
        assertNotNull("Async device expected to be not null", asyncDevice);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        ListenableFuture<List<Device>> devicesFuture = deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId1, deviceIds);
        List<Device> devices = devicesFuture.get(30, TimeUnit.SECONDS);
        assertEquals(20, devices.size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdAndCustomerIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        ListenableFuture<List<Device>> devicesFuture = deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId1, customerId1, deviceIds);
        List<Device> devices = devicesFuture.get(30, TimeUnit.SECONDS);
        assertEquals(20, devices.size());
    }

    /**
     * 功能：保存或创建`Devices`。
     * 参数：
     * - `tenantId1`：租户信息或租户标识。
     * - `tenantId2`：租户信息或租户标识。
     * - `customerId1`：`customerId1` 参数。
     * - `customerId2`：`customerId2` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List<UUID> createDevices(UUID tenantId1, UUID tenantId2, UUID customerId1, UUID customerId2, int count) {
        List<UUID> savedDevicesUUID = new ArrayList<>();
        for (int i = 0; i < count / 2; i++) {
            savedDevicesUUID.add(deviceDao.save(TenantId.fromUUID(tenantId1), getDevice(tenantId1, customerId1, i)).getUuidId());
            savedDevicesUUID.add(deviceDao.save(TenantId.fromUUID(tenantId2), getDevice(tenantId2, customerId2, i + count / 2)).getUuidId());
        }
        return savedDevicesUUID;
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerID`：客户IDID。
     * - `nameSuffix`：名称。
     * 返回：处理结果。
     */
    private Device getDevice(UUID tenantId, UUID customerID, Object nameSuffix) {
        return getDevice(tenantId, customerID, Uuids.timeBased(), nameSuffix);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerID`：客户IDID。
     * - `deviceId`：设备IDID。
     * - `nameSuffix`：名称。
     * 返回：处理结果。
     */
    private Device getDevice(UUID tenantId, UUID customerID, UUID deviceId, Object nameSuffix) {
        Device device = new Device();
        device.setId(new DeviceId(deviceId));
        device.setTenantId(TenantId.fromUUID(tenantId));
        device.setCustomerId(new CustomerId(customerID));
        device.setName(PREFIX_FOR_DEVICE_NAME + nameSuffix);
        device.setDeviceProfileId(savedDeviceProfile.getId());
        return device;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaDeviceDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
