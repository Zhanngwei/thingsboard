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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.user.UserDao;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 4/18/2017.
 */
/**
 * 中文说明：
 * 1. `JpaUserDaoTest` 是 ThingsBoard DAO 中验证 `JpaUserDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JpaUserDaoTest extends AbstractJpaDaoTest {

    // it comes from the DefaultSystemDataLoaderService super class
    /**
     * 用户常量，用于统一引用固定值。
     */
    final int COUNT_CREATED_USER = 1;
    final int COUNT_SYSADMIN_USER = 90;
    /**
     * 租户ID，用于定位对应业务对象。
     */
    UUID tenantId;
    UUID customerId;
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
        customerId = Uuids.timeBased();
        create30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() {
        delete30TenantAdminsAnd60CustomerUsers(tenantId, customerId);
    }

    /**
     * 功能：验证`Find All`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAll() {
        List<User> users = userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID);
        assertEquals(users.size(), COUNT_CREATED_USER + COUNT_SYSADMIN_USER);
    }

    /**
     * 功能：验证邮箱相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindByEmail() throws JsonProcessingException {
        User user = new User();
        user.setId(new UserId(UUID.randomUUID()));
        user.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        user.setCustomerId(new CustomerId(UUID.randomUUID()));
        user.setEmail("user@thingsboard.org");
        user.setFirstName("Jackson");
        user.setLastName("Roberts");
        String additionalInfo = "{\"key\":\"value-100\"}";
        JsonNode jsonNode = JacksonUtil.toJsonNode(additionalInfo);
        user.setAdditionalInfo(jsonNode);
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
        assertEquals(1 + COUNT_SYSADMIN_USER + COUNT_CREATED_USER, userDao.find(AbstractServiceTest.SYSTEM_TENANT_ID).size());
        User savedUser = userDao.findByEmail(AbstractServiceTest.SYSTEM_TENANT_ID, "user@thingsboard.org");
        assertNotNull(savedUser);
        assertEquals(additionalInfo, savedUser.getAdditionalInfo().toString());
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantAdmins() {
        PageLink pageLink = new PageLink(20);
        PageData<User> tenantAdmins1 = userDao.findTenantAdmins(tenantId, pageLink);
        assertEquals(20, tenantAdmins1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins2 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(10, tenantAdmins2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> tenantAdmins3 = userDao.findTenantAdmins(tenantId,
                pageLink);
        assertEquals(0, tenantAdmins3.getData().size());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerUsers() {
        PageLink pageLink = new PageLink(40);
        PageData<User> customerUsers1 = userDao.findCustomerUsers(tenantId, customerId, pageLink);
        assertEquals(40, customerUsers1.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers2 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(20, customerUsers2.getData().size());
        pageLink = pageLink.nextPageLink();
        PageData<User> customerUsers3 = userDao.findCustomerUsers(tenantId, customerId,
                pageLink);
        assertEquals(0, customerUsers3.getData().size());
    }

    /**
     * 功能：执行 `create30TenantAdminsAnd60CustomerUsers` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void create30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        // Create 30 tenant admins and 60 customer users
        for (int i = 0; i < 30; i++) {
            saveUser(tenantId, NULL_UUID);
            saveUser(tenantId, customerId);
            saveUser(tenantId, customerId);
        }
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void saveUser(UUID tenantId, UUID customerId) {
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
        userDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, user);
    }

    /**
     * 功能：执行 `delete30TenantAdminsAnd60CustomerUsers` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    private void delete30TenantAdminsAnd60CustomerUsers(UUID tenantId, UUID customerId) {
        List<User> data = userDao.findCustomerUsers(tenantId, customerId, new PageLink(60)).getData();
        data.addAll(userDao.findTenantAdmins(tenantId, new PageLink(30)).getData());
        for (User user : data) {
            userDao.removeById(user.getTenantId(), user.getUuidId());
        }
    }
}
