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
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleChainEntity` 是 ThingsBoard DAO 中表示规则链持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = ModelConstants.RULE_CHAIN_TABLE_NAME)
public class RuleChainEntity extends BaseSqlEntity<RuleChain> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.RULE_CHAIN_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.RULE_CHAIN_NAME_PROPERTY)
    private String name;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.RULE_CHAIN_TYPE_PROPERTY)
    private RuleChainType type;

    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY)
    private UUID firstRuleNodeId;

    /**
     * 当前对象是否为根节点。
     */
    @Column(name = ModelConstants.RULE_CHAIN_ROOT_PROPERTY)
    private boolean root;

    /**
     * 是否开启调试模式。
     */
    @Column(name = ModelConstants.DEBUG_MODE)
    private boolean debugMode;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.RULE_CHAIN_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `RuleChainEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RuleChainEntity() {
    }

    /**
     * 功能：创建 `RuleChainEntity` 实例，并初始化必要字段。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChainEntity(RuleChain ruleChain) {
        if (ruleChain.getId() != null) {
            this.setUuid(ruleChain.getUuidId());
        }
        this.setCreatedTime(ruleChain.getCreatedTime());
        this.tenantId = DaoUtil.getId(ruleChain.getTenantId());
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        if (ruleChain.getFirstRuleNodeId() != null) {
            this.firstRuleNodeId = ruleChain.getFirstRuleNodeId().getId();
        }
        this.root = ruleChain.isRoot();
        this.debugMode = ruleChain.isDebugMode();
        this.configuration = ruleChain.getConfiguration();
        this.additionalInfo = ruleChain.getAdditionalInfo();
        if (ruleChain.getExternalId() != null) {
            this.externalId = ruleChain.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleChain toData() {
        RuleChain ruleChain = new RuleChain(new RuleChainId(this.getUuid()));
        ruleChain.setCreatedTime(createdTime);
        ruleChain.setTenantId(TenantId.fromUUID(tenantId));
        ruleChain.setName(name);
        ruleChain.setType(type);
        if (firstRuleNodeId != null) {
            ruleChain.setFirstRuleNodeId(new RuleNodeId(firstRuleNodeId));
        }
        ruleChain.setRoot(root);
        ruleChain.setDebugMode(debugMode);
        ruleChain.setConfiguration(configuration);
        ruleChain.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            ruleChain.setExternalId(new RuleChainId(externalId));
        }
        return ruleChain;
    }
}
