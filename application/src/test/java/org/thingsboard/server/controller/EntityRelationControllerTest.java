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
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`EntityRelationControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class EntityRelationControllerTest extends AbstractControllerTest {

    /**
     * 字段说明：
     * 1. 保存 `BASE_DEVICE_NAME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String BASE_DEVICE_NAME = "Test dummy device";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `relationService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    RelationService relationService;

    /**
     * 字段说明：
     * 1. 保存 `idComparator` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private IdComparator<EntityView> idComparator;
    private Tenant savedTenant;
    /**
     * 字段说明：
     * 1. 保存 `tenantAdmin` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private User tenantAdmin;
    private Device mainDevice;

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
        idComparator = new IdComparator<>();

        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");

        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device device = new Device();
        device.setName("Main test device");
        device.setType("default");
        mainDevice = doPost("/api/device", device, Device.class);
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
     * 1. 职责：执行 `testSaveAndFindRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, foundRelation);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWithDeviceFromNotCreated` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveWithDeviceFromNotCreated() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWithDeviceToNotCreated` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveWithDeviceToNotCreated() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Parameter entityId can't be empty!")));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveWithDeviceToMissing` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveWithDeviceToMissing() throws Exception {
        Device device = new Device();
        device.setName("Test device 2");
        device.setType("default");
        device.setId(new DeviceId(UUID.randomUUID()));
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/relation", relation)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", device.getId().getId().toString()))));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByFrom() throws Exception {
        final int numOfDevices = 30;

        Mockito.reset(tbClusterService, auditLogService);

        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelation relationTest = createFromRelation(mainDevice, mainDevice, "TEST_NOTIFY_ENTITY");
        testNotifyEntityAllManyRelation(relationTest, savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_ADD_OR_UPDATE, numOfDevices);

        String url = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(url, numOfDevices);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByFromWithRelationType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByFromWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByFromWithRelationTypeOther` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByFromWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(mainDevice, device, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?fromId=%s&fromType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByToWithRelationType` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByToWithRelationType() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationType
        );

        assertFoundList(url, 1);
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationsByToWithRelationTypeOther` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationsByToWithRelationTypeOther() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        Device device = buildSimpleDevice("Unique dummy test device ");
        String relationType = "TEST";
        EntityRelation relation = createFromRelation(device, mainDevice, relationType);

        doPost("/api/relation", relation).andExpect(status().isOk());

        String relationTypeOther = "TEST_OTHER";
        String url = String.format("/api/relations?toId=%s&toType=%s&relationType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE, relationTypeOther
        );

        assertFoundList(url, 0);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsInfoByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsInfoByFrom() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByFrom(relationsInfos);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsInfoByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsInfoByTo() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);
        String url = String.format("/api/relations/info?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(url, JsonNode.class), new TypeReference<>() {
                });

        Assert.assertNotNull("Relations is not found!", relationsInfos);
        Assert.assertEquals("List of found relationsInfos is not equal to number of created relations!",
                numOfDevices, relationsInfos.size());

        assertRelationsInfosByTo(relationsInfos);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteRelation() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityAllOneTimeRelation(foundRelation,
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATION_DELETED, foundRelation);

        doGet(url).andExpect(status().is4xxClientError());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelationWithOtherFromDeviceError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteRelationWithOtherFromDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                device2.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelationWithOtherToDeviceError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteRelationWithOtherToDeviceError() throws Exception {
        Device device = buildSimpleDevice("Test device 1");

        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        Device device2 = buildSimpleDevice("Test device 2");
        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device2.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNotFound)));

        testNotifyEntityNever(mainDevice.getId(), null);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteRelations` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteRelations() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME + " from");
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME + " to");

        String urlTo = String.format("/api/relations?toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );
        String urlFrom = String.format("/api/relations?fromId=%s&fromType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        assertFoundList(urlTo, numOfDevices);
        assertFoundList(urlFrom, numOfDevices);

        String url = String.format("/api/relations?entityId=%s&entityType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE
        );

        Mockito.reset(tbClusterService, auditLogService);

        doDelete(url).andExpect(status().isOk());

        testNotifyEntityOneTimeMsgToEdgeServiceNever(null, mainDevice.getId(), mainDevice.getId(),
                savedTenant.getId(), tenantAdmin.getCustomerId(), tenantAdmin.getId(), tenantAdmin.getEmail(),
                ActionType.RELATIONS_DELETED);

        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlTo, List.class).isEmpty()
        );
        Assert.assertTrue(
                "Performed deletion of all relations but some relations were found!",
                doGet(urlFrom, List.class).isEmpty()
        );
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsByFromQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<List<EntityRelation>>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsByToQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelation> relations = readResponse(
                doPost("/api/relations", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertFoundRelations(relations, numOfDevices);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsInfoByFromQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsInfoByFromQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByFrom(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.FROM,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByFrom(relationsInfo);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindRelationsInfoByToQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindRelationsInfoByToQuery() throws Exception {
        final int numOfDevices = 30;
        createDevicesByTo(numOfDevices, BASE_DEVICE_NAME);

        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(new RelationsSearchParameters(
                mainDevice.getUuidId(), EntityType.DEVICE,
                EntitySearchDirection.TO,
                RelationTypeGroup.COMMON,
                1, true
        ));
        query.setFilters(Collections.singletonList(
                new RelationEntityTypeFilter("CONTAINS", List.of(EntityType.DEVICE))
        ));

        List<EntityRelationInfo> relationsInfo = readResponse(
                doPost("/api/relations/info", query).andExpect(status().isOk()),
                new TypeReference<>() {
                }
        );

        assertRelationsInfosByTo(relationsInfo);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateRelationFromTenantToDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateRelationFromTenantToDevice() throws Exception {
        EntityRelation relation = new EntityRelation(tenantAdmin.getTenantId(), mainDevice.getId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                tenantAdmin.getTenantId(), EntityType.TENANT,
                "CONTAINS", mainDevice.getUuidId(), EntityType.DEVICE
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateRelationFromDeviceToTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateRelationFromDeviceToTenant() throws Exception {
        EntityRelation relation = new EntityRelation(mainDevice.getId(), tenantAdmin.getTenantId(), "CONTAINS");
        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", tenantAdmin.getTenantId(), EntityType.TENANT
        );

        EntityRelation foundRelation = doGet(url, EntityRelation.class);

        Assert.assertNotNull("Relation is not found!", foundRelation);
        Assert.assertEquals("Found relation is not equals origin!", relation, foundRelation);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAndFindRelationDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAndFindRelationDifferentTenant() throws Exception {
        Device device = buildSimpleDevice("Test device 1");
        EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");

        doPost("/api/relation", relation).andExpect(status().isOk());

        String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                mainDevice.getUuidId(), EntityType.DEVICE,
                "CONTAINS", device.getUuidId(), EntityType.DEVICE
        );

        loginDifferentTenant();

        doGet(url)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("Device", relation.getFrom().getId().toString()))));

        deleteDifferentTenant();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildSimpleDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Device buildSimpleDevice(String name) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        device = doPost("/api/device", device, Device.class);
        return device;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createFromRelation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private EntityRelation createFromRelation(Device mainDevice, Device device, String relationType) {
        return new EntityRelation(mainDevice.getId(), device.getId(), relationType);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevicesByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void createDevicesByFrom(int numOfDevices, String baseName) throws Exception {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);

            EntityRelation relation = createFromRelation(mainDevice, device, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createDevicesByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void createDevicesByTo(int numOfDevices, String baseName) throws Exception {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < numOfDevices; i++) {
            Device device = buildSimpleDevice(baseName + i);
            EntityRelation relation = createFromRelation(device, mainDevice, "CONTAINS");
            doPost("/api/relation", relation).andExpect(status().isOk());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertFoundRelations` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertFoundRelations(List<EntityRelation> relations, int numOfDevices) {
        Assert.assertNotNull("Relations is not found!", relations);
        Assert.assertEquals("List of found relations is not equal to number of created relations!",
                numOfDevices, relations.size());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertFoundList` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertFoundList(String url, int numOfDevices) throws Exception {
        @SuppressWarnings("unchecked")
        List<EntityRelation> relations = doGet(url, List.class);
        assertFoundRelations(relations, numOfDevices);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertRelationsInfosByFrom` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertRelationsInfosByFrom(List<EntityRelationInfo> relationsInfos) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong FROM entityId!", mainDevice.getId(), info.getFrom());
            Assert.assertTrue("Wrong FROM name!", info.getToName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertRelationsInfosByTo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertRelationsInfosByTo(List<EntityRelationInfo> relationsInfos) {
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (EntityRelationInfo info : relationsInfos) {
            Assert.assertEquals("Wrong TO entityId!", mainDevice.getId(), info.getTo());
            Assert.assertTrue("Wrong TO name!", info.getFromName().contains(BASE_DEVICE_NAME));
            Assert.assertEquals("Wrong relationType!", "CONTAINS", info.getType());
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityRelationControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
