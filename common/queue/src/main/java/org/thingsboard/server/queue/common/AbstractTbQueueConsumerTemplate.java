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
package org.thingsboard.server.queue.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * 中文说明：
 * 1. `AbstractTbQueueConsumerTemplate` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueConsumer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public abstract class AbstractTbQueueConsumerTemplate<R, T extends TbQueueMsg> implements TbQueueConsumer<T> {

    public static final long ONE_MILLISECOND_IN_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    /**
     * 是否满足`subscribed`条件。
     */
    private volatile boolean subscribed;
    protected volatile boolean stopped = false;
    /**
     * `partitions`集合，用于去重保存或快速判断对象是否存在。
     */
    protected volatile Set<TopicPartitionInfo> partitions;
    protected final ReentrantLock consumerLock = new ReentrantLock(); //NonfairSync
    final Queue<Set<TopicPartitionInfo>> subscribeQueue = new ConcurrentLinkedQueue<>();

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Getter
    private final String topic;

    /**
     * 功能：创建 `AbstractTbQueueConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public AbstractTbQueueConsumerTemplate(String topic) {
        this.topic = topic;
    }

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void subscribe() {
        log.debug("enqueue topic subscribe {} ", topic);
        if (stopped) {
            log.error("trying subscribe, but consumer stopped for topic {}", topic);
            return;
        }
        subscribeQueue.add(Collections.singleton(new TopicPartitionInfo(topic, null, null, true)));
    }

    /**
     * 功能：执行 `subscribe` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    public void subscribe(Set<TopicPartitionInfo> partitions) {
        log.debug("enqueue topics subscribe {} ", partitions);
        if (stopped) {
            log.error("trying subscribe, but consumer stopped for topic {}", topic);
            return;
        }
        subscribeQueue.add(partitions);
    }

    /**
     * 功能：执行 `poll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<T> poll(long durationInMillis) {
        List<R> records;
        long startNanos = System.nanoTime();
        if (stopped) {
            log.error("poll invoked but consumer stopped for topic " + topic, new RuntimeException("stacktrace"));
            return emptyList();
        }
        if (!subscribed && partitions == null && subscribeQueue.isEmpty()) {
            return sleepAndReturnEmpty(startNanos, durationInMillis);
        }

        if (consumerLock.isLocked()) {
            log.error("poll. consumerLock is locked. will wait with no timeout. it looks like a race conditions or deadlock topic " + topic, new RuntimeException("stacktrace"));
        }

        consumerLock.lock();
        try {
            while (!subscribeQueue.isEmpty()) {
                subscribed = false;
                partitions = subscribeQueue.poll();
            }
            if (!subscribed) {
                List<String> topicNames = getFullTopicNames();
                log.info("Subscribing to topics {}", topicNames);
                doSubscribe(topicNames);
                subscribed = true;
            }
            records = partitions.isEmpty() ? emptyList() : doPoll(durationInMillis);
        } finally {
            consumerLock.unlock();
        }

        if (records.isEmpty() && !isLongPollingSupported()) {
            return sleepAndReturnEmpty(startNanos, durationInMillis);
        }

        return decodeRecords(records);
    }

    /**
     * 功能：解析`Records`。
     * 参数：
     * - `records`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Nonnull
    List<T> decodeRecords(@Nonnull List<R> records) {
        List<T> result = new ArrayList<>(records.size());
        records.forEach(record -> {
            try {
                if (record != null) {
                    result.add(decode(record));
                }
            } catch (IOException e) {
                log.error("Failed decode record: [{}]", record);
                throw new RuntimeException("Failed to decode record: ", e);
            }
        });
        return result;
    }

    /**
     * 功能：执行 `sleepAndReturnEmpty` 对应的处理。
     * 参数：
     * - `startNanos`：`startNanos` 参数。
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    List<T> sleepAndReturnEmpty(final long startNanos, final long durationInMillis) {
        long durationNanos = TimeUnit.MILLISECONDS.toNanos(durationInMillis);
        long spentNanos = System.nanoTime() - startNanos;
        long nanosLeft = durationNanos - spentNanos;
        if (nanosLeft >= ONE_MILLISECOND_IN_NANOS) {
            try {
                long sleepMs = TimeUnit.NANOSECONDS.toMillis(nanosLeft);
                log.trace("Going to sleep after poll: topic {} for {}ms", topic, sleepMs);
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                if (!stopped) {
                    log.error("Failed to wait", e);
                }
            }
        }
        return emptyList();
    }

    /**
     * 功能：执行 `commit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void commit() {
        if (consumerLock.isLocked()) {
            log.error("commit. consumerLock is locked. will wait with no timeout. it looks like a race conditions or deadlock topic " + topic, new RuntimeException("stacktrace"));
        }
        consumerLock.lock();
        try {
            doCommit();
        } finally {
            consumerLock.unlock();
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

    /**
     * 功能：执行 `unsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void unsubscribe() {
        log.info("Unsubscribing and stopping consumer for topics {}", getFullTopicNames());
        stopped = true;
        consumerLock.lock();
        try {
            if (subscribed) {
                doUnsubscribe();
            }
        } finally {
            consumerLock.unlock();
        }
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
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    abstract protected List<R> doPoll(long durationInMillis);

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `record`：`record` 参数。
     * 返回：处理结果。
     */
    abstract protected T decode(R record) throws IOException;

    /**
     * 功能：执行 `doSubscribe` 对应的处理。
     * 参数：
     * - `topicNames`：主题名称或主题对象。
     * 返回：无。
     */
    abstract protected void doSubscribe(List<String> topicNames);

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    abstract protected void doCommit();

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    abstract protected void doUnsubscribe();

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> getFullTopicNames() {
        if (partitions == null) {
            return Collections.emptyList();
        }
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.toList());
    }

    /**
     * 功能：判断`Long Polling Supported`。
     * 参数：无。
     * 返回：判断结果。
     */
    protected boolean isLongPollingSupported() {
        return false;
    }

}
