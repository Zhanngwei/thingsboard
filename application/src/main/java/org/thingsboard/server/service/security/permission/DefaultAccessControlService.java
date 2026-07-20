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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `DefaultAccessControlService` 是 ThingsBoard Application 中负责 `Access Control` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AccessControlService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
public class DefaultAccessControlService implements AccessControlService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";

    private final Map<Authority, Permissions> authorityPermissions = new HashMap<>();

    /**
     * 功能：创建 `DefaultAccessControlService` 实例，并初始化必要字段。
     * 参数：
     * - `sysAdminPermissions`：`sysAdminPermissions` 参数。
     * - `tenantAdminPermissions`：租户信息或租户标识。
     * - `customerUserPermissions`：`customerUserPermissions` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultAccessControlService(
            @Qualifier("sysAdminPermissions") Permissions sysAdminPermissions,
            @Qualifier("tenantAdminPermissions") Permissions tenantAdminPermissions,
            @Qualifier("customerUserPermissions") Permissions customerUserPermissions) {
        authorityPermissions.put(Authority.SYS_ADMIN, sysAdminPermissions);
        authorityPermissions.put(Authority.TENANT_ADMIN, tenantAdminPermissions);
        authorityPermissions.put(Authority.CUSTOMER_USER, customerUserPermissions);
    }

    /**
     * 功能：校验`Permission`。
     * 参数：
     * - `user`：`user` 参数。
     * - `resource`：`resource` 参数。
     * - `operation`：`operation` 参数。
     * 返回：无。
     */
    @Override
    public void checkPermission(SecurityUser user, Resource resource, Operation operation) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation)) {
            permissionDenied();
        }
    }

    /**
     * 功能：校验`Permission`。
     * 参数：
     * - `user`：`user` 参数。
     * - `resource`：`resource` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    @SuppressWarnings("unchecked")
    public <I extends EntityId, T extends HasTenantId> void checkPermission(SecurityUser user, Resource resource,
                                                                                            Operation operation, I entityId, T entity) throws ThingsboardException {
        PermissionChecker permissionChecker = getPermissionChecker(user.getAuthority(), resource);
        if (!permissionChecker.hasPermission(user, operation, entityId, entity)) {
            permissionDenied();
        }
    }

    /**
     * 功能：获取`Permission Checker`。
     * 参数：
     * - `authority`：`authority` 参数。
     * - `resource`：`resource` 参数。
     * 返回：处理结果。
     */
    private PermissionChecker getPermissionChecker(Authority authority, Resource resource) throws ThingsboardException {
        Permissions permissions = authorityPermissions.get(authority);
        if (permissions == null) {
            permissionDenied();
        }
        Optional<PermissionChecker> permissionChecker = permissions.getPermissionChecker(resource);
        if (!permissionChecker.isPresent()) {
            permissionDenied();
        }
        return permissionChecker.get();
    }

    /**
     * 功能：执行 `permissionDenied` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                ThingsboardErrorCode.PERMISSION_DENIED);
    }

}
