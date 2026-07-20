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
package org.thingsboard.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.OAuth2DomainId;
import org.thingsboard.server.common.data.id.OAuth2ParamsId;
import org.thingsboard.server.common.data.oauth2.OAuth2Domain;
import org.thingsboard.server.common.data.oauth2.SchemeType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `OAuth2DomainEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_DOMAIN_TABLE_NAME)
public class OAuth2DomainEntity extends BaseSqlEntity<OAuth2Domain> {

    /**
     * `oauth2ParamsId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    /**
     * `domainScheme` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    /**
     * 功能：创建 `OAuth2DomainEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OAuth2DomainEntity() {
        super();
    }

    /**
     * 功能：创建 `OAuth2DomainEntity` 实例，并初始化必要字段。
     * 参数：
     * - `domain`：`domain` 参数。
     * 返回：新创建的对象实例。
     */
    public OAuth2DomainEntity(OAuth2Domain domain) {
        if (domain.getId() != null) {
            this.setUuid(domain.getId().getId());
        }
        this.setCreatedTime(domain.getCreatedTime());
        if (domain.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = domain.getOauth2ParamsId().getId();
        }
        this.domainName = domain.getDomainName();
        this.domainScheme = domain.getDomainScheme();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OAuth2Domain toData() {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setId(new OAuth2DomainId(id));
        domain.setCreatedTime(createdTime);
        domain.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        domain.setDomainName(domainName);
        domain.setDomainScheme(domainScheme);
        return domain;
    }
}
