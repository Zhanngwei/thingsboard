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
package org.thingsboard.server.common.data.rule;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`RuleChain` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleChain extends BaseDataWithAdditionalInfo<RuleChainId> implements HasName, HasTenantId, ExportableEntity<RuleChainId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -5656679015121935465L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, required = true, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 4, required = true, value = "Rule Chain name", example = "Humidity data processing")
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 5, value = "Rule Chain type. 'EDGE' rule chains are processing messages on the edge devices only.", example = "A4B72CCDFF33")
    private RuleChainType type;
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 6, value = "JSON object with Rule Chain Id. Pointer to the first rule node that should receive all messages pushed to this rule chain.")
    private RuleNodeId firstRuleNodeId;
    /**
     * 当前对象是否为根节点。
     */
    @ApiModelProperty(position = 7, value = "Indicates root rule chain. The root rule chain process messages from all devices and entities by default. User may configure default rule chain per device profile.")
    private boolean root;
    /**
     * 是否开启调试模式。
     */
    @ApiModelProperty(position = 8, value = "Reserved for future usage.")
    private boolean debugMode;
    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @ApiModelProperty(position = 9, value = "Reserved for future usage. The actual list of rule nodes and their relations is stored in the database separately.")
    private transient JsonNode configuration;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private RuleChainId externalId;

    /**
     * `configurationBytes`列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] configurationBytes;

    /**
     * 功能：创建 `RuleChain` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RuleChain() {
        super();
    }

    /**
     * 功能：创建 `RuleChain` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public RuleChain(RuleChainId id) {
        super(id);
    }

    /**
     * 功能：创建 `RuleChain` 实例，并初始化必要字段。
     * 参数：
     * - `ruleChain`：`ruleChain` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleChain(RuleChain ruleChain) {
        super(ruleChain);
        this.tenantId = ruleChain.getTenantId();
        this.name = ruleChain.getName();
        this.type = ruleChain.getType();
        this.firstRuleNodeId = ruleChain.getFirstRuleNodeId();
        this.root = ruleChain.isRoot();
        this.setConfiguration(ruleChain.getConfiguration());
        this.setExternalId(ruleChain.getExternalId());
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Rule Chain Id. " +
            "Specify this field to update the Rule Chain. " +
            "Referencing non-existing Rule Chain Id will cause error. " +
            "Omit this field to create new rule chain." )
    @Override
    public RuleChainId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the rule chain creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取`Configuration`。
     * 参数：无。
     * 返回：处理结果。
     */
    public JsonNode getConfiguration() {
        return BaseDataWithAdditionalInfo.getJson(() -> configuration, () -> configurationBytes);
    }

    /**
     * 功能：更新`Configuration`。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    public void setConfiguration(JsonNode data) {
        setJson(data, json -> this.configuration = json, bytes -> this.configurationBytes = bytes);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RuleChain` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
