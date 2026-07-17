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
 * 1. `Dashboard` 是 ThingsBoard Common Data 中承载仪表盘信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DashboardInfo`、`ExportableEntity`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
