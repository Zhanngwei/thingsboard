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
package org.thingsboard.server.service.security.model.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `OAuth2AppTokenFactory` 是 ThingsBoard Application 中创建或提供 `O Auth2 App` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@Slf4j
public class OAuth2AppTokenFactory {

    /**
     * 回调常量，用于统一引用固定值。
     */
    private static final String CALLBACK_URL_SCHEME = "callbackUrlScheme";

    private static final long MAX_EXPIRATION_TIME_DIFF_MS = TimeUnit.MINUTES.toMillis(5);

    /**
     * 功能：校验回调。
     * 参数：
     * - `appPackage`：`appPackage` 参数。
     * - `appToken`：`appToken` 参数。
     * - `appSecret`：`appSecret` 参数。
     * 返回：判断结果。
     */
    public String validateTokenAndGetCallbackUrlScheme(String appPackage, String appToken, String appSecret) {
        Jws<Claims> jwsClaims;
        try {
            jwsClaims = Jwts.parser().setSigningKey(appSecret).parseClaimsJws(appToken);
        }
        catch (UnsupportedJwtException | MalformedJwtException | IllegalArgumentException | SignatureException ex) {
            throw new IllegalArgumentException("Invalid Application token: ", ex);
        } catch (ExpiredJwtException expiredEx) {
            throw new IllegalArgumentException("Application token expired", expiredEx);
        }
        Claims claims = jwsClaims.getBody();
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new IllegalArgumentException("Application token must have expiration date");
        }
        long timeDiff = expiration.getTime() - System.currentTimeMillis();
        if (timeDiff > MAX_EXPIRATION_TIME_DIFF_MS) {
            throw new IllegalArgumentException("Application token expiration time can't be longer than 5 minutes");
        }
        if (!claims.getIssuer().equals(appPackage)) {
            throw new IllegalArgumentException("Application token issuer doesn't match application package");
        }
        String callbackUrlScheme = claims.get(CALLBACK_URL_SCHEME, String.class);
        if (StringUtils.isEmpty(callbackUrlScheme)) {
            throw new IllegalArgumentException("Application token doesn't have callbackUrlScheme");
        }
        return callbackUrlScheme;
    }

}
