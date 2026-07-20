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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserCredentialsId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.mobile.MobileSessionInfo;
import org.thingsboard.server.common.data.mobile.UserMobileInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.common.data.StringUtils.generateSafeToken;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * 中文说明：
 * 1. `UserServiceImpl` 是 ThingsBoard DAO 中负责用户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`UserService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("UserDaoService")
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends AbstractEntityService implements UserService {

    /**
     * 密码常量，用于统一引用固定值。
     */
    public static final String USER_PASSWORD_HISTORY = "userPasswordHistory";

    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String LAST_LOGIN_TS = "lastLoginTs";
    public static final String FAILED_LOGIN_ATTEMPTS = "failedLoginAttempts";

    /**
     * 令牌常量，用于统一引用固定值。
     */
    private static final int DEFAULT_TOKEN_LENGTH = 30;
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 用户常量，用于统一引用固定值。
     */
    private static final String USER_CREDENTIALS_ENABLED = "userCredentialsEnabled";

    /**
     * 是否满足用户条件。
     */
    @Value("${security.user_login_case_sensitive:true}")
    private boolean userLoginCaseSensitive;

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    private final UserDao userDao;
    private final UserCredentialsDao userCredentialsDao;
    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserAuthSettingsDao userAuthSettingsDao;
    private final UserSettingsService userSettingsService;
    /**
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserSettingsDao userSettingsDao;
    private final DataValidator<User> userValidator;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    private final DataValidator<UserCredentials> userCredentialsValidator;
    private final ApplicationEventPublisher eventPublisher;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final EntityCountService countService;

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    @Override
    public User findUserByEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByEmail [{}]", email);
        validateString(email, "Incorrect email " + email);
        if (userLoginCaseSensitive) {
            return userDao.findByEmail(tenantId, email);
        } else {
            return userDao.findByEmail(tenantId, email.toLowerCase());
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    @Override
    public User findUserByTenantIdAndEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByTenantIdAndEmail [{}][{}]", tenantId, email);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(email, "Incorrect email " + email);
        return userDao.findByTenantIdAndEmail(tenantId, email);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    @Override
    public User findUserById(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserById [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findById(tenantId, userId.getId());
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserByIdAsync [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findByIdAsync(tenantId, userId.getId());
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public User saveUser(TenantId tenantId, User user) {
        log.trace("Executing saveUser [{}]", user);
        User oldUser = userValidator.validate(user, User::getTenantId);
        if (!userLoginCaseSensitive) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        User savedUser = userDao.save(user.getTenantId(), user);
        if (user.getId() == null) {
            countService.publishCountEntityEvictEvent(savedUser.getTenantId(), EntityType.USER);
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setEnabled(false);
            userCredentials.setActivateToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
            userCredentials.setUserId(new UserId(savedUser.getUuidId()));
            userCredentials.setAdditionalInfo(JacksonUtil.newObjectNode());
            userCredentialsDao.save(user.getTenantId(), userCredentials);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(tenantId == null ? TenantId.SYS_TENANT_ID : tenantId)
                .entity(savedUser)
                .oldEntity(oldUser)
                .entityId(savedUser.getId())
                .created(user.getId() == null).build());
        return savedUser;
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserCredentialsByUserId [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userCredentialsDao.findByUserId(tenantId, userId.getId());
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activateToken`：`activateToken` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken) {
        log.trace("Executing findUserCredentialsByActivateToken [{}]", activateToken);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        return userCredentialsDao.findByActivateToken(tenantId, activateToken);
    }

    /**
     * 功能：获取用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resetToken`：`resetToken` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken) {
        log.trace("Executing findUserCredentialsByResetToken [{}]", resetToken);
        validateString(resetToken, "Incorrect resetToken " + resetToken);
        return userCredentialsDao.findByResetToken(tenantId, resetToken);
    }

    /**
     * 功能：保存或创建用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        log.trace("Executing saveUserCredentials [{}]", userCredentials);
        userCredentialsValidator.validate(userCredentials, data -> tenantId);
        UserCredentials result = userCredentialsDao.save(tenantId, userCredentials);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(userCredentials.getUserId())
                .actionType(ActionType.CREDENTIALS_UPDATED).build());
        return result;
    }

    /**
     * 功能：执行 `activateUserCredentials` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `activateToken`：`activateToken` 参数。
     * - `password`：`password` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password) {
        log.trace("Executing activateUserCredentials activateToken [{}], password [{}]", activateToken, password);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        validateString(password, "Incorrect password " + password);
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken(tenantId, activateToken);
        if (userCredentials == null) {
            throw new IncorrectParameterException(String.format("Unable to find user credentials by activateToken [%s]", activateToken));
        }
        if (userCredentials.isEnabled()) {
            throw new IncorrectParameterException("User credentials already activated");
        }
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userCredentials.setPassword(password);
        if (userCredentials.getPassword() != null) {
            updatePasswordHistory(userCredentials);
        }
        return saveUserCredentials(tenantId, userCredentials);
    }

    /**
     * 功能：执行 `requestPasswordReset` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `email`：`email` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials requestPasswordReset(TenantId tenantId, String email) {
        log.trace("Executing requestPasswordReset email [{}]", email);
        DataValidator.validateEmail(email);
        User user = findUserByEmail(tenantId, email);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unable to find user by email [%s]", email));
        }
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, user.getUuidId());
        if (!userCredentials.isEnabled()) {
            throw new DisabledException(String.format("User credentials not enabled [%s]", email));
        }
        userCredentials.setResetToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        return saveUserCredentials(tenantId, userCredentials);
    }

    /**
     * 功能：执行 `requestExpiredPasswordReset` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentialsId`：用户ID。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials requestExpiredPasswordReset(TenantId tenantId, UserCredentialsId userCredentialsId) {
        UserCredentials userCredentials = userCredentialsDao.findById(tenantId, userCredentialsId.getId());
        if (!userCredentials.isEnabled()) {
            throw new IncorrectParameterException("Unable to reset password for inactive user");
        }
        userCredentials.setResetToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        return saveUserCredentials(tenantId, userCredentials);
    }

    /**
     * 功能：执行 `replaceUserCredentials` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：处理结果。
     */
    @Override
    public UserCredentials replaceUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        log.trace("Executing replaceUserCredentials [{}]", userCredentials);
        userCredentialsValidator.validate(userCredentials, data -> tenantId);
        userCredentialsDao.removeById(tenantId, userCredentials.getUuidId());
        userCredentials.setId(null);
        if (userCredentials.getPassword() != null) {
            updatePasswordHistory(userCredentials);
        }
        UserCredentials result = userCredentialsDao.save(tenantId, userCredentials);
        eventPublisher.publishEvent(ActionEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(userCredentials.getUserId())
                .actionType(ActionType.CREDENTIALS_UPDATED).build());
        return result;
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteUser(TenantId tenantId, User user) {
        Objects.requireNonNull(user, "User is null");
        UserId userId = user.getId();
        log.trace("[{}] Executing deleteUser [{}]", tenantId, userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        userCredentialsDao.removeByUserId(tenantId, userId);
        userAuthSettingsDao.removeByUserId(userId);
        deleteEntityRelations(tenantId, userId);

        userDao.removeById(tenantId, userId.getId());
        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(userId));
        countService.publishCountEntityEvictEvent(tenantId, EntityType.USER);
        eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId)
                .entityId(userId)
                .entity(user).build());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findUsersByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findUsersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return userDao.findByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findTenantAdmins(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantAdmins, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return userDao.findTenantAdmins(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取`Sys Admins`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findSysAdmins(PageLink pageLink) {
        return userDao.findAllByAuthority(Authority.SYS_ADMIN, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findAllTenantAdmins(PageLink pageLink) {
        return userDao.findAllByAuthority(Authority.TENANT_ADMIN, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantsIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findTenantAdminsByTenantsIds(List<TenantId> tenantsIds, PageLink pageLink) {
        return userDao.findByAuthorityAndTenantsIds(Authority.TENANT_ADMIN, tenantsIds, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantProfilesIds`：租户信息或租户标识。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findTenantAdminsByTenantProfilesIds(List<TenantProfileId> tenantProfilesIds, PageLink pageLink) {
        return userDao.findByAuthorityAndTenantProfilesIds(Authority.TENANT_ADMIN, tenantProfilesIds, pageLink);
    }

    /**
     * 功能：获取`All Users`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findAllUsers(PageLink pageLink) {
        return userDao.findAll(pageLink);
    }

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteTenantAdmins(TenantId tenantId) {
        log.trace("Executing deleteTenantAdmins, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAdminsRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findCustomerUsers, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validatePageLink(pageLink);
        return userDao.findCustomerUsers(tenantId.getId(), customerId.getId(), pageLink);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerIds`：数据列表。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<User> findUsersByCustomerIds(TenantId tenantId, List<CustomerId> customerIds, PageLink pageLink) {
        log.trace("Executing findTenantAndCustomerUsers, tenantId [{}], customerIds [{}], pageLink [{}]", tenantId, customerIds, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        customerIds.forEach(customerId -> {validateId(customerId, "Incorrect customerId " + customerId);});
        return userDao.findUsersByCustomerIds(tenantId.getId(), customerIds, pageLink);
    }

    /**
     * 功能：删除或清理客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void deleteCustomerUsers(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomerUsers, customerId [{}]", customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        customerUsersRemover.removeEntities(tenantId, customerId);
    }

    /**
     * 功能：更新用户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `enabled`：`enabled` 参数。
     * 返回：无。
     */
    @Override
    public void setUserCredentialsEnabled(TenantId tenantId, UserId userId, boolean enabled) {
        log.trace("Executing setUserCredentialsEnabled [{}], [{}]", userId, enabled);
        validateId(userId, INCORRECT_USER_ID + userId);
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, userId.getId());
        userCredentials.setEnabled(enabled);
        saveUserCredentials(tenantId, userCredentials);

        User user = findUserById(tenantId, userId);
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(USER_CREDENTIALS_ENABLED, enabled);
        user.setAdditionalInfo(additionalInfo);
        if (enabled) {
            resetFailedLoginAttempts(user);
        }
        userDao.save(user.getTenantId(), user);
    }


    /**
     * 功能：执行 `resetFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    @Override
    public void resetFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing onUserLoginSuccessful [{}]", userId);
        User user = findUserById(tenantId, userId);
        resetFailedLoginAttempts(user);
        saveUser(tenantId, user);
    }

    /**
     * 功能：执行 `resetFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * 返回：无。
     */
    private void resetFailedLoginAttempts(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(FAILED_LOGIN_ATTEMPTS, 0);
        user.setAdditionalInfo(additionalInfo);
    }

    /**
     * 功能：更新时间戳。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：无。
     */
    @Override
    public void setLastLoginTs(TenantId tenantId, UserId userId) {
        User user = findUserById(tenantId, userId);
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        ((ObjectNode) additionalInfo).put(LAST_LOGIN_TS, System.currentTimeMillis());
        user.setAdditionalInfo(additionalInfo);
        saveUser(tenantId, user);
    }

    /**
     * 功能：保存或创建会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `mobileToken`：`mobileToken` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    @Override
    public void saveMobileSession(TenantId tenantId, UserId userId, String mobileToken, MobileSessionInfo sessionInfo) {
        removeMobileSession(tenantId, mobileToken); // unassigning fcm token from other users, in case we didn't clean up it on log out or mobile app uninstall

        UserMobileInfo mobileInfo = findMobileInfo(tenantId, userId).orElseGet(() -> {
            UserMobileInfo newMobileInfo = new UserMobileInfo();
            newMobileInfo.setSessions(new HashMap<>());
            return newMobileInfo;
        });
        mobileInfo.getSessions().put(mobileToken, sessionInfo);
        userSettingsService.updateUserSettings(tenantId, userId, UserSettingsType.MOBILE, JacksonUtil.valueToTree(mobileInfo));
    }

    /**
     * 功能：获取`Mobile Sessions`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    @Override
    public Map<String, MobileSessionInfo> findMobileSessions(TenantId tenantId, UserId userId) {
        return findMobileInfo(tenantId, userId).map(UserMobileInfo::getSessions).orElse(Collections.emptyMap());
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `mobileToken`：`mobileToken` 参数。
     * 返回：处理结果。
     */
    @Override
    public MobileSessionInfo findMobileSession(TenantId tenantId, UserId userId, String mobileToken) {
        return findMobileInfo(tenantId, userId).map(mobileInfo -> mobileInfo.getSessions().get(mobileToken)).orElse(null);
    }

    /**
     * 功能：删除或清理会话。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `mobileToken`：`mobileToken` 参数。
     * 返回：无。
     */
    @Override
    public void removeMobileSession(TenantId tenantId, String mobileToken) {
        for (UserSettings userSettings : userSettingsDao.findByTypeAndPath(tenantId, UserSettingsType.MOBILE, "sessions", mobileToken)) {
            ((ObjectNode) userSettings.getSettings().get("sessions")).remove(mobileToken);
            userSettingsService.saveUserSettings(tenantId, userSettings);
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：可能存在的结果。
     */
    private Optional<UserMobileInfo> findMobileInfo(TenantId tenantId, UserId userId) {
        return Optional.ofNullable(userSettingsService.findUserSettings(tenantId, userId, UserSettingsType.MOBILE))
                .map(UserSettings::getSettings).map(settings -> JacksonUtil.treeToValue(settings, UserMobileInfo.class));
    }

    /**
     * 功能：执行 `increaseFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：数值结果。
     */
    @Override
    public int increaseFailedLoginAttempts(TenantId tenantId, UserId userId) {
        log.trace("Executing onUserLoginIncorrectCredentials [{}]", userId);
        User user = findUserById(tenantId, userId);
        int failedLoginAttempts = increaseFailedLoginAttempts(user);
        saveUser(tenantId, user);
        return failedLoginAttempts;
    }

    /**
     * 功能：执行 `increaseFailedLoginAttempts` 对应的处理。
     * 参数：
     * - `user`：`user` 参数。
     * 返回：数值结果。
     */
    private int increaseFailedLoginAttempts(User user) {
        JsonNode additionalInfo = user.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        int failedLoginAttempts = 0;
        if (additionalInfo.has(FAILED_LOGIN_ATTEMPTS)) {
            failedLoginAttempts = additionalInfo.get(FAILED_LOGIN_ATTEMPTS).asInt();
        }
        failedLoginAttempts = failedLoginAttempts + 1;
        ((ObjectNode) additionalInfo).put(FAILED_LOGIN_ATTEMPTS, failedLoginAttempts);
        user.setAdditionalInfo(additionalInfo);
        return failedLoginAttempts;
    }

    /**
     * 功能：更新密码。
     * 参数：
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    private void updatePasswordHistory(UserCredentials userCredentials) {
        JsonNode additionalInfo = userCredentials.getAdditionalInfo();
        if (!(additionalInfo instanceof ObjectNode)) {
            additionalInfo = JacksonUtil.newObjectNode();
        }
        Map<String, String> userPasswordHistoryMap = null;
        JsonNode userPasswordHistoryJson;
        if (additionalInfo.has(USER_PASSWORD_HISTORY)) {
            userPasswordHistoryJson = additionalInfo.get(USER_PASSWORD_HISTORY);
            userPasswordHistoryMap = JacksonUtil.convertValue(userPasswordHistoryJson, new TypeReference<>() {
            });
        }
        if (userPasswordHistoryMap != null) {
            userPasswordHistoryMap.put(Long.toString(System.currentTimeMillis()), userCredentials.getPassword());
            userPasswordHistoryJson = JacksonUtil.valueToTree(userPasswordHistoryMap);
            ((ObjectNode) additionalInfo).replace(USER_PASSWORD_HISTORY, userPasswordHistoryJson);
        } else {
            userPasswordHistoryMap = new HashMap<>();
            userPasswordHistoryMap.put(Long.toString(System.currentTimeMillis()), userCredentials.getPassword());
            userPasswordHistoryJson = JacksonUtil.valueToTree(userPasswordHistoryMap);
            ((ObjectNode) additionalInfo).set(USER_PASSWORD_HISTORY, userPasswordHistoryJson);
        }
        userCredentials.setAdditionalInfo(additionalInfo);
    }

    private final PaginatedRemover<TenantId, User> tenantAdminsRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return userDao.findTenantAdmins(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, User user) {
            deleteUser(tenantId, user);
        }
    };

    private final PaginatedRemover<CustomerId, User> customerUsersRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<User> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return userDao.findCustomerUsers(tenantId.getId(), id.getId(), pageLink);

        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, entity);
        }
    };

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findUserById(tenantId, new UserId(entityId.getId())));
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long countByTenantId(TenantId tenantId) {
        return userDao.countByTenantId(tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.USER;
    }

}
