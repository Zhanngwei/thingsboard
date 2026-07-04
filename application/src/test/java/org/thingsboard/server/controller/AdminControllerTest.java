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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
/**
 * 中文说明：
 * 1. 类目的：`AdminControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class AdminControllerTest extends AbstractControllerTest {
    final JwtSettings defaultJwtSettings = new JwtSettings(9000, 604800, "thingsboard.io", "thingsboardDefaultSigningKey");

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testFindAdminSettingsByKey` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testFindAdminSettingsByKey() throws Exception {
        loginSysAdmin();
        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("general")))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://localhost:8080")));

        doGet("/api/admin/settings/mail")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.key", is("mail")))
                .andExpect(jsonPath("$.jsonValue.smtpProtocol", is("smtp")))
                .andExpect(jsonPath("$.jsonValue.smtpHost", is("localhost")))
                .andExpect(jsonPath("$.jsonValue.smtpPort", is("25")));

        doGet("/api/admin/settings/unknown")
                .andExpect(status().isNotFound());

    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAdminSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAdminSettings() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/general", AdminSettings.class);

        JsonNode jsonValue = adminSettings.getJsonValue();
        ((ObjectNode) jsonValue).put("baseUrl", "http://myhost.org");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings).andExpect(status().isOk());

        doGet("/api/admin/settings/general")
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.jsonValue.baseUrl", is("http://myhost.org")));

        ((ObjectNode) jsonValue).put("baseUrl", "http://localhost:8080");
        adminSettings.setJsonValue(jsonValue);

        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isOk());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSaveAdminSettingsWithEmptyKey` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSaveAdminSettingsWithEmptyKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey(null);
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Key should be specified")));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testChangeAdminSettingsKey` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testChangeAdminSettingsKey() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        adminSettings.setKey("newKey");
        doPost("/api/admin/settings", adminSettings)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("is prohibited")));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSendTestMail` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSendTestMail() throws Exception {
        Mockito.doNothing().when(mailService).sendTestMail(any(), anyString());
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        doPost("/api/admin/settings/testMail", adminSettings)
                .andExpect(status().isOk());
        Mockito.verify(mailService).sendTestMail(Mockito.any(), Mockito.anyString());
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testSendTestMailTimeout` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testSendTestMailTimeout() throws Exception {
        loginSysAdmin();
        AdminSettings adminSettings = doGet("/api/admin/settings/mail", AdminSettings.class);
        ObjectNode objectNode = JacksonUtil.fromString(adminSettings.getJsonValue().toString(), ObjectNode.class);

        objectNode.put("smtpHost", "mail.gandi.net");
        objectNode.put("timeout", 1_000);
        objectNode.put("username", "username");
        objectNode.put("password", "password");

        adminSettings.setJsonValue(objectNode);

        doPost("/api/admin/settings/testMail", adminSettings).andExpect(status().is5xxServerError());
        Mockito.verify(mailService).sendTestMail(Mockito.any(), Mockito.anyString());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `resetJwtSettingsToDefault` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void resetJwtSettingsToDefault() throws Exception {
        loginSysAdmin();
        doPost("/api/admin/jwtSettings", defaultJwtSettings).andExpect(status().isOk()); // jwt test scenarios are always started from
        loginTenantAdmin();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testGetAndSaveDefaultJwtSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testGetAndSaveDefaultJwtSettings() throws Exception {
        JwtSettings jwtSettings;
        loginSysAdmin();

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        resetJwtSettingsToDefault();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testCreateJwtSettings` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testCreateJwtSettings() throws Exception {
        loginSysAdmin();

        JwtSettings jwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(defaultJwtSettings);

        jwtSettings.setTokenSigningKey(Base64.getEncoder().encodeToString(
                RandomStringUtils.randomAlphanumeric(256 / Byte.SIZE).getBytes(StandardCharsets.UTF_8)));

        doPost("/api/admin/jwtSettings", jwtSettings).andExpect(status().isOk());

        doGet("/api/admin/jwtSettings").andExpect(status().isUnauthorized()); //the old JWT token does not work after signing key was changed!

        loginSysAdmin();
        JwtSettings newJwtSettings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        assertThat(jwtSettings).isEqualTo(newJwtSettings);

        resetJwtSettingsToDefault();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AdminControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
