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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 24.09.18.
 */
/**
 * 中文说明：
 * 1. `TbKafkaProducerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Kafka Producer` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueProducer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbKafkaProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {

    /**
     * 消息生产者列表，用于保存一组待处理对象。
     */
    private final KafkaProducer<String, byte[]> producer;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Getter
    private final String defaultTopic;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private final TbKafkaSettings settings;

    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;

    /**
     * `topics`集合，用于去重保存或快速判断对象是否存在。
     */
    private final Set<TopicPartitionInfo> topics;

    /**
     * 客户端ID，用于定位对应业务对象。
     */
    @Getter
    private final String clientId;

    /**
     * 功能：创建 `TbKafkaProducerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `settings`：配置对象。
     * - `defaultTopic`：主题名称或主题对象。
     * - `clientId`：客户端ID。
     * - `admin`：`admin` 参数。
     * 返回：新创建的对象实例。
     */
    @Builder
    private TbKafkaProducerTemplate(TbKafkaSettings settings, String defaultTopic, String clientId, TbQueueAdmin admin) {
        Properties props = settings.toProducerProps();

        this.clientId = Objects.requireNonNull(clientId, "Kafka producer client.id is null");
        if (!StringUtils.isEmpty(clientId)) {
            props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        }
        this.settings = settings;

        this.producer = new KafkaProducer<>(props);
        this.defaultTopic = defaultTopic;
        this.admin = admin;
        topics = ConcurrentHashMap.newKeySet();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void init() {
    }

    /**
     * 功能：保存或创建`Analytic Headers`。
     * 参数：
     * - `headers`：数据列表。
     * 返回：无。
     */
    void addAnalyticHeaders(List<Header> headers) {
        headers.add(new RecordHeader("_producerId", getClientId().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("_threadName", Thread.currentThread().getName().getBytes(StandardCharsets.UTF_8)));
        if (log.isTraceEnabled()) {
            try {
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                int maxLevel = Math.min(stackTrace.length, 20);
                for (int i = 2; i < maxLevel; i++) { // ignore two levels: getStackTrace and addAnalyticHeaders
                    headers.add(new RecordHeader("_stackTrace" + i, stackTrace[i].toString().getBytes(StandardCharsets.UTF_8)));
                }
            } catch (Throwable t) {
                log.trace("Failed to add stacktrace headers in Kafka producer {}", getClientId(), t);
            }
        }
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void send(TopicPartitionInfo tpi, T msg, TbQueueCallback callback) {
        try {
            createTopicIfNotExist(tpi);
            String key = msg.getKey().toString();
            byte[] data = msg.getData();
            ProducerRecord<String, byte[]> record;
            List<Header> headers = msg.getHeaders().getData().entrySet().stream().map(e -> new RecordHeader(e.getKey(), e.getValue())).collect(Collectors.toList());
            if (log.isDebugEnabled()) {
                addAnalyticHeaders(headers);
            }
            record = new ProducerRecord<>(tpi.getFullTopicName(), null, key, data, headers);
            producer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    if (callback != null) {
                        callback.onSuccess(new KafkaTbQueueMsgMetadata(metadata));
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure(exception);
                    } else {
                        log.warn("Producer template failure: {}", exception.getMessage(), exception);
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            } else {
                log.warn("Producer template failure (send method wrapper): {}", e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * 功能：保存或创建主题。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * 返回：无。
     */
    private void createTopicIfNotExist(TopicPartitionInfo tpi) {
        if (topics.contains(tpi)) {
            return;
        }
        admin.createTopicIfNotExists(tpi.getFullTopicName());
        topics.add(tpi);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        if (producer != null) {
            producer.close();
        }
    }
}
