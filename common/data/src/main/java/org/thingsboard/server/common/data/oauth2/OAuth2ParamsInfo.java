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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 中文说明：
 * 1. `OAuth2ParamsInfo` 是 ThingsBoard Common Data 中承载 `O Auth2 Params` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode
@Data
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class OAuth2ParamsInfo {

    /**
     * `domainInfos`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "List of configured domains where OAuth2 platform will redirect a user after successful " +
            "authentication. Cannot be empty. There have to be only one domain with specific name with scheme type 'MIXED'. " +
            "Configured domains with the same name must have different scheme types", required = true)
    private List<OAuth2DomainInfo> domainInfos;
    /**
     * `mobileInfos`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "Mobile applications settings. Application package name must be unique within the list", required = true)
    private List<OAuth2MobileInfo> mobileInfos;
    /**
     * 客户端列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "List of OAuth2 provider settings. Cannot be empty", required = true)
    private List<OAuth2RegistrationInfo> clientRegistrations;

}
