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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.UserId;

/**
 * 中文说明：
 * 1. `UserEmailInfo` 是 ThingsBoard Common Data 中承载用户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `HasId`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
@AllArgsConstructor
public class UserEmailInfo implements HasId<UserId> {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "User id")
    private UserId id;
    /**
     * 邮箱，用于展示或标识当前对象。
     */
    @ApiModelProperty(position = 2, value = "User email", example = "john@gmail.com")
    private String email;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 3, value = "User first name", example = "John")
    private String firstName;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 4, value = "User last name", example = "Brown")
    private String lastName;

}
