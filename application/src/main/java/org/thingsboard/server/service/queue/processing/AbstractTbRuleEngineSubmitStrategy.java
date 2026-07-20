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

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `AbstractTbRuleEngineSubmitStrategy` 是 ThingsBoard Application 中处理 `Tb Rule Engine` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TbRuleEngineSubmitStrategy`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public abstract class AbstractTbRuleEngineSubmitStrategy implements TbRuleEngineSubmitStrategy {

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    protected final String queueName;
    protected List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> orderedMsgList;
    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped;

    /**
     * 功能：创建 `AbstractTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public AbstractTbRuleEngineSubmitStrategy(String queueName) {
        this.queueName = queueName;
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    protected abstract void doOnSuccess(UUID id);

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `msgs`：待处理消息。
     * 返回：无。
     */
    @Override
    public void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
    }

    /**
     * 功能：获取`Pending Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getPendingMap() {
        return orderedMsgList.stream().collect(Collectors.toConcurrentMap(pair -> pair.uuid, pair -> pair.msg));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `reprocessMap`：键值映射。
     * 返回：无。
     */
    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> newOrderedMsgList = new ArrayList<>(reprocessMap.size());
        for (IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            if (reprocessMap.containsKey(pair.uuid)) {
                if (StringUtils.isNotEmpty(pair.getMsg().getValue().getFailureMessage())) {
                    var toRuleEngineMsg = TransportProtos.ToRuleEngineMsg.newBuilder(pair.getMsg().getValue())
                            .clearFailureMessage()
                            .clearRelationTypes()
                            .build();
                    var newMsg = new TbProtoQueueMsg<>(pair.getMsg().getKey(), toRuleEngineMsg, pair.getMsg().getHeaders());
                    newOrderedMsgList.add(new IdMsgPair<>(pair.getUuid(), newMsg));
                } else {
                    newOrderedMsgList.add(pair);
                }
            }
        }
        orderedMsgList = newOrderedMsgList;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    public void onSuccess(UUID id) {
        if (!stopped) {
            doOnSuccess(id);
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        stopped = true;
    }
}
