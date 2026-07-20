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
package org.thingsboard.server.service.security.auth.jwt.settings;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `DefaultJwtSettingsValidator` 是 ThingsBoard Application 中围绕 `Jwt Validator` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `JwtSettingsValidator`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@RequiredArgsConstructor
public class DefaultJwtSettingsValidator implements JwtSettingsValidator {

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `jwtSettings`：配置对象。
     * 返回：无。
     */
    @Override
    public void validate(JwtSettings jwtSettings) {
        if (StringUtils.isEmpty(jwtSettings.getTokenIssuer())) {
            throw new DataValidationException("JWT token issuer should be specified!");
        }
        if (Optional.ofNullable(jwtSettings.getRefreshTokenExpTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(15)) {
            throw new DataValidationException("JWT refresh token expiration time should be at least 15 minutes!");
        }
        if (Optional.ofNullable(jwtSettings.getTokenExpirationTime()).orElse(0) < TimeUnit.MINUTES.toSeconds(1)) {
            throw new DataValidationException("JWT token expiration time should be at least 1 minute!");
        }
        if (jwtSettings.getTokenExpirationTime() >= jwtSettings.getRefreshTokenExpTime()) {
            throw new DataValidationException("JWT token expiration time should greater than JWT refresh token expiration time!");
        }
        if (StringUtils.isEmpty(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be specified!");
        }

        byte[] decodedKey;
        try {
            decodedKey = Base64.getDecoder().decode(jwtSettings.getTokenSigningKey());
        } catch (Exception e) {
            throw new DataValidationException("JWT token signing key should be a valid Base64 encoded string! " + e.getMessage());
        }

        if (Arrays.isNullOrEmpty(decodedKey)) {
            throw new DataValidationException("JWT token signing key should be non-empty after Base64 decoding!");
        }
        if (decodedKey.length * Byte.SIZE < 256 && !JwtSettingsService.TOKEN_SIGNING_KEY_DEFAULT.equals(jwtSettings.getTokenSigningKey())) {
            throw new DataValidationException("JWT token signing key should be a Base64 encoded string representing at least 256 bits of data!");
        }

        System.arraycopy(decodedKey, 0, RandomUtils.nextBytes(decodedKey.length), 0, decodedKey.length); //secure memory
    }

}
