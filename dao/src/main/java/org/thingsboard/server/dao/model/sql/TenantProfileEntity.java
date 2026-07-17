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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 中文说明：
 * 1. `TenantProfileEntity` 是 ThingsBoard DAO 中表示租户持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ModelConstants.TENANT_PROFILE_TABLE_NAME)
public final class TenantProfileEntity extends BaseSqlEntity<TenantProfile> {

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.TENANT_PROFILE_NAME_PROPERTY)
    private String name;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.TENANT_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    /**
     * 当前对象是否为默认项。
     */
    @Column(name = ModelConstants.TENANT_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    /**
     * 是否满足规则引擎条件。
     */
    @Column(name = ModelConstants.TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE)
    private boolean isolatedTbRuleEngine;

    /**
     * 配置，保存当前步骤读取或计算得到的内容。
     */
    @Type(type = "jsonb")
    @Column(name = ModelConstants.TENANT_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    /**
     * 功能：创建 `TenantProfileEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TenantProfileEntity() {
        super();
    }

    /**
     * 功能：创建 `TenantProfileEntity` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public TenantProfileEntity(TenantProfile tenantProfile) {
        if (tenantProfile.getId() != null) {
            this.setUuid(tenantProfile.getId().getId());
        }
        this.setCreatedTime(tenantProfile.getCreatedTime());
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.profileData = JacksonUtil.convertValue(tenantProfile.getProfileData(), ObjectNode.class);
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TenantProfile toData() {
        TenantProfile tenantProfile = new TenantProfile(new TenantProfileId(this.getUuid()));
        tenantProfile.setCreatedTime(createdTime);
        tenantProfile.setName(name);
        tenantProfile.setDescription(description);
        tenantProfile.setDefault(isDefault);
        tenantProfile.setIsolatedTbRuleEngine(isolatedTbRuleEngine);
        tenantProfile.setProfileData(JacksonUtil.convertValue(profileData, TenantProfileData.class));
        return tenantProfile;
    }

}
