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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. 类目的：`CustomerServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class CustomerServiceTest extends AbstractServiceTest {

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    CustomerService customerService;

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    static final int TIMEOUT = 30;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService executor;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
      }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        executor.shutdownNow();
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomer() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);

        Assert.assertNotNull(savedCustomer);
        Assert.assertNotNull(savedCustomer.getId());
        Assert.assertTrue(savedCustomer.getCreatedTime() > 0);
        Assert.assertEquals(customer.getTenantId(), savedCustomer.getTenantId());
        Assert.assertEquals(customer.getTitle(), savedCustomer.getTitle());


        savedCustomer.setTitle("My new customer");

        customerService.saveCustomer(savedCustomer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertEquals(foundCustomer.getTitle(), savedCustomer.getTitle());

        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    /**
     * 功能：验证客户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomerById() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNotNull(foundCustomer);
        Assert.assertEquals(savedCustomer, foundCustomer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithEmptyTitle() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithEmptyTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithInvalidTenant() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveCustomerWithInvalidEmail() {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle("My customer");
        customer.setEmail("invalid@mail");
        Assertions.assertThrows(DataValidationException.class, () -> {
            customerService.saveCustomer(customer);
        });
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteCustomer() {
        Customer customer = new Customer();
        customer.setTitle("My customer");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);
        customerService.deleteCustomer(tenantId, savedCustomer.getId());
        Customer foundCustomer = customerService.findCustomerById(tenantId, savedCustomer.getId());
        Assert.assertNull(foundCustomer);
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomersByTenantId() throws Exception {

        List<ListenableFuture<Customer>> futures = new ArrayList<>(135);
        for (int i = 0; i < 135; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            customer.setTitle("Customer" + i);
            futures.add(executor.submit(() ->
                    customerService.saveCustomer(customer)));
        }
        List<Customer> customers = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Customer> loadedCustomers = new ArrayList<>(135);
        PageLink pageLink = new PageLink(23);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customers).containsExactlyInAnyOrderElementsOf(loadedCustomers);

        customerService.deleteCustomersByTenantId(tenantId);

        pageLink = new PageLink(33);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindCustomersByTenantIdAndTitle() throws Exception {
        String title1 = "Customer title 1";
        List<ListenableFuture<Customer>> futures = new ArrayList<>(143);
        for (int i = 0; i < 143; i++) {
            Customer customer = new Customer();
            customer.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            customer.setTitle(title);
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
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
            futures.add(executor.submit(() -> customerService.saveCustomer(customer)));
        }
        List<Customer> customersTitle2 = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);

        List<Customer> loadedCustomersTitle1 = new ArrayList<>(143);
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Customer> pageData = null;
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle1);

        List<Customer> loadedCustomersTitle2 = new ArrayList<>(175);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
            loadedCustomersTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(customersTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedCustomersTitle2);

        List<ListenableFuture<Void>> deleteFutures = new ArrayList<>(143);
        for (Customer customer : loadedCustomersTitle1) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title1);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        deleteFutures = new ArrayList<>(175);
        for (Customer customer : loadedCustomersTitle2) {
            deleteFutures.add(executor.submit(() -> {
                customerService.deleteCustomer(tenantId, customer.getId());
                return null;
            }));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(4, 0, title2);
        pageData = customerService.findCustomersByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`CustomerServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
