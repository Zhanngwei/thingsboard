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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.device.DeviceCredentialsDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `JpaDeviceCredentialsDaoTest` 是 ThingsBoard DAO 中验证 `JpaDeviceCredentialsDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaDeviceCredentialsDaoTest extends AbstractJpaDaoTest {

    /**
     * 设备凭据，用于读取或保存对应领域对象。
     */
    @Autowired
    DeviceCredentialsDao deviceCredentialsDao;

    /**
     * 设备凭据列表，用于保存一组待处理对象。
     */
    List<DeviceCredentials> deviceCredentialsList;
    DeviceCredentials neededDeviceCredentials;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        deviceCredentialsList = List.of(createAndSaveDeviceCredentials(), createAndSaveDeviceCredentials());
        neededDeviceCredentials = deviceCredentialsList.get(0);
        assertNotNull(neededDeviceCredentials);
    }

    /**
     * 功能：保存或创建设备凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    DeviceCredentials createAndSaveDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(UUID.randomUUID().toString());
        deviceCredentials.setCredentialsValue("CHECK123");
        deviceCredentials.setDeviceId(new DeviceId(UUID.randomUUID()));
        return deviceCredentialsDao.save(TenantId.SYS_TENANT_ID, deviceCredentials);
    }

    /**
     * 功能：删除或清理设备凭据。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void deleteDeviceCredentials() {
        for (DeviceCredentials credentials : deviceCredentialsList) {
            deviceCredentialsDao.removeById(TenantId.SYS_TENANT_ID, credentials.getUuidId());
        }
    }

    /**
     * 功能：验证设备ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByDeviceId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByDeviceId(SYSTEM_TENANT_ID, neededDeviceCredentials.getDeviceId().getId());
        assertNotNull(foundedDeviceCredentials);
        assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
        assertEquals(neededDeviceCredentials.getCredentialsId(), foundedDeviceCredentials.getCredentialsId());
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void findByCredentialsId() {
        DeviceCredentials foundedDeviceCredentials = deviceCredentialsDao.findByCredentialsId(SYSTEM_TENANT_ID, neededDeviceCredentials.getCredentialsId());
        assertNotNull(foundedDeviceCredentials);
        assertEquals(neededDeviceCredentials.getId(), foundedDeviceCredentials.getId());
    }
}
