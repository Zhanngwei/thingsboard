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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.security.Authority;

/**
 * 中文说明：
 * 1. `JwtPair` 是 ThingsBoard Common Data 中承载 `Jwt Pair` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel(value = "JWT Pair")
@Data
@NoArgsConstructor
public class JwtPair {

    /**
     * 令牌，用于认证或安全校验。
     */
    @ApiModelProperty(position = 1, value = "The JWT Access Token. Used to perform API calls.", example = "AAB254FF67D..")
    private String token;
    /**
     * 令牌，用于认证或安全校验。
     */
    @ApiModelProperty(position = 1, value = "The JWT Refresh Token. Used to get new JWT Access Token if old one has expired.", example = "AAB254FF67D..")
    private String refreshToken;

    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    private Authority scope;

    /**
     * 功能：创建 `JwtPair` 实例，并初始化必要字段。
     * 参数：
     * - `token`：`token` 参数。
     * - `refreshToken`：`refreshToken` 参数。
     * 返回：新创建的对象实例。
     */
    public JwtPair(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

}
