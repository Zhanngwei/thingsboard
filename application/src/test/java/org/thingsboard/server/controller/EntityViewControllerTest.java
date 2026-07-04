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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.objects.AttributesEntityView;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@TestPropertySource(properties = {
        "transport.mqtt.enabled=true",
        "js.evaluator=mock",
})
@Slf4j
@ContextConfiguration(classes = {EntityViewControllerTest.Config.class})
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`EntityViewControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class EntityViewControllerTest extends AbstractControllerTest {
    static final TypeReference<PageData<EntityView>> PAGE_DATA_ENTITY_VIEW_TYPE_REF = new TypeReference<>() {
    };
    static final TypeReference<PageData<EntityViewInfo>> PAGE_DATA_ENTITY_VIEW_INFO_TYPE_REF = new TypeReference<>() {
    };

    /**
     * 字段说明：
     * 1. 保存 `testDevice` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private Device testDevice;
    private TelemetryEntityView telemetry;

    List<ListenableFuture<ResultActions>> deleteFutures = new ArrayList<>();
    /**
     * 字段说明：
     * 1. 保存 `executor` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    ListeningExecutorService executor;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `entityViewDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private EntityViewDao entityViewDao;

    /**
     * 中文说明：
     * 1. 类目的：`Config` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
     * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 MVC Controller / Facade。
     */
    static class Config {
        @Bean
        @Primary
        /**
         * 方法说明：
         * 1. 职责：执行 `entityViewDao` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public EntityViewDao entityViewDao(EntityViewDao entityViewDao) {
            // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
            return Mockito.mock(EntityViewDao.class, AdditionalAnswers.delegatesTo(entityViewDao));
        }
    }

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
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));

        loginTenantAdmin();

        Device device = new Device();
        device.setName("Test device 4view");
        device.setType("default");
        testDevice = doPost("/api/device", device, Device.class);

        telemetry = new TelemetryEntityView(
                List.of("tsKey1", "tsKey2", "tsKey3"),
                new AttributesEntityView(
                        List.of("caKey1", "caKey2", "caKey3", "caKey4"),
                        List.of("saKey1", "saKey2", "saKey3", "saKey4"),
                        List.of("shKey1", "shKey2", "shKey3", "shKey4")));
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
        executor.shutdownNow();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindEntityViewById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindEntityViewById() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        Assert.assertNotNull(foundView);
        assertEquals(savedView, foundView);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveEntityView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveEntityView() throws Exception {
        String name = "Test entity view";

        Mockito.reset(tbClusterService, auditLogService);

        EntityView savedView = getNewSavedEntityView(name);

        Assert.assertNotNull(savedView);
        Assert.assertNotNull(savedView.getId());
        Assert.assertTrue(savedView.getCreatedTime() > 0);
        assertEquals(tenantId, savedView.getTenantId());
        Assert.assertNotNull(savedView.getCustomerId());
        assertEquals(NULL_UUID, savedView.getCustomerId().getId());
        assertEquals(name, savedView.getName());

        EntityView foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        assertEquals(savedView, foundEntityView);

        testBroadcastEntityStateChangeEventTime(foundEntityView.getId(), tenantId, 1);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundEntityView, foundEntityView,
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        savedView.setName("New test entity view");

        doPost("/api/entityView", savedView, EntityView.class);
        foundEntityView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);

        assertEquals(savedView, foundEntityView);

        testBroadcastEntityStateChangeEventTime(foundEntityView.getId(), tenantId, 1);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundEntityView, foundEntityView,
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UPDATED, 1, 1, 5);

        doGet("/api/tenant/entityViews?entityViewName=" + name)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveEntityViewWithViolationOfValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveEntityViewWithViolationOfValidation() throws Exception {
        EntityView entityView = createEntityView(StringUtils.randomAlphabetic(300), 0, 0);

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = msgErrorFieldLength("name");
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        entityView.setName("Normal name");
        msgError = msgErrorFieldLength("type");
        entityView.setType(StringUtils.randomAlphabetic(300));
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateEntityViewFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateEntityViewFromDifferentTenant() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/entityView", savedView)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedView.getId(), savedView);

        deleteDifferentTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteEntityView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteEntityView() throws Exception {
        EntityView view = getNewSavedEntityView("Test entity view");
        Customer customer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(customer.getId());
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        Mockito.reset(tbClusterService, auditLogService);

        String entityIdStr = savedView.getId().getId().toString();
        doDelete("/api/entityView/" + entityIdStr)
                .andExpect(status().isOk());

        testNotifyEntityBroadcastEntityStateChangeEventOneTime(savedView, savedView.getId(), savedView.getId(),
                tenantId, view.getCustomerId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.DELETED, entityIdStr);

        doGet("/api/entityView/" + entityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(EntityType.ENTITY_VIEW.getNormalName(), entityIdStr))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveEntityViewWithEmptyName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveEntityViewWithEmptyName() throws Exception {
        EntityView entityView = new EntityView();
        entityView.setType("default");

        Mockito.reset(tbClusterService, auditLogService);

        String msgError = "Entity view name " + msgErrorShouldBeSpecified;
        doPost("/api/entityView", entityView)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(entityView,
                tenantId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAndUnAssignedEntityViewToCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAndUnAssignedEntityViewToCustomer() throws Exception {
        EntityView view = getNewSavedEntityView("Test entity view");
        Customer savedCustomer = doPost("/api/customer", getNewCustomer("My customer"), Customer.class);
        view.setCustomerId(savedCustomer.getId());

        Mockito.reset(tbClusterService, auditLogService);

        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        testBroadcastEntityStateChangeEventTime(savedView.getId(), tenantId, 1);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(savedView, savedView,
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UPDATED, 1, 1, 5);
        Mockito.reset(tbClusterService, auditLogService);

        EntityView assignedView = doPost(
                "/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString(),
                EntityView.class);
        assertEquals(savedCustomer.getId(), assignedView.getCustomerId());

        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(savedCustomer.getId(), foundView.getCustomerId());

        testBroadcastEntityStateChangeEventNever(foundView.getId());
        testNotifyAssignUnassignEntityAllOneTime(foundView, foundView.getId(), foundView.getId(),
                tenantId, foundView.getCustomerId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED,
                foundView.getId().getId().toString(), foundView.getCustomerId().getId().toString(), savedCustomer.getTitle());

        EntityView unAssignedView = doDelete("/api/customer/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, unAssignedView.getCustomerId().getId());

        foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, foundView.getCustomerId().getId());

        testBroadcastEntityStateChangeEventNever(foundView.getId());
        testNotifyAssignUnassignEntityAllOneTime(unAssignedView, savedView.getId(), savedView.getId(),
                tenantId, savedView.getCustomerId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED,
                assignedView.getId().getId().toString(), savedView.getCustomerId().getId().toString(), savedCustomer.getTitle());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAndUnAssignedEntityViewToPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAndUnAssignedEntityViewToPublicCustomer() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");
        Mockito.reset(tbClusterService, auditLogService);

        EntityView assignedView = doPost(
                "/api/customer/public/entityView/" + savedView.getId().getId().toString(),
                EntityView.class);
        Customer publicCustomer = doGet("/api/customer/" + assignedView.getCustomerId(), Customer.class);
        Assert.assertTrue(publicCustomer.isPublic());

        testBroadcastEntityStateChangeEventNever(assignedView.getId());
        testNotifyAssignUnassignEntityAllOneTime(assignedView, assignedView.getId(), assignedView.getId(),
                tenantId, assignedView.getCustomerId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED,
                assignedView.getId().getId().toString(), assignedView.getCustomerId().getId().toString(), publicCustomer.getTitle());

        EntityView foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(publicCustomer.getId(), foundView.getCustomerId());

        EntityView unAssignedView = doDelete("/api/customer/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, unAssignedView.getCustomerId().getId());

        foundView = doGet("/api/entityView/" + savedView.getId().getId().toString(), EntityView.class);
        assertEquals(ModelConstants.NULL_UUID, foundView.getCustomerId().getId());

        testBroadcastEntityStateChangeEventNever(foundView.getId());
        testNotifyAssignUnassignEntityAllOneTime(unAssignedView, unAssignedView.getId(), unAssignedView.getId(),
                tenantId, publicCustomer.getId(), tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED,
                unAssignedView.getId().getId().toString(), publicCustomer.getId().getId().toString(), publicCustomer.getTitle());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignEntityViewToNonExistentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignEntityViewToNonExistentCustomer() throws Exception {
        EntityView savedView = getNewSavedEntityView("Test entity view");

        Mockito.reset(tbClusterService, auditLogService);

        String customerIdStr = Uuids.timeBased().toString();
        String msgError = msgErrorNoFound("Customer", customerIdStr);
        doPost("/api/customer/" + customerIdStr + "/device/" + savedView.getId().getId().toString())
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityNever(savedView.getId(), savedView);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignEntityViewToCustomerFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignEntityViewToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = getNewTenant("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");
        createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = getNewCustomer("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(TENANT_ADMIN_EMAIL, TENANT_ADMIN_PASSWORD);

        EntityView savedView = getNewSavedEntityView("Test entity view");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString() + "/entityView/" + savedView.getId().getId().toString())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(savedView.getId(), savedView);

        loginSysAdmin();

        doDelete("/api/tenant/" + savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetCustomerEntityViews` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetCustomerEntityViews() throws Exception {
        Customer customer = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class);
        CustomerId customerId = customer.getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViewInfos?";

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 128;
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<ListenableFuture<EntityViewInfo>> viewFutures = new ArrayList<>(cntEntity);
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < cntEntity; i++) {
            String entityName = "Test entity view " + i;
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            viewFutures.add(executor.submit(() ->
                    new EntityViewInfo(doPost("/api/customer/" + customerId.getId().toString() + "/entityView/"
                            + getNewSavedEntityView(entityName).getId().getId().toString(), EntityView.class),
                            customer.getTitle(), customer.isPublic())));
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<EntityViewInfo> entityViewInfos = Futures.allAsList(viewFutures).get(TIMEOUT, SECONDS);
        List<EntityViewInfo> loadedViews = loadListOfInfo(new PageLink(23), urlTemplate);

        assertThat(entityViewInfos).containsExactlyInAnyOrderElementsOf(loadedViews);

        testNotifyEntityBroadcastEntityStateChangeEventMany(new EntityView(), new EntityView(),
                tenantId, tenantAdminCustomerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ADDED, ActionType.ADDED, cntEntity, cntEntity, cntEntity * 2, 0);

        testNotifyEntityBroadcastEntityStateChangeEventMany(new EntityView(), new EntityView(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.ASSIGNED_TO_CUSTOMER, ActionType.UPDATED, cntEntity, cntEntity,
                cntEntity*2, 3);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetCustomerEntityViewsByName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetCustomerEntityViewsByName() throws Exception {
        CustomerId customerId = doPost("/api/customer", getNewCustomer("Test customer"), Customer.class).getId();
        String urlTemplate = "/api/customer/" + customerId.getId().toString() + "/entityViews?";

        String name1 = "Entity view name1";
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<EntityView> namesOfView1 = Futures.allAsList(fillListByTemplate(125, name1, "/api/customer/" + customerId.getId().toString()
                + "/entityView/")).get(TIMEOUT, SECONDS);
        List<EntityView> loadedNamesOfView1 = loadListOf(new PageLink(15, 0, name1), urlTemplate);
        assertThat(namesOfView1).as(name1).containsExactlyInAnyOrderElementsOf(loadedNamesOfView1);

        String name2 = "Entity view name2";
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        List<EntityView> namesOfView2 = Futures.allAsList(fillListByTemplate(143, name2, "/api/customer/" + customerId.getId().toString()
                + "/entityView/")).get(TIMEOUT, SECONDS);
        List<EntityView> loadedNamesOfView2 = loadListOf(new PageLink(4, 0, name2), urlTemplate);
        assertThat(namesOfView2).as(name2).containsExactlyInAnyOrderElementsOf(loadedNamesOfView2);

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        deleteFutures.clear();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedNamesOfView1.size();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityView view : loadedNamesOfView1) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        testBroadcastEntityStateChangeEventNever(loadedNamesOfView1.get(0).getId());
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(new EntityView(), new EntityView(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL,
                ActionType.UNASSIGNED_FROM_CUSTOMER, ActionType.UPDATED, cntEntity, cntEntity, 3);

        PageData<EntityView> pageData = doGetTypedWithPageLink(urlTemplate, PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        deleteFutures.clear();
        for (EntityView view : loadedNamesOfView2) {
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/customer/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        pageData = doGetTypedWithPageLink(urlTemplate, PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetTenantEntityViews` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetTenantEntityViews() throws Exception {
        List<ListenableFuture<EntityViewInfo>> entityViewInfoFutures = new ArrayList<>(178);
        for (int i = 0; i < 178; i++) {
            ListenableFuture<EntityView> entityViewFuture = getNewSavedEntityViewAsync("Test entity view" + i);
            entityViewInfoFutures.add(Futures.transform(entityViewFuture,
                    view -> new EntityViewInfo(view, null, false),
                    MoreExecutors.directExecutor()));
        }
        List<EntityViewInfo> entityViewInfos = Futures.allAsList(entityViewInfoFutures).get(TIMEOUT, SECONDS);
        List<EntityViewInfo> loadedViews = loadListOfInfo(new PageLink(23), "/api/tenant/entityViewInfos?");
        assertThat(entityViewInfos).containsExactlyInAnyOrderElementsOf(loadedViews);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetTenantEntityViewsByName` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetTenantEntityViewsByName() throws Exception {
        String name1 = "Entity view name1";
        List<EntityView> namesOfView1 = Futures.allAsList(fillListOf(17, name1)).get(TIMEOUT, SECONDS);
        List<EntityView> loadedNamesOfView1 = loadListOf(new PageLink(5, 0, name1), "/api/tenant/entityViews?");
        assertThat(namesOfView1).as(name1).containsExactlyInAnyOrderElementsOf(loadedNamesOfView1);

        String name2 = "Entity view name2";
        List<EntityView> namesOfView2 = Futures.allAsList(fillListOf(15, name2)).get(TIMEOUT, SECONDS);
        ;
        List<EntityView> loadedNamesOfView2 = loadListOf(new PageLink(4, 0, name2), "/api/tenant/entityViews?");
        assertThat(namesOfView2).as(name2).containsExactlyInAnyOrderElementsOf(loadedNamesOfView2);

        deleteFutures.clear();
        for (EntityView view : loadedNamesOfView1) {
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        PageData<EntityView> pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name1));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());

        deleteFutures.clear();
        for (EntityView view : loadedNamesOfView2) {
            deleteFutures.add(executor.submit(() ->
                    doDelete("/api/entityView/" + view.getId().getId().toString()).andExpect(status().isOk())));
        }
        Futures.allAsList(deleteFutures).get(TIMEOUT, SECONDS);

        pageData = doGetTypedWithPageLink("/api/tenant/entityViews?", PAGE_DATA_ENTITY_VIEW_TYPE_REF,
                new PageLink(4, 0, name2));
        Assert.assertFalse(pageData.hasNext());
        assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTheCopyOfAttrsIntoTSForTheView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTheCopyOfAttrsIntoTSForTheView() throws Exception {
        Set<String> expectedActualAttributesSet = Set.of("caKey1", "caKey2", "caKey3", "caKey4");
        Set<String> actualAttributesSet =
                putAttributesAndWait("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}", expectedActualAttributesSet);
        EntityView savedView = getNewSavedEntityView("Test entity view");

        List<Map<String, Object>> values = await("telemetry/ENTITY_VIEW")
                .atMost(TIMEOUT, SECONDS)
                .until(() -> doGetAsyncTyped("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                                "/values/attributes?keys=" + String.join(",", actualAttributesSet), new TypeReference<>() {
                        }),
                        x -> x.size() >= expectedActualAttributesSet.size());

        assertEquals("value1", getValue(values, "caKey1"));
        assertEquals(true, getValue(values, "caKey2"));
        assertEquals(42.0, getValue(values, "caKey3"));
        assertEquals(73, getValue(values, "caKey4"));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testTheCopyOfAttrsOutOfTSForTheView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testTheCopyOfAttrsOutOfTSForTheView() throws Exception {
        long now = System.currentTimeMillis();
        Set<String> expectedActualAttributesSet = Set.of("caKey1", "caKey2", "caKey3", "caKey4");
        Set<String> actualAttributesSet =
                putAttributesAndWait("{\"caKey1\":\"value1\", \"caKey2\":true, \"caKey3\":42.0, \"caKey4\":73}", expectedActualAttributesSet);

        List<Map<String, Object>> values = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + testDevice.getId() +
                "/values/attributes?keys=" + String.join(",", expectedActualAttributesSet), new TypeReference<>() {
        });
        assertEquals(expectedActualAttributesSet.size(), values.size());

        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(tenantId);
        view.setName("Test entity view");
        view.setType("default");
        view.setKeys(telemetry);
        view.setStartTimeMs(now - HOURS.toMillis(1));
        view.setEndTimeMs(now - 1);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);

        values = doGetAsyncTyped("/api/plugins/telemetry/ENTITY_VIEW/" + savedView.getId().getId().toString() +
                "/values/attributes?keys=" + String.join(",", expectedActualAttributesSet), new TypeReference<>() {
        });
        assertEquals(0, values.size());
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetTelemetryWhenEntityViewTimeRangeInsideTimestampRange` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetTelemetryWhenEntityViewTimeRangeInsideTimestampRange() throws Exception {
        DeviceTypeFilter dtf = new DeviceTypeFilter(List.of(testDevice.getType()), testDevice.getName());
        List<String> tsKeys = List.of("tsKey1", "tsKey2", "tsKey3");

        DeviceCredentials deviceCredentials = doGet("/api/device/" + testDevice.getId().getId() + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());
        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        long now = System.currentTimeMillis();
        getWsClient().subscribeTsUpdate(tsKeys, now, TimeUnit.HOURS.toMillis(1), dtf);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value1\", \"tsKey2\":true, \"tsKey3\":40.0}", accessToken);
        getWsClient().waitForUpdate();

        long startTimeMs = getCurTsButNotPrevTs(now);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value2\", \"tsKey2\":false, \"tsKey3\":80.0}", accessToken);
        getWsClient().waitForUpdate();

        long middleOfTestMs = getCurTsButNotPrevTs(startTimeMs);

        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value3\", \"tsKey2\":false, \"tsKey3\":120.0}", accessToken);
        getWsClient().waitForUpdate();

        long endTimeMs = getCurTsButNotPrevTs(middleOfTestMs);
        getWsClient().registerWaitForUpdate();
        uploadTelemetry("{\"tsKey1\":\"value4\", \"tsKey2\":true, \"tsKey3\":160.0}", accessToken);
        getWsClient().waitForUpdate();

        String deviceId = testDevice.getId().getId().toString();
        Set<String> keys = getTelemetryKeys("DEVICE", deviceId);

        EntityView view = createEntityView("Test entity view", startTimeMs, endTimeMs);
        EntityView savedView = doPost("/api/entityView", view, EntityView.class);
        String entityViewId = savedView.getId().getId().toString();

        Map<String, List<Map<String, String>>> actualDeviceValues = getTelemetryValues("DEVICE", deviceId, keys, 0L, middleOfTestMs);
        Assert.assertEquals(2, actualDeviceValues.get("tsKey1").size());
        Assert.assertEquals(2, actualDeviceValues.get("tsKey2").size());
        Assert.assertEquals(2, actualDeviceValues.get("tsKey3").size());

        Map<String, List<Map<String, String>>> actualEntityViewValues = getTelemetryValues("ENTITY_VIEW", entityViewId, keys, 0L, middleOfTestMs);
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey1").size());
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey2").size());
        Assert.assertEquals(1, actualEntityViewValues.get("tsKey3").size());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getCurTsButNotPrevTs` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static long getCurTsButNotPrevTs(long prevTs) throws InterruptedException {
        long result = System.currentTimeMillis();
        if (prevTs == result) {
            Thread.sleep(1);
            return getCurTsButNotPrevTs(prevTs);
        } else {
            return result;
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `uploadTelemetry` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void uploadTelemetry(String strKvs, String accessToken) throws Exception {
        String viewDeviceId = testDevice.getId().getId().toString();

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, SECONDS.toMillis(30));
        MqttMessage message = new MqttMessage();
        message.setPayload(strKvs.getBytes());
        IMqttDeliveryToken token = client.publish("v1/devices/me/telemetry", message);
        await("mqtt ack").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS).until(() -> token.getMessage() == null);
        client.disconnect();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `awaitConnected` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void awaitConnected(MqttAsyncClient client, long ms) throws InterruptedException {
        await("awaitConnected").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS)
                .until(client::isConnected);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTelemetryKeys` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Set<String> getTelemetryKeys(String type, String id) throws Exception {
        return new HashSet<>(doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id + "/keys/timeseries", new TypeReference<>() {
        }));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getAttributeKeys` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Set<String> getAttributeKeys(String type, String id) throws Exception {
        return new HashSet<>(doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id + "/keys/attributes", new TypeReference<>() {
        }));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getTelemetryValues` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Map<String, List<Map<String, String>>> getTelemetryValues(String type, String id, Set<String> keys, Long startTs, Long endTs) throws Exception {
        return doGetAsyncTyped("/api/plugins/telemetry/" + type + "/" + id +
                "/values/timeseries?keys=" + String.join(",", keys) + "&startTs=" + startTs + "&endTs=" + endTs, new TypeReference<>() {
        });
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `putAttributesAndWait` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Set<String> putAttributesAndWait(String stringKV, Set<String> expectedKeySet) throws Exception {
        DeviceTypeFilter dtf = new DeviceTypeFilter(List.of(testDevice.getType()), testDevice.getName());
        List<EntityKey> keysToSubscribe = expectedKeySet.stream()
                .map(key -> new EntityKey(EntityKeyType.CLIENT_ATTRIBUTE, key))
                .collect(Collectors.toList());

        getWsClient().subscribeLatestUpdate(keysToSubscribe, dtf);

        String viewDeviceId = testDevice.getId().getId().toString();
        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + viewDeviceId + "/credentials", DeviceCredentials.class);
        assertEquals(testDevice.getId(), deviceCredentials.getDeviceId());

        String accessToken = deviceCredentials.getCredentialsId();
        assertNotNull(accessToken);

        String clientId = MqttAsyncClient.generateClientId();
        MqttAsyncClient client = new MqttAsyncClient("tcp://localhost:1883", clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(accessToken);
        client.connect(options);
        awaitConnected(client, SECONDS.toMillis(30));
        MqttMessage message = new MqttMessage();
        message.setPayload((stringKV).getBytes());
        getWsClient().registerWaitForUpdate();
        IMqttDeliveryToken token = client.publish("v1/devices/me/attributes", message);
        await("mqtt ack").pollInterval(5, MILLISECONDS).atMost(TIMEOUT, SECONDS).until(() -> token.getMessage() == null);
        assertThat(getWsClient().waitForUpdate()).as("ws update received").isNotBlank();
        return getAttributeKeys("DEVICE", viewDeviceId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getValue` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Object getValue(List<Map<String, Object>> values, String stringValue) {
        return values.size() == 0 ? null :
                values.stream()
                        .filter(value -> value.get("key").equals(stringValue))
                        .findFirst().get().get("value");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNewSavedEntityView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EntityView getNewSavedEntityView(String name) {
        EntityView view = createEntityView(name, 0, 0);
        return doPost("/api/entityView", view, EntityView.class);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNewSavedEntityViewAsync` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<EntityView> getNewSavedEntityViewAsync(String name) {
        return executor.submit(() -> getNewSavedEntityView(name));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createEntityView` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EntityView createEntityView(String name, long startTimeMs, long endTimeMs) {
        EntityView view = new EntityView();
        view.setEntityId(testDevice.getId());
        view.setTenantId(tenantId);
        view.setName(name);
        view.setType("default");
        view.setKeys(telemetry);
        view.setStartTimeMs(startTimeMs);
        view.setEndTimeMs(endTimeMs);
        return view;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNewCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Customer getNewCustomer(String title) {
        Customer customer = new Customer();
        customer.setTitle(title);
        return customer;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getNewTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Tenant getNewTenant(String title) {
        Tenant tenant = new Tenant();
        tenant.setTitle(title);
        return tenant;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fillListByTemplate` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<ListenableFuture<EntityView>> fillListByTemplate(int limit, String partOfName, String urlTemplate) {
        List<ListenableFuture<EntityView>> futures = new ArrayList<>(limit);
        for (ListenableFuture<EntityView> viewFuture : fillListOf(limit, partOfName)) {
            futures.add(Futures.transform(viewFuture, view ->
                            doPost(urlTemplate + view.getId().getId().toString(), EntityView.class),
                    MoreExecutors.directExecutor()));
        }
        return futures;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `fillListOf` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<ListenableFuture<EntityView>> fillListOf(int limit, String partOfName) {
        List<ListenableFuture<EntityView>> viewNameFutures = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            boolean even = i % 2 == 0;
            ListenableFuture<CustomerId> customerFuture = executor.submit(() -> {
                Customer customer = getNewCustomer("Test customer " + Math.random());
                return doPost("/api/customer", customer, Customer.class).getId();
            });

            viewNameFutures.add(Futures.transform(customerFuture, customerId -> {
                String fullName = partOfName + ' ' + StringUtils.randomAlphanumeric(15);
                fullName = even ? fullName.toLowerCase() : fullName.toUpperCase();
                EntityView view = getNewSavedEntityView(fullName);
                view.setCustomerId(customerId);
                return doPost("/api/entityView", view, EntityView.class);
            }, MoreExecutors.directExecutor()));
        }
        return viewNameFutures;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loadListOf` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<EntityView> loadListOf(PageLink pageLink, String urlTemplate) throws Exception {
        List<EntityView> loadedItems = new ArrayList<>();
        PageData<EntityView> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, PAGE_DATA_ENTITY_VIEW_TYPE_REF, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `loadListOfInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<EntityViewInfo> loadListOfInfo(PageLink pageLink, String urlTemplate) throws Exception {
        List<EntityViewInfo> loadedItems = new ArrayList<>();
        PageData<EntityViewInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink(urlTemplate, PAGE_DATA_ENTITY_VIEW_INFO_TYPE_REF, pageLink);
            loadedItems.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        return loadedItems;
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignEntityViewToEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignEntityViewToEdge() throws Exception {
        Edge edge = constructEdge("My edge", "default");
        Edge savedEdge = doPost("/api/edge", edge, Edge.class);

        EntityView savedEntityView = getNewSavedEntityView("My entityView");

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/device/" + testDevice.getId().getId().toString(), Device.class);

        doPost("/api/edge/" + savedEdge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);

        PageData<EntityView> pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/entityViews?",
                PAGE_DATA_ENTITY_VIEW_TYPE_REF, new PageLink(100));

        Assert.assertEquals(1, pageData.getData().size());

        doDelete("/api/edge/" + savedEdge.getId().getId().toString()
                + "/entityView/" + savedEntityView.getId().getId().toString(), EntityView.class);

        pageData = doGetTypedWithPageLink("/api/edge/" + savedEdge.getId().getId().toString() + "/entityViews?",
                PAGE_DATA_ENTITY_VIEW_TYPE_REF, new PageLink(100));

        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteEntityViewWithDeleteRelationsOk` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteEntityViewWithDeleteRelationsOk() throws Exception {
        EntityViewId entityViewId = getNewSavedEntityView("EntityView for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(tenantId, entityViewId, "/api/entityView/" + entityViewId);
    }

    @Ignore
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteEntityViewExceptionWithRelationsTransactional` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteEntityViewExceptionWithRelationsTransactional() throws Exception {
        EntityViewId entityViewId = getNewSavedEntityView("EntityView for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(entityViewDao, tenantId, entityViewId, "/api/entityView/" + entityViewId);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityViewControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
