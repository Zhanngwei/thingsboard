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
package org.thingsboard.server.service.queue.ruleengine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `TbQueueConsumerTask` 是 ThingsBoard Application 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@RequiredArgsConstructor
@Slf4j
public class TbQueueConsumerTask {

    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Getter
    private final Object key;
    /**
     * 消息消费者，承载当前流程需要传递的内容。
     */
    @Getter
    private final TbQueueConsumer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> consumer;

    /**
     * `task` 字段，保存当前对象的对应属性。
     */
    @Setter
    private Future<?> task;

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.trace("[{}] Subscribing to partitions: {}", key, partitions);
        consumer.subscribe(partitions);
    }

    /**
     * 功能：执行 `initiateStop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void initiateStop() {
        log.debug("[{}] Initiating stop", key);
        consumer.stop();
    }

    /**
     * 功能：执行 `awaitCompletion` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void awaitCompletion() {
        log.trace("[{}] Awaiting finish", key);
        if (isRunning()) {
            try {
                task.get(30, TimeUnit.SECONDS);
                log.trace("[{}] Awaited finish", key);
            } catch (Exception e) {
                log.warn("[{}] Failed to await for consumer to stop", key, e);
            }
            task = null;
        }
    }

    /**
     * 功能：判断`Running`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isRunning() {
        return task != null;
    }

}
