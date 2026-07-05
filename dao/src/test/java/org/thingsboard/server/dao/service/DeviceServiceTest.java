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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.service.validator.DeviceCredentialsDataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. 类目的：`DeviceServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class DeviceServiceTest extends AbstractServiceTest {

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    CustomerService customerService;
    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceProfileService deviceProfileService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceService deviceService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    OtaPackageService otaPackageService;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    TenantProfileService tenantProfileService;
    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    /**
     * 校验器，封装可复用的处理规则。
     */
    @SpyBean
    private DeviceCredentialsDataValidator validator;

    private IdComparator<Device> idComparator = new IdComparator<>();
    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId anotherTenantId;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        anotherTenantId = createTenant().getId();
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
        tenantService.deleteTenant(anotherTenantId);

        tenantProfileService.deleteTenantProfiles(tenantId);
        tenantProfileService.deleteTenantProfiles(anotherTenantId);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDevicesWithoutMaxDeviceLimit() {
        Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDevicesWithInfiniteMaxDeviceLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(Long.MAX_VALUE).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Device device = this.saveDevice(tenantId, "My device");
        deleteDevice(tenantId, device);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDevicesWithMaxDeviceOutOfLimit() {
        TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
        defaultTenantProfile.getProfileData().setConfiguration(DefaultTenantProfileConfiguration.builder().maxDevices(1).build());
        tenantProfileService.saveTenantProfile(tenantId, defaultTenantProfile);

        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));

        this.saveDevice(tenantId, "My first device");
        Assert.assertEquals(1, deviceService.countByTenantId(tenantId));

        Assertions.assertThrows(DataValidationException.class, () -> {
            this.saveDevice(tenantId, "My second device that out of maxDeviceCount limit");
        });
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDevicesWithTheSameAccessToken() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(StringUtils.randomAlphabetic(10));
        device.setType("default");
        String accessToken = StringUtils.generateSafeToken(10);
        Device savedDevice = deviceService.saveDeviceWithAccessToken(device, accessToken);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(accessToken, deviceCredentials.getCredentialsId());

        Device duplicatedDevice = new Device();
        duplicatedDevice.setTenantId(tenantId);
        duplicatedDevice.setName(StringUtils.randomAlphabetic(10));
        duplicatedDevice.setType("default");
        assertThatThrownBy(() -> deviceService.saveDeviceWithAccessToken(duplicatedDevice, accessToken))
                .isInstanceOf(DeviceCredentialsValidationException.class)
                .hasMessageContaining("Device credentials are already assigned to another device!");

        Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, duplicatedDevice.getName());
        Assertions.assertNull(deviceByName);
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldRollbackValidatedDeviceIfDeviceCredentialsValidationFailed() {
        Mockito.reset(validator);
        Mockito.doThrow(new DataValidationException("mock message"))
                .when(validator).validate(any(), any());

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(StringUtils.randomAlphabetic(10));
        device.setType("default");

        assertThatThrownBy(() -> deviceService.saveDevice(device))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("mock message");

        Device deviceByName = deviceService.findDeviceByTenantIdAndName(tenantId, device.getName());
        Assertions.assertNull(deviceByName);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCountByTenantId() {
        Assert.assertEquals(0, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        Device anotherDevice = this.saveDevice(anotherTenantId, "My device 1");
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));

        int maxDevices = 8;
        List<Device> devices = new ArrayList<>(maxDevices);

        for (int i = 1; i <= maxDevices; i++) {
            devices.add(this.saveDevice(tenantId, "My device " + i));
            Assert.assertEquals(i, deviceService.countByTenantId(tenantId));
        }

        Assert.assertEquals(maxDevices, deviceService.countByTenantId(tenantId));
        Assert.assertEquals(1, deviceService.countByTenantId(anotherTenantId));
        Assert.assertEquals(0, deviceService.countByTenantId(TenantId.SYS_TENANT_ID));

        devices.forEach(device -> deleteDevice(tenantId, device));
        deleteDevice(anotherTenantId, anotherDevice);
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：无。
     */
    void deleteDevice(TenantId tenantId, Device device) {
        deviceService.deleteDevice(tenantId, device.getId());
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    Device saveDevice(TenantId tenantId, final String name) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(name);
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(device.getTenantId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());

        savedDevice.setName("New " + savedDevice.getName());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
        return foundDevice;
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithFirmware() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);
        Assert.assertNotNull(savedDevice.getId());
        Assert.assertTrue(savedDevice.getCreatedTime() > 0);
        Assert.assertEquals(device.getTenantId(), savedDevice.getTenantId());
        Assert.assertNotNull(savedDevice.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedDevice.getCustomerId().getId());
        Assert.assertEquals(device.getName(), savedDevice.getName());

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNotNull(deviceCredentials);
        Assert.assertNotNull(deviceCredentials.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        Assert.assertEquals(DeviceCredentialsType.ACCESS_TOKEN, deviceCredentials.getCredentialsType());
        Assert.assertNotNull(deviceCredentials.getCredentialsId());
        Assert.assertEquals(20, deviceCredentials.getCredentialsId().length());


        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(device.getDeviceProfileId());
        firmware.setType(FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        deviceService.saveDevice(savedDevice);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertEquals(foundDevice.getName(), savedDevice.getName());
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignFirmwareToDeviceWithDifferentDeviceProfile() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);

        Assert.assertNotNull(savedDevice);

        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "New device Profile");
        DeviceProfile savedProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
        Assert.assertNotNull(savedProfile);

        OtaPackage firmware = new OtaPackage();
        firmware.setTenantId(tenantId);
        firmware.setDeviceProfileId(savedProfile.getId());
        firmware.setType(FIRMWARE);
        firmware.setTitle("my firmware");
        firmware.setVersion("v1.0");
        firmware.setFileName("test.txt");
        firmware.setContentType("text/plain");
        firmware.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        firmware.setChecksum("4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a");
        firmware.setData(ByteBuffer.wrap(new byte[]{1}));
        firmware.setDataSize(1L);
        OtaPackage savedFirmware = otaPackageService.saveOtaPackage(firmware);

        savedDevice.setFirmwareId(savedFirmware.getId());

        assertThatThrownBy(() -> deviceService.saveDevice(savedDevice))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("Can't assign firmware with different deviceProfile!");
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithEmptyName() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithEmptyTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    /**
     * 功能：验证校验异常相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithNameContains0x00_thenDataValidationException() {
        Device device = new Device();
        device.setType("default");
        device.setTenantId(tenantId);
        device.setName("F0929906\000\000\000\000\000\000\000\000\000");
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceWithInvalidTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceService.saveDevice(device);
        });
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotPutInCacheRolledbackDeviceProfile() {
        DeviceProfile deviceProfile = createDeviceProfile(tenantId, "New device Profile" + StringUtils.randomAlphabetic(5));


        Device device = new Device();
        device.setType(deviceProfile.getName());
        device.setTenantId(tenantId);
        device.setName("My device"+ StringUtils.randomAlphabetic(5));

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            deviceProfileService.saveDeviceProfile(deviceProfile);
            deviceService.saveDevice(device);
        } finally {
            platformTransactionManager.rollback(status);
        }
        DeviceProfile deviceProfileByName = deviceProfileService.findDeviceProfileByName(tenantId, deviceProfile.getName());
        Assert.assertNull(deviceProfileByName);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDeviceToNonExistentCustomer() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        Device savedDevice = deviceService.saveDevice(device);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceService.assignDeviceToCustomer(tenantId, savedDevice.getId(), new CustomerId(Uuids.timeBased()));
            });
        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
        }
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDeviceToCustomerFromDifferentTenant() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        Device savedDevice = deviceService.saveDevice(device);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceService.assignDeviceToCustomer(tenantId, savedDevice.getId(), savedCustomer.getId());
            });
        } finally {
            deviceService.deleteDevice(tenantId, savedDevice.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    /**
     * 功能：验证设备ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceById() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        Assert.assertEquals(savedDevice, foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceTypesByTenantId() throws Exception {
        List<Device> devices = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device B" + i);
                device.setType("typeB");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 7; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device C" + i);
                device.setType("typeC");
                devices.add(deviceService.saveDevice(device));
            }
            for (int i = 0; i < 9; i++) {
                Device device = new Device();
                device.setTenantId(tenantId);
                device.setName("My device A" + i);
                device.setType("typeA");
                devices.add(deviceService.saveDevice(device));
            }
            List<EntitySubtype> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId).get();
            Assert.assertNotNull(deviceTypes);
            Assert.assertEquals(3, deviceTypes.size());
            Assert.assertEquals("typeA", deviceTypes.get(0).getType());
            Assert.assertEquals("typeB", deviceTypes.get(1).getType());
            Assert.assertEquals("typeC", deviceTypes.get(2).getType());
        } finally {
            devices.forEach((device) -> {
                deviceService.deleteDevice(tenantId, device.getId());
            });
        }
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteDevice() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        Device foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNotNull(foundDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDevice = deviceService.findDeviceById(tenantId, savedDevice.getId());
        Assert.assertNull(foundDevice);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(foundDeviceCredentials);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantId() {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 178; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            devices.add(deviceService.saveDevice(device));
        }

        List<Device> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);

        Assert.assertEquals(devices, loadedDevices);

        deviceService.deleteDevicesByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDevicesByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdAndName() {
        String title1 = "Device title 1";
        List<DeviceInfo> devicesTitle1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle1.add(new DeviceInfo(deviceService.saveDevice(device), null, false, "default", false));
        }
        String title2 = "Device title 2";
        List<DeviceInfo> devicesTitle2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            devicesTitle2.add(new DeviceInfo(deviceService.saveDevice(device), null, false, "default", false));
        }

        List<DeviceInfo> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);

        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);

        List<DeviceInfo> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);

        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdAndType() {
        String title1 = "Device title 1";
        String type1 = "typeA";
        List<Device> devicesType1 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            devicesType1.add(deviceService.saveDevice(device));
        }
        String title2 = "Device title 2";
        String type2 = "typeB";
        List<Device> devicesType2 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            devicesType2.add(deviceService.saveDevice(device));
        }

        List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdAndCustomerId() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<DeviceInfo> devices = new ArrayList<>();
        for (int i = 0; i < 278; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            device.setName("Device" + i);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devices.add(new DeviceInfo(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId), customer.getTitle(), customer.isPublic(), "default", false));
        }

        List<DeviceInfo> loadedDevices = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<DeviceInfo> pageData = null;
        do {
            pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).customerId(customerId).build(), pageLink);
            loadedDevices.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devices, idComparator);
        Collections.sort(loadedDevices, idComparator);

        Assert.assertEquals(devices, loadedDevices);

        deviceService.unassignCustomerDevices(tenantId, customerId);

        pageLink = new PageLink(33);
        pageData = deviceService.findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).customerId(customerId).build(), pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Device title 1";
        List<Device> devicesTitle1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        String title2 = "Device title 2";
        List<Device> devicesTitle2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType("default");
            device = deviceService.saveDevice(device);
            devicesTitle2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }

        List<Device> loadedDevicesTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle1, idComparator);
        Collections.sort(loadedDevicesTitle1, idComparator);

        Assert.assertEquals(devicesTitle1, loadedDevicesTitle1);

        List<Device> loadedDevicesTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDevicesTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesTitle2, idComparator);
        Collections.sort(loadedDevicesTitle2, idComparator);

        Assert.assertEquals(devicesTitle2, loadedDevicesTitle2);

        for (Device device : loadedDevicesTitle1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesTitle2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDevicesByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Device title 1";
        String type1 = "typeC";
        List<Device> devicesType1 = new ArrayList<>();
        for (int i = 0; i < 175; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type1);
            device = deviceService.saveDevice(device);
            devicesType1.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }
        String title2 = "Device title 2";
        String type2 = "typeD";
        List<Device> devicesType2 = new ArrayList<>();
        for (int i = 0; i < 143; i++) {
            Device device = new Device();
            device.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            device.setName(name);
            device.setType(type2);
            device = deviceService.saveDevice(device);
            devicesType2.add(deviceService.assignDeviceToCustomer(tenantId, device.getId(), customerId));
        }

        List<Device> loadedDevicesType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15);
        PageData<Device> pageData = null;
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedDevicesType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType1, idComparator);
        Collections.sort(loadedDevicesType1, idComparator);

        Assert.assertEquals(devicesType1, loadedDevicesType1);

        List<Device> loadedDevicesType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedDevicesType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(devicesType2, idComparator);
        Collections.sort(loadedDevicesType2, idComparator);

        Assert.assertEquals(devicesType2, loadedDevicesType2);

        for (Device device : loadedDevicesType1) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Device device : loadedDevicesType2) {
            deviceService.deleteDevice(tenantId, device.getId());
        }

        pageLink = new PageLink(4);
        pageData = deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCleanCacheIfDeviceRenamed() {
        String deviceNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String deviceNameAfterRename = StringUtils.randomAlphanumeric(15);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(deviceNameBeforeRename);
        device.setType("default");
        deviceService.saveDevice(device);

        Device savedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        savedDevice.setName(deviceNameAfterRename);
        deviceService.saveDevice(savedDevice);

        Device renamedDevice = deviceService.findDeviceByTenantIdAndName(tenantId, deviceNameBeforeRename);

        Assert.assertNull("Can't find device by name in cache if it was renamed", renamedDevice);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceInfoByTenantId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setType("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());

        Device savedDevice = deviceService.saveDevice(device);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                    && d.getTenantId().equals(tenantId)
                                    && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );

        PageLink pageLinkWithType = new PageLink(100, 0, device.getType());
        List<DeviceInfo> deviceInfosWithType = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).build(), pageLinkWithType).getData();

        Assert.assertFalse(deviceInfosWithType.isEmpty());
        Assert.assertTrue(
                deviceInfosWithType.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getType().equals(device.getType())
                        )
        );
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceInfoByTenantIdAndType() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setType("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());
        Device savedDevice = deviceService.saveDevice(device);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).type(device.getType()).build(), pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                    && d.getTenantId().equals(tenantId)
                                    && d.getDeviceProfileName().equals(savedDevice.getType())
                                    && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).type(device.getType()).build(), pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileName().equals(savedDevice.getType())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceInfoByTenantIdAndDeviceProfileId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("default");
        device.setLabel("label");
        device.setCustomerId(savedCustomer.getId());
        Device savedDevice = deviceService.saveDevice(device);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<DeviceInfo> deviceInfosWithLabel = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).deviceProfileId(savedDevice.getDeviceProfileId()).build(), pageLinkWithLabel).getData();

        Assert.assertFalse(deviceInfosWithLabel.isEmpty());
        Assert.assertTrue(
                deviceInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileId().equals(savedDevice.getDeviceProfileId())
                                        && d.getLabel().equals(savedDevice.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<DeviceInfo> deviceInfosWithCustomer = deviceService
                .findDeviceInfosByFilter(DeviceInfoFilter.builder().tenantId(tenantId).deviceProfileId(savedDevice.getDeviceProfileId()).build(), pageLinkWithCustomer).getData();

        Assert.assertFalse(deviceInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                deviceInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedDevice.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getDeviceProfileId().equals(savedDevice.getDeviceProfileId())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
