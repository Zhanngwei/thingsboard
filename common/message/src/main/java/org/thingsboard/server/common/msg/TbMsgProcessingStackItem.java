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
package org.thingsboard.server.common.msg;

import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.gen.MsgProtos;

import java.io.Serializable;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TbMsgProcessingStackItem` 是 ThingsBoard Common Message 中承载消息信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class TbMsgProcessingStackItem implements Serializable {

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    private final RuleChainId ruleChainId;
    private final RuleNodeId ruleNodeId;

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    MsgProtos.TbMsgProcessingStackItemProto toProto() {
        return MsgProtos.TbMsgProcessingStackItemProto.newBuilder()
                .setRuleChainIdMSB(ruleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(ruleChainId.getId().getLeastSignificantBits())
                .setRuleNodeIdMSB(ruleNodeId.getId().getMostSignificantBits())
                .setRuleNodeIdLSB(ruleNodeId.getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `item`：`item` 参数。
     * 返回：处理结果。
     */
    static TbMsgProcessingStackItem fromProto(MsgProtos.TbMsgProcessingStackItemProto item){
        return new TbMsgProcessingStackItem(
                new RuleChainId(new UUID(item.getRuleChainIdMSB(), item.getRuleChainIdLSB())),
                new RuleNodeId(new UUID(item.getRuleNodeIdMSB(), item.getRuleNodeIdLSB()))
        );
    }

}
