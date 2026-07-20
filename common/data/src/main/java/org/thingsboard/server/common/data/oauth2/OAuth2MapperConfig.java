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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;

/**
 * 中文说明：
 * 1. `OAuth2MapperConfig` 是 ThingsBoard Common Data 中描述 `O Auth2` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
public class OAuth2MapperConfig {
    /**
     * 当前操作是否被允许。
     */
    @ApiModelProperty(value = "Whether user should be created if not yet present on the platform after successful authentication")
    private boolean allowUserCreation;
    /**
     * 是否满足用户条件。
     */
    @ApiModelProperty(value = "Whether user credentials should be activated when user is created after successful authentication")
    private boolean activateUser;
    /**
     * 类型映射关系，用于按键查找对应值。
     */
    @ApiModelProperty(value = "Type of OAuth2 mapper. Depending on this param, different mapper config fields must be specified", required = true)
    private MapperType type;
    /**
     * `basic`映射关系，用于按键查找对应值。
     */
    @Valid
    @ApiModelProperty(value = "Mapper config for BASIC and GITHUB mapper types")
    private OAuth2BasicMapperConfig basic;
    /**
     * `custom`映射关系，用于按键查找对应值。
     */
    @Valid
    @ApiModelProperty(value = "Mapper config for CUSTOM mapper type")
    private OAuth2CustomMapperConfig custom;
}
