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
 * 1. `AuthControllerTest` 是 ThingsBoard Application 中验证 `AuthController` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
