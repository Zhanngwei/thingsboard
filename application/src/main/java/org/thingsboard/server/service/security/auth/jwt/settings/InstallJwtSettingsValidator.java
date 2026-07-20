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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.model.JwtSettings;

/**
 * During Install or upgrade the validation is suppressed to keep existing data
 * */

/**
 * 中文说明：
 * 1. `InstallJwtSettingsValidator` 是 ThingsBoard Application 中围绕 `Install Jwt Validator` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `JwtSettingsValidator`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Primary
@Profile("install")
@Component
@RequiredArgsConstructor
public class InstallJwtSettingsValidator implements JwtSettingsValidator {

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `jwtSettings`：配置对象。
     * 返回：无。
     */
    @Override
    public void validate(JwtSettings jwtSettings) {

    }

}
