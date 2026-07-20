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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. `CustomerControllerTest` 是 ThingsBoard Application 中验证 `CustomerController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ContextConfiguration(classes = {CustomerControllerTest.Config.class})
@DaoSqlTest
public class CustomerControllerTest extends AbstractControllerTest {
    static final TypeReference<PageData<Customer>> PAGE_DATA_CUSTOMER_TYPE_REFERENCE = new TypeReference<>() {
    };

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService executor;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private Tenant savedTenant;
    private User tenantAdmin;

    /**
     * 客户，用于读取或保存对应领域对象。
     */
    @Autowired
    private CustomerDao customerDao;

    /**
     * 中文说明：
     * 1. `Config` 是 ThingsBoard Application 中描述配置行为的配置类型。
     * 2. 它集中保存该组件启动或运行时需要的可配置选项。
     * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
     * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
     * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
     * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
     */
    static class Config {
        /**
         * 功能：执行 `customerDao` 对应的处理。
         * 参数：
         * - `customerDao`：`customerDao` 参数。
         * 返回：处理结果。
         */
        @Bean
        @Primary
        public CustomerDao customerDao(CustomerDao customerDao) {
            return Mockito.mock(CustomerDao.class, AdditionalAnswers.delegatesTo(customerDao));
        }
    }


    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        executor.shutdownNow();

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("My customer");

        Mockito.reset(tbClusterService, auditLogService);

        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        testNotifyEntityAllOneTime(savedCustomer, savedCustomer.getId(), savedCustomer.getId(), savedCustomer.getTenantId(),
                new CustomerId(CustomerId.NULL_UUID), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());

        savedCustomer.setTitle("My new customer");

        doPost("/api/customer", savedCustomer, Customer.class);

        testNotifyEntityAllOneTime(savedCustomer, savedCustomer.getId(), savedCustomer.getId(), savedCustomer.getTenantId(),
                new CustomerId(CustomerId.NULL_UUID), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.UPDATED);

        Customer foundCustomer = doGet("/api/customer/" + savedCustomer.getId().getId().toString(), Customer.class);
        Assert.assertEquals(foundCustomer.getTitle(), savedCustomer.getTitle());

        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithViolationOfValidation() throws Exception {
        Customer customer = new Customer();
        customer.setTitle(StringUtils.randomAlphabetic(300));

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("title");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        customer.setTenantId(savedTenant.getId());
        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        customer.setTitle("Normal title");
        customer.setCity(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("city");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        customer.setCity("Normal city");
        customer.setCountry(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("country");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        customer.setCountry("Ukraine");
        customer.setPhone(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("phone");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        customer.setPhone("+3892555554512");
        customer.setState(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("state");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        customer.setState("Normal state");
        customer.setZip(StringUtils.randomAlphabetic(300));
        msgError = msgErrorFieldLength("zip or postal code");
        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateCustomerFromDifferentTenant() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        doPost("/api/customer", savedCustomer, Customer.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer", savedCustomer, Customer.class, status().isForbidden());

        testNotifyEntityNever(savedCustomer.getId(), savedCustomer);

        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedCustomer.getId(), savedCustomer);

        deleteDifferentTenant();
        login(tenantAdmin.getName(), "testPassword1");

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedCustomer, savedCustomer.getId(),
                savedCustomer.getId(), savedCustomer.getTenantId(), savedCustomer.getId(), tenantAdmin.getId(),
                tenantAdmin.getEmail(), ActionType.DELETED, savedCustomer.getId().getId().toString());
    }

    /**
     * 功能：验证客户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerById() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Customer foundCustomer = doGet("/api/customer/" + savedCustomer.getId().getId().toString(), Customer.class);
        Assert.assertNotNull(foundCustomer);
        Assert.assertEquals(savedCustomer, foundCustomer);

        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/customer/" + savedCustomer.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedCustomer, savedCustomer.getId(),
                savedCustomer.getId(), savedCustomer.getTenantId(), savedCustomer.getId(), tenantAdmin.getId(),
                tenantAdmin.getEmail(), ActionType.DELETED, savedCustomer.getId().getId().toString());

        String customerIdStr = savedCustomer.getId().getId().toString();
        doGet("/api/customer/" + customerIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Customer", customerIdStr))));
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithEmptyTitle() throws Exception {
        Customer customer = new Customer();
        String msgError = "Customer title " + msgErrorShouldBeSpecified;

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer,savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithInvalidEmail() throws Exception {
        Customer customer = new Customer();
        String msgError = "Invalid email address format 'invalid@mail'";
        customer.setTitle("My customer");
        customer.setEmail("invalid@mail");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer", customer)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(customer, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED, new DataValidationException(msgError));
    }

    /**
     * 功能：验证`Find Customers`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomers() throws Exception {
        TenantId tenantId = savedTenant.getId();

        int cntEntity = 135;

        Mockito.reset(tbClusterService, auditLogService);

        List<ListenableFuture<Customer>> futures = new ArrayList<>(cntEntity);
        for (int i = 0; i < cntEntity; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle("Customer" + i);
            futures.add(executor.submit(() ->
                    doPost("/api/customer", customer, Customer.class)));
        }
        List<Customer> customers = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(new Customer(), new Customer(),
                tenantId, tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        List<Customer> loadedCustomers = new ArrayList<>(135);
        PageLink pageLink = new PageLink(23);
        PageData<Customer> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customers?", PAGE_DATA_CUSTOMER_TYPE_REFERENCE, pageLink);
            loadedCustomers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customers).containsExactlyInAnyOrderElementsOf(loadedCustomers);

        deleteEntitiesAsync("/api/customer/", loadedCustomers, executor).get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 功能：验证`Find Customers By Title`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomersByTitle() throws Exception {
        TenantId tenantId = savedTenant.getId();

        String title1 = "Customer title 1";
        List<ListenableFuture<Customer>> futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() ->
                    doPost("/api/customer", customer, Customer.class)));
        }
        List<Customer> customersTitle1 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        String title2 = "Customer title 2";
        futures = new ArrayList<>(175);
        for (int i = 0; i < 175; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() ->
                    doPost("/api/customer", customer, Customer.class)));
        }

        List<Customer> customersTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Customer> loadedCustomersTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Customer> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customers?", PAGE_DATA_CUSTOMER_TYPE_REFERENCE, pageLink);
            loadedCustomersTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle1);

        List<Customer> loadedCustomersTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/customers?", PAGE_DATA_CUSTOMER_TYPE_REFERENCE, pageLink);
            loadedCustomersTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle2);

        deleteEntitiesAsync("/api/customer/", loadedCustomersTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/customers?", PAGE_DATA_CUSTOMER_TYPE_REFERENCE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteEntitiesAsync("/api/customer/", loadedCustomersTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/customers?", PAGE_DATA_CUSTOMER_TYPE_REFERENCE, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteCustomerWithDeleteRelationsOk() throws Exception {
        CustomerId customerId = createCustomer("Customer for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(savedTenant.getId(), customerId, "/api/customer/" + customerId);
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Ignore
    @Test
    public void testDeleteCustomerExceptionWithRelationsTransactional() throws Exception {
        CustomerId customerId = createCustomer("Customer for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(customerDao, savedTenant.getId(), customerId, "/api/customer/" + customerId);
    }

    /**
     * 功能：保存或创建客户。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    private Customer createCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return doPost("/api/customer", customer, Customer.class);
    }
}
