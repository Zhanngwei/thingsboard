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
package org.thingsboard.server.service.security.system;

import org.springframework.security.core.AuthenticationException;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;

/**
 * 中文说明：
 * 1. `SystemSecurityService` 是 ThingsBoard Application 中定义安全配置能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface SystemSecurityService {

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    SecuritySettings getSecuritySettings();

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `securitySettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    SecuritySettings saveSecuritySettings(SecuritySettings securitySettings);

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `passwordPolicy`：`passwordPolicy` 参数。
     * 返回：无。
     */
    void validatePasswordByPolicy(String password, UserPasswordPolicy passwordPolicy);

    /**
     * 功能：校验用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：无。
     */
    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;

    /**
     * 功能：校验`Two Fa Verification`。
     * 参数：
     * - `securityUser`：`securityUser` 参数。
     * - `verificationSuccess`：`verificationSuccess` 参数。
     * - `twoFaSettings`：配置对象。
     * 返回：无。
     */
    void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings);

    /**
     * 功能：校验密码。
     * 参数：
     * - `password`：`password` 参数。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    void validatePassword(String password, UserCredentials userCredentials) throws DataValidationException;

    /**
     * 功能：获取基础访问地址。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `httpServletRequest`：请求对象。
     * 返回：文本结果。
     */
    String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest);

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e);

    /**
     * 功能：执行 `logLoginAction` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `authenticationDetails`：`authenticationDetails` 参数。
     * - `actionType`：类型。
     * - `provider`：`provider` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e);
}
