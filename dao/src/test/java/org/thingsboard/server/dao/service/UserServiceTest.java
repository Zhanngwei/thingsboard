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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.user.UserService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`UserServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class UserServiceTest extends AbstractServiceTest {

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    CustomerService customerService;
    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    UserService userService;

    private IdComparator<User> idComparator = new IdComparator<>();

    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    private UserSettings userSettings;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("tenant@thingsboard.org");
        userService.saveUser(TenantId.SYS_TENANT_ID, tenantAdmin);

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customer@thingsboard.org");
        customerUser = userService.saveUser(tenantId, customerUser);

        userSettings = createUserSettings(customerUser.getId());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindUserByEmail() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.SYS_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.TENANT_ADMIN, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        Assert.assertNotNull(user);
        Assert.assertEquals(Authority.CUSTOMER_USER, user.getAuthority());
        user = userService.findUserByEmail(tenantId, "fake@thingsboard.org");
        Assert.assertNull(user);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindUserById() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID, "sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        User foundUser = userService.findUserById(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindUserCredentials() {
        User user = userService.findUserByEmail(SYSTEM_TENANT_ID,"sysadmin@thingsboard.org");
        Assert.assertNotNull(user);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(SYSTEM_TENANT_ID, user.getId());
        Assert.assertNotNull(userCredentials);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveUser() {
        User tenantAdminUser = userService.findUserByEmail(SYSTEM_TENANT_ID,"tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(TenantId.SYS_TENANT_ID, user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(user.getEmail(), savedUser.getEmail());
        Assert.assertEquals(user.getTenantId(), savedUser.getTenantId());
        Assert.assertEquals(user.getAuthority(), savedUser.getAuthority());
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, savedUser.getId());
        Assert.assertNotNull(userCredentials);
        Assert.assertNotNull(userCredentials.getId());
        Assert.assertNotNull(userCredentials.getUserId());
        Assert.assertNotNull(userCredentials.getActivateToken());

        savedUser.setFirstName("Joe");
        savedUser.setLastName("Downs");

        userService.saveUser(TenantId.SYS_TENANT_ID, savedUser);
        savedUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertEquals("Joe", savedUser.getFirstName());
        Assert.assertEquals("Downs", savedUser.getLastName());

        userService.deleteUser(tenantId, savedUser);
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveUserWithSameEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("sysadmin@thingsboard.org");
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantId, tenantAdminUser);
        });
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveUserWithInvalidEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail("tenant_thingsboard.org");
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantId, tenantAdminUser);
        });
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveUserWithEmptyEmail() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setEmail(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantId, tenantAdminUser);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveUserWithoutTenant() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        tenantAdminUser.setTenantId(null);
        Assertions.assertThrows(DataValidationException.class, () -> {
            userService.saveUser(tenantId, tenantAdminUser);
        });
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteUser() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantAdminUser.getTenantId());
        user.setEmail("tenant2@thingsboard.org");
        User savedUser = userService.saveUser(TenantId.SYS_TENANT_ID, user);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        User foundUser = userService.findUserById(tenantId, savedUser.getId());
        Assert.assertNotNull(foundUser);
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        Assert.assertNotNull(userCredentials);
        userService.deleteUser(tenantId, foundUser);
        userCredentials = userService.findUserCredentialsByUserId(tenantId, foundUser.getId());
        foundUser = userService.findUserById(tenantId, foundUser.getId());
        Assert.assertNull(foundUser);
        Assert.assertNull(userCredentials);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantAdmins() {
        User tenantAdminUser = userService.findUserByEmail(tenantId, "tenant@thingsboard.org");
        PageData<User> pageData = userService.findTenantAdmins(tenantAdminUser.getTenantId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(tenantAdminUser, users.get(0));

        TenantId secondTenantId = createTenant().getId();

        List<User> tenantAdmins = new ArrayList<>();
        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(secondTenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(userService.saveUser(TenantId.SYS_TENANT_ID, user));
        }

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findTenantAdmins(secondTenantId, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        Assert.assertEquals(tenantAdmins, loadedTenantAdmins);

        tenantService.deleteTenant(secondTenantId);

        pageLink = new PageLink(33);
        pageData = userService.findTenantAdmins(secondTenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantAdminsByEmail() {
        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        for (int i = 0; i < 94; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(userService.saveUser(TenantId.SYS_TENANT_ID, user));
        }

        String email2 = "testEmail2";
        List<User> tenantAdminsEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(userService.saveUser(TenantId.SYS_TENANT_ID, user));
        }

        List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail1, idComparator);
        Collections.sort(loadedTenantAdminsEmail1, idComparator);

        Assert.assertEquals(tenantAdminsEmail1, loadedTenantAdminsEmail1);

        List<User> loadedTenantAdminsEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findTenantAdmins(tenantId, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        for (User user : loadedTenantAdminsEmail1) {
            userService.deleteUser(tenantId, user);
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedTenantAdminsEmail2) {
            userService.deleteUser(tenantId, user);
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findTenantAdmins(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerUsers() {
        User customerUser = userService.findUserByEmail(tenantId, "customer@thingsboard.org");
        PageData<User> pageData = userService.findCustomerUsers(customerUser.getTenantId(),
                customerUser.getCustomerId(), new PageLink(10));
        Assert.assertFalse(pageData.hasNext());
        List<User> users = pageData.getData();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(customerUser, users.get(0));

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        List<User> customerUsers = new ArrayList<>();
        for (int i = 0; i < 156; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(userService.saveUser(tenantId, user));
        }

        List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        tenantService.deleteTenant(tenantId);

        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerUsersByEmail() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);

        CustomerId customerId = customer.getId();

        String email1 = "testEmail1";
        List<User> customerUsersEmail1 = new ArrayList<>();

        for (int i = 0; i < 124; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail1.add(userService.saveUser(tenantId, user));
        }

        String email2 = "testEmail2";
        List<User> customerUsersEmail2 = new ArrayList<>();

        for (int i = 0; i < 132; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setTenantId(tenantId);
            user.setCustomerId(customerId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            customerUsersEmail2.add(userService.saveUser(tenantId, user));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail1, idComparator);
        Collections.sort(loadedCustomerUsersEmail1, idComparator);

        Assert.assertEquals(customerUsersEmail1, loadedCustomerUsersEmail1);

        List<User> loadedCustomerUsersEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (User user : loadedCustomerUsersEmail1) {
            userService.deleteUser(tenantId, user);
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            userService.deleteUser(tenantId, user);
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = userService.findCustomerUsers(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    private UserSettings createUserSettings(UserId userId) {
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(userId);
        userSettings.setSettings(JacksonUtil.newObjectNode().put("text", StringUtils.randomAlphanumeric(10)));
        return userSettings;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`UserServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
