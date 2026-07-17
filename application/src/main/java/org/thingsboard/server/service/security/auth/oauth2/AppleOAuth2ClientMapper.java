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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `AppleOAuth2ClientMapper` 是 ThingsBoard Application 中转换 `Apple O Auth2` 数据结构的适配组件。
 * 2. 它把输入对象、协议内容或持久化数据转换为目标模型。
 * 3. 转换过程负责字段映射、格式解析以及必要的默认值处理。
 * 4. 直接依赖的类型边界包括 `AbstractOAuth2ClientMapper`、`OAuth2ClientMapper`。
 * 5. 独立转换器可以避免不同模块重复编写并逐渐分叉的映射逻辑。
 * 6. 阅读时重点关注字段对应关系、空值处理和不兼容输入的处理方式。
 */
@Service(value = "appleOAuth2ClientMapper")
@Slf4j
@TbCoreComponent
public class AppleOAuth2ClientMapper extends AbstractOAuth2ClientMapper implements OAuth2ClientMapper {

    /**
     * 用户常量，用于统一引用固定值。
     */
    private static final String USER = "user";
    private static final String NAME = "name";
    /**
     * 名称常量，用于统一引用固定值。
     */
    private static final String FIRST_NAME = "firstName";
    private static final String LAST_NAME = "lastName";
    /**
     * 邮箱常量，用于统一引用固定值。
     */
    private static final String EMAIL = "email";

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
        Map<String, Object> attributes = updateAttributesFromRequestParams(request, token.getPrincipal().getAttributes());
        String email = BasicMapperUtils.getStringAttributeByKey(attributes, config.getBasic().getEmailAttributeKey());
        OAuth2User oauth2User = BasicMapperUtils.getOAuth2User(email, attributes, config);

        return getOrCreateSecurityUserFromOAuth2User(oauth2User, registration);
    }

    /**
     * 功能：更新请求。
     * 参数：
     * - `request`：请求对象。
     * - `attributes`：键值映射。
     * 返回：处理结果。
     */
    private static Map<String, Object> updateAttributesFromRequestParams(HttpServletRequest request, Map<String, Object> attributes) {
        Map<String, Object> updated = attributes;
        MultiValueMap<String, String> params = toMultiMap(request.getParameterMap());
        String userValue = params.getFirst(USER);
        if (StringUtils.hasText(userValue)) {
            JsonNode user = null;
            try {
                user = JacksonUtil.toJsonNode(userValue);
            } catch (Exception e) {}
            if (user != null) {
                updated = new HashMap<>(attributes);
                if (user.has(NAME)) {
                    JsonNode name = user.get(NAME);
                    if (name.isObject()) {
                        JsonNode firstName = name.get(FIRST_NAME);
                        if (firstName != null && firstName.isTextual()) {
                            updated.put(FIRST_NAME, firstName.asText());
                        }
                        JsonNode lastName = name.get(LAST_NAME);
                        if (lastName != null && lastName.isTextual()) {
                            updated.put(LAST_NAME, lastName.asText());
                        }
                    }
                }
                if (user.has(EMAIL)) {
                    JsonNode email = user.get(EMAIL);
                    if (email != null && email.isTextual()) {
                        updated.put(EMAIL, email.asText());
                    }
                }
            }
        }
        return updated;
    }

    /**
     * 功能：执行 `toMultiMap` 对应的处理。
     * 参数：
     * - `map`：键值映射。
     * 返回：处理结果。
     */
    private static MultiValueMap<String, String> toMultiMap(Map<String, String[]> map) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(map.size());
        map.forEach((key, values) -> {
            if (values.length > 0) {
                for (String value : values) {
                    params.add(key, value);
                }
            }
        });
        return params;
    }
}
