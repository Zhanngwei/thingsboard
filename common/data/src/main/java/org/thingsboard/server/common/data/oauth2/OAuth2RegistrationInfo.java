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

import com.fasterxml.jackson.databind.JsonNode;
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
 * 1. `OAuth2RegistrationInfo` 是 ThingsBoard Common Data 中承载 `O Auth2 Registration` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel
public class OAuth2RegistrationInfo {
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @ApiModelProperty(value = "Config for mapping OAuth2 log in response to platform entities", required = true)
    private OAuth2MapperConfig mapperConfig;
    /**
     * 客户端ID，用于定位对应业务对象。
     */
    @ApiModelProperty(value = "OAuth2 client ID. Cannot be empty", required = true)
    private String clientId;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @ApiModelProperty(value = "OAuth2 client secret. Cannot be empty", required = true)
    private String clientSecret;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @ApiModelProperty(value = "Authorization URI of the OAuth2 provider. Cannot be empty", required = true)
    private String authorizationUri;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @ApiModelProperty(value = "Access token URI of the OAuth2 provider. Cannot be empty", required = true)
    private String accessTokenUri;
    /**
     * `scope`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "OAuth scopes that will be requested from OAuth2 platform. Cannot be empty", required = true)
    private List<String> scope;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    @ApiModelProperty(value = "User info URI of the OAuth2 provider")
    private String userInfoUri;
    /**
     * 用户，用于标识或展示当前对象。
     */
    @ApiModelProperty(value = "Name of the username attribute in OAuth2 provider response. Cannot be empty")
    private String userNameAttributeName;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @ApiModelProperty(value = "JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @ApiModelProperty(value = "Client authentication method to use: 'BASIC' or 'POST'. Cannot be empty", required = true)
    private String clientAuthenticationMethod;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @ApiModelProperty(value = "OAuth2 provider label. Cannot be empty", required = true)
    private String loginButtonLabel;
    /**
     * `loginButtonIcon` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    /**
     * `platforms`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "List of platforms for which usage of the OAuth2 client is allowed (empty for all allowed)")
    private List<PlatformType> platforms;
    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @ApiModelProperty(value = "Additional info of OAuth2 client (e.g. providerName)", required = true)
    private JsonNode additionalInfo;
}
