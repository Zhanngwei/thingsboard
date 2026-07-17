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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.mobile.MobileSessionInfo;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.Map;

/**
 * 中文说明：
 * 1. `UserService` 是 ThingsBoard Common 中定义用户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface UserService extends EntityDaoService {

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    User findUserById(TenantId tenantId, UserId userId);

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId);

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    User findUserByEmail(TenantId tenantId, String email);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    User findUserByTenantIdAndEmail(TenantId tenantId, String email);

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    User saveUser(TenantId tenantId, User user);

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId);

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activateToken`：`activateToken` 参数。
     * 返回：处理结果。
     */
    UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken);

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resetToken`：`resetToken` 参数。
     * 返回：处理结果。
     */
    UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken);

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    /**
     * 功能：执行 `activateUserCredentials` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activateToken`：`activateToken` 参数。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password);

    /**
     * 功能：执行 `requestPasswordReset` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    UserCredentials requestPasswordReset(TenantId tenantId, String email);

    /**
     * 功能：执行 `requestExpiredPasswordReset` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentialsId`：用户ID。
     * 返回：处理结果。
     */
    UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId);

    /**
     * 功能：执行 `replaceUserCredentials` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials);

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void deleteUser(TenantId tenantId, User user);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取`Sys Admins`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findSysAdmins(PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findAllTenantAdmins(PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantsIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantProfilesIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink);

    /**
     * 功能：获取`All Users`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findAllUsers(PageLink pageLink);

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteTenantAdmins(TenantId tenantId);

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerIds`：数据列表。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<User> findUsersByCustomerIds(TenantId tenantId, List<CustomerId> customerIds, PageLink pageLink);

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：更新用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `enabled`：`enabled` 参数。
     * 返回：无。
     */
    void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled);

    /**
     * 功能：执行 `resetFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    void resetFailedLoginAttempts(TenantId tenantId, UserId userId);

    /**
     * 功能：执行 `increaseFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：数值结果。
     */
    int increaseFailedLoginAttempts(TenantId tenantId, UserId userId);

    /**
     * 功能：更新时间戳。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    void setLastLoginTs(TenantId tenantId, UserId userId);

    /**
     * 功能：保存或创建会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `mobileToken`：`mobileToken` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    void saveMobileSession(TenantId tenantId, UserId userId, String mobileToken, MobileSessionInfo sessionInfo);

    /**
     * 功能：获取`Mobile Sessions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    Map<String, MobileSessionInfo> findMobileSessions(TenantId tenantId, UserId userId);

    /**
     * 功能：获取会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `mobileToken`：`mobileToken` 参数。
     * 返回：处理结果。
     */
    MobileSessionInfo findMobileSession(TenantId tenantId, UserId userId, String mobileToken);

    /**
     * 功能：删除或清理会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `mobileToken`：`mobileToken` 参数。
     * 返回：无。
     */
    void removeMobileSession(TenantId tenantId, String mobileToken);

}
