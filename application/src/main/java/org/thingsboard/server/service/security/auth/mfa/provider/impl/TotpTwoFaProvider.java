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

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.model.mfa.account.TotpTwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TotpTwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFaProvider;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 中文说明：
 * 1. `TotpTwoFaProvider` 是 ThingsBoard Application 中创建或提供 `Totp Two Fa` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TwoFaProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Service
@RequiredArgsConstructor
@TbCoreComponent
public class TotpTwoFaProvider implements TwoFaProvider<TotpTwoFaProviderConfig, TotpTwoFaAccountConfig> {

    /**
     * 功能：执行 `generateNewAccountConfig` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * - `providerConfig`：配置对象。
     * 返回：处理结果。
     */
    @Override
    public final TotpTwoFaAccountConfig generateNewAccountConfig(User user, TotpTwoFaProviderConfig providerConfig) {
        TotpTwoFaAccountConfig config = new TotpTwoFaAccountConfig();
        String secretKey = generateSecretKey();
        config.setAuthUrl(getTotpAuthUrl(user, secretKey, providerConfig));
        return config;
    }

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
    public final boolean checkVerificationCode(SecurityUser user, String code, TotpTwoFaProviderConfig providerConfig, TotpTwoFaAccountConfig accountConfig) {
        String secretKey = UriComponentsBuilder.fromUriString(accountConfig.getAuthUrl()).build().getQueryParams().getFirst("secret");
        return new Totp(secretKey).verify(code);
    }

    /**
     * 功能：获取URL 地址。
     * 参数：
     * - `user`：`user` 参数。
     * - `secretKey`：键。
     * - `providerConfig`：配置对象。
     * 返回：文本结果。
     */
    @SneakyThrows
    private String getTotpAuthUrl(User user, String secretKey, TotpTwoFaProviderConfig providerConfig) {
        URIBuilder uri = new URIBuilder()
                .setScheme("otpauth")
                .setHost("totp")
                .setParameter("issuer", providerConfig.getIssuerName())
                .setPath("/" + providerConfig.getIssuerName() + ":" + user.getEmail())
                .setParameter("secret", secretKey);
        return uri.build().toASCIIString();
    }

    /**
     * 功能：执行 `generateSecretKey` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    private String generateSecretKey() {
        return Base32.encode(RandomUtils.nextBytes(20));
    }


    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.TOTP;
    }

}
