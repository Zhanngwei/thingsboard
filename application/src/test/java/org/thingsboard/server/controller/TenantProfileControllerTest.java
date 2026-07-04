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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`TenantProfileControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class TenantProfileControllerTest extends AbstractControllerTest {

    private IdComparator<TenantProfile> idComparator = new IdComparator<>();
    private IdComparator<EntityInfo> tenantProfileInfoIdComparator = new IdComparator<>();

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantProfile() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        Assert.assertNotNull(savedTenantProfile);
        Assert.assertNotNull(savedTenantProfile.getId());
        Assert.assertTrue(savedTenantProfile.getCreatedTime() > 0);
        Assert.assertEquals(tenantProfile.getName(), savedTenantProfile.getName());
        Assert.assertEquals(tenantProfile.getDescription(), savedTenantProfile.getDescription());
        Assert.assertEquals(tenantProfile.getProfileData(), savedTenantProfile.getProfileData());
        Assert.assertEquals(tenantProfile.isDefault(), savedTenantProfile.isDefault());
        Assert.assertEquals(tenantProfile.isIsolatedTbRuleEngine(), savedTenantProfile.isIsolatedTbRuleEngine());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.CREATED, 1);

        savedTenantProfile.setName("New tenant profile");
        doPost("/api/tenantProfile", savedTenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertEquals(foundTenantProfile.getName(), savedTenantProfile.getName());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.UPDATED, 1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantProfileWithViolationOfLengthValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantProfileWithViolationOfLengthValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = this.createTenantProfile(StringUtils.randomAlphabetic(300));
        doPost("/api/tenantProfile", tenantProfile)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgErrorFieldLength("name"))));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantProfileById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantProfileById() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile foundTenantProfile = doGet("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString(), TenantProfile.class);
        Assert.assertNotNull(foundTenantProfile);
        Assert.assertEquals(savedTenantProfile, foundTenantProfile);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantProfileInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantProfileInfoById() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        EntityInfo foundTenantProfileInfo = doGet("/api/tenantProfileInfo/"+savedTenantProfile.getId().getId().toString(), EntityInfo.class);
        Assert.assertNotNull(foundTenantProfileInfo);
        Assert.assertEquals(savedTenantProfile.getId(), foundTenantProfileInfo.getId());
        Assert.assertEquals(savedTenantProfile.getName(), foundTenantProfileInfo.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindDefaultTenantProfileInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindDefaultTenantProfileInfo() throws Exception {
        loginSysAdmin();
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals("Default", foundDefaultTenantProfile.getName());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSetDefaultTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSetDefaultTenantProfile() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile 1");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        TenantProfile defaultTenantProfile = doPost("/api/tenantProfile/"+savedTenantProfile.getId().getId().toString()+"/default", TenantProfile.class);
        Assert.assertNotNull(defaultTenantProfile);
        EntityInfo foundDefaultTenantProfile = doGet("/api/tenantProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultTenantProfile);
        Assert.assertEquals(savedTenantProfile.getName(), foundDefaultTenantProfile.getName());
        Assert.assertEquals(savedTenantProfile.getId(), foundDefaultTenantProfile.getId());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantProfileWithEmptyName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantProfileWithEmptyName() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile = new TenantProfile();
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isBadRequest())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile name " + msgErrorShouldBeSpecified)));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantProfileWithSameName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantProfileWithSameName() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile).andExpect(status().isOk());

        Mockito.reset(tbClusterService);

        TenantProfile tenantProfile2 = this.createTenantProfile("Tenant Profile");
        doPost("/api/tenantProfile", tenantProfile2)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant profile with such name already exists")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteTenantProfileWithExistingTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteTenantProfileWithExistingTenant() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with tenant profile");
        tenant.setTenantProfileId(savedTenantProfile.getId());
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("The tenant profile referenced by the tenants cannot be deleted")));

        testBroadcastEntityStateChangeEventNeverTenantProfile();

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteTenantProfile() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile");
        TenantProfile savedTenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        Mockito.reset(tbClusterService);

        doDelete("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isOk());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(savedTenantProfile, ComponentLifecycleEvent.DELETED, 1);

        doGet("/api/tenantProfile/" + savedTenantProfile.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Tenant profile", savedTenantProfile.getId().getId().toString()))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantProfiles` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantProfiles() throws Exception {
        loginSysAdmin();
        List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        tenantProfiles.addAll(pageData.getData());

        Mockito.reset(tbClusterService);

        int cntEntity = 28;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i=0;i<28;i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile"+i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.CREATED, cntEntity);

        List<TenantProfile> loadedTenantProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                    new TypeReference<>(){}, pageLink);
            loadedTenantProfiles.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfiles, idComparator);

        Assert.assertEquals(tenantProfiles, loadedTenantProfiles);

        Mockito.reset(tbClusterService);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TenantProfile tenantProfile : loadedTenantProfiles) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<PageData<TenantProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());

        testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(new TenantProfile(), ComponentLifecycleEvent.DELETED, cntEntity);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantProfileInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantProfileInfos() throws Exception {
        loginSysAdmin();
        List<TenantProfile> tenantProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantProfile> tenantProfilePageData = doGetTypedWithPageLink("/api/tenantProfiles?",
                new TypeReference<>(){}, pageLink);
        Assert.assertFalse(tenantProfilePageData.hasNext());
        Assert.assertEquals(1, tenantProfilePageData.getTotalElements());
        tenantProfiles.addAll(tenantProfilePageData.getData());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i=0;i<28;i++) {
            TenantProfile tenantProfile = this.createTenantProfile("Tenant Profile"+i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        List<EntityInfo> loadedTenantProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                    new TypeReference<PageData<EntityInfo>>(){}, pageLink);
            loadedTenantProfileInfos.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantProfiles, idComparator);
        Collections.sort(loadedTenantProfileInfos, tenantProfileInfoIdComparator);

        List<EntityInfo> tenantProfileInfos = tenantProfiles.stream().map(tenantProfile -> new EntityInfo(tenantProfile.getId(),
                tenantProfile.getName())).collect(Collectors.toList());

        Assert.assertEquals(tenantProfileInfos, loadedTenantProfileInfos);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TenantProfile tenantProfile : tenantProfiles) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!tenantProfile.isDefault()) {
                doDelete("/api/tenantProfile/" + tenantProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantProfileInfos?",
                new TypeReference<PageData<EntityInfo>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TenantProfile createTenantProfile(String name) {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName(name);
        tenantProfile.setDescription(name + " Test");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);
        return tenantProfile;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addMainQueueConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addMainQueueConfig(TenantProfile tenantProfile) {
        TenantProfileQueueConfiguration mainQueueConfiguration = new TenantProfileQueueConfiguration();
        mainQueueConfiguration.setName(DataConstants.MAIN_QUEUE_NAME);
        mainQueueConfiguration.setTopic(DataConstants.MAIN_QUEUE_TOPIC);
        mainQueueConfiguration.setPollInterval(25);
        mainQueueConfiguration.setPartitions(10);
        mainQueueConfiguration.setConsumerPerPartition(true);
        mainQueueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy mainQueueSubmitStrategy = new SubmitStrategy();
        mainQueueSubmitStrategy.setType(SubmitStrategyType.BURST);
        mainQueueSubmitStrategy.setBatchSize(1000);
        mainQueueConfiguration.setSubmitStrategy(mainQueueSubmitStrategy);
        ProcessingStrategy mainQueueProcessingStrategy = new ProcessingStrategy();
        mainQueueProcessingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        mainQueueProcessingStrategy.setRetries(3);
        mainQueueProcessingStrategy.setFailurePercentage(0);
        mainQueueProcessingStrategy.setPauseBetweenRetries(3);
        mainQueueProcessingStrategy.setMaxPauseBetweenRetries(3);
        mainQueueConfiguration.setProcessingStrategy(mainQueueProcessingStrategy);
        TenantProfileData profileData = tenantProfile.getProfileData();
        profileData.setQueueConfiguration(Collections.singletonList(mainQueueConfiguration));
        tenantProfile.setProfileData(profileData);
    }


    /**
     * 方法说明：
     * 1. 职责：执行 `testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testBroadcastEntityStateChangeEventTimeManyTimeTenantProfile(TenantProfile tenantProfile, ComponentLifecycleEvent event, int cntTime) {
        ArgumentMatcher<TenantProfile> matcherTenantProfile = cntTime == 1 ? argument -> argument.equals(tenantProfile) :
                argument -> argument.getClass().equals(TenantProfile.class);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (ComponentLifecycleEvent.DELETED.equals(event)) {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantProfileDelete(Mockito.argThat( matcherTenantProfile),
                    Mockito.isNull());
            testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        } else {
            Mockito.verify(tbClusterService, times( cntTime)).onTenantProfileChange(Mockito.argThat(matcherTenantProfile),
                    Mockito.isNull());
            TenantProfileId tenantProfileIdId = cntTime == 1 ? tenantProfile.getId() : (TenantProfileId) createEntityId_NULL_UUID(tenantProfile);
            testBroadcastEntityStateChangeEventTime(tenantProfileIdId, null,  cntTime);
        }
        Mockito.reset(tbClusterService);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testBroadcastEntityStateChangeEventNeverTenantProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testBroadcastEntityStateChangeEventNeverTenantProfile() {
        Mockito.verify(tbClusterService, never()).onTenantProfileChange(Mockito.any(TenantProfile.class),
                Mockito.isNull());
        testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        Mockito.reset(tbClusterService, auditLogService);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfileControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
