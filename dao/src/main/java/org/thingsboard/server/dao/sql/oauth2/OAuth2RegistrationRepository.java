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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.sql.OAuth2RegistrationEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `OAuth2RegistrationRepository` 是 ThingsBoard DAO 中定义 `O Auth2 Registration` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface OAuth2RegistrationRepository extends JpaRepository<OAuth2RegistrationEntity, UUID> {

    /**
     * 功能：获取名称。
     * 参数：
     * - `domainSchemes`：数据列表。
     * - `domainName`：名称。
     * - `pkgName`：名称。
     * - `platformFilter`：`platformFilter` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT reg " +
            "FROM OAuth2RegistrationEntity reg " +
            "LEFT JOIN OAuth2ParamsEntity params on reg.oauth2ParamsId = params.id " +
            "LEFT JOIN OAuth2DomainEntity domain on reg.oauth2ParamsId = domain.oauth2ParamsId " +
            "WHERE params.enabled = true " +
            "AND domain.domainName = :domainName " +
            "AND domain.domainScheme IN (:domainSchemes) " +
            "AND (:pkgName IS NULL OR EXISTS (SELECT mobile FROM OAuth2MobileEntity mobile WHERE mobile.oauth2ParamsId = reg.oauth2ParamsId AND mobile.pkgName = :pkgName)) " +
            "AND (:platformFilter IS NULL OR reg.platforms IS NULL OR reg.platforms = '' OR reg.platforms LIKE :platformFilter)")
    List<OAuth2RegistrationEntity> findEnabledByDomainSchemesDomainNameAndPkgNameAndPlatformType(@Param("domainSchemes") List<SchemeType> domainSchemes,
    /**
     * 功能：获取`By Oauth2 Params Id`。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * 返回：匹配的数据集合。
     */
                                                                                                 @Param("domainName") String domainName,
                                                                                                 @Param("pkgName") String pkgName,
                                                                                                 @Param("platformFilter") String platformFilter);
    List<OAuth2RegistrationEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

    /**
     * 功能：获取密钥。
     * 参数：
     * - `id`：`id`ID。
     * - `pkgName`：名称。
     * 返回：文本结果。
     */
    @Query("SELECT mobile.appSecret " +
            "FROM OAuth2MobileEntity mobile " +
            "LEFT JOIN OAuth2RegistrationEntity reg on mobile.oauth2ParamsId = reg.oauth2ParamsId " +
            "WHERE reg.id = :registrationId " +
            "AND mobile.pkgName = :pkgName")
    String findAppSecret(@Param("registrationId") UUID id,
                         @Param("pkgName") String pkgName);

}
