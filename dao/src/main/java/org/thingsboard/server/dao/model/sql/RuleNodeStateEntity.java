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
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.RuleNodeStateId;
import org.thingsboard.server.common.data.rule.RuleNodeState;
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
 * 1. `RuleNodeStateEntity` 是 ThingsBoard DAO 中表示规则节点持久化结构的实体类型。
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
@Table(name = ModelConstants.RULE_NODE_STATE_TABLE_NAME)
public class RuleNodeStateEntity extends BaseSqlEntity<RuleNodeState> {

    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.RULE_NODE_STATE_NODE_ID_PROPERTY)
    private UUID ruleNodeId;

    /**
     * 实体，用于区分不同处理分支。
     */
    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_TYPE_PROPERTY)
    private String entityType;

    /**
     * 实体ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.RULE_NODE_STATE_ENTITY_ID_PROPERTY)
    private UUID entityId;

    /**
     * 数据，表示当前对象所处状态。
     */
    @Column(name = ModelConstants.RULE_NODE_STATE_DATA_PROPERTY)
    private String stateData;

    /**
     * 功能：创建 `RuleNodeStateEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RuleNodeStateEntity() {
    }

    /**
     * 功能：创建 `RuleNodeStateEntity` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNodeState`：`ruleNodeState` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleNodeStateEntity(RuleNodeState ruleNodeState) {
        if (ruleNodeState.getId() != null) {
            this.setUuid(ruleNodeState.getUuidId());
        }
        this.setCreatedTime(ruleNodeState.getCreatedTime());
        this.ruleNodeId = DaoUtil.getId(ruleNodeState.getRuleNodeId());
        this.entityId = ruleNodeState.getEntityId().getId();
        this.entityType = ruleNodeState.getEntityId().getEntityType().name();
        this.stateData = ruleNodeState.getStateData();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleNodeState toData() {
        RuleNodeState ruleNode = new RuleNodeState(new RuleNodeStateId(this.getUuid()));
        ruleNode.setCreatedTime(createdTime);
        ruleNode.setRuleNodeId(new RuleNodeId(ruleNodeId));
        ruleNode.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
        ruleNode.setStateData(stateData);
        return ruleNode;
    }
}
