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
import com.google.protobuf.InvalidProtocolBufferException;
import com.microsoft.azure.servicebus.TransactionContext;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.CoreMessageReceiver;
import com.microsoft.azure.servicebus.primitives.MessageWithDeliveryTag;
import com.microsoft.azure.servicebus.primitives.MessagingEntityType;
import com.microsoft.azure.servicebus.primitives.MessagingFactory;
import com.microsoft.azure.servicebus.primitives.SettleModePair;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. 类目的：`TbServiceBusConsumerTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbServiceBusConsumerTemplate<T extends TbQueueMsg> extends AbstractTbQueueConsumerTemplate<MessageWithDeliveryTag, T> {
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final TbQueueMsgDecoder<T> decoder;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbServiceBusSettings serviceBusSettings;

    private final Gson gson = new Gson();

    /**
     * `receivers`集合，用于去重保存或快速判断对象是否存在。
     */
    private Set<CoreMessageReceiver> receivers;
    private final Map<CoreMessageReceiver, Collection<MessageWithDeliveryTag>> pendingMessages = new ConcurrentHashMap<>();
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private volatile int messagesPerQueue;

    /**
     * 功能：创建 `TbServiceBusConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `serviceBusSettings`：服务对象。
     * - `topic`：主题名称或主题对象。
     * - `decoder`：`decoder` 参数。
     * 返回：新创建的对象实例。
     */
    public TbServiceBusConsumerTemplate(TbQueueAdmin admin, TbServiceBusSettings serviceBusSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.serviceBusSettings = serviceBusSettings;
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected List<MessageWithDeliveryTag> doPoll(long durationInMillis) {
        List<CompletableFuture<Collection<MessageWithDeliveryTag>>> messageFutures =
                receivers.stream()
                        .map(receiver -> receiver
                                .receiveAsync(messagesPerQueue, Duration.ofMillis(durationInMillis))
                                .whenComplete((messages, err) -> {
                                    if (!CollectionUtils.isEmpty(messages)) {
                                        pendingMessages.put(receiver, messages);
                                    } else if (err != null) {
                                        log.error("Failed to receive messages.", err);
                                    }
                                }))
                        .collect(Collectors.toList());
        try {
            return fromList(messageFutures)
                    .get()
                    .stream()
                    .flatMap(messages -> CollectionUtils.isEmpty(messages) ? Stream.empty() : messages.stream())
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to receive messages", e);
            }
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
        createReceivers();
        messagesPerQueue = receivers.size() / Math.max(partitions.size(), 1);
    }

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doCommit() {
        pendingMessages.forEach((receiver, msgs) ->
                msgs.forEach(msg -> receiver.completeMessageAsync(msg.getDeliveryTag(), TransactionContext.NULL_TXN)));
        pendingMessages.clear();
    }

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doUnsubscribe() {
        receivers.forEach(CoreMessageReceiver::closeAsync);
    }

    /**
     * 功能：保存或创建`Receivers`。
     * 参数：无。
     * 返回：无。
     */
    private void createReceivers() {
        List<CompletableFuture<CoreMessageReceiver>> receiverFutures = partitions.stream()
                .map(TopicPartitionInfo::getFullTopicName)
                .map(queue -> {
                    MessagingFactory factory;
                    try {
                        factory = MessagingFactory.createFromConnectionStringBuilder(createConnection(queue));
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Failed to create factory for the queue [{}]", queue);
                        throw new RuntimeException("Failed to create the factory", e);
                    }

                    return CoreMessageReceiver.create(factory, queue, queue, 0,
                            new SettleModePair(SenderSettleMode.UNSETTLED, ReceiverSettleMode.SECOND),
                            MessagingEntityType.QUEUE);
                }).collect(Collectors.toList());

        try {
            receivers = new HashSet<>(fromList(receiverFutures).get());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Service Bus consumer is stopped.", getTopic());
            } else {
                log.error("Failed to create receivers", e);
            }
        }
    }

    /**
     * 功能：保存或创建`Connection`。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    private ConnectionStringBuilder createConnection(String queue) {
        admin.createTopicIfNotExists(queue);
        return new ConnectionStringBuilder(
                serviceBusSettings.getNamespaceName(),
                queue,
                serviceBusSettings.getSasKeyName(),
                serviceBusSettings.getSasKey());
    }

    /**
     * 功能：执行 `fromList` 对应的处理。
     * 参数：
     * - `futures`：数据列表。
     * 返回：匹配的数据集合。
     */
    private <V> CompletableFuture<List<V>> fromList(List<CompletableFuture<V>> futures) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Collection<V>>[] arrayFuture = new CompletableFuture[futures.size()];
        futures.toArray(arrayFuture);

        return CompletableFuture
                .allOf(arrayFuture)
                .thenApply(v -> futures
                        .stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    @Override
    protected T decode(MessageWithDeliveryTag data) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(new String(((Data) data.getMessage().getBody()).getValue().getArray()), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbServiceBusConsumerTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
