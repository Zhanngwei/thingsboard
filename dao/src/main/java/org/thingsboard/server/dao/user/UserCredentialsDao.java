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
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface UserCredentialsDao.
 */
/**
 * 中文说明：
 * 1. `UserCredentialsDao` 是 ThingsBoard DAO 中定义用户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface UserCredentialsDao extends Dao<UserCredentials> {

    /**
     * Save or update user credentials object
     *
     * @param userCredentials the user credentials object
     * @return saved user credentials object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    UserCredentials save(TenantId tenantId, UserCredentials userCredentials);

    /**
     * Find user credentials by user id.
     *
     * @param userId the user id
     * @return the user credentials object
     */
    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    UserCredentials findByUserId(TenantId tenantId, UUID userId);

    /**
     * Find user credentials by activate token.
     *
     * @param activateToken the activate token
     * @return the user credentials object
     */
    /**
     * 功能：获取令牌。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activateToken`：`activateToken` 参数。
     * 返回：处理结果。
     */
    UserCredentials findByActivateToken(TenantId tenantId, String activateToken);

    /**
     * Find user credentials by reset token.
     *
     * @param resetToken the reset token
     * @return the user credentials object
     */
    /**
     * 功能：获取令牌。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resetToken`：`resetToken` 参数。
     * 返回：处理结果。
     */
    UserCredentials findByResetToken(TenantId tenantId, String resetToken);

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    void removeByUserId(TenantId tenantId, UserId userId);

}
