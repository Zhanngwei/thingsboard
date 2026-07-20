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
package org.thingsboard.server.service.queue.processing;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `SequentialByOriginatorIdTbRuleEngineSubmitStrategy` 是 ThingsBoard Application 中处理 `Sequential By Originator` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `SequentialByEntityIdTbRuleEngineSubmitStrategy`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class SequentialByOriginatorIdTbRuleEngineSubmitStrategy extends SequentialByEntityIdTbRuleEngineSubmitStrategy {

    /**
     * 功能：创建 `SequentialByOriginatorIdTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public SequentialByOriginatorIdTbRuleEngineSubmitStrategy(String queueName) {
        super(queueName);
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected EntityId getEntityId(TransportProtos.ToRuleEngineMsg msg) {
        try {
            MsgProtos.TbMsgProto proto = MsgProtos.TbMsgProto.parseFrom(msg.getTbMsg());
            return EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
        } catch (InvalidProtocolBufferException e) {
            log.warn("[{}] Failed to parse TbMsg: {}", queueName, msg);
            return null;
        }
    }
}
