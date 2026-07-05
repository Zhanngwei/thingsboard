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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.DashboardId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`Dashboard` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@EqualsAndHashCode(callSuper = true)
public class Dashboard extends DashboardInfo implements ExportableEntity<DashboardId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 872682138346187503L;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    private transient JsonNode configuration;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private DashboardId externalId;

    /**
     * 功能：创建 `Dashboard` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Dashboard() {
        super();
    }

    /**
     * 功能：创建 `Dashboard` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Dashboard(DashboardId id) {
        super(id);
    }

    /**
     * 功能：创建 `Dashboard` 实例，并初始化必要字段。
     * 参数：
     * - `dashboardInfo`：`dashboardInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public Dashboard(DashboardInfo dashboardInfo) {
        super(dashboardInfo);
    }

    /**
     * 功能：创建 `Dashboard` 实例，并初始化必要字段。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：新创建的对象实例。
     */
    public Dashboard(Dashboard dashboard) {
        super(dashboard);
        this.configuration = dashboard.getConfiguration();
        this.externalId = dashboard.getExternalId();
    }

    /**
     * 功能：获取`Configuration`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 9, value = "JSON object with main configuration of the dashboard: layouts, widgets, aliases, etc. " +
            "The JSON structure of the dashboard configuration is quite complex. " +
            "The easiest way to learn it is to export existing dashboard to JSON."
            , dataType = "com.fasterxml.jackson.databind.JsonNode")
    public JsonNode getConfiguration() {
        return configuration;
    }

    /**
     * 功能：更新`Configuration`。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：无。
     */
    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public List<ObjectNode> getEntityAliasesConfig() {
        return getChildObjects("entityAliases");
    }

    /**
     * 功能：获取部件。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    public List<ObjectNode> getWidgetsConfig() {
        return getChildObjects("widgets");
    }

    /**
     * 功能：获取`Child Objects`。
     * 参数：
     * - `propertyName`：名称。
     * 返回：匹配的数据集合。
     */
    @JsonIgnore
    private List<ObjectNode> getChildObjects(String propertyName) {
        return Optional.ofNullable(configuration)
                .map(config -> config.get(propertyName))
                .filter(node -> !node.isEmpty() && (node.isObject() || node.isArray()))
                .map(node -> Streams.stream(node.elements())
                        .filter(JsonNode::isObject)
                        .map(jsonNode -> (ObjectNode) jsonNode)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Dashboard [tenantId=");
        builder.append(getTenantId());
        builder.append(", title=");
        builder.append(getTitle());
        builder.append(", configuration=");
        builder.append(configuration);
        builder.append("]");
        return builder.toString();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Dashboard` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
