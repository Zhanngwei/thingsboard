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
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ExecutorProvider;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.util.PropertyUtils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`TbAwsSqsAdmin` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbAwsSqsAdmin implements TbQueueAdmin {

    /**
     * `attributes`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> attributes;
    private final AmazonSQS sqsClient;
    /**
     * `queues`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> queues;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Getter
    private final ExecutorService producerExecutor;

    /**
     * 功能：创建 `TbAwsSqsAdmin` 实例，并初始化必要字段。
     * 参数：
     * - `sqsSettings`：配置对象。
     * - `attributes`：键值映射。
     * 返回：新创建的对象实例。
     */
    public TbAwsSqsAdmin(TbAwsSqsSettings sqsSettings, Map<String, String> attributes) {
        this.attributes = attributes;

        AWSCredentialsProvider credentialsProvider;
        if (sqsSettings.getUseDefaultCredentialProviderChain()) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            AWSCredentials awsCredentials = new BasicAWSCredentials(sqsSettings.getAccessKeyId(), sqsSettings.getSecretAccessKey());
            credentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        }
        producerExecutor = ThingsBoardExecutors.newWorkStealingPool(sqsSettings.getThreadPoolSize(), "aws-sqs-queue-executor");

        sqsClient = AmazonSQSClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(sqsSettings.getRegion())
                .build();

        queues = sqsClient
                .listQueues()
                .getQueueUrls()
                .stream()
                .map(this::getQueueNameFromUrl)
                .collect(Collectors.toMap(this::convertTopicToQueueName, Function.identity()));
    }

    /**
     * 功能：保存或创建主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `properties`：`properties` 参数。
     * 返回：无。
     */
    @Override
    public void createTopicIfNotExists(String topic, String properties) {
        String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            return;
        }
        Map<String, String> attributes = PropertyUtils.getProps(this.attributes, properties, TbAwsSqsQueueAttributes::toConfigs);
        final CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName).withAttributes(attributes);
        String queueUrl = sqsClient.createQueue(createQueueRequest).getQueueUrl();
        queues.put(getQueueNameFromUrl(queueUrl), queueUrl);
    }

    /**
     * 功能：转换队列名称。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：文本结果。
     */
    private String convertTopicToQueueName(String topic) {
        return topic.replaceAll("\\.", "_") + ".fifo";
    }

    /**
     * 功能：删除或清理主题。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    @Override
    public void deleteTopic(String topic) {
        String queueName = convertTopicToQueueName(topic);
        if (queues.containsKey(queueName)) {
            sqsClient.deleteQueue(queues.get(queueName));
        } else {
            GetQueueUrlResult queueUrl = sqsClient.getQueueUrl(queueName);
            if (queueUrl != null) {
                sqsClient.deleteQueue(queueUrl.getQueueUrl());
            } else {
                log.warn("Aws SQS queue [{}] does not exist!", queueName);
            }
        }
    }

    /**
     * 功能：获取队列名称。
     * 参数：
     * - `queueUrl`：队列名称或队列对象。
     * 返回：文本结果。
     */
    private String getQueueNameFromUrl(String queueUrl) {
        int delimiterIndex = queueUrl.lastIndexOf("/");
        return queueUrl.substring(delimiterIndex + 1);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        if (producerExecutor != null) {
            producerExecutor.shutdownNow();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbAwsSqsAdmin` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
