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
 * 1. `GithubOAuth2ClientMapper` 是 ThingsBoard Application 中转换 `Github O Auth2` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 直接依赖的类型边界包括 `AbstractOAuth2ClientMapper`、`OAuth2ClientMapper`。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
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
     * 1. `GithubEmailsResponse` 是 ThingsBoard Application 中承载响应信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 直接依赖的类型边界包括 `ArrayList`。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    private static class GithubEmailsResponse extends ArrayList<GithubEmailResponse> {}

    /**
     * 中文说明：
     * 1. `GithubEmailResponse` 是 ThingsBoard Application 中承载响应信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
