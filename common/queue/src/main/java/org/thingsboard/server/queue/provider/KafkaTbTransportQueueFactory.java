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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
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
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

import javax.annotation.PreDestroy;

/**
 * 中文说明：
 * 1. 类目的：`KafkaTbTransportQueueFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='kafka' && (('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport')")
@Slf4j
public class KafkaTbTransportQueueFactory implements TbTransportQueueFactory {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbKafkaSettings kafkaSettings;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbKafkaConsumerStatsService consumerStatsService;
    private final TopicService topicService;

    /**
     * `coreAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final TbQueueAdmin transportApiRequestAdmin;
    private final TbQueueAdmin transportApiResponseAdmin;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final TbQueueAdmin notificationAdmin;

    /**
     * 功能：创建 `KafkaTbTransportQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `kafkaSettings`：配置对象。
     * - `serviceInfoProvider`：服务对象。
     * - `coreSettings`：配置对象。
     * - `ruleEngineSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public KafkaTbTransportQueueFactory(TbKafkaSettings kafkaSettings,
                                        TbServiceInfoProvider serviceInfoProvider,
                                        TbQueueCoreSettings coreSettings,
                                        TbQueueRuleEngineSettings ruleEngineSettings,
                                        TbQueueTransportApiSettings transportApiSettings,
                                        TbQueueTransportNotificationSettings transportNotificationSettings,
                                        TbKafkaConsumerStatsService consumerStatsService,
                                        TbKafkaTopicConfigs kafkaTopicConfigs,
                                        TopicService topicService) {
        this.kafkaSettings = kafkaSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.consumerStatsService = consumerStatsService;
        this.topicService = topicService;

        this.coreAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getRuleEngineConfigs());
        this.transportApiRequestAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiRequestConfigs());
        this.transportApiResponseAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getTransportApiResponseConfigs());
        this.notificationAdmin = new TbKafkaAdmin(kafkaSettings, kafkaTopicConfigs.getNotificationsConfigs());
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<TransportApiRequestMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-api-request-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(transportApiSettings.getRequestsTopic()));
        requestBuilder.admin(transportApiRequestAdmin);

        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<TransportApiResponseMsg>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(topicService.buildTopicName(transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId()));
        responseBuilder.clientId("transport-api-response-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId(topicService.buildTopicName("transport-node-" + serviceInfoProvider.getServiceId()));
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));
        responseBuilder.admin(transportApiResponseAdmin);
        responseBuilder.statsService(consumerStatsService);

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(transportApiResponseAdmin);
        templateBuilder.requestTemplate(requestBuilder.build());
        templateBuilder.responseTemplate(responseBuilder.build());
        templateBuilder.maxPendingRequests(transportApiSettings.getMaxPendingRequests());
        templateBuilder.maxRequestTimeout(transportApiSettings.getMaxRequestsTimeout());
        templateBuilder.pollInterval(transportApiSettings.getResponsePollInterval());
        return templateBuilder.build();
    }

    /**
     * 功能：保存或创建规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToRuleEngineMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-node-rule-engine-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(ruleEngineSettings.getTopic()));
        requestBuilder.admin(ruleEngineAdmin);
        return requestBuilder.build();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        TbKafkaProducerTemplate.TbKafkaProducerTemplateBuilder<TbProtoQueueMsg<ToCoreMsg>> requestBuilder = TbKafkaProducerTemplate.builder();
        requestBuilder.settings(kafkaSettings);
        requestBuilder.clientId("transport-node-core-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getTopic()));
        requestBuilder.admin(coreAdmin);
        return requestBuilder.build();
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
        requestBuilder.clientId("transport-node-to-core-notifications-" + serviceInfoProvider.getServiceId());
        requestBuilder.defaultTopic(topicService.buildTopicName(coreSettings.getTopic()));
        requestBuilder.admin(notificationAdmin);
        return requestBuilder.build();
    }

    /**
     * 功能：保存或创建消息消费者。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        TbKafkaConsumerTemplate.TbKafkaConsumerTemplateBuilder<TbProtoQueueMsg<ToTransportMsg>> responseBuilder = TbKafkaConsumerTemplate.builder();
        responseBuilder.settings(kafkaSettings);
        responseBuilder.topic(topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId()));
        responseBuilder.clientId("transport-api-notifications-" + serviceInfoProvider.getServiceId());
        responseBuilder.groupId(topicService.buildTopicName("transport-node-" + serviceInfoProvider.getServiceId()));
        responseBuilder.decoder(msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
        responseBuilder.admin(notificationAdmin);
        responseBuilder.statsService(consumerStatsService);
        return responseBuilder.build();
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
        requestBuilder.clientId("transport-node-us-producer-" + serviceInfoProvider.getServiceId());
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
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
        if (transportApiRequestAdmin != null) {
            transportApiRequestAdmin.destroy();
        }
        if (transportApiResponseAdmin != null) {
            transportApiResponseAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`KafkaTbTransportQueueFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
