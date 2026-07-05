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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgDecoder;
import org.thingsboard.server.queue.common.AbstractParallelTbQueueConsumerTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. 类目的：`TbAwsSqsConsumerTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbAwsSqsConsumerTemplate<T extends TbQueueMsg> extends AbstractParallelTbQueueConsumerTemplate<Message, T> {

    /**
     * `MAX_NUM_MSGS`常量，用于统一引用固定值。
     */
    private static final int MAX_NUM_MSGS = 10;

    private final Gson gson = new Gson();
    /**
     * `admin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin admin;
    private final AmazonSQS sqsClient;
    /**
     * 解码器，表示当前对象的对应属性。
     */
    private final TbQueueMsgDecoder<T> decoder;
    private final TbAwsSqsSettings sqsSettings;

    private final List<AwsSqsMsgWrapper> pendingMessages = new CopyOnWriteArrayList<>();
    /**
     * 队列集合，用于去重保存或快速判断对象是否存在。
     */
    private volatile Set<String> queueUrls;

    /**
     * 功能：创建 `TbAwsSqsConsumerTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `admin`：`admin` 参数。
     * - `sqsSettings`：配置对象。
     * - `topic`：主题名称或主题对象。
     * - `decoder`：`decoder` 参数。
     * 返回：新创建的对象实例。
     */
    public TbAwsSqsConsumerTemplate(TbQueueAdmin admin, TbAwsSqsSettings sqsSettings, String topic, TbQueueMsgDecoder<T> decoder) {
        super(topic);
        this.admin = admin;
        this.decoder = decoder;
        this.sqsSettings = sqsSettings;

        AWSCredentialsProvider credentialsProvider;
        if (sqsSettings.getUseDefaultCredentialProviderChain()) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        }

        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

    }

    /**
     * 功能：执行 `doSubscribe` 对应的处理。
     * 参数：
     * - `topicNames`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    protected void doSubscribe(List<String> topicNames) {
        queueUrls = topicNames.stream().map(this::getQueueUrl).collect(Collectors.toSet());
        initNewExecutor(queueUrls.size() * sqsSettings.getThreadsPerTopic() + 1);
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：
     * - `durationInMillis`：`durationInMillis` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected List<Message> doPoll(long durationInMillis) {
        int duration = (int) TimeUnit.MILLISECONDS.toSeconds(durationInMillis);
        List<ListenableFuture<List<Message>>> futureList = queueUrls
                .stream()
                .map(url -> poll(url, duration))
                .collect(Collectors.toList());
        ListenableFuture<List<List<Message>>> futureResult = Futures.allAsList(futureList);
        try {
            return futureResult.get().stream()
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            if (stopped) {
                log.info("[{}] Aws SQS consumer is stopped.", getTopic());
            } else {
                log.error("Failed to pool messages.", e);
            }
            return Collections.emptyList();
        }
    }

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    public T decode(Message message) throws InvalidProtocolBufferException {
        DefaultTbQueueMsg msg = gson.fromJson(message.getBody(), DefaultTbQueueMsg.class);
        return decoder.decode(msg);
    }

    /**
     * 功能：执行 `doCommit` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doCommit() {
        pendingMessages.forEach(msg ->
                consumerExecutor.submit(() -> {
                    List<DeleteMessageBatchRequestEntry> entries = msg.getMessages()
                            .stream()
                            .map(message -> new DeleteMessageBatchRequestEntry(message.getMessageId(), message.getReceiptHandle()))
                            .collect(Collectors.toList());
                    sqsClient.deleteMessageBatch(msg.getUrl(), entries);
                }));
        pendingMessages.clear();
    }

    /**
     * 功能：执行 `doUnsubscribe` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void doUnsubscribe() {
        stopped = true;
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        shutdownExecutor();
    }

    /**
     * 功能：执行 `poll` 对应的处理。
     * 参数：
     * - `url`：`url` 参数。
     * - `waitTimeSeconds`：`waitTimeSeconds` 参数。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<Message>> poll(String url, int waitTimeSeconds) {
        List<ListenableFuture<List<Message>>> result = new ArrayList<>();

        for (int i = 0; i < sqsSettings.getThreadsPerTopic(); i++) {
            result.add(consumerExecutor.submit(() -> {
                ReceiveMessageRequest request = new ReceiveMessageRequest();
                request
                        .withWaitTimeSeconds(waitTimeSeconds)
                        .withQueueUrl(url)
                        .withMaxNumberOfMessages(MAX_NUM_MSGS);
                return sqsClient.receiveMessage(request).getMessages();
            }));
        }
        return Futures.transform(Futures.allAsList(result), list -> {
            if (!CollectionUtils.isEmpty(list)) {
                return list.stream()
                        .flatMap(messageList -> {
                            if (!messageList.isEmpty()) {
                                this.pendingMessages.add(new AwsSqsMsgWrapper(url, messageList));
                                return messageList.stream();
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }, consumerExecutor);
    }

    /**
     * 中文说明：
     * 1. 类目的：`AwsSqsMsgWrapper` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    @Data
    private static class AwsSqsMsgWrapper {
        /**
         * URL 地址，用于定位外部资源或本地资源。
         */
        private final String url;
        private final List<Message> messages;

        /**
         * 功能：创建 `TbAwsSqsConsumerTemplate` 实例，并初始化必要字段。
         * 参数：
         * - `url`：`url` 参数。
         * - `messages`：待处理消息。
         * 返回：新创建的对象实例。
         */
        public AwsSqsMsgWrapper(String url, List<Message> messages) {
            this.url = url;
            this.messages = messages;
        }
    }

    /**
     * 功能：获取队列。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：文本结果。
     */
    private String getQueueUrl(String topic) {
        admin.createTopicIfNotExists(topic);
        return sqsClient.getQueueUrl(topic.replaceAll("\\.", "_") + ".fifo").getQueueUrl();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbAwsSqsConsumerTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
