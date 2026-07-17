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
 * 1. `JpaDeviceDaoTest` 是 ThingsBoard DAO 中验证 `JpaDeviceDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
