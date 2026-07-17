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
package org.thingsboard.server.common.data.event;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.StringUtils;

/**
 * 中文说明：
 * 1. `RuleNodeDebugEventFilter` 是 ThingsBoard Common Data 中处理事件的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `DebugEventFilter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel
public class RuleNodeDebugEventFilter extends DebugEventFilter {

    /**
     * 消息，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 2, value = "String value representing msg direction type (incoming to entity or outcoming from entity)", allowableValues = "IN, OUT")
    protected String msgDirectionType;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "String value representing the entity id in the event body (originator of the message)", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String entityId;
    /**
     * 实体，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 4, value = "String value representing the entity type", allowableValues = "DEVICE")
    protected String entityType;
    /**
     * 消息ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 5, value = "String value representing the message id in the rule engine", example = "de9d54a0-2b7a-11ec-a3cc-23386423d98f")
    protected String msgId;
    /**
     * 消息，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 6, value = "String value representing the message type", example = "POST_TELEMETRY_REQUEST")
    protected String msgType;
    /**
     * 关系，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 7, value = "String value representing the type of message routing", example = "Success")
    protected String relationType;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @ApiModelProperty(position = 8, value = "The case insensitive 'contains' filter based on data (key and value) for the message.", example = "humidity")
    protected String dataSearch;
    /**
     * `metadataSearch` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 9, value = "The case insensitive 'contains' filter based on metadata (key and value) for the message.", example = "deviceName")
    protected String metadataSearch;

    /**
     * 功能：获取事件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EventType getEventType() {
        return EventType.DEBUG_RULE_NODE;
    }

    /**
     * 功能：判断`Not Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isNotEmpty() {
        return super.isNotEmpty() || !StringUtils.isEmpty(msgDirectionType) || !StringUtils.isEmpty(entityId)
                || !StringUtils.isEmpty(entityType) || !StringUtils.isEmpty(msgId) || !StringUtils.isEmpty(msgType) ||
                !StringUtils.isEmpty(relationType) || !StringUtils.isEmpty(dataSearch) || !StringUtils.isEmpty(metadataSearch);
    }
}
