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
package org.thingsboard.server.dao.sql.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.oauth2.OAuth2Registration;
import org.thingsboard.server.common.data.oauth2.PlatformType;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationEntity;
import org.thingsboard.server.dao.oauth2.OAuth2RegistrationDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaOAuth2RegistrationDao` 是 ThingsBoard DAO 中负责 `Jpa O Auth2` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`OAuth2RegistrationDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@RequiredArgsConstructor
@SqlDao
public class JpaOAuth2RegistrationDao extends JpaAbstractDao<OAuth2RegistrationEntity, OAuth2Registration> implements OAuth2RegistrationDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final OAuth2RegistrationRepository repository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<OAuth2RegistrationEntity> getEntityClass() {
        return OAuth2RegistrationEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<OAuth2RegistrationEntity, UUID> getRepository() {
        return repository;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `domainSchemes`：数据列表。
     * - `domainName`：名称。
     * - `pkgName`：名称。
     * - `platformType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<OAuth2Registration> findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(List<SchemeType> domainSchemes, String domainName, String pkgName, PlatformType platformType) {
        return DaoUtil.convertDataList(repository.findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(domainSchemes, domainName, pkgName,
                platformType != null ? "%" + platformType.name() + "%" : null));
    }

    /**
     * 功能：获取`By O Auth2 Params Id`。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<OAuth2Registration> findByOAuth2ParamsId(UUID oauth2ParamsId) {
        return DaoUtil.convertDataList(repository.findByOauth2ParamsId(oauth2ParamsId));
    }

    /**
     * 功能：获取密钥。
     * 参数：
     * - `id`：`id`ID。
     * - `pkgName`：名称。
     * 返回：文本结果。
     */
    @Override
    public String findAppSecret(UUID id, String pkgName) {
        return repository.findAppSecret(id, pkgName);
    }

}
