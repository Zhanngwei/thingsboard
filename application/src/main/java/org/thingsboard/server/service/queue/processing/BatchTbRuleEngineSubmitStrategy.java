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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 中文说明：
 * 1. `BatchTbRuleEngineSubmitStrategy` 是 ThingsBoard Application 中处理 `Batch Tb Rule` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractTbRuleEngineSubmitStrategy`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class BatchTbRuleEngineSubmitStrategy extends AbstractTbRuleEngineSubmitStrategy {

    /**
     * 批量大小，用于控制处理规模或位置。
     */
    private final int batchSize;
    private final AtomicInteger packIdx = new AtomicInteger(0);
    private final Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> pendingPack = new LinkedHashMap<>();
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private volatile BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer;

    /**
     * 功能：创建 `BatchTbRuleEngineSubmitStrategy` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `batchSize`：`batchSize` 参数。
     * 返回：新创建的对象实例。
     */
    public BatchTbRuleEngineSubmitStrategy(String queueName, int batchSize) {
        super(queueName);
        this.batchSize = batchSize;
    }

    /**
     * 功能：发送或提交`Attempt`。
     * 参数：
     * - `msgConsumer`：待处理消息。
     * 返回：无。
     */
    @Override
    public void submitAttempt(BiConsumer<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgConsumer) {
        this.msgConsumer = msgConsumer;
        submitNext();
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `reprocessMap`：键值映射。
     * 返回：无。
     */
    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        super.update(reprocessMap);
        packIdx.set(0);
    }

    /**
     * 功能：执行 `doOnSuccess` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    protected void doOnSuccess(UUID id) {
        boolean endOfPendingPack;
        synchronized (pendingPack) {
            TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg> msg = pendingPack.remove(id);
            endOfPendingPack = msg != null && pendingPack.isEmpty();
        }
        if (endOfPendingPack) {
            packIdx.incrementAndGet();
            submitNext();
        }
    }

    /**
     * 功能：发送或提交`Next`。
     * 参数：无。
     * 返回：无。
     */
    private void submitNext() {
        int listSize = orderedMsgList.size();
        int startIdx = Math.min(packIdx.get() * batchSize, listSize);
        int endIdx = Math.min(startIdx + batchSize, listSize);
        Map<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> tmpPack;
        synchronized (pendingPack) {
            pendingPack.clear();
            for (int i = startIdx; i < endIdx; i++) {
                IdMsgPair<TransportProtos.ToRuleEngineMsg> pair = orderedMsgList.get(i);
                pendingPack.put(pair.uuid, pair.msg);
            }
            tmpPack = new LinkedHashMap<>(pendingPack);
        }
        int submitSize = pendingPack.size();
        if (log.isDebugEnabled() && submitSize > 0) {
            log.debug("[{}] submitting [{}] messages to rule engine", queueName, submitSize);
        }
        tmpPack.forEach(msgConsumer);
    }

}
