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
package org.thingsboard.server.transport.mqtt.session;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.TopicType;
import org.thingsboard.server.transport.mqtt.adaptors.BackwardCompatibilityAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilter;
import org.thingsboard.server.transport.mqtt.util.MqttTopicFilterFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. 类目的：`DeviceSessionCtx` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class DeviceSessionCtx extends MqttDeviceAwareSessionContext {

    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    @Getter
    @Setter
    private ChannelHandlerContext channel;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Getter
    private final MqttTransportContext context;

    private final AtomicInteger msgIdSeq = new AtomicInteger(0);

    private final ConcurrentLinkedQueue<MqttMessage> msgQueue = new ConcurrentLinkedQueue<>();

    @Getter
    private final Lock msgQueueProcessorLock = new ReentrantLock();

    private final AtomicInteger msgQueueSize = new AtomicInteger(0);

    /**
     * 是否满足`provisionOnly`条件。
     */
    @Getter
    @Setter
    private boolean provisionOnly = false;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Getter
    @Setter
    private MqttVersion mqttVersion;

    private volatile MqttTopicFilter telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
    private volatile MqttTopicFilter attributesPublishTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
    private volatile MqttTopicFilter attributesSubscribeTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
    /**
     * 消息载荷，用于区分不同处理分支。
     */
    private volatile TransportPayloadType payloadType = TransportPayloadType.JSON;
    private volatile Descriptors.Descriptor attributesDynamicMessageDescriptor;
    /**
     * 遥测，承载当前步骤需要处理的内容。
     */
    private volatile Descriptors.Descriptor telemetryDynamicMessageDescriptor;
    private volatile Descriptors.Descriptor rpcResponseDynamicMessageDescriptor;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private volatile DynamicMessage.Builder rpcRequestDynamicMessageBuilder;
    private volatile MqttTransportAdaptor adaptor;
    /**
     * 是否使用 JSON 载荷格式。
     */
    private volatile boolean jsonPayloadFormatCompatibilityEnabled;
    private volatile boolean useJsonPayloadFormatForDefaultDownlinkTopics;
    /**
     * 是否在校验异常时发送确认响应。
     */
    private volatile boolean sendAckOnValidationException;

    /**
     * 是否满足设备配置条件。
     */
    @Getter
    private volatile boolean deviceProfileMqttTransportType;

    /**
     * 消息载荷，用于区分不同处理分支。
     */
    @Getter
    @Setter
    private TransportPayloadType provisionPayloadType = payloadType;


    /**
     * 功能：创建 `DeviceSessionCtx` 实例，并初始化必要字段。
     * 参数：
     * - `sessionId`：会话ID。
     * - `mqttQoSMap`：键值映射。
     * - `context`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public DeviceSessionCtx(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap, MqttTransportContext context) {
        super(sessionId, mqttQoSMap);
        this.context = context;
        this.adaptor = context.getJsonMqttAdaptor();
    }

    /**
     * 功能：执行 `nextMsgId` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int nextMsgId() {
        return msgIdSeq.incrementAndGet();
    }

    /**
     * 功能：判断设备。
     * 参数：
     * - `topicName`：主题名称或主题对象。
     * 返回：判断结果。
     */
    public boolean isDeviceTelemetryTopic(String topicName) {
        return telemetryTopicFilter.filter(topicName);
    }

    /**
     * 功能：判断设备。
     * 参数：
     * - `topicName`：主题名称或主题对象。
     * 返回：判断结果。
     */
    public boolean isDeviceAttributesTopic(String topicName) {
        return attributesPublishTopicFilter.filter(topicName);
    }

    /**
     * 功能：判断设备。
     * 参数：
     * - `topicName`：主题名称或主题对象。
     * 返回：判断结果。
     */
    public boolean isDeviceSubscriptionAttributesTopic(String topicName) {
        return attributesSubscribeTopicFilter.filter(topicName);
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttTransportAdaptor getPayloadAdaptor() {
        return adaptor;
    }

    /**
     * 功能：判断消息载荷。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isJsonPayloadType() {
        return payloadType.equals(TransportPayloadType.JSON);
    }

    /**
     * 功能：判断校验异常。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSendAckOnValidationException() {
        return sendAckOnValidationException;
    }

    /**
     * 功能：获取遥测。
     * 参数：无。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getTelemetryDynamicMsgDescriptor() {
        return telemetryDynamicMessageDescriptor;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getAttributesDynamicMessageDescriptor() {
        return attributesDynamicMessageDescriptor;
    }

    /**
     * 功能：获取RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    public Descriptors.Descriptor getRpcResponseDynamicMessageDescriptor() {
        return rpcResponseDynamicMessageDescriptor;
    }

    /**
     * 功能：获取RPC。
     * 参数：无。
     * 返回：处理结果。
     */
    public DynamicMessage.Builder getRpcRequestDynamicMessageBuilder() {
        return rpcRequestDynamicMessageBuilder;
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void setDeviceProfile(DeviceProfile deviceProfile) {
        super.setDeviceProfile(deviceProfile);
        updateDeviceSessionConfiguration(deviceProfile);
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        super.onDeviceProfileUpdate(sessionInfo, deviceProfile);
        updateDeviceSessionConfiguration(deviceProfile);
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    private void updateDeviceSessionConfiguration(DeviceProfile deviceProfile) {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration.getType().equals(DeviceTransportType.MQTT) &&
                transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            MqttDeviceProfileTransportConfiguration mqttConfig = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = mqttConfig.getTransportPayloadTypeConfiguration();
            payloadType = transportPayloadTypeConfiguration.getTransportPayloadType();
            deviceProfileMqttTransportType = true;
            telemetryTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceTelemetryTopic());
            attributesPublishTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceAttributesTopic());
            attributesSubscribeTopicFilter = MqttTopicFilterFactory.toFilter(mqttConfig.getDeviceAttributesSubscribeTopic());
            sendAckOnValidationException = mqttConfig.isSendAckOnValidationException();
            if (TransportPayloadType.PROTOBUF.equals(payloadType)) {
                ProtoTransportPayloadConfiguration protoTransportPayloadConfig = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                updateDynamicMessageDescriptors(protoTransportPayloadConfig);
                jsonPayloadFormatCompatibilityEnabled = protoTransportPayloadConfig.isEnableCompatibilityWithJsonPayloadFormat();
                useJsonPayloadFormatForDefaultDownlinkTopics = jsonPayloadFormatCompatibilityEnabled && protoTransportPayloadConfig.isUseJsonPayloadFormatForDefaultDownlinkTopics();
            }
        } else {
            telemetryTopicFilter = MqttTopicFilterFactory.getDefaultTelemetryFilter();
            attributesPublishTopicFilter = MqttTopicFilterFactory.getDefaultAttributesFilter();
            payloadType = TransportPayloadType.JSON;
            deviceProfileMqttTransportType = false;
            sendAckOnValidationException = false;
        }
        updateAdaptor();
    }

    /**
     * 功能：更新消息。
     * 参数：
     * - `protoTransportPayloadConfig`：配置对象。
     * 返回：无。
     */
    private void updateDynamicMessageDescriptors(ProtoTransportPayloadConfiguration protoTransportPayloadConfig) {
        telemetryDynamicMessageDescriptor = protoTransportPayloadConfig.getTelemetryDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceTelemetryProtoSchema());
        attributesDynamicMessageDescriptor = protoTransportPayloadConfig.getAttributesDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceAttributesProtoSchema());
        rpcResponseDynamicMessageDescriptor = protoTransportPayloadConfig.getRpcResponseDynamicMessageDescriptor(protoTransportPayloadConfig.getDeviceRpcResponseProtoSchema());
        rpcRequestDynamicMessageBuilder = protoTransportPayloadConfig.getRpcRequestDynamicMessageBuilder(protoTransportPayloadConfig.getDeviceRpcRequestProtoSchema());
    }

    /**
     * 功能：获取`Adaptor`。
     * 参数：
     * - `topicType`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public MqttTransportAdaptor getAdaptor(TopicType topicType) {
        switch (topicType) {
            case V2:
                return getDefaultAdaptor();
            case V2_JSON:
                return context.getJsonMqttAdaptor();
            case V2_PROTO:
                return context.getProtoMqttAdaptor();
            default:
                return useJsonPayloadFormatForDefaultDownlinkTopics ? context.getJsonMqttAdaptor() : getDefaultAdaptor();
        }
    }

    /**
     * 功能：获取`Default Adaptor`。
     * 参数：无。
     * 返回：处理结果。
     */
    private MqttTransportAdaptor getDefaultAdaptor() {
        return isJsonPayloadType() ? context.getJsonMqttAdaptor() : context.getProtoMqttAdaptor();
    }

    /**
     * 功能：更新`Adaptor`。
     * 参数：无。
     * 返回：无。
     */
    private void updateAdaptor() {
        if (isJsonPayloadType()) {
            adaptor = context.getJsonMqttAdaptor();
            jsonPayloadFormatCompatibilityEnabled = false;
            useJsonPayloadFormatForDefaultDownlinkTopics = false;
        } else {
            if (jsonPayloadFormatCompatibilityEnabled) {
                adaptor = new BackwardCompatibilityAdaptor(context.getProtoMqttAdaptor(), context.getJsonMqttAdaptor());
            } else {
                adaptor = context.getProtoMqttAdaptor();
            }
        }
    }

    /**
     * 功能：保存或创建队列。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    public void addToQueue(MqttMessage msg) {
        msgQueueSize.incrementAndGet();
        ReferenceCountUtil.retain(msg);
        msgQueue.add(msg);
    }

    /**
     * 功能：执行 `tryProcessQueuedMsgs` 对应的处理。
     * 参数：
     * - `msgProcessor`：处理器对象。
     * 返回：无。
     */
    public void tryProcessQueuedMsgs(Consumer<MqttMessage> msgProcessor) {
        while (!msgQueue.isEmpty()) {
            if (msgQueueProcessorLock.tryLock()) {
                try {
                    MqttMessage msg;
                    while ((msg = msgQueue.poll()) != null) {
                        try {
                            msgQueueSize.decrementAndGet();
                            msgProcessor.accept(msg);
                        } finally {
                            ReferenceCountUtil.safeRelease(msg);
                        }
                    }
                } finally {
                    msgQueueProcessorLock.unlock();
                }
            } else {
                return;
            }
        }
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：数值结果。
     */
    public int getMsgQueueSize() {
        return msgQueueSize.get();
    }

    /**
     * 功能：执行 `release` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void release() {
        if (!msgQueue.isEmpty()) {
            log.warn("doDisconnect for device {} but unprocessed messages {} left in the msg queue", getDeviceId(), msgQueue.size());
            msgQueue.forEach(ReferenceCountUtil::safeRelease);
            msgQueue.clear();
        }
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public Collection<MqttMessage> getMsgQueueSnapshot(){
        return Collections.unmodifiableCollection(msgQueue);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceSessionCtx` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
