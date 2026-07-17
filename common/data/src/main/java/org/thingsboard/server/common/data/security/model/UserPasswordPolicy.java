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

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `UserPasswordPolicy` 是 ThingsBoard Common Data 中承载用户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
public class UserPasswordPolicy implements Serializable {

    /**
     * `minimumLength` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Minimum number of symbols in the password." )
    private Integer minimumLength;
    /**
     * `maximumLength` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Maximum number of symbols in the password." )
    private Integer maximumLength;
    /**
     * `minimumUppercaseLetters` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Minimum number of uppercase letters in the password." )
    private Integer minimumUppercaseLetters;
    /**
     * `minimumLowercaseLetters` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Minimum number of lowercase letters in the password." )
    private Integer minimumLowercaseLetters;
    /**
     * `minimumDigits` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Minimum number of digits in the password." )
    private Integer minimumDigits;
    /**
     * `minimumSpecialCharacters` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 1, value = "Minimum number of special in the password." )
    private Integer minimumSpecialCharacters;
    /**
     * 当前操作是否被允许。
     */
    @ApiModelProperty(position = 1, value = "Allow whitespaces")
    private Boolean allowWhitespaces = true;
    /**
     * 密码ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "Force user to update password if existing one does not pass validation")
    private Boolean forceUserToResetPasswordIfNotValid = false;

    /**
     * 密码，用于认证或安全校验。
     */
    @ApiModelProperty(position = 1, value = "Password expiration period (days). Force expiration of the password." )
    private Integer passwordExpirationPeriodDays;
    /**
     * 密码，用于认证或安全校验。
     */
    @ApiModelProperty(position = 1, value = "Password reuse frequency (days). Disallow to use the same password for the defined number of days" )
    private Integer passwordReuseFrequencyDays;

}
