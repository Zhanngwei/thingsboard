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
package org.thingsboard.server.common.data.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.BaseDataWithAdditionalInfo;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.validation.Length;

import java.util.Objects;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`ComponentDescriptor` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@ToString
public class ComponentDescriptor extends BaseData<ComponentDescriptorId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 功能：创建 `ComponentDescriptor` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    @ApiModelProperty(position = 3, value = "Type of the Rule Node", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private ComponentType type;
    @ApiModelProperty(position = 4, value = "Scope of the Rule Node. Always set to 'TENANT', since no rule chains on the 'SYSTEM' level yet.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "TENANT", example = "TENANT")
    @Getter @Setter private ComponentScope scope;
    @ApiModelProperty(position = 5, value = "Clustering mode of the RuleNode. This mode represents the ability to start Rule Node in multiple microservices.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, allowableValues = "USER_PREFERENCE, ENABLED, SINGLETON", example = "ENABLED")
    @Getter @Setter private ComponentClusteringMode clusteringMode;
    @Length(fieldName = "name")
    @ApiModelProperty(position = 6, value = "Name of the Rule Node. Taken from the @RuleNode annotation.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "Custom Rule Node")
    @Getter @Setter private String name;
    @ApiModelProperty(position = 7, value = "Full name of the Java class that implements the Rule Engine Node interface.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "com.mycompany.CustomRuleNode")
    @Getter @Setter private String clazz;
    @ApiModelProperty(position = 8, value = "Complex JSON object that represents the Rule Node configuration.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private transient JsonNode configurationDescriptor;
    @ApiModelProperty(position = 9, value = "Rule node configuration version. By default, this value is 0. If the rule node is a versioned node, this value might be greater than 0.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private int configurationVersion;
    @Length(fieldName = "actions")
    @ApiModelProperty(position = 10, value = "Rule Node Actions. Deprecated. Always null.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Getter @Setter private String actions;
    @ApiModelProperty(position = 11, value = "Indicates that the RuleNode supports queue name configuration.", accessMode = ApiModelProperty.AccessMode.READ_ONLY, example = "true")
    @Getter @Setter private boolean hasQueueName;
    public ComponentDescriptor() {
        super();
    }

    /**
     * 功能：创建 `ComponentDescriptor` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public ComponentDescriptor(ComponentDescriptorId id) {
        super(id);
    }

    /**
     * 功能：创建 `ComponentDescriptor` 实例，并初始化必要字段。
     * 参数：
     * - `plugin`：`plugin` 参数。
     * 返回：新创建的对象实例。
     */
    public ComponentDescriptor(ComponentDescriptor plugin) {
        super(plugin);
        this.type = plugin.getType();
        this.scope = plugin.getScope();
        this.clusteringMode = plugin.getClusteringMode();
        this.name = plugin.getName();
        this.clazz = plugin.getClazz();
        this.configurationDescriptor = plugin.getConfigurationDescriptor();
        this.configurationVersion = plugin.getConfigurationVersion();
        this.actions = plugin.getActions();
        this.hasQueueName = plugin.isHasQueueName();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the descriptor Id. " +
            "Specify existing descriptor id to update the descriptor. " +
            "Referencing non-existing descriptor Id will cause error. " +
            "Omit this field to create new descriptor." )
    @Override
    public ComponentDescriptorId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the descriptor creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ComponentDescriptor that = (ComponentDescriptor) o;

        if (type != that.type) return false;
        if (scope != that.scope) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(actions, that.actions)) return false;
        if (!Objects.equals(configurationDescriptor, that.configurationDescriptor)) return false;
        if (configurationVersion != that.configurationVersion) return false;
        if (clusteringMode != that.clusteringMode) return false;
        if (hasQueueName != that.isHasQueueName()) return false;
        return Objects.equals(clazz, that.clazz);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (clazz != null ? clazz.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        result = 31 * result + (clusteringMode != null ? clusteringMode.hashCode() : 0);
        result = 31 * result + (hasQueueName ? 1 : 0);
        return result;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ComponentDescriptor` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
