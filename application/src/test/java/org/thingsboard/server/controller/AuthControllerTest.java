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
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.security.auth.rest.LoginRequest;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AuthControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@DaoSqlTest
public class AuthControllerTest extends AbstractControllerTest {

    /**
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void tearDown() throws Exception {
        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);

        securitySettings.getPasswordPolicy().setMaximumLength(72);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(false);

        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());
    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testGetUser() throws Exception {
        
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());
        
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
        
        loginTenantAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.TENANT_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(TENANT_ADMIN_EMAIL)));
        
        loginCustomerUser();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.CUSTOMER_USER.name())))
        .andExpect(jsonPath("$.email",is(CUSTOMER_USER_EMAIL)));
    }
    
    /**
     * 功能：验证`Login Logout`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testLoginLogout() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        TimeUnit.SECONDS.sleep(1); //We need to make sure that event for invalidating token was successfully processed

        logout();
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());

        resetTokens();
    }

    /**
     * 功能：验证令牌相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testRefreshToken() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        refreshToken();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
    }

    /**
     * 功能：验证密码相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotUpdatePasswordWithValueLongerThanDefaultLimit() throws Exception {
        loginTenantAdmin();
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword(RandomStringUtils.randomAlphanumeric(73));
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password must be no more than 72 characters in length.")));
    }

    /**
     * 功能：验证密码相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotAuthorizeUserIfHisPasswordBecameTooLong() throws Exception {
        loginTenantAdmin();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        String newPassword = RandomStringUtils.randomAlphanumeric(16);
        changePasswordRequest.setNewPassword(newPassword);
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isOk());
        loginUser(TENANT_ADMIN_EMAIL, newPassword);

        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);
        securitySettings.getPasswordPolicy().setMaximumLength(15);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(true);
        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());

        //try to login with user password that is not valid after security settings was updated
        doPost("/api/auth/login", new LoginRequest(TENANT_ADMIN_EMAIL, newPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("The entered password violates our policies. If this is your real password, please reset it.")));
    }

    /**
     * 功能：验证密码相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testShouldNotResetPasswordToTooLongValue() throws Exception {
        loginTenantAdmin();

        JsonNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode()
                .put("email", TENANT_ADMIN_EMAIL);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", this.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + this.currentResetPasswordToken));

        String newPassword = RandomStringUtils.randomAlphanumeric(73);
        JsonNode resetPasswordRequest = JacksonUtil.newObjectNode()
                .put("resetToken", this.currentResetPasswordToken)
                .put("password", newPassword);

        Mockito.doNothing().when(mailService).sendPasswordWasResetEmail(anyString(), anyString());
        doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message",
                                is("Password must be no more than 72 characters in length.")));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AuthControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
