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
package org.thingsboard.server.queue.azure.servicebus;

import com.google.gson.Gson;
import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.Message;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. `TbServiceBusProducerTemplate` 是 ThingsBoard Common Queue 中围绕 `Tb Bus Producer` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueProducer`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbServiceBusProducerTemplate<T extends TbQueueMsg> implements TbQueueProducer<T> {
    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String defaultTopic;
    private final Gson gson = new Gson();
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final TbServiceBusSettings serviceBusSettings;
    private final Map<String, QueueClient> clients = new ConcurrentHashMap<>();
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final ExecutorService executorService;

    /**
     * 功能：创建 `TbServiceBusProducerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `serviceBusSettings`：服务对象。
     * - `defaultTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public TbServiceBusProducerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String defaultTopic) {
        this.admin = admin;
        this.defaultTopic = defaultTopic;
        this.serviceBusSettings = serviceBusSettings;
        executorService = Executors.newCachedThreadPool();
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
        IMessage message = new Message(gson.toJson(new DefaultTbQueueMsg(msg)));
        CompletableFuture<Void> future = getClient(tpi.getFullTopicName()).sendAsync(message);
        future.whenCompleteAsync((success, err) -> {
            if (err != null) {
                callback.onFailure(err);
            } else {
                callback.onSuccess(null);
            }
        }, executorService);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        clients.forEach((t, client) -> {
            try {
                client.close();
            } catch (ServiceBusException e) {
                log.error("Failed to close QueueClient.", e);
            }
        });

        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    private QueueClient getClient(String topic) {
        return clients.computeIfAbsent(topic, k -> {
            admin.createTopicIfNotExists(topic);
            ConnectionStringBuilder builder =
                    new ConnectionStringBuilder(
                            serviceBusSettings.getNamespaceName(),
                            topic,
                            serviceBusSettings.getSasKeyName(),
                            serviceBusSettings.getSasKey());
            try {
                return new QueueClient(builder, ReceiveMode.PEEKLOCK);
            } catch (InterruptedException | ServiceBusException e) {
                log.error("Failed to create new client for the Queue: [{}]", topic, e);
                throw new RuntimeException("Failed to create new client for the Queue", e);
            }
        });
    }
}
