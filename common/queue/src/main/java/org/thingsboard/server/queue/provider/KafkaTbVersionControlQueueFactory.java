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
package org.thingsboard.server.queue.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.kafka.TbKafkaAdmin;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerStatsService;
import org.thingsboard.server.queue.kafka.TbKafkaConsumerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaProducerTemplate;
import org.thingsboard.server.queue.kafka.TbKafkaSettings;
import org.thingsboard.server.queue.kafka.TbKafkaTopicConfigs;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;

import javax.annotation.PreDestroy;

/**
 * 中文说明：
 * 1. 类目的：`KafkaTbVersionControlQueueFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && '${service.type:null}'=='tb-vc-executor'")
public class KafkaTbVersionControlQueueFactory implements TbVersionControlQueueFactory {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueVersionControlSettings vcSettings;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TopicService topicService;

    /**
     * `coreAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin vcAdmin;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final TbQueueAdmin notificationAdmin;

    /**
     * 功能：创建 `KafkaTbVersionControlQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `kafkaSettings`：配置对象。
     * - `serviceInfoProvider`：服务对象。
     * - `coreSettings`：配置对象。
     * - `vcSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public KafkaTbVersionControlQueueFactory(TbKafkaSettings kafkaSettings,
                                             TbServiceInfoProvider serviceInfoProvider,
                                             TbQueueCoreSettings coreSettings,
                                             TbQueueVersionControlSettings vcSettings,
                                             TbKafkaConsumerStatsService consumerStatsService,
                                             TbKafkaTopicConfigs kafkaTopicConfigs,
                                             TopicService topicService) {
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.vcSettings = vcSettings;
        this.consumerStatsService = consumerStatsService;
        this.topicService = topicService;

        this.coreAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.vcAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getVcConfigs());
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
    }


    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreNotificationMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-vc-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> createToVersionControlMsgConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumerBuilder = TbKafkaConsumerTemplate.builder();
        consumerBuilder.settings(kafkaSettings);
        consumerBuilder.topic(topicService.buildTopicName(vcSettings.getTopic()));
        consumerBuilder.clientId("tb-vc-consumer-" + serviceInfoProvider.getServiceId());
        consumerBuilder.groupId(topicService.buildTopicName("tb-vc-node"));
        consumerBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToVersionControlServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
        consumerBuilder.admin(vcAdmin);
        consumerBuilder.statsService(consumerStatsService);
        return consumerBuilder.build();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToUsageStatsServiceMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("tb-vc-us-producer-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        if (coreAdmin != null) {
            coreAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`KafkaTbVersionControlQueueFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
