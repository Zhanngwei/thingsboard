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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.validation.Length;

import javax.validation.Valid;
import java.util.List;

/**
 * 中文说明：
 * 1. `OAuth2ClientRegistrationTemplate` 是 ThingsBoard Common Data 中承载 `O Auth2 Client` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseDataWithAdditionalInfo`、`HasName`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
@ApiModel
public class OAuth2ClientRegistrationTemplate extends BaseDataWithAdditionalInfo<OAuth2ClientRegistrationTemplateId> implements HasName {

    /**
     * 提供者ID，用于定位对应业务对象。
     */
    @Length(fieldName = "providerId")
    @ApiModelProperty(value = "OAuth2 provider identifier (e.g. its name)", required = true)
    private String providerId;
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Valid
    @ApiModelProperty(value = "Default config for mapping OAuth2 log in response to platform entities")
    private OAuth2MapperConfig mapperConfig;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Length(fieldName = "authorizationUri")
    @ApiModelProperty(value = "Default authorization URI of the OAuth2 provider")
    private String authorizationUri;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Length(fieldName = "accessTokenUri")
    @ApiModelProperty(value = "Default access token URI of the OAuth2 provider")
    private String accessTokenUri;
    /**
     * `scope`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(value = "Default OAuth scopes that will be requested from OAuth2 platform")
    private List<String> scope;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    @Length(fieldName = "userInfoUri")
    @ApiModelProperty(value = "Default user info URI of the OAuth2 provider")
    private String userInfoUri;
    /**
     * 用户，用于标识或展示当前对象。
     */
    @Length(fieldName = "userNameAttributeName")
    @ApiModelProperty(value = "Default name of the username attribute in OAuth2 provider log in response")
    private String userNameAttributeName;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Length(fieldName = "jwkSetUri")
    @ApiModelProperty(value = "Default JSON Web Key URI of the OAuth2 provider")
    private String jwkSetUri;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Length(fieldName = "clientAuthenticationMethod")
    @ApiModelProperty(value = "Default client authentication method to use: 'BASIC' or 'POST'")
    private String clientAuthenticationMethod;
    /**
     * `comment` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Comment for OAuth2 provider")
    private String comment;
    /**
     * `loginButtonIcon` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "loginButtonIcon")
    @ApiModelProperty(value = "Default log in button icon for OAuth2 provider")
    private String loginButtonIcon;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Length(fieldName = "loginButtonLabel")
    @ApiModelProperty(value = "Default OAuth2 provider label")
    private String loginButtonLabel;
    /**
     * `helpLink` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "helpLink")
    @ApiModelProperty(value = "Help link for OAuth2 provider")
    private String helpLink;

    /**
     * 功能：创建 `OAuth2ClientRegistrationTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public OAuth2ClientRegistrationTemplate(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        super(clientRegistrationTemplate);
        this.providerId = clientRegistrationTemplate.providerId;
        this.mapperConfig = clientRegistrationTemplate.mapperConfig;
        this.authorizationUri = clientRegistrationTemplate.authorizationUri;
        this.accessTokenUri = clientRegistrationTemplate.accessTokenUri;
        this.scope = clientRegistrationTemplate.scope;
        this.userInfoUri = clientRegistrationTemplate.userInfoUri;
        this.userNameAttributeName = clientRegistrationTemplate.userNameAttributeName;
        this.jwkSetUri = clientRegistrationTemplate.jwkSetUri;
        this.clientAuthenticationMethod = clientRegistrationTemplate.clientAuthenticationMethod;
        this.comment = clientRegistrationTemplate.comment;
        this.loginButtonIcon = clientRegistrationTemplate.loginButtonIcon;
        this.loginButtonLabel = clientRegistrationTemplate.loginButtonLabel;
        this.helpLink = clientRegistrationTemplate.helpLink;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return providerId;
    }
}
