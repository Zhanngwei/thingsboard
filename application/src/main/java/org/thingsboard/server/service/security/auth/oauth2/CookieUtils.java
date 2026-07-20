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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.jackson2.SecurityJackson2Modules;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `CookieUtils` 是 ThingsBoard Application 中处理 `Cookie Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class CookieUtils {

    /**
     * 映射器常量，用于统一引用固定值。
     */
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        ClassLoader loader = CookieUtils.class.getClassLoader();
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModules(SecurityJackson2Modules.getModules(loader));
    }

    /**
     * 功能：获取`Cookie`。
     * 参数：
     * - `request`：请求对象。
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * 功能：保存或创建`Cookie`。
     * 参数：
     * - `response`：响应对象。
     * - `name`：名称。
     * - `value`：值。
     * - `maxAge`：`maxAge` 参数。
     * 返回：无。
     */
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
     * 功能：删除或清理`Cookie`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * - `name`：名称。
     * 返回：无。
     */
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    /**
     * 功能：执行 `serialize` 对应的处理。
     * 参数：
     * - `object`：`object` 参数。
     * 返回：文本结果。
     */
    public static String serialize(Object object) {
        try {
            return Base64.getUrlEncoder()
                    .encodeToString(OBJECT_MAPPER.writeValueAsBytes(object));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + object + " cannot be transformed to a String", e);
        }
    }

    /**
     * 功能：执行 `deserialize` 对应的处理。
     * 参数：
     * - `cookie`：`cookie` 参数。
     * - `cls`：`cls` 参数。
     * 返回：处理结果。
     */
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(cookie.getValue());
        try {
            return OBJECT_MAPPER.readValue(decodedBytes, cls);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + Arrays.toString(decodedBytes) + " cannot be transformed to Json object", e);
        }
    }
}
