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
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `RuleNodeEntity` 是 ThingsBoard DAO 中表示规则节点持久化结构的实体类型。
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
@Table(name = ModelConstants.RULE_NODE_TABLE_NAME)
public class RuleNodeEntity extends BaseSqlEntity<RuleNode> {

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.RULE_NODE_CHAIN_ID_PROPERTY)
    private UUID ruleChainId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ModelConstants.RULE_NODE_TYPE_PROPERTY)
    private String type;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.RULE_NODE_NAME_PROPERTY)
    private String name;

    /**
     * 版本号，保存当前对象的配置选项。
     */
    @Column(name = ModelConstants.RULE_NODE_VERSION_PROPERTY)
    private int configurationVersion;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.RULE_NODE_CONFIGURATION_PROPERTY)
    private JsonNode configuration;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 是否开启调试模式。
     */
    @Column(name = ModelConstants.DEBUG_MODE)
    private boolean debugMode;

    /**
     * 是否按单实例方式执行。
     */
    @Column(name = ModelConstants.SINGLETON_MODE)
    private boolean singletonMode;

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.QUEUE_NAME)
    private String queueName;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `RuleNodeEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RuleNodeEntity() {
    }

    /**
     * 功能：创建 `RuleNodeEntity` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleNodeEntity(RuleNode ruleNode) {
        if (ruleNode.getId() != null) {
            this.setUuid(ruleNode.getUuidId());
        }
        this.setCreatedTime(ruleNode.getCreatedTime());
        if (ruleNode.getRuleChainId() != null) {
            this.ruleChainId = DaoUtil.getId(ruleNode.getRuleChainId());
        }
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugMode = ruleNode.isDebugMode();
        this.singletonMode = ruleNode.isSingletonMode();
        this.queueName = ruleNode.getQueueName();
        this.configurationVersion = ruleNode.getConfigurationVersion();
        this.configuration = ruleNode.getConfiguration();
        this.additionalInfo = ruleNode.getAdditionalInfo();
        if (ruleNode.getExternalId() != null) {
            this.externalId = ruleNode.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleNode toData() {
        RuleNode ruleNode = new RuleNode(new RuleNodeId(this.getUuid()));
        ruleNode.setCreatedTime(createdTime);
        if (ruleChainId != null) {
            ruleNode.setRuleChainId(new RuleChainId(ruleChainId));
        }
        ruleNode.setType(type);
        ruleNode.setName(name);
        ruleNode.setDebugMode(debugMode);
        ruleNode.setSingletonMode(singletonMode);
        ruleNode.setQueueName(queueName);
        ruleNode.setConfigurationVersion(configurationVersion);
        ruleNode.setConfiguration(configuration);
        ruleNode.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            ruleNode.setExternalId(new RuleNodeId(externalId));
        }
        return ruleNode;
    }
}
