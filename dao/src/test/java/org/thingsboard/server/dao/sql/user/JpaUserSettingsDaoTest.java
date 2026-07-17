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
package org.thingsboard.server.dao.sql.user;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsCompositeKey;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.user.UserSettingsDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * 中文说明：
 * 1. `JpaUserSettingsDaoTest` 是 ThingsBoard DAO 中验证 `JpaUserSettingsDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaUserSettingsDaoTest extends AbstractJpaDaoTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private UUID tenantId;
    private User user;

    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private UserSettingsDao userSettingsDao;

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserDao userDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        tenantId = Uuids.timeBased();
        user = saveUser(tenantId, Uuids.timeBased());
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        userDao.removeById(user.getTenantId(), user.getUuidId());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSettingsByUserId() {
        UserSettings userSettings = createUserSettings(user.getId());

        UserSettings retrievedUserSettings = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));
        assertEquals(retrievedUserSettings.getSettings(), userSettings.getSettings());

        userSettingsDao.removeById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));

        UserSettings retrievedUserSettings2 = userSettingsDao.findById(SYSTEM_TENANT_ID, new UserSettingsCompositeKey(user.getId().getId(), UserSettingsType.GENERAL.name()));
        assertNull(retrievedUserSettings2);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setType(UserSettingsType.GENERAL);
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        userSettings.setUserId(userId);
        return userSettingsDao.save(SYSTEM_TENANT_ID, userSettings);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    private User saveUser(UUID tenantId, UUID customerId) {
        User user = new User();
        UUID id = Uuids.timeBased();
        user.setId(new UserId(id));
        user.setTenantId(TenantId.fromUUID(tenantId));
        user.setCustomerId(new CustomerId(customerId));
        if (customerId == NULL_UUID) {
            user.setAuthority(Authority.TENANT_ADMIN);
        } else {
            user.setAuthority(Authority.CUSTOMER_USER);
        }
        String idString = id.toString();
        String email = idString.substring(0, idString.indexOf('-')) + "@thingsboard.org";
        user.setEmail(email);
        return  userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }
}
