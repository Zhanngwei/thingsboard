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
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_CUSTOMER_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_LABEL_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_NAME_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROOT_RULE_CHAIN_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_ROUTING_KEY_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_SECRET_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TENANT_ID_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TYPE_PROPERTY;

/**
 * 中文说明：
 * 1. `AbstractEdgeEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Edge`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDef(name = "json", typeClass = JsonStringType.class)
@MappedSuperclass
public abstract class AbstractEdgeEntity<T extends Edge> extends BaseSqlEntity<T> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = EDGE_ROOT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID rootRuleChainId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = EDGE_TYPE_PROPERTY)
    private String type;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = EDGE_NAME_PROPERTY)
    private String name;

    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Column(name = EDGE_LABEL_PROPERTY)
    private String label;

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Column(name = EDGE_ROUTING_KEY_PROPERTY)
    private String routingKey;

    /**
     * 密钥，用于认证或安全校验。
     */
    @Column(name = EDGE_SECRET_PROPERTY)
    private String secret;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.EDGE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `AbstractEdgeEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractEdgeEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractEdgeEntity` 实例，并初始化必要字段。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：新创建的对象实例。
     */
    public AbstractEdgeEntity(Edge edge) {
        if (edge.getId() != null) {
            this.setUuid(edge.getId().getId());
        }
        this.setCreatedTime(edge.getCreatedTime());
        if (edge.getTenantId() != null) {
            this.tenantId = edge.getTenantId().getId();
        }
        if (edge.getCustomerId() != null) {
            this.customerId = edge.getCustomerId().getId();
        }
        if (edge.getRootRuleChainId() != null) {
            this.rootRuleChainId = edge.getRootRuleChainId().getId();
        }
        this.type = edge.getType();
        this.name = edge.getName();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.additionalInfo = edge.getAdditionalInfo();
    }

    /**
     * 功能：创建 `AbstractEdgeEntity` 实例，并初始化必要字段。
     * 参数：
     * - `edgeEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractEdgeEntity(EdgeEntity edgeEntity) {
        this.setId(edgeEntity.getId());
        this.setCreatedTime(edgeEntity.getCreatedTime());
        this.tenantId = edgeEntity.getTenantId();
        this.customerId = edgeEntity.getCustomerId();
        this.rootRuleChainId = edgeEntity.getRootRuleChainId();
        this.type = edgeEntity.getType();
        this.name = edgeEntity.getName();
        this.label = edgeEntity.getLabel();
        this.routingKey = edgeEntity.getRoutingKey();
        this.secret = edgeEntity.getSecret();
        this.additionalInfo = edgeEntity.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toEdge` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Edge toEdge() {
        Edge edge = new Edge(new EdgeId(getUuid()));
        edge.setCreatedTime(createdTime);
        if (tenantId != null) {
            edge.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(customerId));
        }
        if (rootRuleChainId != null) {
            edge.setRootRuleChainId(new RuleChainId(rootRuleChainId));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setLabel(label);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        edge.setAdditionalInfo(additionalInfo);
        return edge;
    }
}
