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
import org.awaitility.Awaitility;
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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ContextConfiguration(classes = {AlarmControllerTest.Config.class})
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`AlarmControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AlarmControllerTest extends AbstractControllerTest {

    /**
     * 字段说明：
     * 1. 保存 `TEST_ALARM_TYPE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    public static final String TEST_ALARM_TYPE = "Test";

    /**
     * 字段说明：
     * 1. 保存 `customerDevice` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected Device customerDevice;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `alarmDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private AlarmDao alarmDao;

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
         * 1. 职责：执行 `alarmDao` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public AlarmDao alarmDao(AlarmDao alarmDao) {
            // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
            return Mockito.mock(AlarmDao.class, AdditionalAnswers.delegatesTo(alarmDao));
        }
    }

    @Before
    /**
     * 方法说明：
     * 1. 职责：执行 `setup` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void setup() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("Test device");
        device.setLabel("Label");
        device.setType("Type");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        resetTokens();
    }

    @After
    /**
     * 方法说明：
     * 1. 职责：执行 `teardown` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void teardown() throws Exception {
        loginSysAdmin();

        deleteDifferentTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateAlarmViaCustomer() throws Exception {
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ADDED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateAlarmViaTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateAlarmViaTenant() throws Exception {
        loginTenantAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        testNotifyEntityOneTimeMsgToEdgeServiceNever(alarm, alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ADDED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, updatedAlarm.getId(), updatedAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.UPDATED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateAlarmViaTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        Alarm updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(AlarmSeverity.MAJOR, updatedAlarm.getSeverity());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);
        testNotifyEntityOneTimeMsgToEdgeServiceNever(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.UPDATED);

        alarm = updatedAlarm;
        alarm.setAcknowledged(true);
        alarm.setAckTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isAcknowledged());
        Assert.assertEquals(alarm.getAckTs(), updatedAlarm.getAckTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ACK, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setCleared(true);
        alarm.setClearTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertTrue(updatedAlarm.isCleared());
        Assert.assertEquals(alarm.getClearTs(), updatedAlarm.getClearTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setAssigneeId(tenantAdminUserId);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertEquals(tenantAdminUserId, updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        alarm = updatedAlarm;
        alarm.setAssigneeId(null);
        alarm.setAssignTs(System.currentTimeMillis() - 1000);
        updatedAlarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(updatedAlarm);
        Assert.assertNull(updatedAlarm.getAssigneeId());
        Assert.assertEquals(alarm.getAssignTs(), updatedAlarm.getAssignTs());

        foundAlarm = doGet("/api/alarm/info/" + updatedAlarm.getId(), AlarmInfo.class);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundAlarm, customerDevice, tenantId,
                customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_UNASSIGNED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateAlarmViaDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        alarm.setSeverity(AlarmSeverity.MAJOR);
        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateAlarmViaDifferentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();
        alarm.setSeverity(AlarmSeverity.MAJOR);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm", alarm)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityAllOneTime(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_DELETE);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmViaTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId()).andExpect(status().isOk());

        testNotifyEntityAllOneTime(new Alarm(alarm), alarm.getId(), alarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_DELETE);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmViaDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmViaDifferentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/alarm/" + alarm.getId())
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClearAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_CLEAR);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmViaTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClearAlarmViaTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear").andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.CLEARED_UNACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_CLEAR);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAcknowledgeAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAcknowledgeAlarmViaCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack").andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(AlarmStatus.ACTIVE_ACK, foundAlarm.getStatus());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_ACK);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmViaDifferentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClearAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testClearAlarmViaDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testClearAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/clear")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAcknowledgeAlarmViaDifferentCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAcknowledgeAlarmViaDifferentCustomer() throws Exception {
        loginCustomerUser();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentCustomer();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(alarm.getId(), alarm);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAcknowledgeAlarmViaDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAcknowledgeAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/ack")
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testAssignAlarmViaDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testAssignAlarmViaDifferentTenant() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isForbidden());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testReassignAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testReassignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        logout();

        loginCustomerUser();
        Mockito.reset(tbClusterService, auditLogService);
        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + customerUserId.getId()).andExpect(status().isOk());

        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(customerUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_ASSIGNED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUnassignAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUnassignAlarm() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);
        doDelete("/api/alarm/" + alarm.getId() + "/assign").andExpect(status().isOk());
        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUnassignTenantAlarmViaCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUnassignTenantAlarmViaCustomer() throws Exception {
        loginTenantAdmin();
        Alarm alarm = createAlarm(TEST_ALARM_TYPE);
        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doPost("/api/alarm/" + alarm.getId() + "/assign/" + tenantAdminUserId.getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(tenantAdminUserId, foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, tenantAdminUserId, TENANT_ADMIN_EMAIL, ActionType.ALARM_ASSIGNED);

        logout();
        loginCustomerUser();

        Mockito.reset(tbClusterService, auditLogService);
        beforeAssignmentTs = System.currentTimeMillis();
        Thread.sleep(2);

        doDelete("/api/alarm/" + alarm.getId() + "/assign").andExpect(status().isOk());
        foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() > beforeAssignmentTs && foundAlarm.getAssignTs() < System.currentTimeMillis());

        testNotifyEntityAllOneTime(foundAlarm, foundAlarm.getId(), foundAlarm.getOriginator(),
                tenantId, customerId, customerUserId, CUSTOMER_USER_EMAIL, ActionType.ALARM_UNASSIGNED);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUnassignTenantUserAlarmOnUserRemoving` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUnassignTenantUserAlarmOnUserRemoving() throws Exception {
        loginDifferentTenant();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenantForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Device device = createDevice("Different tenant device", "default", "differentTenantTest");

        Alarm alarm = Alarm.builder()
                .type(TEST_ALARM_TYPE)
                .tenantId(savedDifferentTenant.getId())
                .originator(device.getId())
                .severity(AlarmSeverity.MAJOR)
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        alarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();

        long beforeAssignmentTs = System.currentTimeMillis();

        doPost("/api/alarm/" + alarmId.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        Alarm foundAlarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);

        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);

        beforeAssignmentTs = System.currentTimeMillis();

        loginSysAdmin();

        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        loginDifferentTenant();

        foundAlarm = Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class), Objects::nonNull);

        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUnassignAlarmOnUserRemoving` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUnassignAlarmOnUserRemoving() throws Exception {
        loginDifferentTenant();

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail("tenantForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Device device = createDevice("Different tenant device", "default", "differentTenantTest");

        Alarm alarm = Alarm.builder()
                .type(TEST_ALARM_TYPE)
                .tenantId(savedDifferentTenant.getId())
                .originator(device.getId())
                .severity(AlarmSeverity.MAJOR)
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();
        alarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(alarm);
        long beforeAssignmentTs = System.currentTimeMillis();

        doPost("/api/alarm/" + alarmId.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);

        beforeAssignmentTs = System.currentTimeMillis();

        doDelete("/api/user/" + savedUser.getId().getId()).andExpect(status().isOk());

        foundAlarm = Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class), Objects::nonNull);

        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUnassignAlarmOnCustomerRemoving` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUnassignAlarmOnCustomerRemoving() throws Exception {
        createDifferentTenantCustomer();
        loginDifferentTenant();

        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(tenantId);
        user.setCustomerId(differentTenantCustomerId);
        user.setEmail("customerForAssign@thingsboard.org");
        User savedUser = createUser(user, "password");

        Device device = createDevice("Different customer device", "default", "differentTenantTest");

        Device assignedDevice = doPost("/api/customer/" + differentTenantCustomerId.getId()
                + "/device/" + device.getId().getId(), Device.class);
        Assert.assertEquals(differentTenantCustomerId, assignedDevice.getCustomerId());

        Alarm alarm = Alarm.builder()
                .type(TEST_ALARM_TYPE)
                .tenantId(savedDifferentTenant.getId())
                .customerId(differentTenantCustomerId)
                .originator(device.getId())
                .severity(AlarmSeverity.MAJOR)
                .build();
        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);
        AlarmId alarmId = alarm.getId();

        alarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(alarm);

        Mockito.reset(tbClusterService, auditLogService);
        long beforeAssignmentTs = System.currentTimeMillis();

        doPost("/api/alarm/" + alarmId.getId() + "/assign/" + savedUser.getId().getId()).andExpect(status().isOk());
        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(savedUser.getId(), foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);

        beforeAssignmentTs = System.currentTimeMillis();

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/customer/" + differentTenantCustomerId.getId()).andExpect(status().isOk());

        foundAlarm = Awaitility.await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doGet("/api/alarm/info/" + alarmId.getId(), AlarmInfo.class), Objects::nonNull);

        Assert.assertNotNull(foundAlarm);
        Assert.assertNull(foundAlarm.getAssigneeId());
        Assert.assertTrue(foundAlarm.getAssignTs() >= beforeAssignmentTs);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAlarmsViaCustomerUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAlarmsViaCustomerUser() throws Exception {
        loginCustomerUser();

        List<Alarm> createdAlarms = new LinkedList<>();

        final int size = 10;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < size; i++) {
            createdAlarms.add(
                    createAlarm(TEST_ALARM_TYPE + i)
            );
        }

        var response = doGetTyped(
                "/api/alarm/" + EntityType.DEVICE + "/"
                        + customerDevice.getUuidId() + "?page=0&pageSize=" + size,
                new TypeReference<PageData<AlarmInfo>>() {
                }
        );
        var foundAlarmInfos = response.getData();
        Assert.assertNotNull("Found pageData is null", foundAlarmInfos);
        Assert.assertNotEquals(
                "Expected alarms are not found!",
                0, foundAlarmInfos.size()
        );

        boolean allMatch = createdAlarms.stream()
                .allMatch(alarm -> foundAlarmInfos.stream()
                        .map(Alarm::getType)
                        .anyMatch(type -> alarm.getType().equals(type))
                );
        Assert.assertTrue("Created alarm doesn't match any found!", allMatch);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAlarmsViaDifferentCustomerUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAlarmsViaDifferentCustomerUser() throws Exception {
        loginCustomerUser();

        final int size = 10;
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < size; i++) {
            createAlarm(TEST_ALARM_TYPE + i);
        }

        loginDifferentCustomer();
        doGet("/api/alarm/" + EntityType.DEVICE + "/"
                + customerDevice.getUuidId() + "?page=0&pageSize=" + size)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAlarmsViaPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAlarmsViaPublicCustomer() throws Exception {
        loginTenantAdmin();

        Device device = new Device();
        device.setName("Test Public Device");
        device.setLabel("Label");
        device.setCustomerId(customerId);
        device = doPost("/api/device", device, Device.class);
        device = doPost("/api/customer/public/device/" + device.getUuidId(), Device.class);

        String publicId = device.getCustomerId().toString();

        Alarm alarm = Alarm.builder()
                .originator(device.getId())
                .severity(AlarmSeverity.CRITICAL)
                .type("Test")
                .build();

        Mockito.reset(tbClusterService, auditLogService);

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull("Saved alarm is null!", alarm);

        testNotifyEntityNever(alarm.getId(), alarm);

        resetTokens();

        JsonNode publicLoginRequest = JacksonUtil.toJsonNode("{\"publicId\": \"" + publicId + "\"}");
        JsonNode tokens = doPost("/api/auth/login/public", publicLoginRequest, JsonNode.class);
        this.token = tokens.get("token").asText();

        PageData<AlarmInfo> pageData = doGetTyped(
                "/api/alarm/DEVICE/" + device.getUuidId() + "?page=0&pageSize=1", new TypeReference<PageData<AlarmInfo>>() {
                }
        );

        Assert.assertNotNull("Found pageData is null", pageData);
        Assert.assertNotEquals("Expected alarms are not found!", 0, pageData.getTotalElements());

        AlarmInfo alarmInfo = pageData.getData().get(0);
        boolean equals = alarm.getId().equals(alarmInfo.getId()) && alarm.getType().equals(alarmInfo.getType());
        Assert.assertTrue("Created alarm doesn't match the found one!", equals);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmWithDeleteRelationsOk` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmWithDeleteRelationsOk() throws Exception {
        loginCustomerUser();
        AlarmId alarmId = createAlarm("Alarm for Test WithRelationsOk").getId();
        testEntityDaoWithRelationsOk(customerDevice.getId(), alarmId, "/api/alarm/" + alarmId);
    }

    @Ignore
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmExceptionWithRelationsTransactional` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmExceptionWithRelationsTransactional() throws Exception {
        loginCustomerUser();
        AlarmId alarmId = createAlarm("Alarm for Test WithRelations Transactional Exception").getId();
        testEntityDaoWithRelationsTransactionalException(alarmDao, customerDevice.getId(), alarmId, "/api/alarm/" + alarmId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private AlarmInfo createAlarm(String type) throws Exception {
        return createAlarm(type, customerDevice.getId(), customerId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private AlarmInfo createAlarm(String type, EntityId originatorId, CustomerId customerId) throws Exception {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(originatorId)
                .severity(AlarmSeverity.CRITICAL)
                .type(type)
                .build();

        alarm = doPost("/api/alarm", alarm, Alarm.class);
        Assert.assertNotNull(alarm);

        AlarmInfo foundAlarm = doGet("/api/alarm/info/" + alarm.getId(), AlarmInfo.class);
        Assert.assertNotNull(foundAlarm);
        Assert.assertEquals(alarm, new Alarm(foundAlarm));

        return foundAlarm;
    }


    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateAlarmWithOtherTenantsAssignee` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateAlarmWithOtherTenantsAssignee() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .assigneeId(savedDifferentTenantUser.getId())
                .build();

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateAlarmWithOtherCustomerAsAssignee` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateAlarmWithOtherCustomerAsAssignee() throws Exception {
        loginDifferentCustomer();
        loginCustomerUser();

        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(customerDevice.getId())
                .severity(AlarmSeverity.CRITICAL)
                .assigneeId(differentCustomerUser.getId())
                .build();

        doPost("/api/alarm", alarm).andExpect(status().isForbidden());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAlarmTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAlarmTypes() throws Exception {
        loginTenantAdmin();

        List<String> types = new ArrayList<>();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i < 13; i++) {
            types.add(createAlarm(TEST_ALARM_TYPE + i).getType());
        }

        Device device = new Device();
        device.setName("Test device 2");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i < 10; i++) {
            createAlarm(TEST_ALARM_TYPE + i);
        }

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .collect(Collectors.toList());

        Collections.sort(types);
        Collections.sort(foundTypes);

        Assert.assertEquals(types, foundTypes);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmTypes() throws Exception {
        loginTenantAdmin();

        List<AlarmInfo> alarms = new ArrayList<>();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i < 13; i++) {
            alarms.add(createAlarm(TEST_ALARM_TYPE + i));
        }

        Device device = new Device();
        device.setName("Test device 2");
        device.setCustomerId(customerId);
        customerDevice = doPost("/api/device", device, Device.class);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 1; i < 14; i++) {
            alarms.add(createAlarm(TEST_ALARM_TYPE + i));
        }

        List<String> expectedTypes = alarms.stream().map(AlarmInfo::getType).distinct().sorted().collect(Collectors.toList());

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(expectedTypes, foundTypes);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 12; i++) {
            doDelete("/api/alarm/" + alarms.get(i).getId()).andExpect(status().isOk());
        }

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(expectedTypes, foundTypes);

        doDelete("/api/alarm/" + alarms.get(12).getId()).andExpect(status().isOk());

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(12, foundTypes.size());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 13; i < alarms.size(); i++) {
            doDelete("/api/alarm/" + alarms.get(i).getId()).andExpect(status().isOk());
        }

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertTrue(foundTypes.isEmpty());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteAlarmTypesAfterDeletingAlarmOriginator` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteAlarmTypesAfterDeletingAlarmOriginator() throws Exception {
        loginTenantAdmin();

        List<Device> devices = new ArrayList<>();
        List<String> types = new ArrayList<>();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 13; i++) {
            Device device = new Device();
            device.setName("Test_device_" + i);
            devices.add(device = doPost("/api/device", device, Device.class));
            types.add(createAlarm(TEST_ALARM_TYPE + i, device.getId(), null).getType());
        }

        devices.sort(Comparator.comparing(Device::getName));
        Collections.sort(types);

        List<String> foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertEquals(13, foundTypes.size());
        Assert.assertEquals(types, foundTypes);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 12; i++) {
            doDelete("/api/device/" + devices.remove(0).getId()).andExpect(status().isOk());
            types.remove(0);
        }

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .collect(Collectors.toList());

        Assert.assertEquals(types.size(), foundTypes.size());
        Assert.assertEquals(types, foundTypes);

        doDelete("/api/device/" + devices.get(0).getId()).andExpect(status().isOk());

        foundTypes = doGetTyped("/api/alarm/types?pageSize=1024&page=0", new TypeReference<PageData<EntitySubtype>>() {
        })
                .getData()
                .stream()
                .map(EntitySubtype::getType)
                .sorted()
                .collect(Collectors.toList());

        Assert.assertTrue(foundTypes.isEmpty());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
