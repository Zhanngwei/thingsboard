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
package org.thingsboard.server.common.data.edge;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`Edge` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
@ToString
@Setter
public class Edge extends BaseDataWithAdditionalInfo<EdgeId> implements HasLabel, HasTenantId, HasCustomerId {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 4934987555236873728L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private CustomerId customerId;
    /**
     * 规则链ID，用于定位对应业务对象。
     */
    private RuleChainId rootRuleChainId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @Length(fieldName = "type")
    private String type;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @NoXss
    @Length(fieldName = "label")
    private String label;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @NoXss
    @Length(fieldName = "routingKey")
    private String routingKey;
    /**
     * 密钥，用于认证或安全校验。
     */
    @NoXss
    @Length(fieldName = "secret")
    private String secret;

    /**
     * 功能：创建 `Edge` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Edge() {
        super();
    }

    /**
     * 功能：创建 `Edge` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Edge(EdgeId id) {
        super(id);
    }

    /**
     * 功能：创建 `Edge` 实例，并初始化必要字段。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：新创建的对象实例。
     */
    public Edge(Edge edge) {
        super(edge);
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.rootRuleChainId = edge.getRootRuleChainId();
        this.type = edge.getType();
        this.label = edge.getLabel();
        this.name = edge.getName();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    public void update(Edge edge) {
        this.tenantId = edge.getTenantId();
        this.customerId = edge.getCustomerId();
        this.rootRuleChainId = edge.getRootRuleChainId();
        this.type = edge.getType();
        this.label = edge.getLabel();
        this.name = edge.getName();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Edge Id. " +
            "Specify this field to update the Edge. " +
            "Referencing non-existing Edge Id will cause error. " +
            "Omit this field to create new Edge." )
    @Override
    public EdgeId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the edge creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Use 'assignDeviceToTenant' to change the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public TenantId getTenantId() {
        return this.tenantId;
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignEdgeToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public CustomerId getCustomerId() {
        return this.customerId;
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 5, value = "JSON object with Root Rule Chain Id. Use 'setEdgeRootRuleChain' to change the Root Rule Chain Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public RuleChainId getRootRuleChainId() {
        return this.rootRuleChainId;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, required = true, value = "Unique Edge Name in scope of Tenant", example = "Silo_A_Edge")
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, required = true, value = "Edge type", example = "Silos")
    public String getType() {
        return this.type;
    }

    /**
     * 功能：获取显示标签。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 8, value = "Label that may be used in widgets", example = "Silo Edge on far field")
    public String getLabel() {
        return this.label;
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 9, required = true, value = "Edge routing key ('username') to authorize on cloud")
    public String getRoutingKey() {
        return this.routingKey;
    }

    /**
     * 功能：获取密钥。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 10, required = true, value = "Edge secret ('password') to authorize on cloud")
    public String getSecret() {
        return this.secret;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Edge` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
