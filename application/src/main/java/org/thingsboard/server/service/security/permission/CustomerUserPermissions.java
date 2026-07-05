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
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

/**
 * 中文说明：
 * 1. 类目的：`CustomerUserPermissions` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component(value = "customerUserPermissions")
public class CustomerUserPermissions extends AbstractPermissions {

    /**
     * 功能：创建 `CustomerUserPermissions` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public CustomerUserPermissions() {
        super();
        put(Resource.ALARM, customerAlarmPermissionChecker);
        put(Resource.ASSET, customerEntityPermissionChecker);
        put(Resource.DEVICE, customerEntityPermissionChecker);
        put(Resource.CUSTOMER, customerPermissionChecker);
        put(Resource.DASHBOARD, customerDashboardPermissionChecker);
        put(Resource.ENTITY_VIEW, customerEntityPermissionChecker);
        put(Resource.USER, userPermissionChecker);
        put(Resource.WIDGETS_BUNDLE, widgetsPermissionChecker);
        put(Resource.WIDGET_TYPE, widgetsPermissionChecker);
        put(Resource.EDGE, customerEntityPermissionChecker);
        put(Resource.RPC, rpcPermissionChecker);
        put(Resource.DEVICE_PROFILE, profilePermissionChecker);
        put(Resource.ASSET_PROFILE, profilePermissionChecker);
        put(Resource.TB_RESOURCE, customerResourcePermissionChecker);
    }

    private static final PermissionChecker customerAlarmPermissionChecker = new PermissionChecker() {
        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!user.getTenantId().equals(entity.getTenantId())) {
                return false;
            }
            if (!(entity instanceof HasCustomerId)) {
                return false;
            }
            return user.getCustomerId().equals(((HasCustomerId) entity).getCustomerId());
        }
    };

    private static final PermissionChecker customerEntityPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_CREDENTIALS,
                    Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY, Operation.RPC_CALL, Operation.CLAIM_DEVICES,
                    Operation.WRITE, Operation.WRITE_ATTRIBUTES, Operation.WRITE_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {

                    if (!super.hasPermission(user, operation, entityId, entity)) {
                        return false;
                    }
                    if (!user.getTenantId().equals(entity.getTenantId())) {
                        return false;
                    }
                    if (!(entity instanceof HasCustomerId)) {
                        return false;
                    }
                    return operation.equals(Operation.CLAIM_DEVICES) || user.getCustomerId().equals(((HasCustomerId) entity).getCustomerId());
                }
            };

    private static final PermissionChecker customerPermissionChecker =
            new PermissionChecker.GenericPermissionChecker(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
                    if (!super.hasPermission(user, operation, entityId, entity)) {
                        return false;
                    }
                    return user.getCustomerId().equals(entityId);
                }

            };

    private static final PermissionChecker customerResourcePermissionChecker =
            new PermissionChecker<TbResourceId, TbResourceInfo>() {

                @Override
                @SuppressWarnings("unchecked")
                public boolean hasPermission(SecurityUser user, Operation operation, TbResourceId resourceId, TbResourceInfo resource) {
                    if (operation != Operation.READ) {
                        return false;
                    }
                    if (resource.getResourceType() == null || !resource.getResourceType().isCustomerAccess()) {
                        return false;
                    }
                    if (resource.getTenantId() == null || resource.getTenantId().isNullUid()) {
                        return true;
                    }
                    return user.getTenantId().equals(resource.getTenantId());
                }

            };

    private static final PermissionChecker customerDashboardPermissionChecker =
            new PermissionChecker.GenericPermissionChecker<DashboardId, DashboardInfo>(Operation.READ, Operation.READ_ATTRIBUTES, Operation.READ_TELEMETRY) {

                @Override
                public boolean hasPermission(SecurityUser user, Operation operation, DashboardId dashboardId, DashboardInfo dashboard) {

                    if (!super.hasPermission(user, operation, dashboardId, dashboard)) {
                        return false;
                    }
                    if (!user.getTenantId().equals(dashboard.getTenantId())) {
                        return false;
                    }
                    return dashboard.isAssignedToCustomer(user.getCustomerId());
                }

            };

    private static final PermissionChecker userPermissionChecker = new PermissionChecker<UserId, User>() {

        @Override
        public boolean hasPermission(SecurityUser user, Operation operation, UserId userId, User userEntity) {
            if (!Authority.CUSTOMER_USER.equals(userEntity.getAuthority())) {
                return false;
            }

            if (!user.getCustomerId().equals(userEntity.getCustomerId())) {
                return false;
            }

            if (Operation.READ.equals(operation)) {
                return true;
            }

            return user.getId().equals(userId);
        }

    };

    private static final PermissionChecker widgetsPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }

    };

    private static final PermissionChecker rpcPermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }
    };

    private static final PermissionChecker profilePermissionChecker = new PermissionChecker.GenericPermissionChecker(Operation.READ) {

        @Override
        @SuppressWarnings("unchecked")
        public boolean hasPermission(SecurityUser user, Operation operation, EntityId entityId, HasTenantId entity) {
            if (!super.hasPermission(user, operation, entityId, entity)) {
                return false;
            }
            if (entity.getTenantId() == null || entity.getTenantId().isNullUid()) {
                return true;
            }
            return user.getTenantId().equals(entity.getTenantId());
        }
    };
}

/*
 * 本类总结：
 * 1. 核心职责：`CustomerUserPermissions` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
