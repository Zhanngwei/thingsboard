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
package org.thingsboard.server.queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusQueueConfigs;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusSettings;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.pubsub.TbPubSubAdmin;
import org.thingsboard.server.queue.pubsub.TbPubSubSettings;
import org.thingsboard.server.queue.pubsub.TbPubSubSubscriptionSettings;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqAdmin;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqQueueArguments;
import org.thingsboard.server.queue.rabbitmq.TbRabbitMqSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

/**
 * 中文说明：
 * 1. 类目的：`RuleEngineTbQueueAdminFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Configuration
public class RuleEngineTbQueueAdminFactory {

    /**
     * 主题，保存当前对象的配置选项。
     */
    @Autowired(required = false)
    private TbKafkaTopicConfigs kafkaTopicConfigs;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbKafkaSettings kafkaSettings;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Autowired(required = false)
    private TbAwsSqsQueueAttributes awsSqsQueueAttributes;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbAwsSqsSettings awsSqsSettings;

    /**
     * 订阅集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbPubSubSubscriptionSettings pubSubSubscriptionSettings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbPubSubSettings pubSubSettings;

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Autowired(required = false)
    private TbRabbitMqQueueArguments rabbitMqQueueArguments;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbRabbitMqSettings rabbitMqSettings;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    private TbServiceBusQueueConfigs serviceBusQueueConfigs;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired(required = false)
    private TbServiceBusSettings serviceBusSettings;

    /**
     * 功能：保存或创建`Kafka Admin`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='kafka'")
    @Bean
    public TbQueueAdmin createKafkaAdmin() {
        return new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
    }

    /**
     * 功能：保存或创建`Aws Sqs Admin`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
    @Bean
    public TbQueueAdmin createAwsSqsAdmin() {
        return new TbAwsSqsAdmin(awsSqsSettings, awsSqsQueueAttributes.getRuleEngineAttributes());
    }

    /**
     * 功能：保存或创建`Pub Sub Admin`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
    @Bean
    public TbQueueAdmin createPubSubAdmin() {
        return new TbPubSubAdmin(pubSubSettings, pubSubSubscriptionSettings.getRuleEngineSettings());
    }

    /**
     * 功能：保存或创建`Rabbit Mq Admin`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
    @Bean
    public TbQueueAdmin createRabbitMqAdmin() {
        return new TbRabbitMqAdmin(rabbitMqSettings, rabbitMqQueueArguments.getRuleEngineArgs());
    }

    /**
     * 功能：保存或创建服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
    @Bean
    public TbQueueAdmin createServiceBusAdmin() {
        return new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
    }

    /**
     * 功能：保存或创建`In Memory Admin`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ConditionalOnExpression("'${queue.type:null}'=='in-memory'")
    @Bean
    public TbQueueAdmin createInMemoryAdmin() {
        return new TbQueueAdmin() {

            @Override
            public void createTopicIfNotExists(String topic, String properties) {
            }

            @Override
            public void deleteTopic(String topic) {
            }

            @Override
            public void destroy() {
            }
        };
    }

/*
 * 本类总结：
 * 1. 核心职责：`RuleEngineTbQueueAdminFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}