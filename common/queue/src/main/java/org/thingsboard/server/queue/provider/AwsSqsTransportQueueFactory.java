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
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

import javax.annotation.PreDestroy;

/**
 * 中文说明：
 * 1. `AwsSqsTransportQueueFactory` 是 ThingsBoard Common Queue 中创建或提供队列对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TbTransportQueueFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && (('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport')")
@Slf4j
public class AwsSqsTransportQueueFactory implements TbTransportQueueFactory {
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbAwsSqsSettings sqsSettings;
    private final TbQueueCoreSettings coreSettings;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;

    /**
     * `coreAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin transportApiAdmin;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final TbQueueAdmin notificationAdmin;
    private final TbQueueAdmin ruleEngineAdmin;

    /**
     * 功能：创建 `AwsSqsTransportQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `transportApiSettings`：配置对象。
     * - `transportNotificationSettings`：配置对象。
     * - `sqsSettings`：配置对象。
     * - `serviceInfoProvider`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AwsSqsTransportQueueFactory(TbQueueTransportApiSettings transportApiSettings,
                                       TbQueueTransportNotificationSettings transportNotificationSettings,
                                       TbAwsSqsSettings sqsSettings,
                                       TbServiceInfoProvider serviceInfoProvider,
                                       TbQueueCoreSettings coreSettings,
                                       TbAwsSqsQueueAttributes sqsQueueAttributes,
                                       TbQueueRuleEngineSettings ruleEngineSettings,
                                       TopicService topicService) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.sqsSettings = sqsSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.topicService = topicService;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.transportApiAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getTransportApiAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.ruleEngineAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getRuleEngineAttributes());
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        TbQueueProducer<TbProtoQueueMsg<TransportApiRequestMsg>> producerTemplate =
                new TbAwsSqsProducerTemplate<>(transportApiAdmin, sqsSettings, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));

        TbQueueConsumer<TbProtoQueueMsg<TransportApiResponseMsg>> consumerTemplate =
                new TbAwsSqsConsumerTemplate<>(transportApiAdmin, sqsSettings,
                        topicService.buildTopicName(transportApiSettings.getResponsesTopic() + "_" + serviceInfoProvider.getServiceId()),
                        msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiResponseMsg.parseFrom(msg.getData()), msg.getHeaders()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();
        templateBuilder.queueAdmin(transportApiAdmin);
        templateBuilder.requestTemplate(producerTemplate);
        templateBuilder.responseTemplate(consumerTemplate);
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
        return new TbAwsSqsProducerTemplate<>(ruleEngineAdmin, sqsSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息消费者。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic() + "_" + serviceInfoProvider.getServiceId()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToTransportMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
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
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
        if (ruleEngineAdmin != null) {
            ruleEngineAdmin.destroy();
        }
    }
}
