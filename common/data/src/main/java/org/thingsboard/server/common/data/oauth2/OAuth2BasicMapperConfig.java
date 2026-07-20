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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.validation.Length;

/**
 * 中文说明：
 * 1. `OAuth2BasicMapperConfig` 是 ThingsBoard Common Data 中描述 `O Auth2 Basic` 行为的配置类型。
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
@ApiModel
public class OAuth2BasicMapperConfig {
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "emailAttributeKey", max = 31)
    @ApiModelProperty(value = "Email attribute key of OAuth2 principal attributes. " +
            "Must be specified for BASIC mapper type and cannot be specified for GITHUB type")
    private final String emailAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "firstNameAttributeKey", max = 31)
    @ApiModelProperty(value = "First name attribute key")
    private final String firstNameAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "lastNameAttributeKey", max = 31)
    @ApiModelProperty(value = "Last name attribute key")
    private final String lastNameAttributeKey;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @ApiModelProperty(value = "Tenant naming strategy. For DOMAIN type, domain for tenant name will be taken from the email (substring before '@')", required = true)
    private final TenantNameStrategyType tenantNameStrategy;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Length(fieldName = "tenantNamePattern")
    @ApiModelProperty(value = "Tenant name pattern for CUSTOM naming strategy. " +
            "OAuth2 attributes in the pattern can be used by enclosing attribute key in '%{' and '}'", example = "%{email}")
    private final String tenantNamePattern;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    @Length(fieldName = "customerNamePattern")
    @ApiModelProperty(value = "Customer name pattern. When creating a user on the first OAuth2 log in, if specified, " +
            "customer name will be used to create or find existing customer in the platform and assign customerId to the user")
    private final String customerNamePattern;
    /**
     * 仪表盘，用于标识或展示当前对象。
     */
    @Length(fieldName = "defaultDashboardName")
    @ApiModelProperty(value = "Name of the tenant's dashboard to set as default dashboard for newly created user")
    private final String defaultDashboardName;
    /**
     * 是否满足`alwaysFullScreen`条件。
     */
    @ApiModelProperty(value = "Whether default dashboard should be open in full screen")
    private final boolean alwaysFullScreen;
}
