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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorStopReason;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbRuleEngineActorMsg;

import java.util.Set;

/**
 * Created by ashvayka on 15.03.18.
 */
/**
 * 中文说明：
 * 1. `QueueToRuleEngineMsg` 是 ThingsBoard Common Message 中承载队列信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbRuleEngineActorMsg`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class QueueToRuleEngineMsg extends TbRuleEngineActorMsg {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private final TenantId tenantId;
    /**
     * 关系集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final Set<String> relationTypes;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    private final String failureMessage;

    /**
     * 功能：创建 `QueueToRuleEngineMsg` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tbMsg`：待处理消息。
     * - `relationTypes`：类型。
     * - `failureMessage`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public QueueToRuleEngineMsg(TenantId tenantId, TbMsg tbMsg, Set<String> relationTypes, String failureMessage) {
        super(tbMsg);
        this.tenantId = tenantId;
        this.relationTypes = relationTypes;
        this.failureMessage = failureMessage;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public MsgType getMsgType() {
        return MsgType.QUEUE_TO_RULE_ENGINE_MSG;
    }

    /**
     * 功能：处理Actor 实例。
     * 参数：
     * - `reason`：`reason` 参数。
     * 返回：无。
     */
    @Override
    public void onTbActorStopped(TbActorStopReason reason) {
        String message;
        if (msg.getRuleChainId() != null) {
            message = reason == TbActorStopReason.STOPPED ?
                    String.format("Rule chain [%s] stopped", msg.getRuleChainId().getId()) :
                    String.format("Failed to initialize rule chain [%s]!", msg.getRuleChainId().getId());
        } else {
            message = reason == TbActorStopReason.STOPPED ? "Rule chain stopped" : "Failed to initialize rule chain!";
        }
        msg.getCallback().onFailure(new RuleEngineException(message));
    }

    /**
     * 功能：判断`Tell Next`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isTellNext() {
        return relationTypes != null && !relationTypes.isEmpty();
    }

}
