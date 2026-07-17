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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;

import java.util.List;

/**
 * 中文说明：
 * 1. `OAuth2Registration` 是 ThingsBoard Common Data 中承载 `O Auth2 Registration` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseDataWithAdditionalInfo`、`HasName`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ToString(exclude = {"clientSecret"})
@NoArgsConstructor
public class OAuth2Registration extends BaseDataWithAdditionalInfo<OAuth2RegistrationId> implements HasName {

    /**
     * `oauth2ParamsId`ID，用于定位对应业务对象。
     */
    private OAuth2ParamsId oauth2ParamsId;
    private OAuth2MapperConfig mapperConfig;
    /**
     * 客户端ID，用于定位对应业务对象。
     */
    private String clientId;
    private String clientSecret;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    private String authorizationUri;
    private String accessTokenUri;
    /**
     * `scope`列表，用于保存一组待处理对象。
     */
    private List<String> scope;
    private String userInfoUri;
    /**
     * 用户，用于标识或展示当前对象。
     */
    private String userNameAttributeName;
    private String jwkSetUri;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private String clientAuthenticationMethod;
    private String loginButtonLabel;
    /**
     * `loginButtonIcon` 字段，保存当前对象的对应属性。
     */
    private String loginButtonIcon;
    private List<PlatformType> platforms;

    /**
     * 功能：创建 `OAuth2Registration` 实例，并初始化必要字段。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：新创建的对象实例。
     */
    public OAuth2Registration(OAuth2Registration registration) {
        super(registration);
        this.oauth2ParamsId = registration.oauth2ParamsId;
        this.mapperConfig = registration.mapperConfig;
        this.clientId = registration.clientId;
        this.clientSecret = registration.clientSecret;
        this.authorizationUri = registration.authorizationUri;
        this.accessTokenUri = registration.accessTokenUri;
        this.scope = registration.scope;
        this.userInfoUri = registration.userInfoUri;
        this.userNameAttributeName = registration.userNameAttributeName;
        this.jwkSetUri = registration.jwkSetUri;
        this.clientAuthenticationMethod = registration.clientAuthenticationMethod;
        this.loginButtonLabel = registration.loginButtonLabel;
        this.loginButtonIcon = registration.loginButtonIcon;
        this.platforms = registration.platforms;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return loginButtonLabel;
    }
}
