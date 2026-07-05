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
 * 1. 类目的：`CookieUtils` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
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

/*
 * 本类总结：
 * 1. 核心职责：`CookieUtils` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
