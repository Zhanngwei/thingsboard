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
package org.thingsboard.server.transport.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonParseException;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.profile.MqttTopics;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.DefaultTransportService;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;
import org.thingsboard.server.transport.mqtt.session.DeviceSessionCtx;
import org.thingsboard.server.transport.mqtt.session.GatewaySessionHandler;
import org.thingsboard.server.transport.mqtt.session.MqttTopicMatcher;
import org.thingsboard.server.transport.mqtt.session.SparkplugNodeSessionHandler;
import org.thingsboard.server.transport.mqtt.util.ReturnCode;
import org.thingsboard.server.transport.mqtt.util.ReturnCodeResolver;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcResponseBody;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.amazonaws.util.StringUtils.UTF8;
import static io.netty.handler.codec.mqtt.MqttMessageType.CONNECT;
import static io.netty.handler.codec.mqtt.MqttMessageType.PINGRESP;
import static io.netty.handler.codec.mqtt.MqttMessageType.SUBACK;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NDEATH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopicPublish;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `MqttTransportHandler` 是 ThingsBoard Common Transport 中处理 MQTT 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `ChannelInboundHandlerAdapter`、`GenericFutureListener`、`SessionMsgListener`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class MqttTransportHandler extends ChannelInboundHandlerAdapter implements GenericFutureListener<Future<? super Void>>, SessionMsgListener {

    private static final Pattern FW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_FIRMWARE_REQUEST_TOPIC_PATTERN);
    private static final Pattern SW_REQUEST_PATTERN = Pattern.compile(MqttTopics.DEVICE_SOFTWARE_REQUEST_TOPIC_PATTERN);


    /**
     * 消息载荷常量，用于统一引用固定值。
     */
    private static final String PAYLOAD_TOO_LARGE = "PAYLOAD_TOO_LARGE";

    /**
     * QoS 等级常量，用于统一引用固定值。
     */
    private static final MqttQoS MAX_SUPPORTED_QOS_LVL = AT_LEAST_ONCE;

    /**
     * 会话ID，用于定位对应业务对象。
     */
    private final UUID sessionId;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    protected final MqttTransportContext context;
    private final TransportService transportService;
    /**
     * 调度器，用于安排延迟任务或周期任务。
     */
    private final SchedulerComponent scheduler;
    private final SslHandler sslHandler;
    /**
     * QoS 等级映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    /**
     * 设备，汇总当前处理所需的上下文信息。
     */
    final DeviceSessionCtx deviceSessionCtx;
    volatile InetSocketAddress address;
    /**
     * 会话，负责处理对应任务或消息。
     */
    volatile GatewaySessionHandler gatewaySessionHandler;
    volatile SparkplugNodeSessionHandler sparkplugSessionHandler;

    /**
     * `otaPackSessions`映射关系，用于按键查找对应值。
     */
    private final ConcurrentHashMap<String, String> otaPackSessions;
    private final ConcurrentHashMap<String, Integer> chunkSizes;
    /**
     * RPC映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<Integer, TransportProtos.ToDeviceRpcRequestMsg> rpcAwaitingAck;

    /**
     * 主题，用于区分不同处理分支。
     */
    private TopicType attrSubTopicType;
    private TopicType rpcSubTopicType;
    /**
     * 主题，用于区分不同处理分支。
     */
    private TopicType attrReqTopicType;
    private TopicType toServerRpcSubTopicType;

    MqttTransportHandler(MqttTransportContext context, SslHandler sslHandler) {
        this.sessionId = UUID.randomUUID();
        this.context = context;
        this.transportService = context.getTransportService();
        this.scheduler = context.getScheduler();
        this.sslHandler = sslHandler;
        this.mqttQoSMap = new ConcurrentHashMap<>();
        this.deviceSessionCtx = new DeviceSessionCtx(sessionId, mqttQoSMap, context);
        this.otaPackSessions = new ConcurrentHashMap<>();
        this.chunkSizes = new ConcurrentHashMap<>();
        this.rpcAwaitingAck = new ConcurrentHashMap<>();
    }

    /**
     * 功能：执行 `channelRegistered` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        context.channelRegistered();
    }

    /**
     * 功能：执行 `channelUnregistered` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        context.channelUnregistered();
    }

    /**
     * 功能：执行 `channelRead` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.trace("[{}] Processing msg: {}", sessionId, msg);
        if (address == null) {
            address = getAddress(ctx);
        }
        try {
            if (msg instanceof MqttMessage) {
                MqttMessage message = (MqttMessage) msg;
                if (message.decoderResult().isSuccess()) {
                    processMqttMsg(ctx, message);
                } else {
                    log.error("[{}] Message decoding failed: {}", sessionId, message.decoderResult().cause().getMessage());
                    closeCtx(ctx);
                }
            } else {
                log.debug("[{}] Received non mqtt message: {}", sessionId, msg.getClass().getSimpleName());
                closeCtx(ctx);
            }
        } finally {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

    /**
     * 功能：停止或关闭上下文。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    private void closeCtx(ChannelHandlerContext ctx) {
        if (!rpcAwaitingAck.isEmpty()) {
            log.debug("[{}] Cleanup RPC awaiting ack map due to session close!", sessionId);
            rpcAwaitingAck.clear();
        }
        ctx.close();
    }

    /**
     * 功能：获取`Address`。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：处理结果。
     */
    InetSocketAddress getAddress(ChannelHandlerContext ctx) {
        var address = ctx.channel().attr(MqttTransportService.ADDRESS).get();
        if (address == null) {
            log.trace("[{}] Received empty address.", ctx.channel().id());
            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            log.trace("[{}] Going to use address: {}", ctx.channel().id(), remoteAddress);
            return remoteAddress;
        } else {
            log.trace("[{}] Received address: {}", ctx.channel().id(), address);
        }
        return address;
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processMqttMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        if (msg.fixedHeader() == null) {
            log.info("[{}:{}] Invalid message received", address.getHostName(), address.getPort());
            closeCtx(ctx);
            return;
        }
        deviceSessionCtx.setChannel(ctx);
        if (CONNECT.equals(msg.fixedHeader().messageType())) {
            processConnect(ctx, (MqttConnectMessage) msg);
        } else if (deviceSessionCtx.isProvisionOnly()) {
            processProvisionSessionMsg(ctx, msg);
        } else {
            enqueueRegularSessionMsg(ctx, msg);
        }
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void processProvisionSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        switch (msg.fixedHeader().messageType()) {
            case PUBLISH:
                MqttPublishMessage mqttMsg = (MqttPublishMessage) msg;
                String topicName = mqttMsg.variableHeader().topicName();
                int msgId = mqttMsg.variableHeader().packetId();
                try {
                    if (topicName.equals(MqttTopics.DEVICE_PROVISION_REQUEST_TOPIC)) {
                        try {
                            TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg = deviceSessionCtx.getContext().getJsonMqttAdaptor().convertToProvisionRequestMsg(deviceSessionCtx, mqttMsg);
                            transportService.process(provisionRequestMsg, new DeviceProvisionCallback(ctx, msgId, provisionRequestMsg));
                            log.trace("[{}][{}] Processing provision publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);
                        } catch (Exception e) {
                            if (e instanceof JsonParseException || (e.getCause() != null && e.getCause() instanceof JsonParseException)) {
                                TransportProtos.ProvisionDeviceRequestMsg provisionRequestMsg = deviceSessionCtx.getContext().getProtoMqttAdaptor().convertToProvisionRequestMsg(deviceSessionCtx, mqttMsg);
                                transportService.process(provisionRequestMsg, new DeviceProvisionCallback(ctx, msgId, provisionRequestMsg));
                                deviceSessionCtx.setProvisionPayloadType(TransportPayloadType.PROTOBUF);
                                log.trace("[{}][{}] Processing provision publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);
                            } else {
                                throw e;
                            }
                        }
                    } else {
                        log.debug("[{}] Unsupported topic for provisioning requests: {}!", sessionId, topicName);
                        closeCtx(ctx);
                    }
                } catch (RuntimeException e) {
                    log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    closeCtx(ctx);
                } catch (AdaptorException e) {
                    log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
                    closeCtx(ctx);
                }
                break;
            case PINGREQ:
                ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                break;
            case DISCONNECT:
                closeCtx(ctx);
                break;
        }
    }

    /**
     * 功能：执行 `enqueueRegularSessionMsg` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void enqueueRegularSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        final int queueSize = deviceSessionCtx.getMsgQueueSize();
        if (queueSize >= context.getMessageQueueSizePerDeviceLimit()) {
            log.info("Closing current session because msq queue size for device {} exceed limit {} with msgQueueSize counter {} and actual queue size {}",
                    deviceSessionCtx.getDeviceId(), context.getMessageQueueSizePerDeviceLimit(), queueSize, deviceSessionCtx.getMsgQueueSize());
            closeCtx(ctx);
            return;
        }

        deviceSessionCtx.addToQueue(msg);
        processMsgQueue(ctx); //Under the normal conditions the msg queue will contain 0 messages. Many messages will be processed on device connect event in separate thread pool
    }

    /**
     * 功能：处理队列。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    void processMsgQueue(ChannelHandlerContext ctx) {
        if (!deviceSessionCtx.isConnected()) {
            log.trace("[{}][{}] Postpone processing msg due to device is not connected. Msg queue size is {}", sessionId, deviceSessionCtx.getDeviceId(), deviceSessionCtx.getMsgQueueSize());
            return;
        }
        deviceSessionCtx.tryProcessQueuedMsgs(msg -> processRegularSessionMsg(ctx, msg));
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processRegularSessionMsg(ChannelHandlerContext ctx, MqttMessage msg) {
        switch (msg.fixedHeader().messageType()) {
            case PUBLISH:
                processPublish(ctx, (MqttPublishMessage) msg);
                break;
            case SUBSCRIBE:
                processSubscribe(ctx, (MqttSubscribeMessage) msg);
                break;
            case UNSUBSCRIBE:
                processUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
                break;
            case PINGREQ:
                if (checkConnected(ctx, msg)) {
                    ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(PINGRESP, false, AT_MOST_ONCE, false, 0)));
                    transportService.recordActivity(deviceSessionCtx.getSessionInfo());
                }
                break;
            case DISCONNECT:
                closeCtx(ctx);
                break;
            case PUBACK:
                int msgId = ((MqttPubAckMessage) msg).variableHeader().messageId();
                TransportProtos.ToDeviceRpcRequestMsg rpcRequest = rpcAwaitingAck.remove(msgId);
                if (rpcRequest != null) {
                    transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequest, RpcStatus.DELIVERED, true, TransportServiceCallback.EMPTY);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 功能：处理`Publish`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            return;
        }
        String topicName = mqttMsg.variableHeader().topicName();
        int msgId = mqttMsg.variableHeader().packetId();
        log.trace("[{}][{}] Processing publish msg [{}][{}]!", sessionId, deviceSessionCtx.getDeviceId(), topicName, msgId);
        if (topicName.startsWith(MqttTopics.BASE_GATEWAY_API_TOPIC)) {
            if (gatewaySessionHandler != null) {
                handleGatewayPublishMsg(ctx, topicName, msgId, mqttMsg);
                transportService.recordActivity(deviceSessionCtx.getSessionInfo());
            } else {
                log.error("[gatewaySessionHandler] is null, [{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId);
            }
        } else if (sparkplugSessionHandler != null) {
            handleSparkplugPublishMsg(ctx, topicName, mqttMsg);
        } else {
            processDevicePublish(ctx, mqttMsg, topicName, msgId);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `topicName`：主题名称或主题对象。
     * - `msgId`：消息ID。
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void handleGatewayPublishMsg(ChannelHandlerContext ctx, String topicName, int msgId, MqttPublishMessage mqttMsg) {
        try {
            switch (topicName) {
                case MqttTopics.GATEWAY_TELEMETRY_TOPIC:
                    gatewaySessionHandler.onDeviceTelemetry(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CLAIM_TOPIC:
                    gatewaySessionHandler.onDeviceClaim(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                    gatewaySessionHandler.onDeviceAttributes(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_ATTRIBUTES_REQUEST_TOPIC:
                    gatewaySessionHandler.onDeviceAttributesRequest(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_RPC_TOPIC:
                    gatewaySessionHandler.onDeviceRpcResponse(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_CONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceConnect(mqttMsg);
                    break;
                case MqttTopics.GATEWAY_DISCONNECT_TOPIC:
                    gatewaySessionHandler.onDeviceDisconnect(mqttMsg);
                    break;
                default:
                    ack(ctx, msgId, ReturnCode.TOPIC_NAME_INVALID);
            }
        } catch (RuntimeException e) {
            log.warn("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            ack(ctx, msgId, ReturnCode.IMPLEMENTATION_SPECIFIC);
            closeCtx(ctx);
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendAckOrCloseSession(ctx, topicName, msgId);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `topicName`：主题名称或主题对象。
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void handleSparkplugPublishMsg(ChannelHandlerContext ctx, String topicName, MqttPublishMessage mqttMsg) {
        int msgId = mqttMsg.variableHeader().packetId();
        try {
            SparkplugTopic sparkplugTopic = parseTopicPublish(topicName);
            if (sparkplugTopic.isNode()) {
                // A node topic
                SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(ProtoMqttAdaptor.toBytes(mqttMsg.payload()));
                switch (sparkplugTopic.getType()) {
                    case NBIRTH:
                    case NCMD:
                    case NDATA:
                        sparkplugSessionHandler.onAttributesTelemetryProto(msgId, sparkplugBProtoNode, sparkplugTopic);
                        break;
                    default:
                }
            } else {
                // A device topic
                SparkplugBProto.Payload sparkplugBProtoDevice = SparkplugBProto.Payload.parseFrom(ProtoMqttAdaptor.toBytes(mqttMsg.payload()));
                switch (sparkplugTopic.getType()) {
                    case DBIRTH:
                    case DCMD:
                    case DDATA:
                        sparkplugSessionHandler.onAttributesTelemetryProto(msgId, sparkplugBProtoDevice, sparkplugTopic);
                        break;
                    case DDEATH:
                        sparkplugSessionHandler.onDeviceDisconnect(mqttMsg, sparkplugTopic.getDeviceId());
                        break;
                    default:
                }
            }
        } catch (RuntimeException e) {
            log.error("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            ack(ctx, msgId, ReturnCode.IMPLEMENTATION_SPECIFIC);
            closeCtx(ctx);
        } catch (AdaptorException | ThingsboardException | InvalidProtocolBufferException e) {
            log.error("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendAckOrCloseSession(ctx, topicName, msgId);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `topicName`：主题名称或主题对象。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    private void processDevicePublish(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, String topicName, int msgId) {
        try {
            Matcher fwMatcher;
            MqttTransportAdaptor payloadAdaptor = deviceSessionCtx.getPayloadAdaptor();
            if (deviceSessionCtx.isDeviceAttributesTopic(topicName)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (deviceSessionCtx.isDeviceTelemetryTopic(topicName)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = payloadAdaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V1;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = payloadAdaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = payloadAdaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V1;
            } else if (topicName.equals(MqttTopics.DEVICE_CLAIM_TOPIC)) {
                TransportProtos.ClaimDeviceMsg claimDeviceMsg = payloadAdaptor.convertToClaimDevice(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), claimDeviceMsg, getPubAckCallback(ctx, msgId, claimDeviceMsg));
            } else if ((fwMatcher = FW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.FIRMWARE);
            } else if ((fwMatcher = SW_REQUEST_PATTERN.matcher(topicName)).find()) {
                getOtaPackageCallback(ctx, mqttMsg, msgId, fwMatcher, OtaPackageType.SOFTWARE);
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = payloadAdaptor.convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_JSON_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getJsonMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_TELEMETRY_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostTelemetryMsg postTelemetryMsg = context.getProtoMqttAdaptor().convertToPostTelemetry(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postTelemetryMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postTelemetryMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = payloadAdaptor.convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getJsonMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.equals(MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC)) {
                TransportProtos.PostAttributeMsg postAttributeMsg = context.getProtoMqttAdaptor().convertToPostAttributes(deviceSessionCtx, mqttMsg);
                transportService.process(deviceSessionCtx.getSessionInfo(), postAttributeMsg, getMetadata(deviceSessionCtx, topicName),
                        getPubAckCallback(ctx, msgId, postAttributeMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = context.getJsonMqttAdaptor().convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = context.getProtoMqttAdaptor().convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC)) {
                TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = payloadAdaptor.convertToDeviceRpcResponse(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(ctx, msgId, rpcResponseMsg));
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = context.getJsonMqttAdaptor().convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2_JSON;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = context.getProtoMqttAdaptor().convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2_PROTO;
            } else if (topicName.startsWith(MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC)) {
                TransportProtos.ToServerRpcRequestMsg rpcRequestMsg = payloadAdaptor.convertToServerRpcRequest(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC);
                transportService.process(deviceSessionCtx.getSessionInfo(), rpcRequestMsg, getPubAckCallback(ctx, msgId, rpcRequestMsg));
                toServerRpcSubTopicType = TopicType.V2;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = context.getJsonMqttAdaptor().convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_JSON_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2_JSON;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = context.getProtoMqttAdaptor().convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_PROTO_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2_PROTO;
            } else if (topicName.startsWith(MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX)) {
                TransportProtos.GetAttributeRequestMsg getAttributeMsg = payloadAdaptor.convertToGetAttributes(deviceSessionCtx, mqttMsg, MqttTopics.DEVICE_ATTRIBUTES_REQUEST_SHORT_TOPIC_PREFIX);
                transportService.process(deviceSessionCtx.getSessionInfo(), getAttributeMsg, getPubAckCallback(ctx, msgId, getAttributeMsg));
                attrReqTopicType = TopicType.V2;
            } else {
                transportService.recordActivity(deviceSessionCtx.getSessionInfo());
                ack(ctx, msgId, ReturnCode.TOPIC_NAME_INVALID);
            }
        } catch (AdaptorException e) {
            log.debug("[{}] Failed to process publish msg [{}][{}]", sessionId, topicName, msgId, e);
            sendAckOrCloseSession(ctx, topicName, msgId);
        }
    }

    /**
     * 功能：获取`Metadata`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `topicName`：主题名称或主题对象。
     * 返回：处理结果。
     */
    private TbMsgMetaData getMetadata(DeviceSessionCtx ctx, String topicName) {
        if (ctx.isDeviceProfileMqttTransportType()) {
            TbMsgMetaData md = new TbMsgMetaData();
            md.putValue(DataConstants.MQTT_TOPIC, topicName);
            return md;
        } else {
            return null;
        }
    }

    /**
     * 功能：发送或提交会话。
     * 参数：
     * - `ctx`：处理上下文。
     * - `topicName`：主题名称或主题对象。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    private void sendAckOrCloseSession(ChannelHandlerContext ctx, String topicName, int msgId) {
        if ((deviceSessionCtx.isSendAckOnValidationException() || MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) && msgId > 0) {
            log.debug("[{}] Send pub ack on invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, ReturnCode.PAYLOAD_FORMAT_INVALID));
        } else {
            log.info("[{}] Closing current session due to invalid publish msg [{}][{}]", sessionId, topicName, msgId);
            closeCtx(ctx);
        }
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * - `msgId`：消息ID。
     * - `fwMatcher`：`fwMatcher` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void getOtaPackageCallback(ChannelHandlerContext ctx, MqttPublishMessage mqttMsg, int msgId, Matcher fwMatcher, OtaPackageType type) {
        String payload = mqttMsg.content().toString(UTF8);
        int chunkSize = StringUtils.isNotEmpty(payload) ? Integer.parseInt(payload) : 0;
        String requestId = fwMatcher.group("requestId");
        int chunk = Integer.parseInt(fwMatcher.group("chunk"));

        if (chunkSize > 0) {
            this.chunkSizes.put(requestId, chunkSize);
        } else {
            chunkSize = chunkSizes.getOrDefault(requestId, 0);
        }

        if (chunkSize > context.getMaxPayloadSize()) {
            sendOtaPackageError(ctx, PAYLOAD_TOO_LARGE);
            return;
        }

        String otaPackageId = otaPackSessions.get(requestId);

        if (otaPackageId != null) {
            sendOtaPackage(ctx, mqttMsg.variableHeader().packetId(), otaPackageId, requestId, chunkSize, chunk, type);
        } else {
            TransportProtos.SessionInfoProto sessionInfo = deviceSessionCtx.getSessionInfo();
            TransportProtos.GetOtaPackageRequestMsg getOtaPackageRequestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                    .setDeviceIdMSB(sessionInfo.getDeviceIdMSB())
                    .setDeviceIdLSB(sessionInfo.getDeviceIdLSB())
                    .setTenantIdMSB(sessionInfo.getTenantIdMSB())
                    .setTenantIdLSB(sessionInfo.getTenantIdLSB())
                    .setType(type.name())
                    .build();
            transportService.process(deviceSessionCtx.getSessionInfo(), getOtaPackageRequestMsg,
                    new OtaPackageCallback(ctx, msgId, getOtaPackageRequestMsg, requestId, chunkSize, chunk));
        }
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msgId`：消息ID。
     * - `returnCode`：`returnCode` 参数。
     * 返回：无。
     */
    private void ack(ChannelHandlerContext ctx, int msgId, ReturnCode returnCode) {
        if (msgId > 0) {
            ctx.writeAndFlush(createMqttPubAckMsg(deviceSessionCtx, msgId, returnCode));
        }
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msgId`：消息ID。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final int msgId, final T msg) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}] Published msg: {}", sessionId, msg);
                ack(ctx, msgId, ReturnCode.SUCCESS);
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
                closeCtx(ctx);
            }
        };
    }

    /**
     * 中文说明：
     * 1. `DeviceProvisionCallback` 是 ThingsBoard Common Transport 中处理设备的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `TransportServiceCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    private class DeviceProvisionCallback implements TransportServiceCallback<ProvisionDeviceResponseMsg> {
        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final ChannelHandlerContext ctx;
        private final int msgId;
        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        private final TransportProtos.ProvisionDeviceRequestMsg msg;

        DeviceProvisionCallback(ChannelHandlerContext ctx, int msgId, TransportProtos.ProvisionDeviceRequestMsg msg) {
            this.ctx = ctx;
            this.msgId = msgId;
            this.msg = msg;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `provisionResponseMsg`：响应对象。
         * 返回：无。
         */
        @Override
        public void onSuccess(TransportProtos.ProvisionDeviceResponseMsg provisionResponseMsg) {
            log.trace("[{}] Published msg: {}", sessionId, msg);
            ack(ctx, msgId, ReturnCode.SUCCESS);
            try {
                if (deviceSessionCtx.getProvisionPayloadType().equals(TransportPayloadType.JSON)) {
                    deviceSessionCtx.getContext().getJsonMqttAdaptor().convertToPublish(deviceSessionCtx, provisionResponseMsg).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
                } else {
                    deviceSessionCtx.getContext().getProtoMqttAdaptor().convertToPublish(deviceSessionCtx, provisionResponseMsg).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
                }
                scheduler.schedule((Callable<ChannelFuture>) ctx::close, 60, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.trace("[{}] Failed to convert device provision response to MQTT msg", sessionId, e);
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.trace("[{}] Failed to publish msg: {}", sessionId, msg, e);
            ack(ctx, msgId, ReturnCode.IMPLEMENTATION_SPECIFIC);
            closeCtx(ctx);
        }
    }

    /**
     * 中文说明：
     * 1. `OtaPackageCallback` 是 ThingsBoard Common Transport 中处理 OTA 的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `TransportServiceCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    private class OtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        /**
         * 上下文，汇总当前处理所需的上下文信息。
         */
        private final ChannelHandlerContext ctx;
        private final int msgId;
        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        private final TransportProtos.GetOtaPackageRequestMsg msg;
        private final String requestId;
        /**
         * `chunkSize` 字段，保存当前对象的对应属性。
         */
        private final int chunkSize;
        private final int chunk;

        OtaPackageCallback(ChannelHandlerContext ctx, int msgId, TransportProtos.GetOtaPackageRequestMsg msg, String requestId, int chunkSize, int chunk) {
            this.ctx = ctx;
            this.msgId = msgId;
            this.msg = msg;
            this.requestId = requestId;
            this.chunkSize = chunkSize;
            this.chunk = chunk;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `response`：响应对象。
         * 返回：无。
         */
        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg response) {
            if (TransportProtos.ResponseStatus.SUCCESS.equals(response.getResponseStatus())) {
                OtaPackageId firmwareId = new OtaPackageId(new UUID(response.getOtaPackageIdMSB(), response.getOtaPackageIdLSB()));
                otaPackSessions.put(requestId, firmwareId.toString());
                sendOtaPackage(ctx, msgId, firmwareId.toString(), requestId, chunkSize, chunk, OtaPackageType.valueOf(response.getType()));
            } else {
                sendOtaPackageError(ctx, response.getResponseStatus().toString());
            }
        }

        /**
         * 功能：处理错误信息。
         * 参数：
         * - `e`：`e` 参数。
         * 返回：无。
         */
        @Override
        public void onError(Throwable e) {
            log.trace("[{}] Failed to get firmware: {}", sessionId, msg, e);
            closeCtx(ctx);
        }
    }

    /**
     * 功能：发送或提交`Ota Package`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msgId`：消息ID。
     * - `firmwareId`：`firmwareId`ID。
     * - `requestId`：请求ID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void sendOtaPackage(ChannelHandlerContext ctx, int msgId, String firmwareId, String requestId, int chunkSize, int chunk, OtaPackageType type) {
        log.trace("[{}] Send firmware [{}] to device!", sessionId, firmwareId);
        ack(ctx, msgId, ReturnCode.SUCCESS);
        try {
            byte[] firmwareChunk = context.getOtaPackageDataCache().get(firmwareId, chunkSize, chunk);
            deviceSessionCtx.getPayloadAdaptor()
                    .convertToPublish(deviceSessionCtx, firmwareChunk, requestId, chunk, type)
                    .ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to send firmware response!", sessionId, e);
        }
    }

    /**
     * 功能：发送或提交错误信息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `error`：错误信息。
     * 返回：无。
     */
    private void sendOtaPackageError(ChannelHandlerContext ctx, String error) {
        log.warn("[{}] {}", sessionId, error);
        deviceSessionCtx.getChannel().writeAndFlush(deviceSessionCtx
                .getPayloadAdaptor()
                .createMqttPublishMsg(deviceSessionCtx, MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC, error.getBytes()));
        closeCtx(ctx);
    }

    /**
     * 功能：处理`Subscribe`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void processSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            int returnCode = ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.NOT_AUTHORIZED_5);
            ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), Collections.singletonList(returnCode)));
            return;
        }
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        List<Integer> grantedQoSList = new ArrayList<>();
        boolean activityReported = false;
        for (MqttTopicSubscription subscription : mqttMsg.payload().topicSubscriptions()) {
            String topic = subscription.topicName();
            MqttQoS reqQoS = subscription.qualityOfService();
            if (deviceSessionCtx.isDeviceSubscriptionAttributesTopic(topic)){
                processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                activityReported = true;
                continue;
            }
            try {
                if (sparkplugSessionHandler != null) {
                    sparkplugSessionHandler.handleSparkplugSubscribeMsg(grantedQoSList, subscription, reqQoS);
                    activityReported = true;
                } else {
                    switch (topic) {
                        case MqttTopics.DEVICE_ATTRIBUTES_TOPIC: {
                            processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC: {
                            processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC: {
                            processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_JSON);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC: {
                            processAttributesSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_PROTO);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC: {
                            processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V1);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC: {
                            processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC: {
                            processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_JSON);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC: {
                            processRpcSubscribe(grantedQoSList, topic, reqQoS, TopicType.V2_PROTO);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                        case MqttTopics.GATEWAY_RPC_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC:
                            registerSubQoS(topic, grantedQoSList, reqQoS);
                            break;
                        default:
                            log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS);
                            grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.TOPIC_FILTER_INVALID));
                            break;
                    }
                }
            } catch (Exception e) {
                log.warn("[{}] Failed to subscribe to [{}][{}]", sessionId, topic, reqQoS, e);
                grantedQoSList.add(ReturnCodeResolver.getSubscriptionReturnCode(deviceSessionCtx.getMqttVersion(), ReturnCode.IMPLEMENTATION_SPECIFIC));
            }
        }
        if (!activityReported) {
            transportService.recordActivity(deviceSessionCtx.getSessionInfo());
        }
        ctx.writeAndFlush(createSubAckMessage(mqttMsg.variableHeader().messageId(), grantedQoSList));
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `grantedQoSList`：数据列表。
     * - `topic`：主题名称或主题对象。
     * - `reqQoS`：`reqQoS` 参数。
     * - `topicType`：主题名称或主题对象。
     * 返回：无。
     */
    private void processRpcSubscribe(List<Integer> grantedQoSList, String topic, MqttQoS reqQoS, TopicType topicType) {
        transportService.process(deviceSessionCtx.getSessionInfo(), TransportProtos.SubscribeToRPCMsg.newBuilder().build(), null);
        rpcSubTopicType = topicType;
        registerSubQoS(topic, grantedQoSList, reqQoS);
    }

    /**
     * 功能：处理`Attributes Subscribe`。
     * 参数：
     * - `grantedQoSList`：数据列表。
     * - `topic`：主题名称或主题对象。
     * - `reqQoS`：`reqQoS` 参数。
     * - `topicType`：主题名称或主题对象。
     * 返回：无。
     */
    private void processAttributesSubscribe(List<Integer> grantedQoSList, String topic, MqttQoS reqQoS, TopicType topicType) {
        transportService.process(deviceSessionCtx.getSessionInfo(), TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().build(), null);
        attrSubTopicType = topicType;
        registerSubQoS(topic, grantedQoSList, reqQoS);
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `grantedQoSList`：数据列表。
     * - `reqQoS`：`reqQoS` 参数。
     * 返回：无。
     */
    public void processAttributesRpcSubscribeSparkplugNode(List<Integer> grantedQoSList, MqttQoS reqQoS) {
        transportService.process(TransportProtos.TransportToDeviceActorMsg.newBuilder()
                .setSessionInfo(deviceSessionCtx.getSessionInfo())
                .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                .build(), null);
        registerSubQoS(MqttTopics.DEVICE_ATTRIBUTES_TOPIC, grantedQoSList, reqQoS);
    }

    /**
     * 功能：保存或创建QoS 等级。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `grantedQoSList`：数据列表。
     * - `reqQoS`：`reqQoS` 参数。
     * 返回：无。
     */
    public void registerSubQoS(String topic, List<Integer> grantedQoSList, MqttQoS reqQoS) {
        grantedQoSList.add(getMinSupportedQos(reqQoS));
        mqttQoSMap.put(new MqttTopicMatcher(topic), getMinSupportedQos(reqQoS));
    }

    /**
     * 功能：处理`Unsubscribe`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void processUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage mqttMsg) {
        if (!checkConnected(ctx, mqttMsg)) {
            ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId(), Collections.singletonList(ReturnCode.NOT_AUTHORIZED_5.shortValue())));
            return;
        }
        boolean activityReported = false;
        List<Short> unSubResults = new ArrayList<>();
        log.trace("[{}] Processing subscription [{}]!", sessionId, mqttMsg.variableHeader().messageId());
        for (String topicName : mqttMsg.payload().topics()) {
            MqttTopicMatcher matcher = new MqttTopicMatcher(topicName);
            if (mqttQoSMap.containsKey(matcher)) {
                mqttQoSMap.remove(matcher);
                try {
                    short resultValue = ReturnCode.SUCCESS.shortValue();
                    switch (topicName) {
                        case MqttTopics.DEVICE_ATTRIBUTES_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC: {
                            transportService.process(deviceSessionCtx.getSessionInfo(),
                                    TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(), null);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_RPC_REQUESTS_SUB_SHORT_PROTO_TOPIC: {
                            transportService.process(deviceSessionCtx.getSessionInfo(),
                                    TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(), null);
                            activityReported = true;
                            break;
                        }
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_RPC_RESPONSE_SUB_SHORT_PROTO_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_JSON_TOPIC:
                        case MqttTopics.DEVICE_ATTRIBUTES_RESPONSES_SHORT_PROTO_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_TOPIC:
                        case MqttTopics.GATEWAY_RPC_TOPIC:
                        case MqttTopics.GATEWAY_ATTRIBUTES_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_PROVISION_RESPONSE_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_FIRMWARE_ERROR_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_RESPONSES_TOPIC:
                        case MqttTopics.DEVICE_SOFTWARE_ERROR_TOPIC: {
                            activityReported = true;
                            break;
                        }
                        default:
                            log.trace("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                            resultValue = ReturnCode.TOPIC_FILTER_INVALID.shortValue();
                    }
                    unSubResults.add(resultValue);
                } catch (Exception e) {
                    log.debug("[{}] Failed to process unsubscription [{}] to [{}]", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                    unSubResults.add(ReturnCode.IMPLEMENTATION_SPECIFIC.shortValue());
                }
            } else {
                log.debug("[{}] Failed to process unsubscription [{}] to [{}] - Subscription not found", sessionId, mqttMsg.variableHeader().messageId(), topicName);
                unSubResults.add(ReturnCode.NO_SUBSCRIPTION_EXISTED.shortValue());
            }
        }
        if (!activityReported) {
            transportService.recordActivity(deviceSessionCtx.getSessionInfo());
        }
        ctx.writeAndFlush(createUnSubAckMessage(mqttMsg.variableHeader().messageId(), unSubResults));
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `msgId`：消息ID。
     * - `resultCodes`：数据列表。
     * 返回：处理结果。
     */
    private MqttMessage createUnSubAckMessage(int msgId, List<Short> resultCodes) {
        MqttMessageBuilders.UnsubAckBuilder unsubAckBuilder = MqttMessageBuilders.unsubAck();
        unsubAckBuilder.packetId(msgId);
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            unsubAckBuilder.addReasonCodes(resultCodes.toArray(Short[]::new));
        }
        return unsubAckBuilder.build();
    }

    /**
     * 功能：处理`Connect`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void processConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        log.debug("[{}][{}] Processing connect msg for client: {}!", address, sessionId, msg.payload().clientIdentifier());
        String userName = msg.payload().userName();
        String clientId = msg.payload().clientIdentifier();
        deviceSessionCtx.setMqttVersion(getMqttVersion(msg.variableHeader().version()));
        if (DataConstants.PROVISION.equals(userName) || DataConstants.PROVISION.equals(clientId)) {
            deviceSessionCtx.setProvisionOnly(true);
            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SUCCESS, msg));
        } else {
            X509Certificate cert;
            if (sslHandler != null && (cert = getX509Certificate()) != null) {
                processX509CertConnect(ctx, cert, msg);
            } else {
                processAuthTokenConnect(ctx, msg);
            }
        }
    }

    /**
     * 功能：处理令牌。
     * 参数：
     * - `ctx`：处理上下文。
     * - `connectMessage`：待处理消息。
     * 返回：无。
     */
    private void processAuthTokenConnect(ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        String userName = connectMessage.payload().userName();
        log.debug("[{}][{}] Processing connect msg for client with user name: {}!", address, sessionId, userName);
        TransportProtos.ValidateBasicMqttCredRequestMsg.Builder request = TransportProtos.ValidateBasicMqttCredRequestMsg.newBuilder()
                .setClientId(connectMessage.payload().clientIdentifier());
        if (userName != null) {
            request.setUserName(userName);
        }
        byte[] passwordBytes = connectMessage.payload().passwordInBytes();
        if (passwordBytes != null) {
            String password = new String(passwordBytes, CharsetUtil.UTF_8);
            request.setPassword(password);
        }
        transportService.process(DeviceTransportType.MQTT, request.build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        onValidateDeviceResponse(msg, ctx, connectMessage);
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] Failed to process credentials: {}", address, userName, e);
                        ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                        closeCtx(ctx);
                    }
                });
    }

    /**
     * 功能：处理`X509 Cert Connect`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `cert`：`cert` 参数。
     * - `connectMessage`：待处理消息。
     * 返回：无。
     */
    private void processX509CertConnect(ChannelHandlerContext ctx, X509Certificate cert, MqttConnectMessage connectMessage) {
        try {
            if (!context.isSkipValidityCheckForClientCert()) {
                cert.checkValidity();
            }
            String strCert = SslUtil.getCertificateString(cert);
            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
            transportService.process(DeviceTransportType.MQTT, ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                            onValidateDeviceResponse(msg, ctx, connectMessage);
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.trace("[{}] Failed to process credentials: {}", address, sha3Hash, e);
                            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                            closeCtx(ctx);
                        }
                    });
        } catch (Exception e) {
            context.onAuthFailure(address);
            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.NOT_AUTHORIZED_5, connectMessage));
            log.trace("[{}] X509 auth failure: {}", sessionId, address, e);
            closeCtx(ctx);
        }
    }

    /**
     * 功能：获取证书。
     * 参数：无。
     * 返回：处理结果。
     */
    private X509Certificate getX509Certificate() {
        try {
            Certificate[] certChain = sslHandler.engine().getSession().getPeerCertificates();
            if (certChain.length > 0) {
                return (X509Certificate) certChain[0];
            }
        } catch (SSLPeerUnverifiedException e) {
            log.warn(e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `returnCode`：`returnCode` 参数。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private MqttConnAckMessage createMqttConnAckMsg(ReturnCode returnCode, MqttConnectMessage msg) {
        MqttMessageBuilders.ConnAckBuilder connAckBuilder = MqttMessageBuilders.connAck();
        connAckBuilder.sessionPresent(!msg.variableHeader().isCleanSession());
        MqttConnectReturnCode finalReturnCode = ReturnCodeResolver.getConnectionReturnCode(deviceSessionCtx.getMqttVersion(), returnCode);
        connAckBuilder.returnCode(finalReturnCode);
        return connAckBuilder.build();
    }

    /**
     * 功能：执行 `channelReadComplete` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 功能：执行 `exceptionCaught` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            if (log.isDebugEnabled()) {
                log.debug("[{}][{}][{}] IOException: {}", sessionId,
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceId).orElse(null),
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceName).orElse(""),
                        cause);
            } else if (log.isInfoEnabled()) {
                log.info("[{}][{}][{}] IOException: {}", sessionId,
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceId).orElse(null),
                        Optional.ofNullable(this.deviceSessionCtx.getDeviceInfo()).map(TransportDeviceInfo::getDeviceName).orElse(""),
                        cause.getMessage());
            }
        } else {
            log.error("[{}] Unexpected Exception", sessionId, cause);
        }

        closeCtx(ctx);
        if (cause instanceof OutOfMemoryError) {
            log.error("Received critical error. Going to shutdown the service.");
            System.exit(1);
        }
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `msgId`：消息ID。
     * - `grantedQoSList`：数据列表。
     * 返回：处理结果。
     */
    private static MqttSubAckMessage createSubAckMessage(Integer msgId, List<Integer> grantedQoSList) {
        MqttFixedHeader mqttFixedHeader =
                new MqttFixedHeader(SUBACK, false, AT_MOST_ONCE, false, 0);
        MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(msgId);
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantedQoSList);
        return new MqttSubAckMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubAckPayload);
    }

    /**
     * 功能：获取QoS 等级。
     * 参数：
     * - `reqQoS`：`reqQoS` 参数。
     * 返回：数值结果。
     */
    private static int getMinSupportedQos(MqttQoS reqQoS) {
        return Math.min(reqQoS.value(), MAX_SUPPORTED_QOS_LVL.value());
    }

    /**
     * 功能：获取版本号。
     * 参数：
     * - `versionCode`：`versionCode` 参数。
     * 返回：处理结果。
     */
    private static MqttVersion getMqttVersion(int versionCode) {
        switch (versionCode) {
            case 3:
                return MqttVersion.MQTT_3_1;
            case 5:
                return MqttVersion.MQTT_5;
            default:
                return MqttVersion.MQTT_3_1_1;
        }
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `deviceSessionCtx`：设备信息或设备标识。
     * - `requestId`：请求ID。
     * - `returnCode`：`returnCode` 参数。
     * 返回：处理结果。
     */
    public static MqttMessage createMqttPubAckMsg(DeviceSessionCtx deviceSessionCtx, int requestId, ReturnCode returnCode) {
        MqttMessageBuilders.PubAckBuilder pubAckMsgBuilder = MqttMessageBuilders.pubAck().packetId(requestId);
        if (MqttVersion.MQTT_5.equals(deviceSessionCtx.getMqttVersion())) {
            pubAckMsgBuilder.reasonCode(returnCode.byteValue());
        }
        return pubAckMsgBuilder.build();
    }

    /**
     * 功能：校验`Connected`。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean checkConnected(ChannelHandlerContext ctx, MqttMessage msg) {
        if (deviceSessionCtx.isConnected()) {
            return true;
        } else {
            log.info("[{}] Closing current session due to invalid msg order: {}", sessionId, msg);
            return false;
        }
    }

    /**
     * 功能：校验会话。
     * 参数：
     * - `sessionMetaData`：会话对象。
     * 返回：无。
     */
    private void checkGatewaySession(SessionMetaData sessionMetaData) {
        TransportDeviceInfo device = deviceSessionCtx.getDeviceInfo();
        try {
            JsonNode infoNode = context.getMapper().readTree(device.getAdditionalInfo());
            if (infoNode != null) {
                JsonNode gatewayNode = infoNode.get("gateway");
                if (gatewayNode != null && gatewayNode.asBoolean()) {
                    gatewaySessionHandler = new GatewaySessionHandler(deviceSessionCtx, sessionId);
                    if (infoNode.has(DefaultTransportService.OVERWRITE_ACTIVITY_TIME) && infoNode.get(DefaultTransportService.OVERWRITE_ACTIVITY_TIME).isBoolean()) {
                        sessionMetaData.setOverwriteActivityTime(infoNode.get(DefaultTransportService.OVERWRITE_ACTIVITY_TIME).asBoolean());
                    }
                }
            }
        } catch (IOException e) {
            log.trace("[{}][{}] Failed to fetch device additional info", sessionId, device.getDeviceName(), e);
        }
    }

    /**
     * 功能：校验会话。
     * 参数：
     * - `connectMessage`：待处理消息。
     * - `ctx`：处理上下文。
     * - `sessionMetaData`：会话对象。
     * 返回：无。
     */
    private void checkSparkplugNodeSession(MqttConnectMessage connectMessage, ChannelHandlerContext ctx, SessionMetaData sessionMetaData) {
        try {
            if (sparkplugSessionHandler == null) {
                SparkplugTopic sparkplugTopicNode = validatedSparkplugTopicConnectedNode(connectMessage);
                if (sparkplugTopicNode != null) {
                    SparkplugBProto.Payload sparkplugBProtoNode = SparkplugBProto.Payload.parseFrom(connectMessage.payload().willMessageInBytes());
                    sparkplugSessionHandler = new SparkplugNodeSessionHandler(this, deviceSessionCtx, sessionId, sparkplugTopicNode);
                    sparkplugSessionHandler.onAttributesTelemetryProto(0, sparkplugBProtoNode, sparkplugTopicNode);
                    sessionMetaData.setOverwriteActivityTime(true);
                } else {
                    log.trace("[{}][{}] Failed to fetch sparkplugDevice connect:  sparkplugTopicName without SparkplugMessageType.NDEATH.", sessionId, deviceSessionCtx.getDeviceInfo().getDeviceName());
                    throw new ThingsboardException("Invalid request body", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to fetch sparkplugDevice connect, sparkplugTopicName", sessionId, deviceSessionCtx.getDeviceInfo().getDeviceName(), e);
            ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
            closeCtx(ctx);
        }
    }

    /**
     * 功能：执行 `validatedSparkplugTopicConnectedNode` 对应的处理。
     * 参数：
     * - `connectMessage`：待处理消息。
     * 返回：处理结果。
     */
    private SparkplugTopic validatedSparkplugTopicConnectedNode(MqttConnectMessage connectMessage) throws ThingsboardException {
        if (StringUtils.isNotBlank(connectMessage.payload().willTopic())
                && connectMessage.payload().willMessageInBytes() != null
                && connectMessage.payload().willMessageInBytes().length > 0) {
            SparkplugTopic sparkplugTopicNode = parseTopicPublish(connectMessage.payload().willTopic());
            if (NDEATH.equals(sparkplugTopicNode.getType())) {
                return sparkplugTopicNode;
            }
        }
        return null;
    }

    /**
     * 功能：执行 `operationComplete` 对应的处理。
     * 参数：
     * - `future`：`future` 参数。
     * 返回：无。
     */
    @Override
    public void operationComplete(Future<? super Void> future) throws Exception {
        log.trace("[{}] Channel closed!", sessionId);
        doDisconnect();
    }

    /**
     * 功能：执行 `doDisconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void doDisconnect() {
        if (deviceSessionCtx.isConnected()) {
            log.debug("[{}] Client disconnected!", sessionId);
            transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_CLOSED, null);
            transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
            if (gatewaySessionHandler != null) {
                gatewaySessionHandler.onDevicesDisconnect();
            }
            if (sparkplugSessionHandler != null) {
                // add Msg Telemetry node: key STATE type: String value: OFFLINE ts: sparkplugBProto.getTimestamp()
                sparkplugSessionHandler.sendSparkplugStateOnTelemetry(deviceSessionCtx.getSessionInfo(),
                        deviceSessionCtx.getDeviceInfo().getDeviceName(), OFFLINE, new Date().getTime());
                sparkplugSessionHandler.onDevicesDisconnect();
            }
            deviceSessionCtx.setDisconnected();
        }
        deviceSessionCtx.release();
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * - `ctx`：处理上下文。
     * - `connectMessage`：待处理消息。
     * 返回：无。
     */
    private void onValidateDeviceResponse(ValidateDeviceCredentialsResponse msg, ChannelHandlerContext ctx, MqttConnectMessage connectMessage) {
        if (!msg.hasDeviceInfo()) {
            context.onAuthFailure(address);
            ReturnCode returnCode = ReturnCode.NOT_AUTHORIZED_5;
            if (sslHandler == null || getX509Certificate() == null) {
                String username = connectMessage.payload().userName();
                byte[] passwordBytes = connectMessage.payload().passwordInBytes();
                String clientId = connectMessage.payload().clientIdentifier();
                if ((username != null && passwordBytes != null && clientId != null)
                        || (username == null ^ passwordBytes == null)) {
                    returnCode = ReturnCode.BAD_USERNAME_OR_PASSWORD;
                } else if (!StringUtils.isBlank(clientId)) {
                    returnCode = ReturnCode.CLIENT_IDENTIFIER_NOT_VALID;
                }
            }
            ctx.writeAndFlush(createMqttConnAckMsg(returnCode, connectMessage));
            closeCtx(ctx);
        } else {
            context.onAuthSuccess(address);
            deviceSessionCtx.setDeviceInfo(msg.getDeviceInfo());
            deviceSessionCtx.setDeviceProfile(msg.getDeviceProfile());
            deviceSessionCtx.setSessionInfo(SessionInfoCreator.create(msg, context, sessionId));
            transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_OPEN, new TransportServiceCallback<Void>() {
                @Override
                public void onSuccess(Void msg) {
                    SessionMetaData sessionMetaData = transportService.registerAsyncSession(deviceSessionCtx.getSessionInfo(), MqttTransportHandler.this);
                    if (deviceSessionCtx.isSparkplug()) {
                        checkSparkplugNodeSession(connectMessage, ctx, sessionMetaData);
                    } else {
                        checkGatewaySession(sessionMetaData);
                    }
                    ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SUCCESS, connectMessage));
                    deviceSessionCtx.setConnected(true);
                    log.debug("[{}] Client connected!", sessionId);
                    transportService.getCallbackExecutor().execute(() -> processMsgQueue(ctx)); //this callback will execute in Producer worker thread and hard or blocking work have to be submitted to the separate thread.
                }

                @Override
                public void onError(Throwable e) {
                    if (e instanceof TbRateLimitsException) {
                        log.trace("[{}] Failed to submit session event: {}", sessionId, e.getMessage());
                    } else {
                        log.warn("[{}] Failed to submit session event", sessionId, e);
                    }
                    ctx.writeAndFlush(createMqttConnAckMsg(ReturnCode.SERVER_UNAVAILABLE_5, connectMessage));
                    closeCtx(ctx);
                }
            });
        }
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg response) {
        log.trace("[{}] Received get attributes response", sessionId);
        String topicBase = attrReqTopicType.getAttributesResponseTopicBase();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(attrReqTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, response, topicBase).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes response to MQTT msg", sessionId, e);
        }
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `sessionId`：会话ID。
     * - `notification`：`notification` 参数。
     * 返回：无。
     */
    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to device", sessionId);
        try {
            if (sparkplugSessionHandler != null) {
                log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
                notification.getSharedUpdatedList().forEach(tsKvProto -> {
                    if (sparkplugSessionHandler.getNodeBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                        SparkplugTopic sparkplugTopic = new SparkplugTopic(sparkplugSessionHandler.getSparkplugTopicNode(),
                                SparkplugMessageType.NCMD);
                        sparkplugSessionHandler.createSparkplugMqttPublishMsg(tsKvProto,
                                sparkplugTopic.toString(),
                                sparkplugSessionHandler.getNodeBirthMetrics().get(tsKvProto.getKv().getKey()))
                                .ifPresent(sparkplugSessionHandler::writeAndFlush);
                    }
                });
            } else {
                String topic = attrSubTopicType.getAttributesSubTopic();
                MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(attrSubTopicType);
                adaptor.convertToPublish(deviceSessionCtx, notification, topic).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
            }
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device attributes update to MQTT msg", sessionId, e);
        }
    }

    /**
     * 功能：处理会话。
     * 参数：
     * - `sessionId`：会话ID。
     * - `sessionCloseNotification`：会话对象。
     * 返回：无。
     */
    @Override
    public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
        log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        closeCtx(deviceSessionCtx.getChannel());
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionId`：会话ID。
     * - `rpcRequest`：请求对象。
     * 返回：无。
     */
    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}][{}] Received RPC command to device: {}", deviceSessionCtx.getDeviceId(), sessionId, rpcRequest);
        try {
            if (sparkplugSessionHandler != null) {
                handleToSparkplugDeviceRpcRequest(rpcRequest);
            } else {
                String baseTopic = rpcSubTopicType.getRpcRequestTopicBase();
                MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(rpcSubTopicType);
                adaptor.convertToPublish(deviceSessionCtx, rpcRequest, baseTopic)
                        .ifPresent(payload -> sendToDeviceRpcRequest(payload, rpcRequest, deviceSessionCtx.getSessionInfo()));
            }
        } catch (Exception e) {
            log.trace("[{}][{}] Failed to convert device RPC command to MQTT msg", deviceSessionCtx.getDeviceId(), sessionId, e);
            this.sendErrorRpcResponse(deviceSessionCtx.getSessionInfo(), rpcRequest.getRequestId(),
                    ThingsboardErrorCode.INVALID_ARGUMENTS,
                    "Failed to convert device RPC command to MQTT msg: " + rpcRequest.getMethodName() + rpcRequest.getParams());
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `rpcRequest`：请求对象。
     * 返回：无。
     */
    private void handleToSparkplugDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequest) throws ThingsboardException {
        SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
        SparkplugRpcRequestHeader header;
        if (StringUtils.isNotEmpty(rpcRequest.getParams())) {
            header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
        } else {
            header = new SparkplugRpcRequestHeader();
        }
        header.setMessageType(messageType.name());
        TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
        if (sparkplugSessionHandler.getNodeBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
            SparkplugTopic sparkplugTopic = new SparkplugTopic(sparkplugSessionHandler.getSparkplugTopicNode(),
                    messageType);
            sparkplugSessionHandler.createSparkplugMqttPublishMsg(tsKvProto,
                    sparkplugTopic.toString(),
                    sparkplugSessionHandler.getNodeBirthMetrics().get(tsKvProto.getKv().getKey()))
                    .ifPresent(payload -> sendToDeviceRpcRequest(payload, rpcRequest, deviceSessionCtx.getSessionInfo()));
        } else {
            sendErrorRpcResponse(deviceSessionCtx.getSessionInfo(), rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, "Failed send To Node Rpc Request: " +
                            rpcRequest.getMethodName() + ". This node does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
        }
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `payload`：`payload` 参数。
     * - `rpcRequest`：请求对象。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    public void sendToDeviceRpcRequest(MqttMessage payload, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        int msgId = ((MqttPublishMessage) payload).variableHeader().packetId();
        int requestId = rpcRequest.getRequestId();
        if (isAckExpected(payload)) {
            rpcAwaitingAck.put(msgId, rpcRequest);
            context.getScheduler().schedule(() -> {
                TransportProtos.ToDeviceRpcRequestMsg msg = rpcAwaitingAck.remove(msgId);
                if (msg != null) {
                    log.trace("[{}][{}][{}] Going to send to device actor RPC request TIMEOUT status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                    transportService.process(sessionInfo, rpcRequest, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
                }
            }, Math.max(0, Math.min(deviceSessionCtx.getContext().getTimeout(), rpcRequest.getExpirationTime() - System.currentTimeMillis())), TimeUnit.MILLISECONDS);
        }
        var cf = publish(payload, deviceSessionCtx);
        cf.addListener(result -> {
            Throwable throwable = result.cause();
            if (throwable != null) {
                log.trace("[{}][{}][{}] Failed send RPC request to device due to: ", deviceSessionCtx.getDeviceId(), sessionId, requestId, throwable);
                this.sendErrorRpcResponse(sessionInfo, requestId,
                        ThingsboardErrorCode.INVALID_ARGUMENTS, " Failed send To Device Rpc Request: " + rpcRequest.getMethodName());
                return;
            }
            if (!isAckExpected(payload)) {
                log.trace("[{}][{}][{}] Going to send to device actor RPC request DELIVERED status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                transportService.process(sessionInfo, rpcRequest, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
            } else if (rpcRequest.getPersisted()) {
                log.trace("[{}][{}][{}] Going to send to device actor RPC request SENT status update ...", deviceSessionCtx.getDeviceId(), sessionId, requestId);
                transportService.process(sessionInfo, rpcRequest, RpcStatus.SENT, TransportServiceCallback.EMPTY);
            }
            if (sparkplugSessionHandler != null) {
                this.sendSuccessRpcResponse(sessionInfo, requestId, ResponseCode.CONTENT, "Success: " + rpcRequest.getMethodName());
            }
        });
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `rpcResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg rpcResponse) {
        log.trace("[{}] Received RPC response from server", sessionId);
        String baseTopic = toServerRpcSubTopicType.getRpcResponseTopicBase();
        MqttTransportAdaptor adaptor = deviceSessionCtx.getAdaptor(toServerRpcSubTopicType);
        try {
            adaptor.convertToPublish(deviceSessionCtx, rpcResponse, baseTopic).ifPresent(deviceSessionCtx.getChannel()::writeAndFlush);
        } catch (Exception e) {
            log.trace("[{}] Failed to convert device RPC command to MQTT msg", sessionId, e);
        }
    }

    /**
     * 功能：执行 `publish` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * - `deviceSessionCtx`：设备信息或设备标识。
     * 返回：异步处理结果。
     */
    private ChannelFuture publish(MqttMessage message, DeviceSessionCtx deviceSessionCtx) {
        return deviceSessionCtx.getChannel().writeAndFlush(message);
    }

    /**
     * 功能：判断`Ack Expected`。
     * 参数：
     * - `message`：待处理消息。
     * 返回：判断结果。
     */
    private boolean isAckExpected(MqttMessage message) {
        return message.fixedHeader().qosLevel().value() > 0;
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
        deviceSessionCtx.onDeviceProfileUpdate(sessionInfo, deviceProfile);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `device`：设备信息或设备标识。
     * - `deviceProfileOpt`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        deviceSessionCtx.onDeviceUpdate(sessionInfo, device, deviceProfileOpt);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(DeviceId deviceId) {
        context.onAuthFailure(address);
        ChannelHandlerContext ctx = deviceSessionCtx.getChannel();
        closeCtx(ctx);
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `requestId`：请求ID。
     * - `result`：`result` 参数。
     * - `errorMsg`：待处理消息。
     * 返回：无。
     */
    public void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ThingsboardErrorCode result, String errorMsg) {
        String payload = JacksonUtil.toString(SparkplugRpcResponseBody.builder().result(result.name()).error(errorMsg).build());
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setError(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `requestId`：请求ID。
     * - `result`：`result` 参数。
     * - `successMsg`：待处理消息。
     * 返回：无。
     */
    public void sendSuccessRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ResponseCode result, String successMsg) {
        String payload = JacksonUtil.toString(SparkplugRpcResponseBody.builder().result(result.getName()).result(successMsg).build());
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setError(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

}
