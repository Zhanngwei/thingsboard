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

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：
 * 1. `TbRabbitMqProducerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Rabbit Mq` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueProducer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbRabbitMqProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String defaultTopic;
    private final Gson gson = new Gson();
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final TbRabbitMqSettings rabbitMqSettings;
    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private final ListeningExecutorService producerExecutor;
    private final Channel channel;
    /**
     * `connection` 字段，保存当前对象的对应属性。
     */
    private final Connection connection;

    private final Set<TopicPartitionInfo> topics = ConcurrentHashMap.newKeySet();

    /**
     * 功能：创建 `TbRabbitMqProducerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `rabbitMqSettings`：配置对象。
     * - `defaultTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public TbRabbitMqProducerTemplate(TbQueueAdmin admin, TbRabbitMqSettings rabbitMqSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.rabbitMqSettings = rabbitMqSettings;
        producerExecutor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
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
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getDefaultTopic() {
        return defaultTopic;
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
        createTopicIfNotExist(tpi);
        AMQP.BasicProperties properties = new AMQP.BasicProperties();
        try {
            channel.basicPublish(rabbitMqSettings.getExchangeName(), tpi.getFullTopicName(), properties, gson.toJson(new DefaultTbQueueMsg(msg)).getBytes());
            if (callback != null) {
                callback.onSuccess(null);
            }
        } catch (IOException e) {
            log.error("Failed publish message: [{}].", msg, e);
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        if (producerExecutor != null) {
            producerExecutor.shutdownNow();
        }
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
}
