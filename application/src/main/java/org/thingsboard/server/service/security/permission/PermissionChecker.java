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
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`PermissionChecker` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public interface PermissionChecker<I extends EntityId, T extends HasTenantId> {

    /**
     * 功能：判断`Permission`。
     * 参数：
     * - `user`：`user` 参数。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    default boolean hasPermission(SecurityUser user, Operation operation) {
        return false;
    }

    /**
     * 功能：判断`Permission`。
     * 参数：
     * - `user`：`user` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * 返回：判断结果。
     */
    default boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
        return false;
    }

    /**
     * 中文说明：
     * 1. 类目的：`GenericPermissionChecker` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
     * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Strategy。
     */
    public class GenericPermissionChecker<I extends EntityId, T extends HasTenantId> implements PermissionChecker<I,T> {

        /**
         * `allowedOperations`集合，用于去重保存或快速判断对象是否存在。
         */
        private final Set<Operation> allowedOperations;

        /**
         * 功能：创建 `PermissionChecker` 实例，并初始化必要字段。
         * 参数：
         * - `operations`：`operations` 参数。
         * 返回：新创建的对象实例。
         */
        public GenericPermissionChecker(Operation... operations) {
            allowedOperations = new HashSet<Operation>(Arrays.asList(operations));
        }

        /**
         * 功能：判断`Permission`。
         * 参数：
         * - `user`：`user` 参数。
         * - `operation`：`operation` 参数。
         * 返回：判断结果。
         */
        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }

        /**
         * 功能：判断`Permission`。
         * 参数：
         * - `user`：`user` 参数。
         * - `operation`：`operation` 参数。
         * - `entityId`：实体IDID。
         * - `entity`：实体对象。
         * 返回：判断结果。
         */
        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, I entityId, T entity) {
            return allowedOperations.contains(Operation.ALL) || allowedOperations.contains(operation);
        }
    }

    public static PermissionChecker denyAllPermissionChecker = new PermissionChecker() {};

    public static PermissionChecker allowAllPermissionChecker = new PermissionChecker<EntityId, HasTenantId>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation) {
            return true;
        }

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            return true;
        }
    };


}

/*
 * 本类总结：
 * 1. 核心职责：`PermissionChecker` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
