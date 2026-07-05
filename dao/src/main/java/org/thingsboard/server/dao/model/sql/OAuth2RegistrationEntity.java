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
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.OAuth2RegistrationId;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.common.data.oauth2.OAuth2BasicMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2CustomMapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;
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
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2RegistrationEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.OAUTH2_REGISTRATION_TABLE_NAME)
public class OAuth2RegistrationEntity extends BaseSqlEntity<OAuth2Registration> {

    /**
     * `oauth2ParamsId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;
    /**
     * 客户端ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.OAUTH2_CLIENT_ID_PROPERTY)
    private String clientId;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    @Column(name = ModelConstants.OAUTH2_CLIENT_SECRET_PROPERTY)
    private String clientSecret;
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
     * `platforms` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_PLATFORMS_PROPERTY)
    private String platforms;
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
     * 显示标签，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY)
    private String loginButtonLabel;
    /**
     * `loginButtonIcon` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.OAUTH2_LOGIN_BUTTON_ICON_PROPERTY)
    private String loginButtonIcon;
    /**
     * 当前操作是否被允许。
     */
    @Column(name = ModelConstants.OAUTH2_ALLOW_USER_CREATION_PROPERTY)
    private Boolean allowUserCreation;
    /**
     * 是否满足用户条件。
     */
    @Column(name = ModelConstants.OAUTH2_ACTIVATE_USER_PROPERTY)
    private Boolean activateUser;
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
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Column(name = ModelConstants.OAUTH2_MAPPER_URL_PROPERTY)
    private String url;
    /**
     * 用户名，用于认证或安全校验。
     */
    @Column(name = ModelConstants.OAUTH2_MAPPER_USERNAME_PROPERTY)
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    @Column(name = ModelConstants.OAUTH2_MAPPER_PASSWORD_PROPERTY)
    private String password;
    /**
     * 是否满足令牌条件。
     */
    @Column(name = ModelConstants.OAUTH2_MAPPER_SEND_TOKEN_PROPERTY)
    private Boolean sendToken;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.OAUTH2_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `OAuth2RegistrationEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OAuth2RegistrationEntity() {
        super();
    }

    /**
     * 功能：创建 `OAuth2RegistrationEntity` 实例，并初始化必要字段。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：新创建的对象实例。
     */
    public OAuth2RegistrationEntity(OAuth2Registration registration) {
        if (registration.getId() != null) {
            this.setUuid(registration.getId().getId());
        }
        this.setCreatedTime(registration.getCreatedTime());
        if (registration.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = registration.getOauth2ParamsId().getId();
        }
        this.clientId = registration.getClientId();
        this.clientSecret = registration.getClientSecret();
        this.authorizationUri = registration.getAuthorizationUri();
        this.tokenUri = registration.getAccessTokenUri();
        this.scope = registration.getScope().stream().reduce((result, element) -> result + "," + element).orElse("");
        this.platforms = registration.getPlatforms() != null ? registration.getPlatforms().stream().map(Enum::name).reduce((result, element) -> result + "," + element).orElse("") : "";
        this.userInfoUri = registration.getUserInfoUri();
        this.userNameAttributeName = registration.getUserNameAttributeName();
        this.jwkSetUri = registration.getJwkSetUri();
        this.clientAuthenticationMethod = registration.getClientAuthenticationMethod();
        this.loginButtonLabel = registration.getLoginButtonLabel();
        this.loginButtonIcon = registration.getLoginButtonIcon();
        this.additionalInfo = registration.getAdditionalInfo();
        OAuth2MapperConfig mapperConfig = registration.getMapperConfig();
        if (mapperConfig != null) {
            this.allowUserCreation = mapperConfig.isAllowUserCreation();
            this.activateUser = mapperConfig.isActivateUser();
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
            OAuth2CustomMapperConfig customConfig = mapperConfig.getCustom();
            if (customConfig != null) {
                this.url = customConfig.getUrl();
                this.username = customConfig.getUsername();
                this.password = customConfig.getPassword();
                this.sendToken = customConfig.isSendToken();
            }
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OAuth2Registration toData() {
        OAuth2Registration registration = new OAuth2Registration();
        registration.setId(new OAuth2RegistrationId(id));
        registration.setCreatedTime(createdTime);
        registration.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        registration.setAdditionalInfo(additionalInfo);
        registration.setMapperConfig(
                OAuth2MapperConfig.builder()
                        .allowUserCreation(allowUserCreation)
                        .activateUser(activateUser)
                        .type(type)
                        .basic(
                                (type == MapperType.BASIC || type == MapperType.GITHUB || type == MapperType.APPLE) ?
                                        OAuth2BasicMapperConfig.builder()
                                                .emailAttributeKey(emailAttributeKey)
                                                .firstNameAttributeKey(firstNameAttributeKey)
                                                .lastNameAttributeKey(lastNameAttributeKey)
                                                .tenantNameStrategy(tenantNameStrategy)
                                                .tenantNamePattern(tenantNamePattern)
                                                .customerNamePattern(customerNamePattern)
                                                .defaultDashboardName(defaultDashboardName)
                                                .alwaysFullScreen(alwaysFullScreen)
                                                .build()
                                        : null
                        )
                        .custom(
                                type == MapperType.CUSTOM ?
                                        OAuth2CustomMapperConfig.builder()
                                                .url(url)
                                                .username(username)
                                                .password(password)
                                                .sendToken(sendToken)
                                                .build()
                                        : null
                        )
                        .build()
        );
        registration.setClientId(clientId);
        registration.setClientSecret(clientSecret);
        registration.setAuthorizationUri(authorizationUri);
        registration.setAccessTokenUri(tokenUri);
        registration.setScope(Arrays.asList(scope.split(",")));
        registration.setPlatforms(StringUtils.isNotEmpty(platforms) ? Arrays.stream(platforms.split(","))
                .map(str -> PlatformType.valueOf(str)).collect(Collectors.toList()) : Collections.emptyList());
        registration.setUserInfoUri(userInfoUri);
        registration.setUserNameAttributeName(userNameAttributeName);
        registration.setJwkSetUri(jwkSetUri);
        registration.setClientAuthenticationMethod(clientAuthenticationMethod);
        registration.setLoginButtonLabel(loginButtonLabel);
        registration.setLoginButtonIcon(loginButtonIcon);
        return registration;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2RegistrationEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
