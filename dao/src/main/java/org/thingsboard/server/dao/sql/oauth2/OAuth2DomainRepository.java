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
import org.thingsboard.server.dao.model.sql.OAuth2DomainEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `OAuth2DomainRepository` 是 ThingsBoard DAO 中定义 `O Auth2 Domain` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface OAuth2DomainRepository extends JpaRepository<OAuth2DomainEntity, UUID> {

    /**
     * 功能：获取`By Oauth2 Params Id`。
     * 参数：
     * - `oauth2ParamsId`：`oauth2ParamsId`ID。
     * 返回：匹配的数据集合。
     */
    List<OAuth2DomainEntity> findByOauth2ParamsId(UUID oauth2ParamsId);

}
