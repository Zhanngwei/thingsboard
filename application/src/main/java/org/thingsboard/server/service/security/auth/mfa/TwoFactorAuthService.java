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
package org.thingsboard.server.service.security.auth.mfa;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 中文说明：
 * 1. `TwoFactorAuthService` 是 ThingsBoard Application 中定义认证能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TwoFactorAuthService {

    /**
     * 功能：判断`Two Fa Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：判断结果。
     */
    boolean isTwoFaEnabled(TenantId tenantId, UserId userId);

    /**
     * 功能：校验提供者。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `providerType`：类型。
     * 返回：无。
     */
    void checkProvider(TenantId tenantId, TwoFaProviderType providerType) throws ThingsboardException;


    /**
     * 功能：执行 `prepareVerificationCode` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * - `checkLimits`：数量限制。
     * 返回：无。
     */
    void prepareVerificationCode(SecurityUser user, TwoFaProviderType providerType, boolean checkLimits) throws Exception;

    /**
     * 功能：执行 `prepareVerificationCode` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `accountConfig`：配置对象。
     * - `checkLimits`：数量限制。
     * 返回：无。
     */
    void prepareVerificationCode(SecurityUser user, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException;


    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * - `verificationCode`：`verificationCode` 参数。
     * - `checkLimits`：数量限制。
     * 返回：判断结果。
     */
    boolean checkVerificationCode(SecurityUser user, TwoFaProviderType providerType, String verificationCode, boolean checkLimits) throws ThingsboardException;

    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `accountConfig`：配置对象。
     * - `checkLimits`：数量限制。
     * 返回：判断结果。
     */
    boolean checkVerificationCode(SecurityUser user, String verificationCode, TwoFaAccountConfig accountConfig, boolean checkLimits) throws ThingsboardException;


    /**
     * 功能：执行 `generateNewAccountConfig` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerType`：类型。
     * 返回：处理结果。
     */
    TwoFaAccountConfig generateNewAccountConfig(User user, TwoFaProviderType providerType) throws ThingsboardException;

}
