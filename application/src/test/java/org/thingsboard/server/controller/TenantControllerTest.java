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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.ProcessingStrategyType;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.DataConstants.MAIN_QUEUE_NAME;
import static org.thingsboard.server.common.data.DataConstants.MAIN_QUEUE_TOPIC;

@TestPropertySource(properties = {
        "js.evaluator=mock",
        "queue.rule-engine.topic-deletion-delay=10"
})
@Slf4j
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`TenantControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class TenantControllerTest extends AbstractControllerTest {

    static final TypeReference<PageData<Tenant>> PAGE_DATA_TENANT_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<TenantInfo>> PAGE_DATA_TENANT_INFO_TYPE_REF = new TypeReference<>() {
    };

    /**
     * 字段说明：
     * 1. 保存 `executor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    ListeningExecutorService executor;

    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `partitionService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private PartitionService partitionService;
    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `actorContext` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private ActorSystemContext actorContext;
    @SpyBean
    /**
     * 字段说明：
     * 1. 保存 `queueAdmin` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbQueueAdmin queueAdmin;

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setUp` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setUp() throws Exception {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `tearDown` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void tearDown() throws Exception {
        loginSysAdmin();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (Queue queue : doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<PageData<Queue>>() {}, new PageLink(100)).getData()) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!queue.getName().equals(MAIN_QUEUE_NAME)) {
                doDelete("/api/queues/" + queue.getId()).andExpect(status().isOk());
            }
        }
        executor.shutdownNow();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");

        Mockito.reset(tbClusterService);

        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        Assert.assertNotNull(savedTenant.getId());
        Assert.assertTrue(savedTenant.getCreatedTime() > 0);
        Assert.assertEquals(tenant.getTitle(), savedTenant.getTitle());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.CREATED, 1);

        savedTenant.setTitle("My new tenant");
        doPost("/api/tenant", savedTenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/" + savedTenant.getId().getId().toString(), Tenant.class);
        Assert.assertEquals(foundTenant.getTitle(), savedTenant.getTitle());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.UPDATED, 1);

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(savedTenant, ComponentLifecycleEvent.DELETED, 1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantWithViolationOfValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantWithViolationOfValidation() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle(StringUtils.randomAlphanumeric(300));

        Mockito.reset(tbClusterService);

        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgErrorFieldLength("title"))));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/" + savedTenant.getId().getId().toString(), Tenant.class);
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(savedTenant, foundTenant);
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantInfoById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        TenantInfo foundTenant = doGet("/api/tenant/info/" + savedTenant.getId().getId().toString(), TenantInfo.class);
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(new TenantInfo(savedTenant, "Default"), foundTenant);
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantWithEmptyTitle` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantWithEmptyTitle() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        Tenant tenant = new Tenant();
        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Tenant title " + msgErrorShouldBeSpecified)));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveTenantWithInvalidEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveTenantWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService);

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        doPost("/api/tenant", tenant)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Invalid email address format")));

        testBroadcastEntityStateChangeEventNeverTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);

        String tenantIdStr = savedTenant.getId().getId().toString();
        doDelete("/api/tenant/" + tenantIdStr)
                .andExpect(status().isOk());
        doGet("/api/tenant/" + tenantIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Tenant", tenantIdStr))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenants` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenants() throws Exception {
        loginSysAdmin();
        List<Tenant> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<Tenant> pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        Mockito.reset(tbClusterService);

        int cntEntity = 56;
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<ListenableFuture<Tenant>> createFutures = new ArrayList<>(56);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < cntEntity; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        tenants.addAll(Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS));

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(new Tenant(), ComponentLifecycleEvent.CREATED, cntEntity);

        List<Tenant> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenants.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(tenants).containsExactlyInAnyOrderElementsOf(loadedTenants);

        deleteEntitiesAsync("/api/tenant/", loadedTenants.stream()
                .filter((t) -> !TEST_TENANT_NAME.equals(t.getTitle()))
                .collect(Collectors.toList()), executor).get(TIMEOUT, TimeUnit.SECONDS);

        testBroadcastEntityStateChangeEventTimeManyTimeTenant(new Tenant(), ComponentLifecycleEvent.DELETED, cntEntity);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantsByTitle` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantsByTitle() throws Exception {
        log.debug("login sys admin");
        loginSysAdmin();
        log.debug("test started");
        String title1 = "Tenant title 1";
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<ListenableFuture<Tenant>> createFutures = new ArrayList<>(134);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 134; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title1 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<Tenant> tenantsTitle1 = Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("saved '{}', qty {}", title1, tenantsTitle1.size());

        String title2 = "Tenant title 2";
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        createFutures = new ArrayList<>(127);
        for (int i = 0; i < 127; i++) {
            Tenant tenant = new Tenant();
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String title = title2 + suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            createFutures.add(executor.submit(() ->
                    doPost("/api/tenant", tenant, Tenant.class)));
        }

        List<Tenant> tenantsTitle2 = Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("saved '{}', qty {}", title2, tenantsTitle2.size());

        List<Tenant> loadedTenantsTitle1 = new ArrayList<>(134);
        PageLink pageLink = new PageLink(15, 0, title1);
        PageData<Tenant> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenantsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        log.debug("found by name '{}', step 15 {}", title1, loadedTenantsTitle1.size());

        assertThat(tenantsTitle1).as(title1).containsExactlyInAnyOrderElementsOf(loadedTenantsTitle1);
        log.debug("asserted");

        List<Tenant> loadedTenantsTitle2 = new ArrayList<>(127);
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
            loadedTenantsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        log.debug("found by name '{}', step 4 {}", title1, loadedTenantsTitle2.size());
        assertThat(tenantsTitle2).as(title2).containsExactlyInAnyOrderElementsOf(loadedTenantsTitle2);
        log.debug("asserted");


        deleteEntitiesAsync("/api/tenant/", loadedTenantsTitle1, executor).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("deleted '{}', size {}", title1, loadedTenantsTitle1.size());

        pageLink = new PageLink(4, 0, title1);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        log.debug("tried to search another '{}', step 4", title1);

        deleteEntitiesAsync("/api/tenant/", loadedTenantsTitle2, executor).get(TIMEOUT, TimeUnit.SECONDS);
        log.debug("deleted '{}', size {}", title2, loadedTenantsTitle2.size());

        pageLink = new PageLink(4, 0, title2);
        pageData = doGetTypedWithPageLink("/api/tenants?", PAGE_DATA_TENANT_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        log.debug("tried to search another '{}', step 4", title2);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantInfos() throws Exception {
        loginSysAdmin();
        List<TenantInfo> tenants = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<TenantInfo> pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        List<ListenableFuture<TenantInfo>> createFutures = new ArrayList<>(56);
        for (int i = 0; i < 56; i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant" + i);
            createFutures.add(executor.submit(() ->
                    new TenantInfo(doPost("/api/tenant", tenant, Tenant.class), "Default")));
        }
        tenants.addAll(Futures.allAsList(createFutures).get(TIMEOUT, TimeUnit.SECONDS));

        List<TenantInfo> loadedTenants = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        assertThat(tenants).containsExactlyInAnyOrderElementsOf(loadedTenants);

        deleteEntitiesAsync("/api/tenant/", loadedTenants.stream()
                .filter((t) -> !TEST_TENANT_NAME.equals(t.getTitle()))
                .collect(Collectors.toList()), executor).get(TIMEOUT, TimeUnit.SECONDS);

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/tenantInfos?", PAGE_DATA_TENANT_INFO_TYPE_REF, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTenantsAsync` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    ListenableFuture<List<ResultActions>> deleteTenantsAsync(String urlTemplate, List<Tenant> tenants) {
        List<ListenableFuture<ResultActions>> futures = new ArrayList<>(tenants.size());
        for (Tenant device : tenants) {
            futures.add(executor.submit(() ->
                    doDelete(urlTemplate + device.getId().getId())
                            .andExpect(status().isOk())));
        }
        return Futures.allAsList(futures);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateQueueConfigForIsolatedTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateQueueConfigForIsolatedTenant() throws Exception {
        Comparator<Queue> queueComparator = Comparator.comparing(Queue::getName);
        final String username = "isolatedtenant@thingsboard.org";
        final String password = "123456";
        loginSysAdmin();

        List<Queue> sysAdminQueues;
        PageLink pageLink = new PageLink(10);
        PageData<Queue> pageData;
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {
        }, pageLink);
        sysAdminQueues = pageData.getData();

        Tenant tenant = new Tenant();
        tenant.setTitle("Isolated tenant");
        tenant = doPost("/api/tenant", tenant, Tenant.class);

        User tenantUser = new User();
        tenantUser.setAuthority(Authority.TENANT_ADMIN);
        tenantUser.setTenantId(tenant.getId());
        tenantUser.setEmail(username);
        createUserAndLogin(tenantUser, password);

        List<Queue> foundTenantQueues;

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(sysAdminQueues, foundTenantQueues);

        loginSysAdmin();

        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("isolated-tb-rule-engine");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile, MAIN_QUEUE_NAME);
        addQueueConfig(tenantProfile, "Test");
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        tenant.setTenantProfileId(tenantProfile.getId());
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(2, foundTenantQueues.size());

        List<Queue> queuesFromConfig = getQueuesFromConfig(tenantProfile.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        TenantProfile tenantProfile2 = new TenantProfile();
        tenantProfile2.setName("isolated-tb-rule-engine2");
        TenantProfileData tenantProfileData2 = new TenantProfileData();
        tenantProfileData2.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile2.setProfileData(tenantProfileData2);
        tenantProfile2.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile2, MAIN_QUEUE_NAME);
        addQueueConfig(tenantProfile2, "Test");
        addQueueConfig(tenantProfile2, "Test2");
        tenantProfile2 = doPost("/api/tenantProfile", tenantProfile2, TenantProfile.class);

        tenant.setTenantProfileId(tenantProfile2.getId());
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(3, foundTenantQueues.size());

        queuesFromConfig = getQueuesFromConfig(tenantProfile2.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        tenantProfile2.getProfileData().getQueueConfiguration().removeIf(q -> q.getName().equals("Test"));
        tenantProfile2.getProfileData().getQueueConfiguration().removeIf(q -> q.getName().equals("Test2"));
        addQueueConfig(tenantProfile2, "Test2");
        addQueueConfig(tenantProfile2, "Test3");

        tenantProfile2 = doPost("/api/tenantProfile", tenantProfile2, TenantProfile.class);

        login(username, password);

        pageLink = new PageLink(10);
        pageData = doGetTypedWithPageLink("/api/queues?serviceType=TB_RULE_ENGINE&", new TypeReference<>() {}, pageLink);
        foundTenantQueues = pageData.getData();

        Assert.assertEquals(3, foundTenantQueues.size());

        queuesFromConfig = getQueuesFromConfig(tenantProfile2.getProfileData().getQueueConfiguration(), foundTenantQueues);
        queuesFromConfig.sort(queueComparator);
        foundTenantQueues.sort(queueComparator);

        Assert.assertEquals(queuesFromConfig, foundTenantQueues);

        loginSysAdmin();

        tenant.setTenantProfileId(null);
        doPost("/api/tenant", tenant, Tenant.class);

        login(username, password);
        for (Queue queue : foundTenantQueues) {
            doGet("/api/queues/" + queue.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(statusReason(containsString(msgErrorNoFound("Queue", queue.getId().toString()))));
        }

        loginSysAdmin();
        doDelete("/api/tenant/" + tenant.getId().getId().toString()).andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateTenantProfileToIsolated` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateTenantProfileToIsolated() throws Exception {
        loginSysAdmin();
        doPost("/api/queues?serviceType=TB_RULE_ENGINE", new Queue(TenantId.SYS_TENANT_ID, getQueueConfig(DataConstants.HP_QUEUE_NAME, DataConstants.HP_QUEUE_TOPIC))).andExpect(status().isOk());
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test profile");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setIsolatedTbRuleEngine(false);
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        createDifferentTenant();
        loginSysAdmin();
        savedDifferentTenant.setTenantProfileId(tenantProfile.getId());
        savedDifferentTenant = doPost("/api/tenant", savedDifferentTenant, Tenant.class);
        TenantId tenantId = differentTenantId;

        loginDifferentTenant();
        DeviceProfile hpQueueProfile = createDeviceProfile("HighPriority profile");
        hpQueueProfile.setDefaultQueueName(DataConstants.HP_QUEUE_NAME);
        hpQueueProfile = doPost("/api/deviceProfile", hpQueueProfile, DeviceProfile.class);
        Device hpQueueDevice = createDevice("HP", hpQueueProfile.getName(), "HP");

        DeviceProfile mainQueueProfile = createDeviceProfile("Main profile");
        mainQueueProfile.setDefaultQueueName(MAIN_QUEUE_NAME);
        mainQueueProfile = doPost("/api/deviceProfile", mainQueueProfile, DeviceProfile.class);
        Device mainQueueDevice = createDevice("Main", mainQueueProfile.getName(), "Main");

        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, tenantId, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(DataConstants.HP_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(TenantId.SYS_TENANT_ID);
        });
        verifyUsedQueueAndMessage(MAIN_QUEUE_NAME, tenantId, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(MAIN_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(TenantId.SYS_TENANT_ID);
        });

        loginSysAdmin();
        tenantProfile.setIsolatedTbRuleEngine(true);
        tenantProfile.getProfileData().setQueueConfiguration(List.of(
                getQueueConfig(MAIN_QUEUE_NAME, MAIN_QUEUE_TOPIC)
        ));
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        loginDifferentTenant();
        verifyUsedQueueAndMessage(MAIN_QUEUE_NAME, tenantId, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(MAIN_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, tenantId, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(DataConstants.HP_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(TenantId.SYS_TENANT_ID);
        });

        loginSysAdmin();
        tenantProfile.setIsolatedTbRuleEngine(true);
        tenantProfile.getProfileData().setQueueConfiguration(List.of(
                getQueueConfig(MAIN_QUEUE_NAME, MAIN_QUEUE_TOPIC),
                getQueueConfig(DataConstants.HP_QUEUE_NAME, DataConstants.HP_QUEUE_TOPIC)
        ));
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);

        loginDifferentTenant();
        verifyUsedQueueAndMessage(DataConstants.HP_QUEUE_NAME, tenantId, hpQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + hpQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(DataConstants.HP_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
        verifyUsedQueueAndMessage(MAIN_QUEUE_NAME, tenantId, mainQueueDevice.getId(), DataConstants.ATTRIBUTES_UPDATED, () -> {
            doPost("/api/plugins/telemetry/DEVICE/" + mainQueueDevice.getId() + "/attributes/SERVER_SCOPE", "{\"test\":123}", String.class);
        }, usedTpi -> {
            assertThat(usedTpi.getTopic()).isEqualTo(MAIN_QUEUE_TOPIC);
            assertThat(usedTpi.getTenantId()).get().isEqualTo(tenantId);
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testIsolatedQueueDeletion` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testIsolatedQueueDeletion() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test profile");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile, MAIN_QUEUE_NAME);
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        createDifferentTenant();
        loginSysAdmin();
        savedDifferentTenant.setTenantProfileId(tenantProfile.getId());
        savedDifferentTenant = doPost("/api/tenant", savedDifferentTenant, Tenant.class);
        TenantId tenantId = differentTenantId;
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> {
                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, tenantId);
                    return !tpi.getTenantId().get().isSysTenantId();
                });
        TopicPartitionInfo tpi = new TopicPartitionInfo(MAIN_QUEUE_TOPIC, tenantId, 0, false);
        String isolatedTopic = tpi.getFullTopicName();
        TbMsg expectedMsg = publishTbMsg(tenantId, tpi);
        awaitTbMsg(tbMsg -> tbMsg.getId().equals(expectedMsg.getId()), 10000); // to wait for consumer start

        loginSysAdmin();
        tenantProfile.setIsolatedTbRuleEngine(false);
        tenantProfile.getProfileData().setQueueConfiguration(Collections.emptyList());
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> partitionService.resolve(ServiceType.TB_RULE_ENGINE, MAIN_QUEUE_NAME, tenantId, tenantId)
                        .getTenantId().get().isSysTenantId());

        List<UUID> submittedMsgs = new ArrayList<>();
        long timeLeft = TimeUnit.SECONDS.toMillis(7); // based on topic-deletion-delay
        int msgs = 100;
        for (int i = 1; i <= msgs; i++) {
            TbMsg tbMsg = publishTbMsg(tenantId, tpi);
            submittedMsgs.add(tbMsg.getId());
            Thread.sleep(timeLeft / msgs);
        }
        await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(queueAdmin, times(1)).deleteTopic(eq(isolatedTopic));
        });

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            for (UUID msgId : submittedMsgs) {
                verify(actorContext).tell(argThat(msg -> {
                    return msg instanceof QueueToRuleEngineMsg && ((QueueToRuleEngineMsg) msg).getMsg().getId().equals(msgId);
                }));
            }
        });
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `whenTenantIsDeleted_thenDeleteQueues` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void whenTenantIsDeleted_thenDeleteQueues() throws Exception {
        loginSysAdmin();
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test profile");
        TenantProfileData tenantProfileData = new TenantProfileData();
        tenantProfileData.setConfiguration(new DefaultTenantProfileConfiguration());
        tenantProfile.setProfileData(tenantProfileData);
        tenantProfile.setIsolatedTbRuleEngine(true);
        addQueueConfig(tenantProfile, MAIN_QUEUE_NAME);
        tenantProfile = doPost("/api/tenantProfile", tenantProfile, TenantProfile.class);
        createDifferentTenant();
        loginSysAdmin();
        savedDifferentTenant.setTenantProfileId(tenantProfile.getId());
        savedDifferentTenant = doPost("/api/tenant", savedDifferentTenant, Tenant.class);
        TenantId tenantId = differentTenantId;
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(partitionService.getMyPartitions(new QueueKey(ServiceType.TB_RULE_ENGINE, tenantId))).isNotNull();
        });
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, tenantId);
        assertThat(tpi.getTenantId()).hasValue(tenantId);
        TbMsg tbMsg = publishTbMsg(tenantId, tpi);
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(actorContext).tell(argThat(msg -> {
                return msg instanceof QueueToRuleEngineMsg && ((QueueToRuleEngineMsg) msg).getMsg().getId().equals(tbMsg.getId());
            }));
        });

        deleteDifferentTenant();

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(partitionService.getMyPartitions(new QueueKey(ServiceType.TB_RULE_ENGINE, tenantId))).isNull();
            assertThatThrownBy(() -> partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, tenantId))
                    .isInstanceOf(TenantNotFoundException.class);

            verify(queueAdmin).deleteTopic(eq(tpi.getFullTopicName()));
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `publishTbMsg` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TbMsg publishTbMsg(TenantId tenantId, TopicPartitionInfo tpi) {
        TbMsg tbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, tenantId, TbMsgMetaData.EMPTY, "{\"test\":1}");
        TransportProtos.ToRuleEngineMsg msg = TransportProtos.ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        tbClusterService.pushMsgToRuleEngine(tpi, tbMsg.getId(), msg, null);
        return tbMsg;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `verifyUsedQueueAndMessage` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void verifyUsedQueueAndMessage(String queue, TenantId tenantId, EntityId entityId, String msgType, Runnable action, Consumer<TopicPartitionInfo> tpiAssert) {
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, queue, tenantId, entityId);
                    tpiAssert.accept(tpi);
                });
        action.run();
        TbMsg tbMsg = awaitTbMsg(msg -> msg.getOriginator().equals(entityId)
                && msg.getType().equals(msgType), 10000);
        assertThat(tbMsg.getQueueName()).isEqualTo(queue);

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, queue, tenantId, entityId);
        tpiAssert.accept(tpi);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `awaitTbMsg` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected TbMsg awaitTbMsg(Predicate<TbMsg> predicate, int timeoutMillis) {
        AtomicReference<TbMsg> tbMsgCaptor = new AtomicReference<>();
        verify(actorContext, timeout(timeoutMillis).atLeastOnce()).tell(argThat(actorMsg -> {
            if (!(actorMsg instanceof QueueToRuleEngineMsg)) {
                return false;
            }
            TbMsg tbMsg = ((QueueToRuleEngineMsg) actorMsg).getMsg();
            if (predicate.test(tbMsg)) {
                tbMsgCaptor.set(tbMsg);
                return true;
            }
            return false;
        }));
        return tbMsgCaptor.get();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addQueueConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addQueueConfig(TenantProfile tenantProfile, String queueName) {
        TenantProfileQueueConfiguration queueConfiguration = getQueueConfig(queueName, "tb_rule_engine." + queueName.toLowerCase());
        TenantProfileData profileData = tenantProfile.getProfileData();

        List<TenantProfileQueueConfiguration> configs = profileData.getQueueConfiguration();
        if (configs == null) {
            configs = new ArrayList<>();
        }
        configs.add(queueConfiguration);
        profileData.setQueueConfiguration(configs);
        tenantProfile.setProfileData(profileData);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getQueueConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private TenantProfileQueueConfiguration getQueueConfig(String queueName, String topic) {
        TenantProfileQueueConfiguration queueConfiguration = new TenantProfileQueueConfiguration();
        queueConfiguration.setName(queueName);
        queueConfiguration.setTopic(topic);
        queueConfiguration.setPollInterval(25);
        queueConfiguration.setPartitions(12);
        queueConfiguration.setConsumerPerPartition(true);
        queueConfiguration.setPackProcessingTimeout(2000);
        SubmitStrategy submitStrategy = new SubmitStrategy();
        submitStrategy.setType(SubmitStrategyType.BURST);
        submitStrategy.setBatchSize(1000);
        queueConfiguration.setSubmitStrategy(submitStrategy);
        ProcessingStrategy processingStrategy = new ProcessingStrategy();
        processingStrategy.setType(ProcessingStrategyType.SKIP_ALL_FAILURES);
        processingStrategy.setRetries(3);
        processingStrategy.setFailurePercentage(0);
        processingStrategy.setPauseBetweenRetries(3);
        processingStrategy.setMaxPauseBetweenRetries(3);
        queueConfiguration.setProcessingStrategy(processingStrategy);
        return queueConfiguration;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getQueuesFromConfig` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<Queue> getQueuesFromConfig(List<TenantProfileQueueConfiguration> queueConfiguration, List<Queue> queues) {
        List<Queue> result = new ArrayList<>();
        Map<String, Queue> queueMap = new HashMap<>();
        for (Queue queue : queues) {
            queueMap.put(queue.getName(), queue);
        }

        for (TenantProfileQueueConfiguration config : queueConfiguration) {
            Queue queue = queueMap.get(config.getName());
            if (queue != null) {
                Queue expectedQueue = new Queue(queue.getTenantId(), config);
                expectedQueue.setId(queue.getId());
                expectedQueue.setCreatedTime(queue.getCreatedTime());
                result.add(queue);
            }
        }
        return result;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testBroadcastEntityStateChangeEventTimeManyTimeTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testBroadcastEntityStateChangeEventTimeManyTimeTenant(Tenant tenant, ComponentLifecycleEvent event, int cntTime) {
        ArgumentMatcher<Tenant> matcherTenant = cntTime == 1 ? argument -> argument.equals(tenant) :
                argument -> argument.getClass().equals(Tenant.class);
        if (ComponentLifecycleEvent.DELETED.equals(event)) {
            Mockito.verify(tbClusterService, times(cntTime)).onTenantDelete(Mockito.argThat(matcherTenant),
                    Mockito.isNull());
        } else {
            Mockito.verify(tbClusterService, times(cntTime)).onTenantChange(Mockito.argThat(matcherTenant),
                    Mockito.isNull());
        }
        TenantId tenantId = cntTime == 1 ? tenant.getId() : (TenantId) createEntityId_NULL_UUID(tenant);
        testBroadcastEntityStateChangeEventTime(tenantId, tenantId, cntTime);
        Mockito.reset(tbClusterService);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `testBroadcastEntityStateChangeEventNeverTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void testBroadcastEntityStateChangeEventNeverTenant() {
        Mockito.verify(tbClusterService, never()).onTenantChange(Mockito.any(Tenant.class),
                Mockito.isNull());
        testBroadcastEntityStateChangeEventNever(createEntityId_NULL_UUID(new Tenant()));
        Mockito.reset(tbClusterService);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TenantControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
