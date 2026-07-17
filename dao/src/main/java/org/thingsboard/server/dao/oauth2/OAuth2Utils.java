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
 * 1. `OAuth2Utils` 是 ThingsBoard DAO 中处理 `O Auth2 Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
