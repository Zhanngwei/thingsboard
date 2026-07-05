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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`WidgetsBundleControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@DaoSqlTest
public class WidgetsBundleControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

    /**
     * 租户对象，用于描述当前业务场景。
     */
    private Tenant savedTenant;
    private User tenantAdmin;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
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
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");

        Mockito.reset(tbClusterService, auditLogService);

        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED);

        Mockito.reset(tbClusterService, auditLogService);

        Assert.assertNotNull(savedWidgetsBundle);
        Assert.assertNotNull(savedWidgetsBundle.getId());
        Assert.assertNotNull(savedWidgetsBundle.getAlias());
        Assert.assertTrue(savedWidgetsBundle.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetsBundle.getTenantId());
        Assert.assertEquals(widgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        savedWidgetsBundle.setTitle("My new widgets bundle");
        doPost("/api/widgetsBundle", savedWidgetsBundle, WidgetsBundle.class);

        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertEquals(foundWidgetsBundle.getTitle(), savedWidgetsBundle.getTitle());

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED);
    }

    /**
     * 功能：验证部件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetBundleWithViolationOfLengthValidation() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle(StringUtils.randomAlphabetic(300));

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("title");
        doPost("/api/widgetsBundle", widgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(widgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException("Validation error: title length must be equal or less than 255"));
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetsBundleFromDifferentTenant() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/widgetsBundle", savedWidgetsBundle)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedWidgetsBundle.getId(), savedWidgetsBundle);

        deleteDifferentTenant();
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetsBundleById() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteWidgetsBundle() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");

        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString())
                .andExpect(status().isOk());

        String savedWidgetsBundleIdStr = savedWidgetsBundle.getId().getId().toString();

        testNotifyEntityAllOneTime(savedWidgetsBundle, savedWidgetsBundle.getId(), savedWidgetsBundle.getId(), savedTenant.getId(),
                tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.DELETED);

        doGet("/api/widgetsBundle/" + savedWidgetsBundleIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Widgets bundle", savedWidgetsBundleIdStr))));
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetsBundleWithEmptyTitle() throws Exception {
        Mockito.reset(tbClusterService, auditLogService);

        WidgetsBundle widgetsBundle = new WidgetsBundle();
        doPost("/api/widgetsBundle", widgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets bundle title " + msgErrorShouldBeSpecified)));

        testNotifyEntityEqualsOneTimeServiceNeverError(widgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.ADDED,
                new DataValidationException("Widgets bundle title should be specified!"));
    }

    /**
     * 功能：验证部件包相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetsBundleAlias() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        savedWidgetsBundle.setAlias("new_alias");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/widgetsBundle", savedWidgetsBundle)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widgets bundle alias is prohibited")));
        testNotifyEntityEqualsOneTimeServiceNeverError(savedWidgetsBundle, savedTenant.getId(),
                tenantAdmin.getId(), tenantAdmin.getEmail(), ActionType.UPDATED,
                new DataValidationException("Update of widgets bundle alias is prohibited!"));
    }

    /**
     * 功能：验证分页查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantWidgetsBundlesByPageLink() throws Exception {
        loginSysAdmin();

        //upload some system bundles
        int sysCntEntity = 10;
        for (int i = 0; i < sysCntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        }

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        login(tenantAdmin.getEmail(), "testPassword1");

        int cntEntity = 73;
        List<WidgetsBundle> tenantWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            tenantWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> allWidgetsBundles = new ArrayList<>(tenantWidgetsBundles);
        allWidgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(allWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(allWidgetsBundles, loadedWidgetsBundles);

        //retrieve tenant only bundles
        List<WidgetsBundle> loadedWidgetsBundles2 = new ArrayList<>();
        PageLink pageLink2 = new PageLink(14);
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?tenantOnly=true&",
                    new TypeReference<>() {
                    }, pageLink2);
            loadedWidgetsBundles2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink2 = pageLink2.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles2, idComparator);

        Assert.assertEquals(tenantWidgetsBundles, loadedWidgetsBundles2);

        // cleanup
        loginSysAdmin();
        for (WidgetsBundle sysWidgetsBundle : sysWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + sysWidgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    /**
     * 功能：验证分页查询条件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSystemWidgetsBundlesByPageLink() throws Exception {

        loginSysAdmin();

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        int cntEntity = 120;
        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < cntEntity; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            createdWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> widgetsBundles = new ArrayList<>(createdWidgetsBundles);
        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = new ArrayList<>();
        PageLink pageLink = new PageLink(14);
        PageData<WidgetsBundle> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(17);
        loadedWidgetsBundles.clear();
        do {
            pageData = doGetTypedWithPageLink("/api/widgetsBundles?",
                    new TypeReference<PageData<WidgetsBundle>>() {
                    }, pageLink);
            loadedWidgetsBundles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }


    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindTenantWidgetsBundles() throws Exception {

        login(tenantAdmin.getEmail(), "testPassword1");

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>() {
                });

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        for (int i = 0; i < 73; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Widgets bundle" + i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        widgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindSystemAndTenantWidgetsBundles() throws Exception {

        loginSysAdmin();


        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        for (int i = 0; i < 82; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Sys widgets bundle" + i);
            createdSystemWidgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> systemWidgetsBundles = new ArrayList<>(createdSystemWidgetsBundles);
        systemWidgetsBundles.addAll(sysWidgetsBundles);

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        widgetsBundles.addAll(systemWidgetsBundles);

        login(tenantAdmin.getEmail(), "testPassword1");

        for (int i = 0; i < 127; i++) {
            WidgetsBundle widgetsBundle = new WidgetsBundle();
            widgetsBundle.setTitle("Tenant widgets bundle" + i);
            widgetsBundles.add(doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class));
        }

        List<WidgetsBundle> loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>() {
                });

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        loginSysAdmin();

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(systemWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(systemWidgetsBundles, loadedWidgetsBundles);

        for (WidgetsBundle widgetsBundle : createdSystemWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        loadedWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetsBundleControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
