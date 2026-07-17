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

import org.thingsboard.server.common.data.id.RuleNodeId;

/**
 * Should be renamed to TbMsgPackContext, but this can't be changed due to backward-compatibility.
 */
/**
 * 中文说明：
 * 1. `TbMsgCallback` 是 ThingsBoard Common Message 中定义消息能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbMsgCallback {

    TbMsgCallback EMPTY = new TbMsgCallback() {

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(RuleEngineException e) {

        }
    };

    /**
     * 功能：处理`on Success`。
     * 参数：无。
     * 返回：无。
     */
    void onSuccess();

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    void onFailure(RuleEngineException e);

    /**
     * 功能：处理数量限制。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    default void onRateLimit(RuleEngineException e) {
        onFailure(e);
    };

    /**
     * Returns 'true' if rule engine is expecting the message to be processed, 'false' otherwise.
     * message may no longer be valid, if the message pack is already expired/canceled/failed.
     *
     * @return 'true' if rule engine is expecting the message to be processed, 'false' otherwise.
     */
    /**
     * 功能：判断消息。
     * 参数：无。
     * 返回：判断结果。
     */
    default boolean isMsgValid() {
        return true;
    }

    /**
     * 功能：处理`on Processing Start`。
     * 参数：
     * - `ruleNodeInfo`：`ruleNodeInfo` 参数。
     * 返回：无。
     */
    default void onProcessingStart(RuleNodeInfo ruleNodeInfo) {
    }

    /**
     * 功能：处理`on Processing End`。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：无。
     */
    default void onProcessingEnd(RuleNodeId ruleNodeId) {
    }

}
