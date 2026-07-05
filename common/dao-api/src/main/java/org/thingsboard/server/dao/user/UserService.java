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
 * 1. 类目的：`UserService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`UserService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
