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
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.UserEmailInfo;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.settings.StarredDashboardInfo;
import org.thingsboard.server.common.data.settings.UserDashboardsInfo;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.dao.model.ModelConstants.SYSTEM_TENANT;

@ContextConfiguration(classes = {UserControllerTest.Config.class})
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`UserControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class UserControllerTest extends AbstractControllerTest {

    private IdComparator<User> idComparator = new IdComparator<>();
    private IdComparator<UserEmailInfo> userDataIdComparator = new IdComparator<>();

    private EntityIdComparator<UserId> userIdComparator = new EntityIdComparator<>();

    private CustomerId customerNUULId = (CustomerId) createEntityId_NULL_UUID(new Customer());

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `userDao` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private UserDao userDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `deviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private DeviceService deviceService;

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
         * 1. 职责：执行 `userDao` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
         * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
         * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
         * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
         * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
         * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
         * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
         */
        public UserDao userDao(UserDao userDao) {
            // DAO 调用是数据库访问边界，事务和缓存一致性由上层服务约定控制。
            return Mockito.mock(UserDao.class, AdditionalAnswers.delegatesTo(userDao));
        }
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
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUser() throws Exception {
        loginSysAdmin();

        User user = createTenantAdminUser();
        String email = user.getEmail();
        Mockito.reset(tbClusterService, auditLogService);

        User savedUser = doPost("/api/user", user, User.class);
        Assert.assertNotNull(savedUser);
        Assert.assertNotNull(savedUser.getId());
        Assert.assertTrue(savedUser.getCreatedTime() > 0);
        Assert.assertEquals(email, savedUser.getEmail());

        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertEquals(foundUser, savedUser);

        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(foundUser, foundUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, 1, 1, 1);
        Mockito.reset(tbClusterService, auditLogService);

        resetTokens();
        doGet("/api/noauth/activate?activateToken={activateToken}", this.currentActivateToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/createPassword?activateToken=" + this.currentActivateToken));

        JsonNode activateRequest = JacksonUtil.newObjectNode()
                .put("activateToken", this.currentActivateToken)
                .put("password", "testPassword");

        JsonNode tokenInfo = readResponse(doPost("/api/noauth/activate", activateRequest).andExpect(status().isOk()), JsonNode.class);
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        resetTokens();

        login(email, "testPassword");

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);

        Mockito.reset(tbClusterService, auditLogService);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(foundUser, foundUser.getId(), foundUser.getId(),
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, ActionType.DELETED, SYSTEM_TENANT.getId().toString());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserWithViolationOfFiledValidation` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserWithViolationOfFiledValidation() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = createTenantAdminUser(StringUtils.randomAlphabetic(300), "Brown");
        String msgError = msgErrorFieldLength("first name");
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
        Mockito.reset(tbClusterService, auditLogService);

        user.setFirstName("Normal name");
        msgError = msgErrorFieldLength("last name");
        user.setLastName(StringUtils.randomAlphabetic(300));
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateUserFromDifferentTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateUserFromDifferentTenant() throws Exception {
        loginSysAdmin();

        User tenantAdmin = createTenantAdminUser();
        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        loginDifferentTenant();

        Mockito.reset(tbClusterService, auditLogService);

        doPost("/api/user", tenantAdmin)
                .andExpect(status().isForbidden())
                .andExpect(statusReason(containsString(msgErrorPermission)));

        testNotifyEntityNever(tenantAdmin.getId(), tenantAdmin);

        deleteDifferentTenant();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testResetPassword` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testResetPassword() throws Exception {
        loginSysAdmin();

        User user = createTenantAdminUser();
        String email = user.getEmail();
        User savedUser = createUserAndLogin(user, "testPassword1");
        resetTokens();

        JsonNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode()
                .put("email", email);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", this.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + this.currentResetPasswordToken));

        JsonNode resetPasswordRequest = JacksonUtil.newObjectNode()
                .put("resetToken", this.currentResetPasswordToken)
                .put("password", "testPassword2");

        Mockito.doNothing().when(mailService).sendPasswordWasResetEmail(anyString(), anyString());
        JsonNode tokenInfo = readResponse(
                doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isOk()), JsonNode.class);
        Mockito.verify(mailService).sendPasswordWasResetEmail(anyString(), anyString());
        validateAndSetJwtToken(tokenInfo, email);

        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        resetTokens();

        login(email, "testPassword2");
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority", is(Authority.TENANT_ADMIN.name())))
                .andExpect(jsonPath("$.email", is(email)));

        loginSysAdmin();
        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindUserById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindUserById() throws Exception {
        loginSysAdmin();

        User user = createTenantAdminUser();

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(savedUser, foundUser);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserWithSameEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserWithSameEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setEmail(TENANT_ADMIN_EMAIL);

        String msgError = "User with email '" + TENANT_ADMIN_EMAIL + "'  already present in database";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserWithInvalidEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserWithInvalidEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        String email = "tenant_thingsboard.org";
        User user = createTenantAdminUser();
        user.setEmail(email);

        String msgError = "Invalid email address format '" + email + "'";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserWithEmptyEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserWithEmptyEmail() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setTenantId(tenantId);
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "User email " + msgErrorShouldBeSpecified;
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("User email " + msgErrorShouldBeSpecified)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserWithoutTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserWithoutTenant() throws Exception {
        loginSysAdmin();

        Mockito.reset(tbClusterService, auditLogService);

        User user = new User();
        user.setAuthority(Authority.TENANT_ADMIN);
        user.setEmail("tenant2@thingsboard.org");
        user.setFirstName("Joe");
        user.setLastName("Downs");

        String msgError = "Tenant administrator should be assigned to tenant";
        doPost("/api/user", user)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(msgError)));

        testNotifyEntityEqualsOneTimeServiceNeverError(user,
                SYSTEM_TENANT, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, new DataValidationException(msgError));

    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteUser() throws Exception {
        loginSysAdmin();

        User user = createTenantAdminUser();

        User savedUser = doPost("/api/user", user, User.class);
        User foundUser = doGet("/api/user/" + savedUser.getId().getId().toString(), User.class);
        Assert.assertNotNull(foundUser);

        doDelete("/api/user/" + savedUser.getId().getId().toString())
                .andExpect(status().isOk());

        String userIdStr = savedUser.getId().getId().toString();
        doGet("/api/user/" + userIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound("User", userIdStr))));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantAdmins` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantAdmins() throws Exception {
        loginSysAdmin();

        //here created a new tenant despite already created on AbstractWebTest and then delete the tenant properly on the last line
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant with many admins");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        TenantId tenantId = savedTenant.getId();

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = 64;
        List<User> tenantAdmins = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < cntEntity; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            user.setEmail("testTenant" + i + "@thingsboard.org");
            tenantAdmins.add(doPost("/api/user", user, User.class));
        }

        User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.ADDED, cntEntity, cntEntity, cntEntity);

        List<User> loadedTenantAdmins = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdmins.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdmins, idComparator);
        Collections.sort(loadedTenantAdmins, idComparator);

        assertThat(tenantAdmins).as("admins list size").hasSameSizeAs(loadedTenantAdmins);
        assertThat(tenantAdmins).as("admins list content").isEqualTo(loadedTenantAdmins);

        doDelete("/api/tenant/" + tenantId.getId().toString())
                .andExpect(status().isOk());

        pageLink = new PageLink(33);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindTenantAdminsByEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindTenantAdminsByEmail() throws Exception {

        loginSysAdmin();

        String email1 = "testEmail1";
        List<User> tenantAdminsEmail1 = new ArrayList<>();

        final int NUMBER_OF_USERS = 124;

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < NUMBER_OF_USERS; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email1 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail1.add(doPost("/api/user", user, User.class));
        }

        String email2 = "testEmail2";
        List<User> tenantAdminsEmail2 = new ArrayList<>();

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 112; i++) {
            User user = new User();
            user.setAuthority(Authority.TENANT_ADMIN);
            user.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
            String email = email2 + suffix + "@thingsboard.org";
            email = i % 2 == 0 ? email.toLowerCase() : email.toUpperCase();
            user.setEmail(email);
            tenantAdminsEmail2.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedTenantAdminsEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdminsEmail1.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail1, idComparator);
        Collections.sort(loadedTenantAdminsEmail1, idComparator);

        Assert.assertEquals(tenantAdminsEmail1, loadedTenantAdminsEmail1);

        List<User> loadedTenantAdminsEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedTenantAdminsEmail2.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantAdminsEmail2, idComparator);
        Collections.sort(loadedTenantAdminsEmail2, idComparator);

        Assert.assertEquals(tenantAdminsEmail2, loadedTenantAdminsEmail2);

        Mockito.reset(tbClusterService, auditLogService);

        int cntEntity = loadedTenantAdminsEmail1.size();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (User user : loadedTenantAdminsEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }
        User testManyUser = new User();
        testManyUser.setTenantId(tenantId);
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(testManyUser, testManyUser,
                SYSTEM_TENANT, customerNUULId, null, SYS_ADMIN_EMAIL,
                ActionType.DELETED, cntEntity, NUMBER_OF_USERS, cntEntity, "");

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (User user : loadedTenantAdminsEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/tenant/" + tenantId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindCustomerUsers` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindCustomerUsers() throws Exception {
        loginSysAdmin();

        User tenantAdmin = createTenantAdminUser();
        createUserAndLogin(tenantAdmin, "testPassword1");

        CustomerId customerId = postCustomer();

        List<User> customerUsers = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 56; i++) {
            User user = new User();
            user.setAuthority(Authority.CUSTOMER_USER);
            user.setCustomerId(customerId);
            user.setEmail("testCustomer" + i + "@thingsboard.org");
            customerUsers.add(doPost("/api/user", user, User.class));
        }

        List<User> loadedCustomerUsers = new ArrayList<>();
        PageLink pageLink = new PageLink(33);
        PageData<User> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsers, idComparator);
        Collections.sort(loadedCustomerUsers, idComparator);

        Assert.assertEquals(customerUsers, loadedCustomerUsers);

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindCustomerUsersByEmail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindCustomerUsersByEmail() throws Exception {
        loginSysAdmin();

        User tenantAdmin = createTenantAdminUser();
        createUserAndLogin(tenantAdmin, "testPassword1");

        CustomerId customerId = postCustomer();

        String email1 = "testEmail1";
        String email2 = "testEmail2";
        List<User> customerUsersEmail1 = new ArrayList<>();
        List<User> customerUsersEmail2 = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 45; i++) {
            User customerUser = createCustomerUser(customerId);
            customerUser.setEmail(email1 + StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10)) + "@thingsboard.org");
            customerUsersEmail1.add(doPost("/api/user", customerUser, User.class));

            customerUser.setEmail(email2 + StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10)) + "@thingsboard.org");
            customerUsersEmail2.add(doPost("/api/user", customerUser, User.class));
        }

        List<User> loadedCustomerUsersEmail1 = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0, email1);
        PageData<User> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsersEmail1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail1, idComparator);
        Collections.sort(loadedCustomerUsersEmail1, idComparator);

        Assert.assertEquals(customerUsersEmail1, loadedCustomerUsersEmail1);

        List<User> loadedCustomerUsersEmail2 = new ArrayList<>();
        pageLink = new PageLink(16, 0, email2);
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                    new TypeReference<>() {
                    }, pageLink);
            loadedCustomerUsersEmail2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(customerUsersEmail2, idComparator);
        Collections.sort(loadedCustomerUsersEmail2, idComparator);

        Assert.assertEquals(customerUsersEmail2, loadedCustomerUsersEmail2);

        for (User user : loadedCustomerUsersEmail1) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (User user : loadedCustomerUsersEmail2) {
            doDelete("/api/user/" + user.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new PageLink(4, 0, email2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/users?",
                new TypeReference<>() {
                }, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetUsersForAssign` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetUsersForAssign() throws Exception {
        loginTenantAdmin();

        String email = "testEmail1";
        List<UserId> expectedCustomerUserIds = new ArrayList<>();
        expectedCustomerUserIds.add(customerUserId);
        for (int i = 0; i < 45; i++) {
            User customerUser = createCustomerUser( customerId);
            customerUser.setEmail(email + StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10)) + "@thingsboard.org");
            User user = doPost("/api/user", customerUser, User.class);
            expectedCustomerUserIds.add(user.getId());
        }
        List<UserId> expectedTenantUserIds = new ArrayList<>(List.copyOf(expectedCustomerUserIds));
        expectedTenantUserIds.add(tenantAdminUserId);

        Device device = new Device();
        device.setName("testDevice");
        Device savedDevice = doPost("/api/device", device, Device.class);

        Alarm alarm = createTestAlarm(savedDevice);

        List<UserId> loadedTenantUserIds = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0);
        PageData<UserEmailInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/users/assign/" + alarm.getId().getId().toString() + "?",
                    new TypeReference<>() {}, pageLink);
            loadedTenantUserIds.addAll(pageData.getData().stream().map(UserEmailInfo::getId)
                    .collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(1, loadedTenantUserIds.size());
        Assert.assertEquals(tenantAdminUserId, loadedTenantUserIds.get(0));

        doDelete("/api/alarm/" + alarm.getId().getId().toString());

        savedDevice.setCustomerId(customerId);
        savedDevice = doPost("/api/customer/" + customerId.getId()
                + "/device/" + savedDevice.getId().getId(), Device.class);

        alarm = createTestAlarm(savedDevice);

        List<UserId> loadedUserIds = new ArrayList<>();
        pageLink = new PageLink(16, 0);
        do {
            pageData = doGetTypedWithPageLink("/api/users/assign/" + alarm.getId().getId().toString() + "?",
                    new TypeReference<>() {}, pageLink);
            loadedUserIds.addAll(pageData.getData().stream().map(UserEmailInfo::getId)
                    .collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        expectedTenantUserIds.sort(userIdComparator);
        loadedUserIds.sort(userIdComparator);

        Assert.assertEquals(expectedTenantUserIds, loadedUserIds);

        loginCustomerUser();

        loadedUserIds = new ArrayList<>();
        pageLink = new PageLink(16, 0);
        do {
            pageData = doGetTypedWithPageLink("/api/users/assign/" + alarm.getId().getId().toString() + "?",
                    new TypeReference<>() {}, pageLink);
            loadedUserIds.addAll(pageData.getData().stream().map(UserEmailInfo::getId)
                    .collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        expectedCustomerUserIds.sort(userIdComparator);
        loadedUserIds.sort(userIdComparator);

        Assert.assertEquals(expectedCustomerUserIds, loadedUserIds);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetUsersForDeletedAlarmOriginator` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetUsersForDeletedAlarmOriginator() throws Exception {
        loginTenantAdmin();

        String email = "testEmail1";
        for (int i = 0; i < 45; i++) {
            User customerUser = createCustomerUser( customerId);
            customerUser.setEmail(email + StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10)) + "@thingsboard.org");
            doPost("/api/user", customerUser, User.class);
        }

        Device device = new Device();
        device.setName("testDevice");
        device.setCustomerId(customerId);
        Device savedDevice = doPost("/api/device", device, Device.class);

        Alarm alarm = createTestAlarm(savedDevice);

        deviceService.deleteDevice(tenantId, savedDevice.getId());

        List<UserId> loadedUserIds = new ArrayList<>();
        PageLink pageLink = new PageLink(33, 0);
        PageData<UserEmailInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/users/assign/" + alarm.getId().getId().toString() + "?",
                    new TypeReference<>() {}, pageLink);
            loadedUserIds.addAll(pageData.getData().stream().map(UserEmailInfo::getId)
                    .collect(Collectors.toList()));
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Assert.assertEquals(1, loadedUserIds.size());
        Assert.assertEquals(tenantAdminUserId, loadedUserIds.get(0));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteUserWithDeleteRelationsOk` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteUserWithDeleteRelationsOk() throws Exception {
        loginSysAdmin();
        User tenantAdminUser = createTenantAdminUser();
        UserId userId = doPost("/api/user", tenantAdminUser, User.class).getId();
        testEntityDaoWithRelationsOk(tenantId, userId, "/api/user/" + userId);
    }

    @Ignore
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteUserExceptionWithRelationsTransactional` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteUserExceptionWithRelationsTransactional() throws Exception {
        loginSysAdmin();
        User tenantAdminUser = createTenantAdminUser("Joe", "Downs");
        UserId userId = doPost("/api/user", tenantAdminUser, User.class).getId();
        testEntityDaoWithRelationsTransactionalException(userDao, tenantId, userId, "/api/user/" + userId);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenInvalidPageLink_thenReturnError` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void givenInvalidPageLink_thenReturnError() throws Exception {
        loginTenantAdmin();

        String invalidSortProperty = "abc(abc)";

        ResultActions result = doGet("/api/users?page={page}&pageSize={pageSize}&sortProperty={sortProperty}", 0, 100, invalidSortProperty)
                .andExpect(status().isBadRequest());
        assertThat(getErrorMessage(result)).containsIgnoringCase("invalid sort property");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveUserSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":10, \"E\":18}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode retrievedSettings = doGet("/api/user/settings", JsonNode.class);
        Assert.assertEquals(retrievedSettings, userSettings);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testShouldNotSaveJsonWithRestrictedSymbols` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testShouldNotSaveJsonWithRestrictedSymbols() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = JacksonUtil.toJsonNode("{\"A.B\":5, \"E\":18}");
        doPost("/api/user/settings", userSettings).andExpect(status().isBadRequest());

        userSettings = JacksonUtil.toJsonNode("{\"A,B\":5, \"E\":18}");
        doPost("/api/user/settings", userSettings).andExpect(status().isBadRequest());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testUpdateUserSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testUpdateUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{\"C\":true, \"D\":\"stringValue\"}}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode newSettings = JacksonUtil.toJsonNode("{\"A\":10}");
        doPut("/api/user/settings", newSettings);
        JsonNode updatedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = JacksonUtil.toJsonNode("{\"A\":10, \"B\":{\"C\":true, \"D\":\"stringValue\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        JsonNode patchedSettings = JacksonUtil.toJsonNode("{\"A\":11, \"B\":{\"C\":false, \"D\":\"stringValue2\"}}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":11, \"B\":{\"C\":false, \"D\":\"stringValue2\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = JacksonUtil.toJsonNode("{\"B.D\": \"stringValue3\"}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":11, \"B\":{\"C\":false, \"D\": \"stringValue3\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = JacksonUtil.toJsonNode("{\"B.D\": {\"E\": 76, \"F\": 92}}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":11, \"B\":{\"C\":false, \"D\": {\"E\":76, \"F\": 92}}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        patchedSettings = JacksonUtil.toJsonNode("{\"B.D.E\": 100}");
        doPut("/api/user/settings", patchedSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":11, \"B\":{\"C\":false, \"D\": {\"E\":100, \"F\": 92}}}");
        Assert.assertEquals(expectedSettings, updatedSettings);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testShouldCreatePathIfNotExists` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testShouldCreatePathIfNotExists() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = JacksonUtil.toJsonNode("{\"A\":5}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        JsonNode newSettings = JacksonUtil.toJsonNode("{\"B\":{\"C\": 10}}");
        doPut("/api/user/settings", newSettings);
        JsonNode updatedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{\"C\": 10}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = JacksonUtil.toJsonNode("{\"B.K\":true}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{\"C\": 10, \"K\": true}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = JacksonUtil.toJsonNode("{\"B\":{}}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = JacksonUtil.toJsonNode("{\"F.G\":\"string\"}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{}, \"F\":{\"G\": \"string\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);

        newSettings = JacksonUtil.toJsonNode("{\"F\":{\"G\":\"string2\"}}");
        doPut("/api/user/settings", newSettings);
        updatedSettings = doGet("/api/user/settings", JsonNode.class);
        expectedSettings = JacksonUtil.toJsonNode("{\"A\":5, \"B\":{}, \"F\":{\"G\": \"string2\"}}");
        Assert.assertEquals(expectedSettings, updatedSettings);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDeleteUserSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDeleteUserSettings() throws Exception {
        loginCustomerUser();

        JsonNode userSettings = JacksonUtil.toJsonNode("{\"A\":10, \"B\":10, \"C\":{\"D\": 16}}");
        JsonNode savedSettings = doPost("/api/user/settings", userSettings, JsonNode.class);
        Assert.assertEquals(userSettings, savedSettings);

        doDelete("/api/user/settings/C.D,B");

        JsonNode retrievedSettings = doGet("/api/user/settings", JsonNode.class);
        JsonNode expectedSettings = JacksonUtil.toJsonNode("{\"A\":10, \"C\":{}}");
        Assert.assertEquals(expectedSettings, retrievedSettings);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `checkCustomerUserDoNotSeeTenantUsersOtherTenantUsersOtherCustomerUsers` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void checkCustomerUserDoNotSeeTenantUsersOtherTenantUsersOtherCustomerUsers() throws Exception {
        loginSysAdmin();
        String searchText = "Joe";

        loginDifferentTenant();
        CustomerId customerId1 = postCustomer();
        doPost("/api/user", createCustomerUser(searchText, "Ress", customerId1), User.class);

        loginSysAdmin();
        User tenantAdmin = createTenantAdminUser(searchText, "Brown");
        createUserAndLogin(tenantAdmin, "testPassword1");

        CustomerId customerId2 = postCustomer();
        User user = createCustomerUser(searchText, "Downs", customerId2);
        doPost("/api/user", user, User.class);

        CustomerId customerId3 = postCustomer();
        User user2 = createCustomerUser(customerId3);
        createUserAndLogin(user2, "testPassword2");

        PageLink pageLink = new PageLink(10, 0, searchText);
        List<UserEmailInfo> usersInfo = getUsersInfo(pageLink);

        Assert.assertEquals(usersInfo.size(), 0);

        //clear users
        loginDifferentTenant();
        doDelete("/api/customer/" + customerId1.getId().toString())
                .andExpect(status().isOk());
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/customer/" + customerId2.getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + customerId3.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `shouldFindCustomerUsersBySearchText` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shouldFindCustomerUsersBySearchText() throws Exception {
        loginSysAdmin();
        User tenantAdmin = createTenantAdminUser();
        createUserAndLogin(tenantAdmin, "testPassword1");

        String searchText = "Philip";

        CustomerId customerId = postCustomer();
        CustomerId customerId2 = postCustomer();

        List<User> customerUsersContainingWord = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String suffix = StringUtils.randomAlphabetic((int) (5 + Math.random() * 10));

            customerUsersContainingWord.add(doPost("/api/user", createCustomerUser(searchText + i, "Last" + i, customerId), User.class));
            customerUsersContainingWord.add(doPost("/api/user", createCustomerUser(null, null, searchText + suffix + "@thingsboard.org", customerId), User.class));
            doPost("/api/user", createCustomerUser(null, null, customerId), User.class);

            suffix = StringUtils.randomAlphabetic((int) (5 + Math.random() * 10));
            doPost("/api/user", createCustomerUser(searchText + i, "Last" + i, customerId2), User.class);
            doPost("/api/user", createCustomerUser(null, null, searchText + suffix + "@thingsboard.org", customerId2), User.class);
        }

        createUserAndLogin(createCustomerUser(customerId), "testPassword2");

        // find users by search text
        PageLink pageLink = new PageLink(10, 0, searchText);
        List<UserEmailInfo> usersInfo = getUsersInfo(pageLink);

        List<UserEmailInfo> expectedUserInfos = customerUsersContainingWord.stream().map(customerUser -> new UserEmailInfo(customerUser.getId(),
                customerUser.getEmail(), customerUser.getFirstName() == null ? "" : customerUser.getFirstName(),
                customerUser.getLastName() == null ? "" : customerUser.getLastName()))
                .sorted(userDataIdComparator).collect(Collectors.toList());
        usersInfo.sort(userDataIdComparator);

        Assert.assertEquals(expectedUserInfos, usersInfo);

        // find user by full name
        pageLink = new PageLink(10, 0, searchText + "5");
        usersInfo = getUsersInfo(pageLink);
        Assert.assertEquals(1, usersInfo.size());

        //clear users
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + customerId2.getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `shouldFindTenantUsersBySearchText` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void shouldFindTenantUsersBySearchText() throws Exception {
        loginSysAdmin();

        User tenantAdmin = createTenantAdminUser();
        createUserAndLogin(tenantAdmin, "testPassword1");
        CustomerId customerId = postCustomer();
        CustomerId customerId2 = postCustomer();

        String searchText = "Brown";

        List<User> usersContainingWord = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String suffix = StringUtils.randomAlphabetic((int) (5 + Math.random() * 10));
            usersContainingWord.add(doPost("/api/user", createCustomerUser("First" + i, searchText + i, customerId), User.class));
            usersContainingWord.add(doPost("/api/user", createCustomerUser(null, null, searchText + suffix + "@thingsboard.org", customerId), User.class));
            doPost("/api/user", createCustomerUser(null, null, customerId), User.class);

            suffix = StringUtils.randomAlphabetic((int) (5 + Math.random() * 10));
            usersContainingWord.add(doPost("/api/user", createCustomerUser("First" + i, searchText + i, customerId2), User.class));
            usersContainingWord.add(doPost("/api/user", createCustomerUser(null, null, searchText + suffix + "@thingsboard.org", customerId2), User.class));
        }

        loginDifferentTenant();
        CustomerId customerId3 = postCustomer();
        doPost("/api/user", createCustomerUser("Jane", searchText, customerId3), User.class);

        // find users by search text
        loginUser(tenantAdmin.getEmail(), "testPassword1");
        PageLink pageLink = new PageLink(10, 0, searchText);
        List<UserEmailInfo> usersInfo = getUsersInfo(pageLink);

        List<UserEmailInfo> expectedUserInfos = usersContainingWord.stream().map(customerUser -> new UserEmailInfo(customerUser.getId(),
                customerUser.getEmail(), customerUser.getFirstName() == null ? "" : customerUser.getFirstName(),
                customerUser.getLastName() == null ? "" : customerUser.getLastName()))
                .sorted(userDataIdComparator).collect(Collectors.toList());
        usersInfo.sort(userDataIdComparator);

        Assert.assertEquals(expectedUserInfos, usersInfo);

        // find user by full last name
        pageLink = new PageLink(10, 0, searchText + "3");
        usersInfo = getUsersInfo(pageLink);
        Assert.assertEquals(2, usersInfo.size());

        //clear users
        doDelete("/api/customer/" + customerId.getId().toString())
                .andExpect(status().isOk());
        doDelete("/api/customer/" + customerId2.getId().toString())
                .andExpect(status().isOk());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `postCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private CustomerId postCustomer() {
        Customer customer = new Customer();
        customer.setTitle(StringUtils.randomAlphabetic(9));
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        return savedCustomer.getId();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createCustomerUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static User createCustomerUser(CustomerId customerId) {
        return createCustomerUser(null, null, customerId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createCustomerUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static User createCustomerUser(String firstName, String lastName, CustomerId customerId) {
        String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));
        return createCustomerUser(firstName, lastName, "testMail" + suffix + "@thingsboard.org", customerId);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createCustomerUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private static User createCustomerUser(String firstName, String lastName, String email, CustomerId customerId) {
        User user = new User();
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setCustomerId(customerId);
        user.setEmail(email);
        return user;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTenantAdminUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private User createTenantAdminUser() {
        return createTenantAdminUser(null, null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTenantAdminUser` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private User createTenantAdminUser(String firstName, String lastName) {
        String suffix = StringUtils.randomAlphanumeric((int) (5 + Math.random() * 10));

        User tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(tenantId);
        tenantAdmin.setEmail("testEmail" + suffix + "@thingsbord.org");
        tenantAdmin.setFirstName(firstName);
        tenantAdmin.setLastName(lastName);
        return tenantAdmin;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getUsersInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private List<UserEmailInfo> getUsersInfo(PageLink pageLink) throws Exception {
        List<UserEmailInfo> loadedCustomerUsers = new ArrayList<>();
        PageData<UserEmailInfo> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/users/info?", new TypeReference<>() {
            }, pageLink);
            loadedCustomerUsers.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
        return loadedCustomerUsers;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `createTestAlarm` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Alarm createTestAlarm(Device device) {
        Alarm alarm = new Alarm();
        alarm.setOriginator(device.getId());
        alarm.setCustomerId(device.getCustomerId());
        alarm.setSeverity(AlarmSeverity.MAJOR);
        alarm.setType("testAlarm");
        alarm.setStartTs(System.currentTimeMillis());
        return doPost("/api/alarm", alarm, Alarm.class);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testEmptyDashboardSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testEmptyDashboardSettings() throws Exception {
        loginCustomerUser();

        UserDashboardsInfo retrievedSettings = doGet("/api/user/dashboards", UserDashboardsInfo.class);
        Assert.assertNotNull(retrievedSettings);
        Assert.assertNotNull(retrievedSettings.getLast());
        Assert.assertTrue(retrievedSettings.getLast().isEmpty());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testDashboardSettingsFlow` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testDashboardSettingsFlow() throws Exception {
        loginTenantAdmin();

        Dashboard dashboard1 = new Dashboard();
        dashboard1.setTitle("My dashboard 1");
        Dashboard savedDashboard1 = doPost("/api/dashboard", dashboard1, Dashboard.class);
        Dashboard dashboard2 = new Dashboard();
        dashboard2.setTitle("My dashboard 2");
        Dashboard savedDashboard2 = doPost("/api/dashboard", dashboard2, Dashboard.class);

        UserDashboardsInfo retrievedSettings = doGet("/api/user/dashboards", UserDashboardsInfo.class);
        Assert.assertNotNull(retrievedSettings);
        Assert.assertNotNull(retrievedSettings.getLast());
        Assert.assertTrue(retrievedSettings.getLast().isEmpty());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());

        UserDashboardsInfo newSettings = doGet("/api/user/dashboards/" + savedDashboard1.getId().getId() + "/visit", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(1, newSettings.getLast().size());
        var lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard1.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), lastVisited.getTitle());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());

        newSettings = doGet("/api/user/dashboards/" + savedDashboard2.getId().getId() + "/visit", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());

        newSettings = doGet("/api/user/dashboards", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());

        newSettings = doGet("/api/user/dashboards/" + savedDashboard1.getId().getId() + "/star", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertFalse(lastVisited.isStarred());
        lastVisited = newSettings.getLast().get(1);
        Assert.assertEquals(savedDashboard1.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertEquals(1, newSettings.getStarred().size());
        StarredDashboardInfo starred = newSettings.getStarred().get(0);
        Assert.assertEquals(savedDashboard1.getId().getId(), starred.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), starred.getTitle());

        newSettings = doGet("/api/user/dashboards/" + savedDashboard2.getId().getId() + "/star", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        lastVisited = newSettings.getLast().get(1);
        Assert.assertEquals(savedDashboard1.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertEquals(2, newSettings.getStarred().size());
        starred = newSettings.getStarred().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), starred.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), starred.getTitle());

        newSettings = doGet("/api/user/dashboards/" + savedDashboard1.getId().getId() + "/unstar", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        lastVisited = newSettings.getLast().get(1);
        Assert.assertEquals(savedDashboard1.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), lastVisited.getTitle());
        Assert.assertFalse(lastVisited.isStarred());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertEquals(1, newSettings.getStarred().size());
        starred = newSettings.getStarred().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), starred.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), starred.getTitle());

        //TEST renaming in the cache.
        savedDashboard1.setTitle(RandomStringUtils.randomAlphanumeric(10));
        savedDashboard1 = doPost("/api/dashboard", savedDashboard1, Dashboard.class);
        savedDashboard2.setTitle(RandomStringUtils.randomAlphanumeric(10));
        savedDashboard2 = doPost("/api/dashboard", savedDashboard2, Dashboard.class);

        newSettings = doGet("/api/user/dashboards/" + savedDashboard1.getId().getId() + "/unstar", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(2, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        lastVisited = newSettings.getLast().get(1);
        Assert.assertEquals(savedDashboard1.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard1.getTitle(), lastVisited.getTitle());
        Assert.assertFalse(lastVisited.isStarred());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertEquals(1, newSettings.getStarred().size());
        starred = newSettings.getStarred().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), starred.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), starred.getTitle());

        doDelete("/api/dashboard/" + savedDashboard1.getId().getId().toString()).andExpect(status().isOk());

        newSettings = doGet("/api/user/dashboards", UserDashboardsInfo.class);
        Assert.assertNotNull(newSettings);
        Assert.assertNotNull(newSettings.getLast());
        Assert.assertEquals(1, newSettings.getLast().size());
        lastVisited = newSettings.getLast().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), lastVisited.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), lastVisited.getTitle());
        Assert.assertTrue(lastVisited.isStarred());
        Assert.assertEquals(1, newSettings.getStarred().size());
        starred = newSettings.getStarred().get(0);
        Assert.assertEquals(savedDashboard2.getId().getId(), starred.getId());
        Assert.assertEquals(savedDashboard2.getTitle(), starred.getTitle());

        doDelete("/api/dashboard/" + savedDashboard2.getId().getId().toString()).andExpect(status().isOk());

        retrievedSettings = doGet("/api/user/dashboards", UserDashboardsInfo.class);
        Assert.assertNotNull(retrievedSettings);
        Assert.assertNotNull(retrievedSettings.getLast());
        Assert.assertTrue(retrievedSettings.getLast().isEmpty());
        Assert.assertNotNull(retrievedSettings.getStarred());
        Assert.assertTrue(retrievedSettings.getStarred().isEmpty());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`UserControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
