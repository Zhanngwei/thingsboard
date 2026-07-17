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
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `RuleNode` 是 ThingsBoard Common Data 中承载规则节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseDataWithAdditionalInfo`、`HasName`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class RuleNode extends BaseDataWithAdditionalInfo<RuleNodeId> implements HasName {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -5656679015121235465L;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with the Rule Chain Id. ", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private RuleChainId ruleChainId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Length(fieldName = "type")
    @ApiModelProperty(position = 4, value = "Full Java Class Name of the rule node implementation. ", example = "com.mycompany.iot.rule.engine.ProcessingNode")
    private String type;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 5, value = "User defined name of the rule node. Used on UI and for logging. ", example = "Process sensor reading")
    private String name;
    /**
     * 是否开启调试模式。
     */
    @ApiModelProperty(position = 6, value = "Enable/disable debug. ", example = "false")
    private boolean debugMode;
    /**
     * 是否按单实例方式执行。
     */
    @ApiModelProperty(position = 7, value = "Enable/disable singleton mode. ", example = "false")
    private boolean singletonMode;
    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 8, value = "Queue name. ", example = "Main")
    private String queueName;
    /**
     * 版本号，保存当前对象的配置选项。
     */
    @ApiModelProperty(position = 9, value = "Version of rule node configuration. ", example = "0")
    private int configurationVersion;
    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @ApiModelProperty(position = 10, value = "JSON with the rule node configuration. Structure depends on the rule node implementation.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode configuration;
    /**
     * `configurationBytes`列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] configurationBytes;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private RuleNodeId externalId;

    /**
     * 功能：创建 `RuleNode` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public RuleNode() {
        super();
    }

    /**
     * 功能：创建 `RuleNode` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public RuleNode(RuleNodeId id) {
        super(id);
    }

    /**
     * 功能：创建 `RuleNode` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleNode(RuleNode ruleNode) {
        super(ruleNode);
        this.ruleChainId = ruleNode.getRuleChainId();
        this.type = ruleNode.getType();
        this.name = ruleNode.getName();
        this.debugMode = ruleNode.isDebugMode();
        this.singletonMode = ruleNode.isSingletonMode();
        this.setConfiguration(ruleNode.getConfiguration());
        this.externalId = ruleNode.getExternalId();
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

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Rule Node Id. " +
            "Specify this field to update the Rule Node. " +
            "Referencing non-existing Rule Node Id will cause error. " +
            "Omit this field to create new rule node." )
    @Override
    public RuleNodeId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the rule node creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 10, value = "Additional parameters of the rule node. Contains 'layoutX' and 'layoutY' properties for visualization.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

}
