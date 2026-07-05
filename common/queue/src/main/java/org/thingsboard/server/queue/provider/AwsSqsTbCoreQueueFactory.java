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
import org.thingsboard.server.queue.settings.TbQueueVersionControlSettings;
import org.thingsboard.server.queue.sqs.TbAwsSqsAdmin;
import org.thingsboard.server.queue.sqs.TbAwsSqsConsumerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsProducerTemplate;
import org.thingsboard.server.queue.sqs.TbAwsSqsQueueAttributes;
import org.thingsboard.server.queue.sqs.TbAwsSqsSettings;

import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

/**
 * 中文说明：
 * 1. 类目的：`AwsSqsTbCoreQueueFactory` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs' && '${service.type:null}'=='tb-core'")
public class AwsSqsTbCoreQueueFactory implements TbCoreQueueFactory {

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbAwsSqsSettings sqsSettings;
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
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final TbQueueVersionControlSettings vcSettings;

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
    private final TbQueueAdmin otaAdmin;
    /**
     * `vcAdmin` 字段，保存当前对象的对应属性。
     */
    private final TbQueueAdmin vcAdmin;

    /**
     * 功能：创建 `AwsSqsTbCoreQueueFactory` 实例，并初始化必要字段。
     * 参数：
     * - `sqsSettings`：配置对象。
     * - `coreSettings`：配置对象。
     * - `transportApiSettings`：配置对象。
     * - `ruleEngineSettings`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AwsSqsTbCoreQueueFactory(TbAwsSqsSettings sqsSettings,
                                    TbQueueCoreSettings coreSettings,
                                    TbQueueTransportApiSettings transportApiSettings,
                                    TbQueueRuleEngineSettings ruleEngineSettings,
                                    TopicService topicService,
                                    TbQueueVersionControlSettings vcSettings,
                                    TbServiceInfoProvider serviceInfoProvider,
                                    TbQueueRemoteJsInvokeSettings jsInvokeSettings,
                                    TbAwsSqsQueueAttributes sqsQueueAttributes,
                                    TbQueueTransportNotificationSettings transportNotificationSettings) {
        this.sqsSettings = sqsSettings;
        this.coreSettings = coreSettings;
        this.transportApiSettings = transportApiSettings;
        this.ruleEngineSettings = ruleEngineSettings;
        this.topicService = topicService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.jsInvokeSettings = jsInvokeSettings;
        this.transportNotificationSettings = transportNotificationSettings;
        this.vcSettings = vcSettings;

        this.coreAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getCoreAttributes());
        this.ruleEngineAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getRuleEngineAttributes());
        this.jsExecutorAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getJsExecutorAttributes());
        this.transportApiAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getTransportApiAttributes());
        this.notificationAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getNotificationsAttributes());
        this.otaAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getOtaAttributes());
        this.vcAdmin = new TbAwsSqsAdmin(sqsSettings, sqsQueueAttributes.getVcAttributes());
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(transportNotificationSettings.getNotificationsTopic()));
    }

    /**
     * 功能：保存或创建规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()));
    }

    /**
     * 功能：保存或创建规则引擎。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(notificationAdmin, sqsSettings, topicService.buildTopicName(ruleEngineSettings.getTopic()));
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
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> createToCoreMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToCoreMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createToCoreNotificationsMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(notificationAdmin, sqsSettings,
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
        return new TbAwsSqsConsumerTemplate<>(transportApiAdmin, sqsSettings, topicService.buildTopicName(transportApiSettings.getRequestsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), TransportApiRequestMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建响应。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiResponseProducer() {
        return new TbAwsSqsProducerTemplate<>(transportApiAdmin, sqsSettings, topicService.buildTopicName(transportApiSettings.getResponsesTopic()));
    }

    /**
     * 功能：保存或创建请求。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    @Bean
    public TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate() {
        TbQueueProducer<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>> producer = new TbAwsSqsProducerTemplate<>(jsExecutorAdmin, sqsSettings, jsInvokeSettings.getRequestTopic());
        TbQueueConsumer<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> consumer = new TbAwsSqsConsumerTemplate<>(jsExecutorAdmin, sqsSettings,
                jsInvokeSettings.getResponseTopic() + "_" + serviceInfoProvider.getServiceId(),
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
    public TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> createToUsageStatsServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(coreAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getUsageStatsTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToUsageStatsServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgConsumer() {
        return new TbAwsSqsConsumerTemplate<>(otaAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()),
                msg -> new TbProtoQueueMsg<>(msg.getKey(), ToOtaPackageStateServiceMsg.parseFrom(msg.getData()), msg.getHeaders()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(otaAdmin, sqsSettings, topicService.buildTopicName(coreSettings.getOtaPackageTopic()));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToVersionControlServiceMsg>> createVersionControlMsgProducer() {
        return new TbAwsSqsProducerTemplate<>(vcAdmin, sqsSettings, topicService.buildTopicName(vcSettings.getTopic()));
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
        if (otaAdmin != null) {
            otaAdmin.destroy();
        }
        if (vcAdmin != null) {
            vcAdmin.destroy();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AwsSqsTbCoreQueueFactory` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
