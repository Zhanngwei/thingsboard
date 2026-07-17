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
package org.thingsboard.server.common.msg.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.rule.RuleNode;

/**
 * 中文说明：
 * 1. `RuleNodeException` 是 ThingsBoard Common Message 中表示规则节点失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `RuleEngineException`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
@Slf4j
public class RuleNodeException extends RuleEngineException {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -1776681087370749776L;
    public static final String UNKNOWN = "Unknown";

    /**
     * 规则链，用于标识或展示当前对象。
     */
    @Getter
    private final String ruleChainName;
    /**
     * 规则节点，用于标识或展示当前对象。
     */
    @Getter
    private final String ruleNodeName;
    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Getter
    private final RuleChainId ruleChainId;
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    @Getter
    private final RuleNodeId ruleNodeId;


    /**
     * 功能：创建 `RuleNodeException` 实例，并初始化必要字段。
     * 参数：
     * - `message`：待处理消息。
     * - `ruleChainName`：名称。
     * - `ruleNode`：`ruleNode` 参数。
     * 返回：新创建的对象实例。
     */
    public RuleNodeException(String message, String ruleChainName, RuleNode ruleNode) {
        super(message);
        this.ruleChainName = ruleChainName;
        if (ruleNode != null) {
            this.ruleNodeName = ruleNode.getName();
            this.ruleChainId = ruleNode.getRuleChainId();
            this.ruleNodeId = ruleNode.getId();
        } else {
            ruleNodeName = UNKNOWN;
            ruleChainId = new RuleChainId(RuleChainId.NULL_UUID);
            ruleNodeId = new RuleNodeId(RuleNodeId.NULL_UUID);
        }
    }

    /**
     * 功能：执行 `toJsonString` 对应的处理。
     * 参数：
     * - `maxMessageLength`：待处理消息。
     * 返回：文本结果。
     */
    public String toJsonString(int maxMessageLength) {
        try {
            return mapper.writeValueAsString(mapper.createObjectNode()
                    .put("ruleNodeId", ruleNodeId.toString())
                    .put("ruleChainId", ruleChainId.toString())
                    .put("ruleNodeName", ruleNodeName)
                    .put("ruleChainName", ruleChainName)
                    .put("message", truncateIfNecessary(getMessage(), maxMessageLength)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize exception ", e);
            throw new RuntimeException(e);
        }
    }

}
