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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.FeaturesInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.UsageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.ApiUsageStateFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityTypeFilter;
import org.thingsboard.server.common.data.query.TsValue;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`HomePageApiTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class HomePageApiTest extends AbstractControllerTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `apiUsageStateClient` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbApiUsageStateClient apiUsageStateClient;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileCache` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `adminSettingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AdminSettingsService adminSettingsService;

    @MockBean
    /**
     * 字段说明：
     * 1. 保存 `mailService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private MailService mailService;

    @MockBean
    /**
     * 字段说明：
     * 1. 保存 `smsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private SmsService smsService;

    /**
     * 字段说明：
     * 1. 保存 `DEFAULT_DASHBOARDS_COUNT` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int DEFAULT_DASHBOARDS_COUNT = 1;

    //For system administrator
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTenantsCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTenantsCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.TENANT);

        List<Tenant> tenants = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("tenant" + i);
            tenants.add(doPost("/api/tenant", tenant, Tenant.class));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.TENANT);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Tenant tenant : tenants) {
            doDelete("/api/tenant/" + tenant.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTenantProfilesCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTenantProfilesCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.TENANT_PROFILE);

        List<TenantProfile> tenantProfiles = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            TenantProfile tenantProfile = new TenantProfile();
            tenantProfile.setName("tenantProfile" + i);
            tenantProfiles.add(doPost("/api/tenantProfile", tenantProfile, TenantProfile.class));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.TENANT_PROFILE);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (TenantProfile tenantProfile : tenantProfiles) {
            doDelete("/api/tenantProfile/" + tenantProfile.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUsersCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUsersCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.USER);

        List<User> users = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setEmail(i + "user@thingsboard.org");
            user.setTenantId(tenantId);
            user.setAuthority(Authority.TENANT_ADMIN);
            users.add(doPost("/api/user", user, User.class));
        }

        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.USER);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (User user : users) {
            doDelete("/api/user/" + user.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCustomersCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCustomersCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.CUSTOMER);
        loginTenantAdmin();

        List<Customer> customers = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            Customer customer = new Customer();
            customer.setTitle("customer" + i);
            customers.add(doPost("/api/customer", customer, Customer.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.CUSTOMER);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Customer customer : customers) {
            doDelete("/api/customer/" + customer.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDevicesCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDevicesCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.DEVICE);
        loginTenantAdmin();

        List<Device> devices = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            Device device = new Device();
            device.setName("device" + i);
            devices.add(doPost("/api/device", device, Device.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.DEVICE);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Device device : devices) {
            doDelete("/api/device/" + device.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssetsCountWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssetsCountWsCmd() throws Exception {
        Long initialCount = getInitialEntityCount(EntityType.ASSET);
        loginTenantAdmin();

        List<Asset> assets = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            Asset asset = new Asset();
            asset.setName("asset" + i);
            assets.add(doPost("/api/asset", asset, Asset.class));
        }

        loginSysAdmin();
        EntityTypeFilter ef = new EntityTypeFilter();
        ef.setEntityType(EntityType.ASSET);
        EntityCountCmd cmd = new EntityCountCmd(1, new EntityCountQuery(ef, Collections.emptyList()));
        getWsClient().send(cmd);
        EntityCountUpdate update = getWsClient().parseCountReply(getWsClient().waitForReply());
        Assert.assertEquals(1, update.getCmdId());
        Assert.assertEquals(initialCount + 100, update.getCount());

        loginTenantAdmin();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Asset asset : assets) {
            doDelete("/api/asset/" + asset.getId().toString());
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSystemInfoTimeSeriesWsCmd` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSystemInfoTimeSeriesWsCmd() throws Exception {
        ApiUsageState apiUsageState = apiUsageStateClient.getApiUsageState(TenantId.SYS_TENANT_ID);
        Assert.assertNotNull(apiUsageState);

        loginSysAdmin();
        long now = System.currentTimeMillis();

        EntityDataUpdate update = getWsClient().sendEntityDataQuery(new ApiUsageStateFilter());

        Assert.assertEquals(1, update.getCmdId());
        PageData<EntityData> pageData = update.getData();
        Assert.assertNotNull(pageData);
        Assert.assertEquals(1, pageData.getData().size());
        Assert.assertEquals(apiUsageState.getId(), pageData.getData().get(0).getEntityId());

        List<String> metrics = List.of("cpuUsage", "memoryUsage", "discUsage", "cpuCount", "totalMemory", "totalDiscSpace");
        update = getWsClient().subscribeTsUpdate(
                metrics,
                now, TimeUnit.HOURS.toMillis(1));
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> listData = update.getUpdate();
        Assert.assertNotNull(listData);
        Assert.assertEquals(1, listData.size());
        Assert.assertEquals(apiUsageState.getId(), listData.get(0).getEntityId());
        Assert.assertEquals(metrics.size(), listData.get(0).getTimeseries().size());

        for (TsValue[] tsv : listData.get(0).getTimeseries().values()) {
            Assert.assertTrue(tsv.length > 0);
        }
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetFeaturesInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetFeaturesInfo() throws Exception {
        String mail = "test@thingsboard.org";
        Mockito.doAnswer(invocation -> {
            AdminSettings mailSettings = adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "mail");
            JsonNode mailFrom = mailSettings.getJsonValue().get("mailFrom");
            if (!mailFrom.asText().equals(mail)) {
                throw new Exception();
            }
            return null;
        }).when(mailService).testConnection(TenantId.SYS_TENANT_ID);

        Mockito.when(smsService.isConfigured(TenantId.SYS_TENANT_ID))
                .then(a -> adminSettingsService.findAdminSettingsByKey(TenantId.SYS_TENANT_ID, "sms") != null);

        loginSysAdmin();

        FeaturesInfo featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertNotNull(featuresInfo);
        Assert.assertFalse(featuresInfo.isEmailEnabled());
        Assert.assertFalse(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings mailSettings = doGet("/api/admin/settings/mail", AdminSettings.class);

        JsonNode jsonValue = mailSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("mailFrom", mail);
        mailSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", mailSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertFalse(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings smsSettings = new AdminSettings();
        smsSettings.setKey("sms");
        smsSettings.setJsonValue(JacksonUtil.newObjectNode());
        doPost("/api/admin/settings", smsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertFalse(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings twoFaSettingsSettings = new AdminSettings();
        twoFaSettingsSettings.setKey("twoFaSettings");

        var twoFaSettings = JacksonUtil.newObjectNode();
        var providers = JacksonUtil.newArrayNode();
        providers.add(JacksonUtil.newObjectNode());
        twoFaSettings.set("providers", providers);
        twoFaSettingsSettings.setJsonValue(twoFaSettings);
        doPost("/api/admin/settings", twoFaSettingsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertFalse(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        AdminSettings notificationsSettings = new AdminSettings();
        notificationsSettings.setKey("notifications");

        var notificationSettings = JacksonUtil.newObjectNode();
        var deliveryMethodsConfigs = JacksonUtil.newObjectNode();
        deliveryMethodsConfigs.set("SLACK", JacksonUtil.newObjectNode());
        notificationSettings.set("deliveryMethodsConfigs", deliveryMethodsConfigs);

        notificationsSettings.setJsonValue(notificationSettings);
        doPost("/api/admin/settings", notificationsSettings).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertTrue(featuresInfo.isNotificationEnabled());
        Assert.assertFalse(featuresInfo.isOauthEnabled());

        OAuth2Info oAuth2Info = createDefaultOAuth2Info();

        doPost("/api/oauth2/config", oAuth2Info).andExpect(status().isOk());

        featuresInfo = doGet("/api/admin/featuresInfo", FeaturesInfo.class);
        Assert.assertNotNull(featuresInfo);
        Assert.assertTrue(featuresInfo.isEmailEnabled());
        Assert.assertTrue(featuresInfo.isSmsEnabled());
        Assert.assertTrue(featuresInfo.isTwoFaEnabled());
        Assert.assertTrue(featuresInfo.isNotificationEnabled());
        Assert.assertTrue(featuresInfo.isOauthEnabled());

        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "notifications");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "twoFaSettings");
        adminSettingsService.deleteAdminSettingsByTenantIdAndKey(TenantId.SYS_TENANT_ID, "sms");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUsageInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUsageInfo() throws Exception {
        loginTenantAdmin();

        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);

        Assert.assertNotNull(tenantProfile);

        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();

        UsageInfo usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertNotNull(usageInfo);
        Assert.assertEquals(0, usageInfo.getDevices());
        Assert.assertEquals(configuration.getMaxDevices(), usageInfo.getMaxDevices());

        Assert.assertEquals(0, usageInfo.getAssets());
        Assert.assertEquals(configuration.getMaxAssets(), usageInfo.getMaxAssets());

        Assert.assertEquals(1, usageInfo.getCustomers());
        Assert.assertEquals(configuration.getMaxCustomers(), usageInfo.getMaxCustomers());

        Assert.assertEquals(2, usageInfo.getUsers());
        Assert.assertEquals(configuration.getMaxUsers(), usageInfo.getMaxUsers());

        Assert.assertEquals(DEFAULT_DASHBOARDS_COUNT, usageInfo.getDashboards());
        Assert.assertEquals(configuration.getMaxDashboards(), usageInfo.getMaxDashboards());

        Assert.assertEquals(0, usageInfo.getTransportMessages());
        Assert.assertEquals(configuration.getMaxTransportMessages(), usageInfo.getMaxTransportMessages());

        Assert.assertEquals(0, usageInfo.getJsExecutions());
        Assert.assertEquals(configuration.getMaxJSExecutions(), usageInfo.getMaxJsExecutions());

        Assert.assertEquals(0, usageInfo.getTbelExecutions());
        Assert.assertEquals(configuration.getMaxTbelExecutions(), usageInfo.getMaxTbelExecutions());

        Assert.assertEquals(0, usageInfo.getEmails());
        Assert.assertEquals(configuration.getMaxEmails(), usageInfo.getMaxEmails());

        Assert.assertEquals(0, usageInfo.getSms());
        Assert.assertEquals(configuration.getMaxSms(), usageInfo.getMaxSms());

        Assert.assertEquals(0, usageInfo.getAlarms());
        Assert.assertEquals(configuration.getMaxCreatedAlarms(), usageInfo.getMaxAlarms());

        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Device device = new Device();
            device.setName("device" + i);
            devices.add(doPost("/api/device", device, Device.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(devices.size(), usageInfo.getDevices());

        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Asset asset = new Asset();
            asset.setName("asset" + i);
            assets.add(doPost("/api/asset", asset, Asset.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(assets.size(), usageInfo.getAssets());

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Customer customer = new Customer();
            customer.setTitle("customer" + i);
            customers.add(doPost("/api/customer", customer, Customer.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(customers.size() + 1, usageInfo.getCustomers());

        List<User> users = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setEmail(i + "user@thingsboard.org");
            users.add(doPost("/api/user", user, User.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        Assert.assertEquals(users.size() + 2, usageInfo.getUsers());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < 97; i++) {
            Dashboard dashboard = new Dashboard();
            dashboard.setTitle("dashboard" + i);
            dashboards.add(doPost("/api/dashboard", dashboard, Dashboard.class));
        }

        usageInfo = doGet("/api/usage", UsageInfo.class);
        int expectedDashboardsCount = dashboards.size() + DEFAULT_DASHBOARDS_COUNT;
        Assert.assertEquals(expectedDashboardsCount, usageInfo.getDashboards());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getInitialEntityCount` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Long getInitialEntityCount(EntityType entityType) throws Exception {
        loginSysAdmin();

        EntityTypeFilter allEntityFilter = new EntityTypeFilter();
        allEntityFilter.setEntityType(entityType);
        EntityCountQuery query = new EntityCountQuery(allEntityFilter);
        return doPostWithResponse("/api/entitiesQuery/count", query, Long.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDefaultOAuth2Info` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private OAuth2Info createDefaultOAuth2Info() {
        return new OAuth2Info(true, Lists.newArrayList(
                OAuth2ParamsInfo.builder()
                        .domainInfos(Lists.newArrayList(
                                OAuth2DomainInfo.builder().name("domain").scheme(SchemeType.MIXED).build()
                        ))
                        .mobileInfos(Collections.emptyList())
                        .clientRegistrations(Lists.newArrayList(
                                validRegistrationInfo()
                        ))
                        .build()
        ));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `validRegistrationInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private OAuth2RegistrationInfo validRegistrationInfo() {
        return OAuth2RegistrationInfo.builder()
                .clientId(UUID.randomUUID().toString())
                .clientSecret(UUID.randomUUID().toString())
                .authorizationUri(UUID.randomUUID().toString())
                .accessTokenUri(UUID.randomUUID().toString())
                .scope(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                .platforms(Collections.emptyList())
                .userInfoUri(UUID.randomUUID().toString())
                .userNameAttributeName(UUID.randomUUID().toString())
                .jwkSetUri(UUID.randomUUID().toString())
                .clientAuthenticationMethod(UUID.randomUUID().toString())
                .loginButtonLabel(UUID.randomUUID().toString())
                .mapperConfig(
                        OAuth2MapperConfig.builder()
                                .type(MapperType.CUSTOM)
                                .custom(
                                        OAuth2CustomMapperConfig.builder()
                                                .url(UUID.randomUUID().toString())
                                                .build()
                                )
                                .build()
                )
                .build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`HomePageApiTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
