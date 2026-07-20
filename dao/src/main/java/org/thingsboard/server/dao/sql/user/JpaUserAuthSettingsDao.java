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
package org.thingsboard.server.dao.sql.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserAuthSettings;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserAuthSettingsEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserAuthSettingsDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaUserAuthSettingsDao` 是 ThingsBoard DAO 中负责用户存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`UserAuthSettingsDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@RequiredArgsConstructor
@SqlDao
public class JpaUserAuthSettingsDao extends JpaAbstractDao<UserAuthSettingsEntity, UserAuthSettings> implements UserAuthSettingsDao {

    /**
     * 存取组件集合，用于去重保存或快速判断对象是否存在。
     */
    private final UserAuthSettingsRepository repository;

    /**
     * 功能：获取用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public UserAuthSettings findByUserId(UserId userId) {
        return DaoUtil.getData(repository.findByUserId(userId.getId()));
    }

    /**
     * 功能：删除或清理用户。
     * 参数：
     * - `userId`：用户ID。
     * 返回：无。
     */
    @Override
    public void removeByUserId(UserId userId) {
        repository.deleteByUserId(userId.getId());
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Class<UserAuthSettingsEntity> getEntityClass() {
        return UserAuthSettingsEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<UserAuthSettingsEntity, UUID> getRepository() {
        return repository;
    }

}
