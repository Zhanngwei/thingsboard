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
 * 1. `WidgetsBundleControllerTest` 是 ThingsBoard Application 中验证 `WidgetsBundleController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
