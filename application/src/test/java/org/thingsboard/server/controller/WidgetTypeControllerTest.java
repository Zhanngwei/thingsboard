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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@DaoSqlTest
public class WidgetTypeControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetType> idComparator = new IdComparator<>();

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

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        Assert.assertNotNull(savedWidgetType);
        Assert.assertNotNull(savedWidgetType.getId());
        Assert.assertNotNull(savedWidgetType.getFqn());
        Assert.assertTrue(savedWidgetType.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedWidgetType.getTenantId());
        Assert.assertEquals(widgetType.getName(), savedWidgetType.getName());
        Assert.assertEquals(widgetType.getDescriptor(), savedWidgetType.getDescriptor());

        savedWidgetType.setName("New Widget Type");

        doPost("/api/widgetType", savedWidgetType, WidgetType.class);

        WidgetTypeDetails foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetTypeDetails.class);
        Assert.assertEquals(foundWidgetType.getName(), savedWidgetType.getName());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetTypeFromDifferentTenant() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        loginDifferentTenant();
        doPost("/api/widgetType", savedWidgetType, WidgetTypeDetails.class, status().isForbidden());
        deleteDifferentTenant();
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindWidgetTypeById() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        WidgetTypeDetails foundWidgetType = doGet("/api/widgetType/" + savedWidgetType.getId().getId().toString(), WidgetTypeDetails.class);
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(savedWidgetType, foundWidgetType);
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);

        doDelete("/api/widgetType/"+savedWidgetType.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/widgetType/"+savedWidgetType.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetTypeWithEmptyName() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type name should be specified")));
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveWidgetTypeWithEmptyDescriptor() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{}", JsonNode.class));
        doPost("/api/widgetType", widgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Widgets type descriptor can't be empty")));
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testUpdateWidgetTypeFqn() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        savedWidgetType.setFqn("some_fqn");
        doPost("/api/widgetType", savedWidgetType)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Update of widget type fqn is prohibited")));

    }

    /**
     * 功能：验证部件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetBundleWidgetTypes() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        widgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);

        List<WidgetType> widgetTypes = new ArrayList<>();
        for (int i=0;i<89;i++) {
            WidgetTypeDetails widgetType = new WidgetTypeDetails();
            widgetType.setName("Widget Type " + i);
            widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
            widgetTypes.add(new WidgetType(doPost("/api/widgetType", widgetType, WidgetTypeDetails.class)));
        }

        List<String> widgetTypeIds = widgetTypes.stream().map(type -> type.getId().getId().toString()).collect(Collectors.toList());
        doPost("/api/widgetsBundle/" + widgetsBundle.getId().getId().toString() + "/widgetTypes", widgetTypeIds);

        List<WidgetType> loadedWidgetTypes = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>(){}, widgetsBundle.getId().getId().toString());

        Collections.sort(widgetTypes, idComparator);
        Collections.sort(loadedWidgetTypes, idComparator);

        Assert.assertEquals(widgetTypes, loadedWidgetTypes);

        loginCustomerUser();

        List<WidgetType> loadedWidgetTypesCustomer = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>(){}, widgetsBundle.getId().getId().toString());
        Collections.sort(loadedWidgetTypesCustomer, idComparator);
        Assert.assertEquals(widgetTypes, loadedWidgetTypesCustomer);

        List<WidgetTypeDetails> customerLoadedWidgetTypesDetails = doGetTyped("/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>(){}, widgetsBundle.getId().getId().toString());
        List<WidgetType> widgetTypesFromDetailsListCustomer = customerLoadedWidgetTypesDetails.stream().map(WidgetType::new).collect(Collectors.toList());
        Collections.sort(widgetTypesFromDetailsListCustomer, idComparator);
        Assert.assertEquals(widgetTypesFromDetailsListCustomer, loadedWidgetTypes);

        loginSysAdmin();

        List<WidgetType> sysAdminLoadedWidgetTypes = doGetTyped("/api/widgetTypes?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>(){}, widgetsBundle.getId().getId().toString());
        Collections.sort(sysAdminLoadedWidgetTypes, idComparator);
        Assert.assertEquals(widgetTypes, sysAdminLoadedWidgetTypes);

        List<WidgetTypeDetails> sysAdminLoadedWidgetTypesDetails = doGetTyped("/api/widgetTypesDetails?widgetsBundleId={widgetsBundleId}",
                new TypeReference<>(){}, widgetsBundle.getId().getId().toString());
        List<WidgetType> widgetTypesFromDetailsListSysAdmin = sysAdminLoadedWidgetTypesDetails.stream().map(WidgetType::new).collect(Collectors.toList());
        Collections.sort(widgetTypesFromDetailsListSysAdmin, idComparator);
        Assert.assertEquals(widgetTypesFromDetailsListSysAdmin, loadedWidgetTypes);
    }

    /**
     * 功能：验证部件类型相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetWidgetType() throws Exception {
        WidgetTypeDetails widgetType = new WidgetTypeDetails();
        widgetType.setName("Widget Type");
        widgetType.setDescriptor(JacksonUtil.fromString("{ \"someKey\": \"someValue\" }", JsonNode.class));
        WidgetTypeDetails savedWidgetType = doPost("/api/widgetType", widgetType, WidgetTypeDetails.class);
        WidgetType foundWidgetType = doGet("/api/widgetType?fqn={fqn}",
                WidgetType.class, "tenant."+savedWidgetType.getFqn());
        Assert.assertNotNull(foundWidgetType);
        Assert.assertEquals(new WidgetType(savedWidgetType), foundWidgetType);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
