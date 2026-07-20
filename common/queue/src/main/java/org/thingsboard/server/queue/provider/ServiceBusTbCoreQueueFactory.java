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

import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusAdmin;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusConsumerTemplate;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusProducerTemplate;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusQueueConfigs;
import org.thingsboard.server.queue.azure.servicebus.TbServiceBusSettings;
import org.thingsboard.server.queue.common.DefaultTbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.settings.TbQueueCoreSettings;
import org.thingsboard.server.queue.settings.TbQueueRemoteJsInvokeSettings;
import org.thingsboard.server.queue.settings.TbQueueRuleEngineSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportApiSettings;
import org.thingsboard.server.queue.settings.TbQueueTransportNotificationSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

/**
 * 中文说明：
 * 1. `ServiceBusTbCoreQueueFactory` 是 ThingsBoard Common Queue 中创建或提供队列对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TbCoreQueueFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus' && '${service.type:null}'=='tb-core'")
public class ServiceBusTbCoreQueueFactory implements TbCoreQueueFactory {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbServiceBusSettings serviceBusSettings;
    private final TbQueueRuleEngineSettings ruleEngineSettings;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueCoreSettings coreSettings;
    private final TbQueueTransportApiSettings transportApiSettings;
    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;
    private final TbServiceInfoProvider serviceInfoProvider;
    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueRemoteJsInvokeSettings jsInvokeSettings;
    private final TbQueueTransportNotificationSettings transportNotificationSettings;

    /**
     * `coreAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin coreAdmin;
    private final TbQueueAdmin ruleEngineAdmin;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final TbQueueAdmin jsExecutorAdmin;
    private final TbQueueAdmin transportApiAdmin;
    /**
     * 通知，表示当前对象的对应属性。
     */
    private final TbQueueAdmin notificationAdmin;

    /**
     * 功能：创建 `ServiceBusTbCoreQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `serviceBusSettings`：服务对象。
     * - `coreSettings`：配置对象。
     * - `transportApiSettings`：配置对象。
     * - `ruleEngineSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public ServiceBusTbCoreQueueFactory(TbServiceBusSettings serviceBusSettings,
                                        TbQueueCoreSettings coreSettings,
                                        TbQueueTransportApiSettings transportApiSettings,
                                        TbQueueRuleEngineSettings ruleEngineSettings,
                                        TopicService topicService,
                                        TbServiceInfoProvider serviceInfoProvider,
                                        TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                        TbQueueTransportNotificationSettings transportNotificationSettings,
                                        TbServiceBusQueueConfigs serviceBusQueueConfigs) {
        this.serviceBusSettings = serviceBusSettings;
        this.coreSettings = coreSettings;
        this.transportApiSettings = transportApiSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.topicService = topicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.jsInvokeSettings = jsInvokeSettings;
        this.transportNotificationSettings = transportNotificationSettings;

        this.coreAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getCoreConfigs());
        this.ruleEngineAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getRuleEngineConfigs());
        this.jsExecutorAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getJsExecutorConfigs());
        this.transportApiAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getTransportApiConfigs());
        this.notificationAdmin = new TbServiceBusAdmin(serviceBusSettings, serviceBusQueueConfigs.getNotificationsConfigs());
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
    }

    /**
     * 功能：保存或创建规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer() {
        return new TbServiceBusProducerTemplate<>(notificationAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbServiceBusConsumerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbServiceBusConsumerTemplate<>(notificationAdmin, serviceBusSettings,
                topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceInfoProvider.getServiceId()).getFullTopicName(),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreNotificationMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<TransportApiRequestMsg>> createTransportApiRequestConsumer() {
        return new TbServiceBusConsumerTemplate<>(transportApiAdmin, serviceBusSettings, topicService.buildTopicName(transportApiSettings.getRequestsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建响应。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbServiceBusProducerTemplate<>(transportApiAdmin, serviceBusSettings, topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> producer = new TbServiceBusProducerTemplate<>(jsExecutorAdmin, serviceBusSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> consumer = new TbServiceBusConsumerTemplate<>(jsExecutorAdmin, serviceBusSettings,
                jsInvokeSettings.getResponseTopic() + "." + serviceInfoProvider.getServiceId(),
                msg -> {
                    JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
                    return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
                });

        DefaultTbQueueRequestTemplate.DefaultTbQueueRequestTemplateBuilder
                <TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> builder = DefaultTbQueueRequestTemplate.builder();
        builder.queueAdmin(jsExecutorAdmin);
        builder.requestTemplate(producer);
        builder.responseTemplate(consumer);
        builder.maxPendingRequests(jsInvokeSettings.getMaxPendingRequests());
        builder.maxRequestTimeout(jsInvokeSettings.getMaxRequestsTimeout());
        builder.pollInterval(jsInvokeSettings.getResponsePollInterval());
        return builder.build();
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbServiceBusConsumerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbServiceBusConsumerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbServiceBusProducerTemplate<>(coreAdmin, serviceBusSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        //TODO: version-control
        return null;
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
        if (jsExecutorAdmin != null) {
            jsExecutorAdmin.destroy();
        }
        if (transportApiAdmin != null) {
            transportApiAdmin.destroy();
        }
        if (notificationAdmin != null) {
            notificationAdmin.destroy();
        }
    }
}
