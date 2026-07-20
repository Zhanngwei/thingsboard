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
 * 1. `SecuritySettings` 是 ThingsBoard Common Data 中描述安全配置行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@ApiModel
@Data
public class SecuritySettings implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -1307613974597312465L;

    /**
     * 密码，用于认证或安全校验。
     */
    @ApiModelProperty(position = 1, value = "The user password policy object." )
    private UserPasswordPolicy passwordPolicy;
    /**
     * 尝试次数，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 2, value = "Maximum number of failed login attempts allowed before user account is locked." )
    private Integer maxFailedLoginAttempts;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    @ApiModelProperty(position = 3, value = "Email to use for notifications about locked users." )
    private String userLockoutNotificationEmail;
}
