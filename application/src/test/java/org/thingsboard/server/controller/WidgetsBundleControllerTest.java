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

@DaoSqlTest
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
public class WidgetsBundleControllerTest extends AbstractControllerTest {

    private IdComparator<WidgetsBundle> idComparator = new IdComparator<>();

    /**
     * 字段说明：
     * 1. 保存 `savedTenant` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `beforeTest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `afterTest` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWidgetsBundle` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWidgetBundleWithViolationOfLengthValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateWidgetsBundleFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindWidgetsBundleById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindWidgetsBundleById() throws Exception {
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("My widgets bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        WidgetsBundle foundWidgetsBundle = doGet("/api/widgetsBundle/" + savedWidgetsBundle.getId().getId().toString(), WidgetsBundle.class);
        Assert.assertNotNull(foundWidgetsBundle);
        Assert.assertEquals(savedWidgetsBundle, foundWidgetsBundle);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteWidgetsBundle` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWidgetsBundleWithEmptyTitle` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateWidgetsBundleAlias` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantWidgetsBundlesByPageLink` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantWidgetsBundlesByPageLink() throws Exception {
        loginSysAdmin();

        //upload some system bundles
        int sysCntEntity = 10;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink2 = pageLink2.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles2, idComparator);

        Assert.assertEquals(tenantWidgetsBundles, loadedWidgetsBundles2);

        // cleanup
        loginSysAdmin();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (WidgetsBundle sysWidgetsBundle : sysWidgetsBundles) {
            doDelete("/api/widgetsBundle/" + sysWidgetsBundle.getId().getId().toString())
                    .andExpect(status().isOk());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindSystemWidgetsBundlesByPageLink` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindSystemWidgetsBundlesByPageLink() throws Exception {

        loginSysAdmin();

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        int cntEntity = 120;
        List<WidgetsBundle> createdWidgetsBundles = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(widgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(widgetsBundles, loadedWidgetsBundles);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(sysWidgetsBundles, idComparator);
        Collections.sort(loadedWidgetsBundles, idComparator);

        Assert.assertEquals(sysWidgetsBundles, loadedWidgetsBundles);
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantWidgetsBundles` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantWidgetsBundles() throws Exception {

        login(tenantAdmin.getEmail(), "testPassword1");

        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<List<WidgetsBundle>>() {
                });

        List<WidgetsBundle> widgetsBundles = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindSystemAndTenantWidgetsBundles` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindSystemAndTenantWidgetsBundles() throws Exception {

        loginSysAdmin();


        List<WidgetsBundle> sysWidgetsBundles = doGetTyped("/api/widgetsBundles?",
                new TypeReference<>() {
                });

        List<WidgetsBundle> createdSystemWidgetsBundles = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
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
