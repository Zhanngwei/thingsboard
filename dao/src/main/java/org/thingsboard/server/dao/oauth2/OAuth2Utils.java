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
package org.thingsboard.server.dao.oauth2;

import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2ClientInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.OAuth2DomainInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.common.data.oauth2.OAuth2Mobile;
import org.thingsboard.server.common.data.oauth2.OAuth2MobileInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Params;
import org.thingsboard.server.common.data.oauth2.OAuth2ParamsInfo;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.OAuth2RegistrationInfo;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2Utils` 是 ThingsBoard DAO 模块 中的OAuth2 配置持久化类型，用于维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 生命周期：由安全配置加载、登录回调或管理 API 调用，配置变更后通常需要缓存刷新。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
public class OAuth2Utils {
    /**
     * 路径常量，用于统一引用固定值。
     */
    public static final String OAUTH2_AUTHORIZATION_PATH_TEMPLATE = "/oauth2/authorization/%s";

    /**
     * 功能：执行 `toClientInfo` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    public static OAuth2ClientInfo toClientInfo(OAuth2Registration registration) {
        OAuth2ClientInfo client = new OAuth2ClientInfo();
        client.setName(registration.getLoginButtonLabel());
        client.setUrl(String.format(OAUTH2_AUTHORIZATION_PATH_TEMPLATE, registration.getUuidId().toString()));
        client.setIcon(registration.getLoginButtonIcon());
        return client;
    }

    /**
     * 功能：执行 `toOAuth2ParamsInfo` 对应的处理。
     * 参数：
     * - `registrations`：数据列表。
     * - `domains`：数据列表。
     * - `mobiles`：数据列表。
     * 返回：处理结果。
     */
    public static OAuth2ParamsInfo toOAuth2ParamsInfo(List<OAuth2Registration> registrations, List<OAuth2Domain> domains, List<OAuth2Mobile> mobiles) {
        OAuth2ParamsInfo oauth2ParamsInfo = new OAuth2ParamsInfo();
        oauth2ParamsInfo.setClientRegistrations(registrations.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2RegistrationInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setDomainInfos(domains.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2DomainInfo).collect(Collectors.toList()));
        oauth2ParamsInfo.setMobileInfos(mobiles.stream().sorted(Comparator.comparing(BaseData::getUuidId)).map(OAuth2Utils::toOAuth2MobileInfo).collect(Collectors.toList()));
        return oauth2ParamsInfo;
    }

    /**
     * 功能：执行 `toOAuth2RegistrationInfo` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    public static OAuth2RegistrationInfo toOAuth2RegistrationInfo(OAuth2Registration registration) {
        return OAuth2RegistrationInfo.builder()
                .mapperConfig(registration.getMapperConfig())
                .clientId(registration.getClientId())
                .clientSecret(registration.getClientSecret())
                .authorizationUri(registration.getAuthorizationUri())
                .accessTokenUri(registration.getAccessTokenUri())
                .scope(registration.getScope())
                .platforms(registration.getPlatforms())
                .userInfoUri(registration.getUserInfoUri())
                .userNameAttributeName(registration.getUserNameAttributeName())
                .jwkSetUri(registration.getJwkSetUri())
                .clientAuthenticationMethod(registration.getClientAuthenticationMethod())
                .loginButtonLabel(registration.getLoginButtonLabel())
                .loginButtonIcon(registration.getLoginButtonIcon())
                .additionalInfo(registration.getAdditionalInfo())
                .build();
    }

    /**
     * 功能：执行 `toOAuth2DomainInfo` 对应的处理。
     * 参数：
     * - `domain`：`domain` 参数。
     * 返回：处理结果。
     */
    public static OAuth2DomainInfo toOAuth2DomainInfo(OAuth2Domain domain) {
        return OAuth2DomainInfo.builder()
                .name(domain.getDomainName())
                .scheme(domain.getDomainScheme())
                .build();
    }

    /**
     * 功能：执行 `toOAuth2MobileInfo` 对应的处理。
     * 参数：
     * - `mobile`：`mobile` 参数。
     * 返回：处理结果。
     */
    public static OAuth2MobileInfo toOAuth2MobileInfo(OAuth2Mobile mobile) {
        return OAuth2MobileInfo.builder()
                .pkgName(mobile.getPkgName())
                .appSecret(mobile.getAppSecret())
                .build();
    }

    /**
     * 功能：执行 `infoToOAuth2Params` 对应的处理。
     * 参数：
     * - `oauth2Info`：`oauth2Info` 参数。
     * 返回：处理结果。
     */
    public static OAuth2Params infoToOAuth2Params(OAuth2Info oauth2Info) {
        OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setEnabled(oauth2Info.isEnabled());
        oauth2Params.setTenantId(TenantId.SYS_TENANT_ID);
        return oauth2Params;
    }

    /**
     * 功能：执行 `toOAuth2Registration` 对应的处理。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * - `registrationInfo`：`registrationInfo` 参数。
     * 返回：处理结果。
     */
    public static OAuth2Registration toOAuth2Registration(OAuth2ParamsId oauth2ParamsId, OAuth2RegistrationInfo registrationInfo) {
        OAuth2Registration registration = new OAuth2Registration();
        registration.setOauth2ParamsId(oauth2ParamsId);
        registration.setMapperConfig(registrationInfo.getMapperConfig());
        registration.setClientId(registrationInfo.getClientId());
        registration.setClientSecret(registrationInfo.getClientSecret());
        registration.setAuthorizationUri(registrationInfo.getAuthorizationUri());
        registration.setAccessTokenUri(registrationInfo.getAccessTokenUri());
        registration.setScope(registrationInfo.getScope());
        registration.setPlatforms(registrationInfo.getPlatforms());
        registration.setUserInfoUri(registrationInfo.getUserInfoUri());
        registration.setUserNameAttributeName(registrationInfo.getUserNameAttributeName());
        registration.setJwkSetUri(registrationInfo.getJwkSetUri());
        registration.setClientAuthenticationMethod(registrationInfo.getClientAuthenticationMethod());
        registration.setLoginButtonLabel(registrationInfo.getLoginButtonLabel());
        registration.setLoginButtonIcon(registrationInfo.getLoginButtonIcon());
        registration.setAdditionalInfo(registrationInfo.getAdditionalInfo());
        return registration;
    }

    /**
     * 功能：执行 `toOAuth2Domain` 对应的处理。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * - `domainInfo`：`domainInfo` 参数。
     * 返回：处理结果。
     */
    public static OAuth2Domain toOAuth2Domain(OAuth2ParamsId oauth2ParamsId, OAuth2DomainInfo domainInfo) {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setOauth2ParamsId(oauth2ParamsId);
        domain.setDomainName(domainInfo.getName());
        domain.setDomainScheme(domainInfo.getScheme());
        return domain;
    }

    /**
     * 功能：执行 `toOAuth2Mobile` 对应的处理。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * - `mobileInfo`：`mobileInfo` 参数。
     * 返回：处理结果。
     */
    public static OAuth2Mobile toOAuth2Mobile(OAuth2ParamsId oauth2ParamsId, OAuth2MobileInfo mobileInfo) {
        OAuth2Mobile mobile = new OAuth2Mobile();
        mobile.setOauth2ParamsId(oauth2ParamsId);
        mobile.setPkgName(mobileInfo.getPkgName());
        mobile.setAppSecret(mobileInfo.getAppSecret());
        return mobile;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2Utils` 在 ThingsBoard DAO 模块 中承担OAuth2 配置持久化类型职责，核心目的是维护 OAuth2 客户端、域名映射、登录配置模板和外部用户信息的数据库状态。
 * 2. 核心流程：读取或保存 OAuth2 配置后交给安全模块完成认证、用户映射和租户解析。
 * 3. 关键依赖：主要依赖或协作对象包括OAuth2Service、Security、UserService、TenantService、缓存和登录流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
