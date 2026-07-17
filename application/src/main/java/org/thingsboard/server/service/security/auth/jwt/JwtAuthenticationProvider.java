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
package org.thingsboard.server.service.security.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.service.security.auth.JwtAuthenticationToken;
import org.thingsboard.server.service.security.auth.TokenOutdatingService;
import org.thingsboard.server.service.security.exception.JwtExpiredTokenException;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.model.token.RawAccessJwtToken;

/**
 * 中文说明：
 * 1. `JwtAuthenticationProvider` 是 ThingsBoard Application 中创建或提供 `Jwt Authentication Provider` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `AuthenticationProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final JwtTokenFactory tokenFactory;
    private final TokenOutdatingService tokenOutdatingService;

    /**
     * 功能：执行 `authenticate` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：处理结果。
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RawAccessJwtToken rawAccessToken = (RawAccessJwtToken) authentication.getCredentials();
        SecurityUser securityUser = authenticate(rawAccessToken.getToken());
        return new JwtAuthenticationToken(securityUser);
    }

    /**
     * 功能：执行 `authenticate` 对应的处理。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    public SecurityUser authenticate(String accessToken) throws AuthenticationException {
        if (StringUtils.isEmpty(accessToken)) {
            throw new BadCredentialsException("Token is invalid");
        }
        SecurityUser securityUser = tokenFactory.parseAccessJwtToken(accessToken);
        if (tokenOutdatingService.isOutdated(accessToken, securityUser.getId())) {
            throw new JwtExpiredTokenException("Token is outdated");
        }
        return securityUser;
    }

    /**
     * 功能：执行 `supports` 对应的处理。
     * 参数：
     * - `authentication`：`authentication` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return (JwtAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
