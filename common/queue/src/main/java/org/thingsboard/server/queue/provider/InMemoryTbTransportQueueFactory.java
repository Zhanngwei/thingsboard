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
import org.thingsboard.server.queue.memory.InMemoryStorage;
import org.thingsboard.server.queue.memory.InMemoryTbQueueConsumer;
import org.thingsboard.server.queue.memory.InMemoryTbQueueProducer;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

/**
 * 中文说明：
 * 1. `InMemoryTbTransportQueueFactory` 是 ThingsBoard Common Queue 中创建或提供队列对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TbTransportQueueFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='in-memory' && (('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true') || '${service.type:null}'=='tb-transport')")
@Slf4j
public class InMemoryTbTransportQueueFactory implements TbTransportQueueFactory {
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueTransportApiSettings transportApiSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueCoreSettings coreSettings;
    /**
     * `storage` 字段，保存当前对象的对应属性。
     */
    private final InMemoryStorage storage;
    private final TopicService topicService;

    /**
     * 功能：创建 `InMemoryTbTransportQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `transportApiSettings`：配置对象。
     * - `transportNotificationSettings`：配置对象。
     * - `serviceInfoProvider`：服务对象。
     * - `coreSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public InMemoryTbTransportQueueFactory(TbQueueTransportApiSettings transportApiSettings,
                                           TbQueueTransportNotificationSettings transportNotificationSettings,
                                           TbServiceInfoProvider serviceInfoProvider,
                                           TbQueueCoreSettings coreSettings,
                                           InMemoryStorage storage,
                                           TopicService topicService) {
        this.transportApiSettings = transportApiSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.serviceInfoProvider = serviceInfoProvider;
        this.coreSettings = coreSettings;
        this.storage = storage;
        this.topicService = topicService;
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate() {
        InMemoryTbQueueProducer<TbProtoQueueMsg<TransportApiRequestMsg>> producerTemplate =
                new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));

        InMemoryTbQueueConsumer<TbProtoQueueMsg<TransportApiResponseMsg>> consumerTemplate =
                new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(transportApiSettings.getResponsesTopic() + "." + serviceInfoProvider.getServiceId()));

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> templateBuilder = DefaultTbQueueRequestTemplate.builder();

        templateBuilder.queueAdmin(new TbQueueAdmin() {
            @Override
            public void createTopicIfNotExists(String topic, String properties) {}

            @Override
            public void destroy() {}

            @Override
            public void deleteTopic(String topic) {}
        });

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
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(transportApiSettings.getRequestsTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息消费者。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer() {
        return new InMemoryTbQueueConsumer<>(storage, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic() + "." + serviceInfoProvider.getServiceId()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new InMemoryTbQueueProducer<>(storage, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

}
