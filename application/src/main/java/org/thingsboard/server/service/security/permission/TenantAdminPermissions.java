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

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

@Component(value="tenantAdminPermissions")
/**
 * 中文说明：
 * 1. 类目的：`TenantAdminPermissions` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
public class TenantAdminPermissions extends AbstractPermissions {

    /**
     * 方法说明：
     * 1. 职责：执行 `TenantAdminPermissions` 对应的安全认证服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public TenantAdminPermissions() {
        super();
        put(Resource.ADMIN_SETTINGS, PermissionChecker.allowAllPermissionChecker);
        put(Resource.ALARM, tenantEntityPermissionChecker);
        put(Resource.ASSET, tenantEntityPermissionChecker);
        put(Resource.DEVICE, tenantEntityPermissionChecker);
        put(Resource.CUSTOMER, tenantEntityPermissionChecker);
        put(Resource.DASHBOARD, tenantEntityPermissionChecker);
        put(Resource.ENTITY_VIEW, tenantEntityPermissionChecker);
        put(Resource.TENANT, tenantPermissionChecker);
        put(Resource.RULE_CHAIN, tenantEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.DEVICE_PROFILE, tenantEntityPermissionChecker);
        put(Resource.ASSET_PROFILE, tenantEntityPermissionChecker);
        put(Resource.API_USAGE_STATE, tenantEntityPermissionChecker);
        put(Resource.TB_RESOURCE, tbResourcePermissionChecker);
        put(Resource.OTA_PACKAGE, tenantEntityPermissionChecker);
        put(Resource.EDGE, tenantEntityPermissionChecker);
        put(Resource.RPC, tenantEntityPermissionChecker);
        put(Resource.QUEUE, queuePermissionChecker);
        put(Resource.VERSION_CONTROL, PermissionChecker.allowAllPermissionChecker);
        put(Resource.NOTIFICATION, tenantEntityPermissionChecker);
    }

    public static final PermissionChecker tenantEntityPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            return true;
        }
    };

    private static final PermissionChecker tenantPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!super.hasPermission(user, operation, entityId, entity)) {
                        return false;
                    }
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (!user.getTenantId().equals(entityId)) {
                        return false;
                    }
                    return true;
                }

            };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (Authority.SYS_ADMIN.equals(userEntity.getAuthority())) {
                return false;
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!user.getTenantId().equals(userEntity.getTenantId())) {
                return false;
            }
            return true;
        }

    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            return true;
        }

    };

    private static final PermissionChecker tbResourcePermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            return true;
        }

    };

    private static final PermissionChecker queuePermissionChecker = new PermissionChecker() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return operation == Operation.READ;
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            return true;
        }

    };

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantAdminPermissions` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
