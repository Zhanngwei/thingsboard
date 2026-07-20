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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `InMemoryTbQueueConsumer` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueConsumer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    /**
     * `storage` 字段，保存当前对象的对应属性。
     */
    private final InMemoryStorage storage;
    private volatile Set<TopicPartitionInfo> partitions;
    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped;
    private volatile boolean subscribed;

    /**
     * 功能：创建 `InMemoryTbQueueConsumer` 实例，并初始化必要字段。
     * 参数：
     * - `storage`：`storage` 参数。
     * - `topic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public InMemoryTbQueueConsumer(InMemoryStorage storage, String topic) {
        this.storage = storage;
        this.topic = topic;
        stopped = false;
    }

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String topic;

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getTopic() {
        return topic;
    }

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void subscribe() {
        partitions = Collections.singleton(new TopicPartitionInfo(topic, null, null, true));
        subscribed = true;
    }

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        subscribed = true;
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

    /**
     * 功能：执行 `unsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void unsubscribe() {
        stopped = true;
        subscribed = false;
    }

    /**
     * 功能：执行 `poll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<T> poll(long durationInMillis) {
        if (subscribed) {
            @SuppressWarnings("unchecked")
            List<T> messages = partitions
                    .stream()
                    .map(tpi -> {
                        try {
                            return storage.get(tpi.getFullTopicName());
                        } catch (InterruptedException e) {
                            if (!stopped) {
                                log.error("Queue was interrupted.", e);
                            }
                            return Collections.emptyList();
                        }
                    })
                    .flatMap(List::stream)
                    .map(msg -> (T) msg).collect(Collectors.toList());
            if (messages.size() > 0) {
                return messages;
            }
            try {
                Thread.sleep(durationInMillis);
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.error("Failed to sleep.", e);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void commit() {
    }

    /**
     * 功能：判断`Stopped`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isStopped() {
        return stopped;
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> getFullTopicNames() {
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
    }

}
