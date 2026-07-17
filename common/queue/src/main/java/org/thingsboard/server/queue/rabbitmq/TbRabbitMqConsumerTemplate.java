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
package org.thingsboard.server.queue.rabbitmq;

import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbRabbitMqConsumerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Rabbit Mq` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbRabbitMqConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<GetResponse, T> {

    private final Gson gson = new Gson();
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final TbQueueMsgDecoder<T> decoder;
    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    private final Channel channel;
    private final Connection connection;

    /**
     * `queues`集合，用于去重保存或快速判断对象是否存在。
     */
    private volatile Set<String> queues;

    /**
     * 功能：创建 `TbRabbitMqConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `rabbitMqSettings`：配置对象。
     * - `topic`：主题名称或主题对象。
     * - `decoder`：`decoder` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRabbitMqConsumerTemplate(TbQueueAdmin admin, TbRabbitMqSettings rabbitMqSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        try {
            connection = rabbitMqSettings.getConnectionFactory().newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("Failed to create connection.", e);
            throw new RuntimeException("Failed to create connection.", e);
        }
        try {
            channel = connection.createChannel();
        } catch (IOException e) {
            log.error("Failed to create chanel.", e);
            throw new RuntimeException("Failed to create chanel.", e);
        }
        stopped = false;
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected List<GetResponse> doPoll(long durationInMillis) {
        List<GetResponse> result = queues.stream()
                .map(queue -> {
                    try {
                        return channel.basicGet(queue, false);
                    } catch (IOException e) {
                        log.error("Failed to get messages from queue: [{}]", queue);
                        throw new RuntimeException("Failed to get messages from queue.", e);
                    }
                }).filter(Objects::nonNull).collect(Collectors.toList());
        if (result.size() > 0) {
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 功能：执行 `doSubscribe` 对应的处理。
     * 参数：
     * - `topicNames`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    protected void doSubscribe(List<String> topicNames) {
        queues = partitions.stream()
                .map(TopicPartitionInfo::getFullTopicName)
                .collect(Collectors.toSet());
        queues.forEach(admin::createTopicIfNotExists);
    }

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doCommit() {
        try {
            channel.basicAck(0, true);
        } catch (IOException e) {
            log.error("Failed to ack messages.", e);
        }
    }

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doUnsubscribe() {
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException | TimeoutException e) {
                log.error("Failed to close the channel.");
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (IOException e) {
                log.error("Failed to close the connection.");
            }
        }
    }

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    public T decode(GetResponse message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(message.getBody()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }
}
