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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.MapperType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2ClientMapperProvider` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component
@Slf4j
@TbCoreComponent
public class OAuth2ClientMapperProvider {

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("basicOAuth2ClientMapper")
    private OAuth2ClientMapper basicOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("customOAuth2ClientMapper")
    private OAuth2ClientMapper customOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("githubOAuth2ClientMapper")
    private OAuth2ClientMapper githubOAuth2ClientMapper;

    /**
     * 客户端映射关系，用于按键查找对应值。
     */
    @Autowired
    @Qualifier("appleOAuth2ClientMapper")
    private OAuth2ClientMapper appleOAuth2ClientMapper;

    /**
     * 功能：获取类型。
     * 参数：
     * - `oauth2MapperType`：类型。
     * 返回：键值映射结果。
     */
    public OAuth2ClientMapper getOAuth2ClientMapperByType(MapperType oauth2MapperType) {
        switch (oauth2MapperType) {
            case CUSTOM:
                return customOAuth2ClientMapper;
            case BASIC:
                return basicOAuth2ClientMapper;
            case GITHUB:
                return githubOAuth2ClientMapper;
            case APPLE:
                return appleOAuth2ClientMapper;
            default:
                throw new RuntimeException("OAuth2ClientRegistrationMapper with type " + oauth2MapperType + " is not supported!");
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2ClientMapperProvider` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
