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
package org.thingsboard.server.service.security.auth.mfa.provider.impl;

import lombok.Data;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.security.model.mfa.account.OtpBasedTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.OtpBasedTwoFaProviderConfig;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `OtpBasedTwoFaProvider` 是 ThingsBoard Application 中创建或提供 `Otp Based Two` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `OtpBasedTwoFaProviderConfig`、`TwoFaProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public abstract class OtpBasedTwoFaProvider<C extends OtpBasedTwoFaProviderConfig, A extends OtpBasedTwoFaAccountConfig> implements TwoFaProvider<C, A> {

    /**
     * `verificationCodesCache` 字段，保存当前对象的对应属性。
     */
    private final Cache verificationCodesCache;

    /**
     * 功能：创建 `OtpBasedTwoFaProvider` 实例，并初始化必要字段。
     * 参数：
     * - `cacheManager`：管理器对象。
     * 返回：新创建的对象实例。
     */
    protected OtpBasedTwoFaProvider(CacheManager cacheManager) {
        this.verificationCodesCache = cacheManager.getCache(CacheConstants.TWO_FA_VERIFICATION_CODES_CACHE);
    }


    /**
     * 功能：执行 `prepareVerificationCode` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerConfig`：配置对象。
     * - `accountConfig`：配置对象。
     * 返回：无。
     */
    @Override
    public final void prepareVerificationCode(SecurityUser user, C providerConfig, A accountConfig) throws ThingsboardException {
        String verificationCode = StringUtils.randomNumeric(6);
        sendVerificationCode(user, verificationCode, providerConfig, accountConfig);
        verificationCodesCache.put(user.getId(), new Otp(System.currentTimeMillis(), verificationCode, accountConfig));
    }

    /**
     * 功能：发送或提交编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `verificationCode`：`verificationCode` 参数。
     * - `providerConfig`：配置对象。
     * - `accountConfig`：配置对象。
     * 返回：无。
     */
    protected abstract void sendVerificationCode(SecurityUser user, String verificationCode, C providerConfig, A accountConfig) throws ThingsboardException;


    /**
     * 功能：校验编码。
     * 参数：
     * - `user`：`user` 参数。
     * - `code`：`code` 参数。
     * - `providerConfig`：配置对象。
     * - `accountConfig`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public final boolean checkVerificationCode(SecurityUser user, String code, C providerConfig, A accountConfig) {
        Otp correctVerificationCode = verificationCodesCache.get(user.getId(), Otp.class);
        if (correctVerificationCode != null) {
            if (System.currentTimeMillis() - correctVerificationCode.getTimestamp()
                    > TimeUnit.SECONDS.toMillis(providerConfig.getVerificationCodeLifetime())) {
                verificationCodesCache.evict(user.getId());
                return false;
            }
            if (code.equals(correctVerificationCode.getValue())
                    && accountConfig.equals(correctVerificationCode.getAccountConfig())) {
                verificationCodesCache.evict(user.getId());
                return true;
            }
        }
        return false;
    }


    /**
     * 中文说明：
     * 1. `Otp` 是 ThingsBoard Application 中围绕 `Otp` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `Serializable`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    public static class Otp implements Serializable {
        /**
         * 时间戳，用于标识当前数据或事件发生的时间。
         */
        private final long timestamp;
        private final String value;
        /**
         * 配置，保存当前对象的配置选项。
         */
        private final OtpBasedTwoFaAccountConfig accountConfig;
    }

}
