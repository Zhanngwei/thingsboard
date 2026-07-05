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
 * 1. 类目的：`OAuth2ClientRegistrationTemplate` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ClientRegistrationTemplate` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
