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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `DeviceCredentialsCacheTest` 是 ThingsBoard DAO 中验证 `DeviceCredentialsCache` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class DeviceCredentialsCacheTest extends AbstractServiceTest {

    private final String CREDENTIALS_ID_1 = StringUtils.randomAlphanumeric(20);
    private final String CREDENTIALS_ID_2 = StringUtils.randomAlphanumeric(20);

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    /**
     * 凭据，用于认证或安全校验。
     */
    @Autowired
    private DataValidator<DeviceCredentials> credentialsValidator;

    /**
     * 设备凭据，用于读取或保存对应领域对象。
     */
    private DeviceCredentialsDao deviceCredentialsDao;
    private DeviceService deviceService;

    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    private CacheManager cacheManager;

    private UUID deviceId = UUID.randomUUID();

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setup() throws Exception {
        deviceService = mock(DeviceService.class);
        deviceCredentialsDao = mock(DeviceCredentialsDao.class);

        ReflectionTestUtils.setField(credentialsValidator, "deviceService", deviceService);
        ReflectionTestUtils.setField(credentialsValidator, "deviceCredentialsDao", deviceCredentialsDao);

        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "deviceCredentialsDao", deviceCredentialsDao);
        ReflectionTestUtils.setField(unwrapDeviceCredentialsService(), "credentialsValidator", credentialsValidator);
    }

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void cleanup() {
        cacheManager.getCache(CacheConstants.DEVICE_CREDENTIALS_CACHE).clear();
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceCredentialsByCredentialsId_Cached() {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteDeviceCredentials_EvictsCache() {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);

        deviceCredentialsService.deleteDeviceCredentials(SYSTEM_TENANT_ID, createDummyDeviceCredentials(CREDENTIALS_ID_1, deviceId));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(2)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentials_EvictsPreviousCache() throws Exception {
        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1);

        verify(deviceCredentialsDao, times(1)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);

        when(deviceCredentialsDao.findByDeviceId(SYSTEM_TENANT_ID, deviceId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));

        UUID deviceCredentialsId = UUID.randomUUID();
        when(deviceCredentialsDao.findById(SYSTEM_TENANT_ID, deviceCredentialsId)).thenReturn(createDummyDeviceCredentialsEntity(CREDENTIALS_ID_1));
        when(deviceService.findDeviceById(SYSTEM_TENANT_ID, new DeviceId(deviceId))).thenReturn(new Device());

        var dummy = createDummyDeviceCredentials(deviceCredentialsId, CREDENTIALS_ID_2, deviceId);
        when(deviceCredentialsDao.saveAndFlush(SYSTEM_TENANT_ID, dummy)).thenReturn(dummy);

        deviceCredentialsService.updateDeviceCredentials(SYSTEM_TENANT_ID, dummy);

        when(deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1)).thenReturn(null);

        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1); // cache miss, DB read (not found), Cache put null value
        deviceCredentialsService.findDeviceCredentialsByCredentialsId(CREDENTIALS_ID_1); // cache hit (null value, credentials not found)

        verify(deviceCredentialsDao, times(2)).findByCredentialsId(SYSTEM_TENANT_ID, CREDENTIALS_ID_1);
    }

    /**
     * 功能：执行 `unwrapDeviceCredentialsService` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private DeviceCredentialsService unwrapDeviceCredentialsService() throws Exception {
        if (AopUtils.isAopProxy(deviceCredentialsService) && deviceCredentialsService instanceof Advised) {
            Object target = ((Advised) deviceCredentialsService).getTargetSource().getTarget();
            return (DeviceCredentialsService) target;
        }
        return deviceCredentialsService;
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `deviceCredentialsId`：设备凭据ID。
     * 返回：处理结果。
     */
    private DeviceCredentials createDummyDeviceCredentialsEntity(String deviceCredentialsId) {
        DeviceCredentials result = new DeviceCredentials(new DeviceCredentialsId(UUID.randomUUID()));
        result.setCredentialsId(deviceCredentialsId);
        return result;
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `deviceCredentialsId`：设备凭据ID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private DeviceCredentials createDummyDeviceCredentials(String deviceCredentialsId, UUID deviceId) {
        return createDummyDeviceCredentials(null, deviceCredentialsId, deviceId);
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `id`：`id`ID。
     * - `deviceCredentialsId`：设备凭据ID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private DeviceCredentials createDummyDeviceCredentials(UUID id, String deviceCredentialsId, UUID deviceId) {
        DeviceCredentials result = new DeviceCredentials();
        result.setId(new DeviceCredentialsId(id));
        result.setDeviceId(new DeviceId(deviceId));
        result.setCredentialsId(deviceCredentialsId);
        result.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        return result;
    }
}
