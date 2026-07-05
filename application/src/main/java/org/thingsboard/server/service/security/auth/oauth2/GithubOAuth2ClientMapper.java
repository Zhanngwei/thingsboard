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
package org.thingsboard.server.service.security.auth.oauth2;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.dao.oauth2.OAuth2Configuration;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`GithubOAuth2ClientMapper` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Service(value = "githubOAuth2ClientMapper")
@Slf4j
@TbCoreComponent
public class GithubOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {
    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String EMAIL_URL_KEY = "emailUrl";

    /**
     * `AUTHORIZATION`常量，用于统一引用固定值。
     */
    private static final String AUTHORIZATION = "Authorization";

    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    /**
     * `oAuth2Configuration`，保存当前对象的配置选项。
     */
    @Autowired
    private OAuth2Configuration oAuth2Configuration;

    /**
     * 功能：获取用户。
     * 参数：
     * - `request`：请求对象。
     * - `token`：`token` 参数。
     * - `providerAccessToken`：`providerAccessToken` 参数。
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    @Override
    public SecurityUser getOrCreateUserByClientPrincipal(HttpServletRequest request, OAuth2AuthenticationToken token, String providerAccessToken, OAuth2Registration registration) {
        OAuth2MapperConfig config = registration.getMapperConfig();
        Map<String, String> githubMapperConfig = oAuth2Configuration.getGithubMapper();
        String email = getEmail(githubMapperConfig.get(EMAIL_URL_KEY), providerAccessToken);
        Map<String, Object> attributes = token.getPrincipal().getAttributes();
        OAuth2User oAuth2User = BasicMapperUtils.getOAuth2User(email, attributes, config);
        return getOrCreateSecurityUserFromOAuth2User(oAuth2User, registration);
    }

    /**
     * 功能：获取邮箱。
     * 参数：
     * - `emailUrl`：`emailUrl` 参数。
     * - `oauth2Token`：`oauth2Token` 参数。
     * 返回：文本结果。
     */
    private synchronized String getEmail(String emailUrl, String oauth2Token) {
        restTemplateBuilder = restTemplateBuilder.defaultHeader(AUTHORIZATION, "token " + oauth2Token);

        RestTemplate restTemplate = restTemplateBuilder.build();
        GithubEmailsResponse githubEmailsResponse;
        try {
            githubEmailsResponse = restTemplate.getForEntity(emailUrl, GithubEmailsResponse.class).getBody();
            if (githubEmailsResponse == null){
                throw new RuntimeException("Empty Github response!");
            }
        } catch (Exception e) {
            log.error("There was an error during connection to Github API", e);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
        Optional<String> emailOpt = githubEmailsResponse.stream()
                .filter(GithubEmailResponse::isPrimary)
                .map(GithubEmailResponse::getEmail)
                .findAny();
        if (emailOpt.isPresent()){
            return emailOpt.get();
        } else {
            log.error("Could not find primary email from {}.", githubEmailsResponse);
            throw new RuntimeException("Unable to login. Please contact your Administrator!");
        }
    }
    /**
     * 中文说明：
     * 1. 类目的：`GithubEmailsResponse` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
     * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Strategy。
     */
    private static class GithubEmailsResponse extends ArrayList<GithubEmailResponse> {}

    /**
     * 中文说明：
     * 1. 类目的：`GithubEmailResponse` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
     * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Strategy。
     */
    @Data
    @ToString
    private static class GithubEmailResponse {
        /**
         * 邮箱，用于展示或标识当前对象。
         */
        private String email;
        private boolean verified;
        /**
         * 是否满足`primary`条件。
         */
        private boolean primary;
        private String visibility;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`GithubOAuth2ClientMapper` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
