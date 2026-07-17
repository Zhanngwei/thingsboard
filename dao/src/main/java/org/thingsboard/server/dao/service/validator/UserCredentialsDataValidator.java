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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.user.UserCredentialsDao;
import org.thingsboard.server.dao.user.UserService;

/**
 * 中文说明：
 * 1. `UserCredentialsDataValidator` 是 ThingsBoard DAO 中负责用户存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class UserCredentialsDataValidator extends DataValidator<UserCredentials> {

    /**
     * 用户，用于读取或保存对应领域对象。
     */
    @Autowired
    private UserCredentialsDao userCredentialsDao;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private UserService userService;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, UserCredentials userCredentials) {
        throw new IncorrectParameterException("Creation of new user credentials is prohibited.");
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userCredentials`：`userCredentials` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, UserCredentials userCredentials) {
        if (userCredentials.getUserId() == null) {
            throw new DataValidationException("User credentials should be assigned to user!");
        }
        if (userCredentials.isEnabled()) {
            if (StringUtils.isEmpty(userCredentials.getPassword())) {
                throw new DataValidationException("Enabled user credentials should have password!");
            }
            if (StringUtils.isNotEmpty(userCredentials.getActivateToken())) {
                throw new DataValidationException("Enabled user credentials can't have activate token!");
            }
        }
        UserCredentials existingUserCredentialsEntity = userCredentialsDao.findById(tenantId, userCredentials.getId().getId());
        if (existingUserCredentialsEntity == null) {
            throw new DataValidationException("Unable to update non-existent user credentials!");
        }
        User user = userService.findUserById(tenantId, userCredentials.getUserId());
        if (user == null) {
            throw new DataValidationException("Can't assign user credentials to non-existent user!");
        }
    }
}
