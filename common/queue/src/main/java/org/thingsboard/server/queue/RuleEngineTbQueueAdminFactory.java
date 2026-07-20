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
 * 1. `RuleEngineTbQueueAdminFactory` 是 ThingsBoard Common Queue 中创建或提供队列对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
}