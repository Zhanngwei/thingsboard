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
package org.thingsboard.server.service.security.auth.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 中文说明：
 * 1. `LoginRequest` 是 ThingsBoard Application 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
public class LoginRequest {

    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;

    /**
     * 密码，用于认证或安全校验。
     */
    private String password;

    /**
     * 功能：创建 `LoginRequest` 实例，并初始化必要字段。
     * 参数：
     * - `username`：名称。
     * - `password`：`password` 参数。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public LoginRequest(@JsonProperty("username") String username, @JsonProperty("password") String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * 功能：获取用户名。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 1, required = true, value = "User email", example = "tenant@thingsboard.org")
    public String getUsername() {
        return username;
    }

    /**
     * 功能：获取密码。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 2, required = true, value = "User password", example = "tenant")
    public String getPassword() {
        return password;
    }
}
