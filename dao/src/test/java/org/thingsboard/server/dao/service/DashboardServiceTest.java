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
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. 类目的：`DashboardServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class DashboardServiceTest extends AbstractServiceTest {

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    CustomerService customerService;
    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    DashboardService dashboardService;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    EdgeService edgeService;

    private IdComparator<DashboardInfo> idComparator = new IdComparator<>();
    
    /**
     * 功能：验证仪表盘相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDashboard() throws IOException {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        
        Assert.assertNotNull(savedDashboard);
        Assert.assertNotNull(savedDashboard.getId());
        Assert.assertTrue(savedDashboard.getCreatedTime() > 0);
        Assert.assertEquals(dashboard.getTenantId(), savedDashboard.getTenantId());
        Assert.assertEquals(dashboard.getTitle(), savedDashboard.getTitle());
        
        savedDashboard.setTitle("My new dashboard");
        
        dashboardService.saveDashboard(savedDashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertEquals(foundDashboard.getTitle(), savedDashboard.getTitle());
        
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }
    
    /**
     * 功能：验证仪表盘相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDashboardWithEmptyTitle() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        Assertions.assertThrows(DataValidationException.class, () -> {
            dashboardService.saveDashboard(dashboard);
        });
    }
    
    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDashboardWithEmptyTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        Assertions.assertThrows(DataValidationException.class, () -> {
            dashboardService.saveDashboard(dashboard);
        });
    }
    
    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDashboardWithInvalidTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            dashboardService.saveDashboard(dashboard);
        });
    }
    
    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDashboardToNonExistentCustomer() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                dashboardService.assignDashboardToCustomer(tenantId, savedDashboard.getId(), new CustomerId(Uuids.timeBased()));
            });
        } finally {
            dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
        }
    }
    
    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDashboardToCustomerFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [dashboard]");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                dashboardService.assignDashboardToCustomer(tenantId, savedDashboard.getId(), savedCustomer.getId());
            });
        } finally {
            dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
    
    /**
     * 功能：验证仪表盘ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDashboardById() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        Assert.assertEquals(savedDashboard, foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
    }
    
    /**
     * 功能：验证仪表盘相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteDashboard() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTenantId(tenantId);
        dashboard.setTitle("My dashboard");
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Dashboard foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNotNull(foundDashboard);
        dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
        foundDashboard = dashboardService.findDashboardById(tenantId, savedDashboard.getId());
        Assert.assertNull(foundDashboard);
    }
    
    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDashboardsByTenantId() {
        List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<165;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboards.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        
        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(16);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);
        
        Assert.assertEquals(dashboards, loadedDashboards);
        
        dashboardService.deleteDashboardsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindMobileDashboardsByTenantId() {
        List<DashboardInfo> mobileDashboards = new ArrayList<>();
        for (int i=0;i<165;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboard.setMobileHide(i % 2 == 0);
            if (!dashboard.isMobileHide()) {
                dashboard.setMobileOrder(i % 4 == 0 ? (int)(Math.random() * 100) : null);
            }
            Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
            if (!dashboard.isMobileHide()) {
                mobileDashboards.add(new DashboardInfo(savedDashboard));
            }
        }

        List<DashboardInfo> loadedMobileDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(16, 0, null, new SortOrder("title", SortOrder.Direction.ASC));
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink);
            loadedMobileDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(mobileDashboards, (o1, o2) -> {
            Integer order1 = o1.getMobileOrder();
            Integer order2 = o2.getMobileOrder();
            if (order1 == null && order2 == null) {
                return o1.getTitle().compareTo(o2.getTitle());
            } else if (order1 == null && order2 != null) {
                return 1;
            }  else if (order2 == null) {
                return -1;
            } else {
                return order1 - order2;
            }
        });

        Assert.assertEquals(mobileDashboards, loadedMobileDashboards);

        dashboardService.deleteDashboardsByTenantId(tenantId);

        pageLink = new PageLink(31);
        pageData = dashboardService.findMobileDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }
    
    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDashboardsByTenantIdAndTitle() {
        String title1 = "Dashboard title 1";
        List<DashboardInfo> dashboardsTitle1 = new ArrayList<>();
        for (int i=0;i<123;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int)(Math.random()*17));
            String title = title1+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle1.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        String title2 = "Dashboard title 2";
        List<DashboardInfo> dashboardsTitle2 = new ArrayList<>();
        for (int i=0;i<193;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int)(Math.random()*15));
            String title = title2+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            dashboard.setTitle(title);
            dashboardsTitle2.add(new DashboardInfo(dashboardService.saveDashboard(dashboard)));
        }
        
        List<DashboardInfo> loadedDashboardsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(19, 0, title1);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboardsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(dashboardsTitle1, idComparator);
        Collections.sort(loadedDashboardsTitle1, idComparator);
        
        Assert.assertEquals(dashboardsTitle1, loadedDashboardsTitle1);
        
        List<DashboardInfo> loadedDashboardsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
            loadedDashboardsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(dashboardsTitle2, idComparator);
        Collections.sort(loadedDashboardsTitle2, idComparator);
        
        Assert.assertEquals(dashboardsTitle2, loadedDashboardsTitle2);

        for (DashboardInfo dashboard : loadedDashboardsTitle1) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
        
        pageLink = new PageLink(4, 0, title1);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (DashboardInfo dashboard : loadedDashboardsTitle2) {
            dashboardService.deleteDashboard(tenantId, dashboard.getId());
        }
        
        pageLink = new PageLink(4, 0, title2);
        pageData = dashboardService.findDashboardsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
    
    /**
     * 功能：验证租户ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDashboardsByTenantIdAndCustomerId() throws ExecutionException, InterruptedException {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();
        
        List<DashboardInfo> dashboards = new ArrayList<>();
        for (int i=0;i<223;i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTenantId(tenantId);
            dashboard.setTitle("Dashboard"+i);
            dashboard = dashboardService.saveDashboard(dashboard);
            dashboards.add(new DashboardInfo(dashboardService.assignDashboardToCustomer(tenantId, dashboard.getId(), customerId)));
        }
        
        List<DashboardInfo> loadedDashboards = new ArrayList<>();
        PageLink pageLink = new PageLink(23);
        PageData<DashboardInfo> pageData = null;
        do {
            pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedDashboards.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(dashboards, idComparator);
        Collections.sort(loadedDashboards, idComparator);
        
        Assert.assertEquals(dashboards, loadedDashboards);
        
        dashboardService.unassignCustomerDashboards(tenantId, customerId);

        pageLink = new PageLink(42);
        pageData = dashboardService.findDashboardsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    /**
     * 功能：验证仪表盘相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDashboardToNonExistentEdge() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                dashboardService.assignDashboardToEdge(tenantId, savedDashboard.getId(), new EdgeId(Uuids.timeBased()));
            });
        } finally {
            dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
        }
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAssignDashboardToEdgeFromDifferentTenant() {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle("My dashboard");
        dashboard.setTenantId(tenantId);
        Dashboard savedDashboard = dashboardService.saveDashboard(dashboard);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant [edge]");
        tenant = tenantService.saveTenant(tenant);
        Edge edge = new Edge();
        edge.setTenantId(tenant.getId());
        edge.setType("default");
        edge.setName("Test different edge");
        edge.setType("default");
        edge.setSecret(StringUtils.randomAlphanumeric(20));
        edge.setRoutingKey(StringUtils.randomAlphanumeric(20));
        Edge savedEdge = edgeService.saveEdge(edge);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                dashboardService.assignDashboardToEdge(tenantId, savedDashboard.getId(), savedEdge.getId());
            });
        } finally {
            dashboardService.deleteDashboard(tenantId, savedDashboard.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DashboardServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
