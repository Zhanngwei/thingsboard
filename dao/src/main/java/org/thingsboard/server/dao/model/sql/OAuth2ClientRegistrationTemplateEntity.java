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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.OAuth2ClientRegistrationTemplateId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientRegistrationTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.TenantNameStrategyType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.Arrays;

/**
 * 中文说明：
 * 1. `OAuth2ClientRegistrationTemplateEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_CLIENT_REGISTRATION_TEMPLATE_TABLE_NAME)
public class OAuth2ClientRegistrationTemplateEntity extends BaseSqlEntity<OAuth2ClientRegistrationTemplate> {

    /**
     * 提供者ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY)
    private String providerId;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Column(name = ModelConstants.OAUTH2_AUTHORIZATION_URI_PROPERTY)
    private String authorizationUri;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Column(name = ModelConstants.OAUTH2_TOKEN_URI_PROPERTY)
    private String tokenUri;
    /**
     * `scope` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_SCOPE_PROPERTY)
    private String scope;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    @Column(name = ModelConstants.OAUTH2_USER_INFO_URI_PROPERTY)
    private String userInfoUri;
    /**
     * 用户，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_USER_NAME_ATTRIBUTE_NAME_PROPERTY)
    private String userNameAttributeName;
    /**
     * URI 地址，用于定位外部资源或本地资源。
     */
    @Column(name = ModelConstants.OAUTH2_JWK_SET_URI_PROPERTY)
    private String jwkSetUri;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Column(name = ModelConstants.OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY)
    private String clientAuthenticationMethod;
    /**
     * 类型映射关系，用于按键查找对应值。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_MAPPER_TYPE_PROPERTY)
    private MapperType type;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Column(name = ModelConstants.OAUTH2_EMAIL_ATTRIBUTE_KEY_PROPERTY)
    private String emailAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Column(name = ModelConstants.OAUTH2_FIRST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String firstNameAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Column(name = ModelConstants.OAUTH2_LAST_NAME_ATTRIBUTE_KEY_PROPERTY)
    private String lastNameAttributeKey;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_STRATEGY_PROPERTY)
    private TenantNameStrategyType tenantNameStrategy;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Column(name = ModelConstants.OAUTH2_TENANT_NAME_PATTERN_PROPERTY)
    private String tenantNamePattern;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    @Column(name = ModelConstants.OAUTH2_CUSTOMER_NAME_PATTERN_PROPERTY)
    private String customerNamePattern;
    /**
     * 仪表盘，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_DEFAULT_DASHBOARD_NAME_PROPERTY)
    private String defaultDashboardName;
    /**
     * 是否满足`alwaysFullScreen`条件。
     */
    @Column(name = ModelConstants.OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY)
    private Boolean alwaysFullScreen;
    /**
     * `comment` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_COMMENT_PROPERTY)
    private String comment;
    /**
     * `loginButtonIcon` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY)
    private String loginButtonIcon;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_LOGIN_BUTTON_LABEL_PROPERTY)
    private String loginButtonLabel;
    /**
     * `helpLink` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_HELP_LINK_PROPERTY)
    private String helpLink;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.OAUTH2_TEMPLATE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `OAuth2ClientRegistrationTemplateEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OAuth2ClientRegistrationTemplateEntity() {
    }

    /**
     * 功能：创建 `OAuth2ClientRegistrationTemplateEntity` 实例，并初始化必要字段。
     * 参数：
     * - `clientRegistrationTemplate`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public OAuth2ClientRegistrationTemplateEntity(OAuth2ClientRegistrationTemplate clientRegistrationTemplate) {
        if (clientRegistrationTemplate.getId() != null) {
            this.setUuid(clientRegistrationTemplate.getId().getId());
        }
        this.createdTime = clientRegistrationTemplate.getCreatedTime();
        this.providerId = clientRegistrationTemplate.getProviderId();
        this.authorizationUri = clientRegistrationTemplate.getAuthorizationUri();
        this.tokenUri = clientRegistrationTemplate.getAccessTokenUri();
        this.scope = clientRegistrationTemplate.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.userInfoUri = clientRegistrationTemplate.getUserInfoUri();
        this.userNameAttributeName = clientRegistrationTemplate.getUserNameAttributeName();
        this.jwkSetUri = clientRegistrationTemplate.getJwkSetUri();
        this.clientAuthenticationMethod = clientRegistrationTemplate.getClientAuthenticationMethod();
        this.comment = clientRegistrationTemplate.getComment();
        this.loginButtonIcon = clientRegistrationTemplate.getLoginButtonIcon();
        this.loginButtonLabel = clientRegistrationTemplate.getLoginButtonLabel();
        this.helpLink = clientRegistrationTemplate.getHelpLink();
        this.additionalInfo = clientRegistrationTemplate.getAdditionalInfo();
        OAuth2MapperConfig mapperConfig = clientRegistrationTemplate.getMapperConfig();
        if (mapperConfig != null){
            this.type = mapperConfig.getType();
            OAuth2BasicMapperConfig basicConfig = mapperConfig.getBasic();
            if (basicConfig != null) {
                this.emailAttributeKey = basicConfig.getEmailAttributeKey();
                this.firstNameAttributeKey = basicConfig.getFirstNameAttributeKey();
                this.lastNameAttributeKey = basicConfig.getLastNameAttributeKey();
                this.tenantNameStrategy = basicConfig.getTenantNameStrategy();
                this.tenantNamePattern = basicConfig.getTenantNamePattern();
                this.customerNamePattern = basicConfig.getCustomerNamePattern();
                this.defaultDashboardName = basicConfig.getDefaultDashboardName();
                this.alwaysFullScreen = basicConfig.isAlwaysFullScreen();
            }
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OAuth2ClientRegistrationTemplate toData() {
        OAuth2ClientRegistrationTemplate clientRegistrationTemplate = new OAuth2ClientRegistrationTemplate();
        clientRegistrationTemplate.setId(new OAuth2ClientRegistrationTemplateId(id));
        clientRegistrationTemplate.setCreatedTime(createdTime);
        clientRegistrationTemplate.setAdditionalInfo(additionalInfo);

        clientRegistrationTemplate.setProviderId(providerId);
        clientRegistrationTemplate.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .type(type)
                        .basic(OAuth2BasicMapperConfig.builder()
                                .emailAttributeKey(emailAttributeKey)
                                .firstNameAttributeKey(firstNameAttributeKey)
                                .lastNameAttributeKey(lastNameAttributeKey)
                                .tenantNameStrategy(tenantNameStrategy)
                                .tenantNamePattern(tenantNamePattern)
                                .customerNamePattern(customerNamePattern)
                                .defaultDashboardName(defaultDashboardName)
                                .alwaysFullScreen(alwaysFullScreen)
                                .build()
                        )
                        .build()
        );
        clientRegistrationTemplate.setAuthorizationUri(authorizationUri);
        clientRegistrationTemplate.setAccessTokenUri(tokenUri);
        clientRegistrationTemplate.setScope(Arrays.asList(scope.split(",")));
        clientRegistrationTemplate.setUserInfoUri(userInfoUri);
        clientRegistrationTemplate.setUserNameAttributeName(userNameAttributeName);
        clientRegistrationTemplate.setJwkSetUri(jwkSetUri);
        clientRegistrationTemplate.setClientAuthenticationMethod(clientAuthenticationMethod);
        clientRegistrationTemplate.setComment(comment);
        clientRegistrationTemplate.setLoginButtonIcon(loginButtonIcon);
        clientRegistrationTemplate.setLoginButtonLabel(loginButtonLabel);
        clientRegistrationTemplate.setHelpLink(helpLink);
        return clientRegistrationTemplate;
    }
}
