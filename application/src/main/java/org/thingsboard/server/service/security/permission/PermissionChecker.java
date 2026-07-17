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
 * 1. `PermissionChecker` 是 ThingsBoard Application 中定义 `Permission Checker` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityId`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
     * 1. `GenericPermissionChecker` 是 ThingsBoard Application 中围绕 `Generic Permission Checker` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `EntityId`、`PermissionChecker`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
