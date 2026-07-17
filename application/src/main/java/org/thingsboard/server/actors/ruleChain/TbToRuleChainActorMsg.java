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
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;

/**
 * 中文说明：
 * 1. `TbToRuleChainActorMsg` 是 ThingsBoard Application 中承载规则链信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbRuleEngineActorMsg`、`RuleChainAwareMsg`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public abstract class TbToRuleChainActorMsg extends TbRuleEngineActorMsg implements RuleChainAwareMsg {

    /**
     * 目标对象，表示当前对象的对应属性。
     */
    @Getter
    private final RuleChainId target;

    /**
     * 功能：创建 `TbToRuleChainActorMsg` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * - `target`：`target` 参数。
     * 返回：新创建的对象实例。
     */
    public TbToRuleChainActorMsg(TbMsg msg, RuleChainId target) {
        super(msg);
        this.target = target;
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public RuleChainId getRuleChainId() {
        return target;
    }

    /**
     * 功能：处理Actor 实例。
     * 参数：
     * - `reason`：`reason` 参数。
     * 返回：无。
     */
    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message = reason == TbActorStopReason.STOPPED ? String.format("Rule chain [%s] stopped", target.getId()) : String.format("Failed to initialize rule chain [%s]!", target.getId());
        msg.getCallback().onFailure(new RuleEngineException(message));
    }
}
