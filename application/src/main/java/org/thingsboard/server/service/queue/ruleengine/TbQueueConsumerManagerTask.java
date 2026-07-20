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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

import java.util.Set;

/**
 * 中文说明：
 * 1. `TbQueueConsumerManagerTask` 是 ThingsBoard Application 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Getter
@ToString
@AllArgsConstructor
public class TbQueueConsumerManagerTask {

    /**
     * 事件，表示当前对象的对应属性。
     */
    private final QueueEvent event;
    private Queue queue;
    /**
     * `partitions`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<TopicPartitionInfo> partitions;
    private boolean drainQueue;

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `drainQueue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    public static TbQueueConsumerManagerTask delete(boolean drainQueue) {
        return new TbQueueConsumerManagerTask(QueueEvent.DELETE, null, null, drainQueue);
    }

    /**
     * 功能：执行 `configUpdate` 对应的处理。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    public static TbQueueConsumerManagerTask configUpdate(Queue queue) {
        return new TbQueueConsumerManagerTask(QueueEvent.CONFIG_UPDATE, queue, null, false);
    }

    /**
     * 功能：执行 `partitionChange` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：处理结果。
     */
    public static TbQueueConsumerManagerTask partitionChange(Set<TopicPartitionInfo> partitions) {
        return new TbQueueConsumerManagerTask(QueueEvent.PARTITION_CHANGE, null, partitions, false);
    }

}
