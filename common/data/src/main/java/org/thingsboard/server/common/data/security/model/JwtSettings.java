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
package org.thingsboard.server.common.data.security.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 中文说明：
 * 1. `JwtSettings` 是 ThingsBoard Common Data 中描述 `Jwt` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@ApiModel(value = "JWT Settings")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JwtSettings {

    /**
     * {@link JwtToken} will expire after this time.
     */
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    @ApiModelProperty(position = 1, value = "The JWT will expire after seconds.", example = "9000")
    private Integer tokenExpirationTime;

    /**
     * {@link JwtToken} can be refreshed during this timeframe.
     */
    /**
     * 刷新令牌过期时间，用于控制时间范围或等待时长。
     */
    @ApiModelProperty(position = 2, value = "The JWT can be refreshed during seconds.", example = "604800")
    private Integer refreshTokenExpTime;

    /**
     * Token issuer.
     */
    /**
     * 令牌，用于认证或安全校验。
     */
    @ApiModelProperty(position = 3, value = "The JWT issuer.", example = "thingsboard.io")
    private String tokenIssuer;

    /**
     * Key is used to sign {@link JwtToken}.
     * Base64 encoded
     */
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @ApiModelProperty(position = 4, value = "The JWT key is used to sing token. Base64 encoded.", example = "cTU4WnNqemI2aU5wbWVjdm1vYXRzanhjNHRUcXliMjE=")
    private String tokenSigningKey;

}
