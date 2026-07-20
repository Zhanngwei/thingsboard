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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.user.UserCredentialsDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.service.AbstractServiceTest.SYSTEM_TENANT_ID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
/**
 * 中文说明：
 * 1. `JpaUserCredentialsDaoTest` 是 ThingsBoard DAO 中验证 `JpaUserCredentialsDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaUserCredentialsDaoTest extends AbstractJpaDaoTest {

    /**
     * 令牌常量，用于统一引用固定值。
     */
    public static final String ACTIVATE_TOKEN = "ACTIVATE_TOKEN_0";
    public static final String RESET_TOKEN = "RESET_TOKEN_0";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final int COUNT_USER_CREDENTIALS = 2;
    List<UserCredentials> userCredentialsList;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    UserCredentials neededUserCredentials;

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserCredentialsDao userCredentialsDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        userCredentialsList = new ArrayList<>();
        for (int i=0; i<COUNT_USER_CREDENTIALS; i++) {
            userCredentialsList.add(createUserCredentials(i));
        }
        neededUserCredentials = userCredentialsList.get(0);
        assertNotNull(neededUserCredentials);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：处理结果。
     */
    UserCredentials createUserCredentials(int number) {
        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setEnabled(true);
        userCredentials.setUserId(new UserId(UUID.randomUUID()));
        userCredentials.setPassword("password");
        userCredentials.setActivateToken("ACTIVATE_TOKEN_" + number);
        userCredentials.setResetToken("RESET_TOKEN_" + number);
        return userCredentialsDao.save(SYSTEM_TENANT_ID, userCredentials);
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        for (UserCredentials userCredentials : userCredentialsList) {
            userCredentialsDao.removeById(TenantId.SYS_TENANT_ID, userCredentials.getUuidId());
        }
    }

    /**
     * 功能：验证`Find All`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAll() {
        List<UserCredentials> userCredentials = userCredentialsDao.find(SYSTEM_TENANT_ID);
        assertEquals(COUNT_USER_CREDENTIALS + 1, userCredentials.size());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByUserId() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByUserId(SYSTEM_TENANT_ID, neededUserCredentials.getUserId().getId());
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials, foundedUserCredentials);
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByActivateToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByActivateToken(SYSTEM_TENANT_ID, ACTIVATE_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByResetToken() {
        UserCredentials foundedUserCredentials = userCredentialsDao.findByResetToken(SYSTEM_TENANT_ID, RESET_TOKEN);
        assertNotNull(foundedUserCredentials);
        assertEquals(neededUserCredentials.getId(), foundedUserCredentials.getId());
    }
}
