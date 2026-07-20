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

import lombok.Getter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * 中文说明：
 * 1. `TbRuleEngineProcessingResult` 是 ThingsBoard Application 中承载 `Tb Rule Engine` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class TbRuleEngineProcessingResult {

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @Getter
    private final String queueName;
    /**
     * 当前操作是否成功。
     */
    @Getter
    private final boolean success;
    /**
     * 是否满足超时时间条件。
     */
    @Getter
    private final boolean timeout;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Getter
    private final TbMsgPackProcessingContext ctx;

    /**
     * 功能：创建 `TbRuleEngineProcessingResult` 实例，并初始化必要字段。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `timeout`：`timeout` 参数。
     * - `ctx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public TbRuleEngineProcessingResult(String queueName, boolean timeout, TbMsgPackProcessingContext ctx) {
        this.queueName = queueName;
        this.timeout = timeout;
        this.ctx = ctx;
        this.success = !timeout && ctx.getPendingMap().isEmpty() && ctx.getFailedMap().isEmpty();
    }

    /**
     * 功能：获取`Pending Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getPendingMap() {
        return ctx.getPendingMap();
    }

    /**
     * 功能：获取`Success Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getSuccessMap() {
        return ctx.getSuccessMap();
    }

    /**
     * 功能：获取`Failed Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> getFailedMap() {
        return ctx.getFailedMap();
    }

    /**
     * 功能：获取`Exceptions Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<TenantId, RuleEngineException> getExceptionsMap() {
        return ctx.getExceptionsMap();
    }
}
