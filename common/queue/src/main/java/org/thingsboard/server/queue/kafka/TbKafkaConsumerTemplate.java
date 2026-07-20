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
package org.thingsboard.server.queue.kafka;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.util.StopWatch;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Created by ashvayka on 24.09.18.
 */
/**
 * 中文说明：
 * 1. `TbKafkaConsumerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Kafka Consumer` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbKafkaConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<ConsumerRecord<String, byte[]>, T> {

    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final KafkaConsumer<String, byte[]> consumer;
    /**
     * 解码器，表示当前对象的对应属性。
     */
    private final TbKafkaDecoder<T> decoder;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbKafkaConsumerStatsService statsService;
    private final String groupId;

    /**
     * 功能：创建 `TbKafkaConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `settings`：配置对象。
     * - `decoder`：`decoder` 参数。
     * - `clientId`：客户端ID。
     * - `groupId`：`groupId`ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private TbKafkaConsumerTemplate(TbKafkaSettings settings, TbKafkaDecoder<T> decoder,
                                    String clientId, String groupId, String topic,
                                    TbQueueAdmin admin, TbKafkaConsumerStatsService statsService) {
        super(topic);
        Properties props = settings.toConsumerProps(topic);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        if (groupId != null) {
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        }

        this.statsService = statsService;
        this.groupId = groupId;

        if (statsService != null) {
            statsService.registerClientGroup(groupId);
        }

        this.admin = admin;
        this.consumer = new KafkaConsumer<>(props);
        this.decoder = decoder;
    }

    /**
     * 功能：执行 `doSubscribe` 对应的处理。
     * 参数：
     * - `topicNames`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    protected void doSubscribe(List<String> topicNames) {
        if (!topicNames.isEmpty()) {
            topicNames.forEach(admin::createTopicIfNotExists);
            consumer.subscribe(topicNames);
        } else {
            log.info("unsubscribe due to empty topic list");
            consumer.unsubscribe();
        }
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：处理结果。
     */
    @Override
    protected List<ConsumerRecord<String, byte[]>> doPoll(long durationInMillis) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.trace("poll topic {} maxDuration {}", getTopic(), durationInMillis);

        ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(durationInMillis));

        stopWatch.stop();
        log.trace("poll topic {} took {}ms", getTopic(), stopWatch.getTotalTimeMillis());

        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<ConsumerRecord<String, byte[]>> recordList = new ArrayList<>(256);
            records.forEach(recordList::add);
            return recordList;
        }
    }

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `record`：`record` 参数。
     * 返回：处理结果。
     */
    @Override
    public T decode(ConsumerRecord<String, byte[]> record) throws IOException {
        return decoder.decode(new KafkaTbQueueMsg(record));
    }

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doCommit() {
        consumer.commitSync();
    }

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doUnsubscribe() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
        }
        if (statsService != null) {
            statsService.unregisterClientGroup(groupId);
        }
    }

    /**
     * 功能：判断`Long Polling Supported`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isLongPollingSupported() {
        return true;
    }

}
