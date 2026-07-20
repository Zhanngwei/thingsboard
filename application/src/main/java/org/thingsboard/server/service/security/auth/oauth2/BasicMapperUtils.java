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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.oauth2.OAuth2MapperConfig;
import org.thingsboard.server.dao.oauth2.OAuth2User;

import java.util.Map;

/**
 * 中文说明：
 * 1. `BasicMapperUtils` 是 ThingsBoard Application 中处理 `Basic Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class BasicMapperUtils {
    /**
     * `START_PLACEHOLDER_PREFIX`常量，用于统一引用固定值。
     */
    private static final String START_PLACEHOLDER_PREFIX = "%{";
    private static final String END_PLACEHOLDER_PREFIX = "}";

    /**
     * 功能：获取用户。
     * 参数：
     * - `email`：`email` 参数。
     * - `attributes`：键值映射。
     * - `config`：配置对象。
     * 返回：处理结果。
     */
    public static OAuth2User getOAuth2User(String email, Map<String, Object> attributes, OAuth2MapperConfig config) {
        OAuth2User oauth2User = new OAuth2User();
        oauth2User.setEmail(email);
        oauth2User.setTenantName(getTenantName(email, attributes, config));
        if (!StringUtils.isEmpty(config.getBasic().getLastNameAttributeKey())) {
            String lastName = getStringAttributeByKey(attributes, config.getBasic().getLastNameAttributeKey());
            oauth2User.setLastName(lastName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getFirstNameAttributeKey())) {
            String firstName = getStringAttributeByKey(attributes, config.getBasic().getFirstNameAttributeKey());
            oauth2User.setFirstName(firstName);
        }
        if (!StringUtils.isEmpty(config.getBasic().getCustomerNamePattern())) {
            StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
            String customerName = sub.replace(config.getBasic().getCustomerNamePattern());
            oauth2User.setCustomerName(customerName);
        }
        oauth2User.setAlwaysFullScreen(config.getBasic().isAlwaysFullScreen());
        if (!StringUtils.isEmpty(config.getBasic().getDefaultDashboardName())) {
            oauth2User.setDefaultDashboardName(config.getBasic().getDefaultDashboardName());
        }
        return oauth2User;
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `email`：`email` 参数。
     * - `attributes`：键值映射。
     * - `config`：配置对象。
     * 返回：文本结果。
     */
    public static String getTenantName(String email, Map<String, Object> attributes, OAuth2MapperConfig config) {
        switch (config.getBasic().getTenantNameStrategy()) {
            case EMAIL:
                return email;
            case DOMAIN:
                return email.substring(email .indexOf("@") + 1);
            case CUSTOM:
                StrSubstitutor sub = new StrSubstitutor(attributes, START_PLACEHOLDER_PREFIX, END_PLACEHOLDER_PREFIX);
                return sub.replace(config.getBasic().getTenantNamePattern());
            default:
                throw new RuntimeException("Tenant Name Strategy with type " + config.getBasic().getTenantNameStrategy() + " is not supported!");
        }
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `attributes`：键值映射。
     * - `key`：键。
     * 返回：文本结果。
     */
    public static String getStringAttributeByKey(Map<String, Object> attributes, String key) {
        String result = null;
        try {
            result = (String) attributes.get(key);
        } catch (Exception e) {
            log.warn("Can't convert attribute to String by key " + key);
        }
        return result;
    }
}
