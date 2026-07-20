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


import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.ProtocolStringList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.transport.mqtt.MqttTransportContext;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.adaptors.JsonMqttAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;
import org.thingsboard.server.transport.mqtt.adaptors.ProtoMqttAdaptor;
import org.thingsboard.server.transport.mqtt.util.ReturnCode;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;
import static org.thingsboard.server.common.data.DataConstants.DEFAULT_DEVICE_TYPE;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_CLOSED;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SESSION_EVENT_MSG_OPEN;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG;
import static org.thingsboard.server.common.transport.service.DefaultTransportService.SUBSCRIBE_TO_RPC_ASYNC_MSG;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.OFFLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.STATE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.messageName;

/**
 * Created by ashvayka on 19.01.17.
 */
/**
 * 中文说明：
 * 1. `AbstractGatewaySessionHandler` 是 ThingsBoard Common Transport 中处理会话的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractGatewayDeviceSessionContext`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class AbstractGatewaySessionHandler<T extends AbstractGatewayDeviceSessionContext> {

    /**
     * 值常量，用于统一引用固定值。
     */
    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";
    private static final String DEVICE_PROPERTY = "device";

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    protected final MqttTransportContext context;
    protected final TransportService transportService;
    /**
     * `gateway` 字段，保存当前对象的对应属性。
     */
    protected final TransportDeviceInfo gateway;
    protected final UUID sessionId;
    /**
     * 设备映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<String, Lock> deviceCreationLockMap;
    private final ConcurrentMap<String, T> devices;
    /**
     * 设备列表，用于保存一组待处理对象。
     */
    private final ConcurrentMap<String, ListenableFuture<T>> deviceFutures;
    protected final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;
    /**
     * 网络通道，表示当前网络连接使用的通道。
     */
    protected final ChannelHandlerContext channel;
    protected final DeviceSessionCtx deviceSessionCtx;

    /**
     * 功能：创建 `AbstractGatewaySessionHandler` 实例，并初始化必要字段。
     * 参数：
     * - `deviceSessionCtx`：设备信息或设备标识。
     * - `sessionId`：会话ID。
     * 返回：新创建的对象实例。
     */
    public AbstractGatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId) {
        this.context = deviceSessionCtx.getContext();
        this.transportService = context.getTransportService();
        this.deviceSessionCtx = deviceSessionCtx;
        this.gateway = deviceSessionCtx.getDeviceInfo();
        this.sessionId = sessionId;
        this.devices = new ConcurrentHashMap<>();
        this.deviceFutures = new ConcurrentHashMap<>();
        this.deviceCreationLockMap = createWeakMap();
        this.mqttQoSMap = deviceSessionCtx.getMqttQoSMap();
        this.channel = deviceSessionCtx.getChannel();
    }

    /**
     * 功能：保存或创建`Weak Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    ConcurrentReferenceHashMap<String, Lock> createWeakMap() {
        return new ConcurrentReferenceHashMap<>(16, ReferenceType.WEAK);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceDisconnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceDisconnectJson(mqttMsg);
        } else {
            onGatewayDeviceDisconnectProto(mqttMsg);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceClaim(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceClaimJson(msgId, payload);
        } else {
            onDeviceClaimProto(msgId, payload);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceAttributes(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceAttributesJson(msgId, payload);
        } else {
            onDeviceAttributesProto(msgId, payload);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceAttributesRequest(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceAttributesRequestJson(mqttMsg);
        } else {
            onDeviceAttributesRequestProto(mqttMsg);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceRpcResponse(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceRpcResponseJson(msgId, payload);
        } else {
            onDeviceRpcResponseProto(msgId, payload);
        }
    }

    /**
     * 功能：处理`on Devices Disconnect`。
     * 参数：无。
     * 返回：无。
     */
    public void onDevicesDisconnect() {
        devices.forEach(this::deregisterSession);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceDeleted(String deviceName) {
        deregisterSession(deviceName);
    }

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getNodeId() {
        return context.getNodeId();
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：处理结果。
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttTransportAdaptor getPayloadAdaptor() {
        return deviceSessionCtx.getPayloadAdaptor();
    }

    /**
     * 功能：执行 `deregisterSession` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    void deregisterSession(String deviceName) {
        MqttDeviceAwareSessionContext deviceSessionCtx = devices.remove(deviceName);
        if (deviceSessionCtx != null) {
            deregisterSession(deviceName, deviceSessionCtx);
        } else {
            log.debug("[{}][{}][{}] Device [{}] was already removed from the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
        }
    }

    /**
     * 功能：执行 `writeAndFlush` 对应的处理。
     * 参数：
     * - `mqttMessage`：待处理消息。
     * 返回：异步处理结果。
     */
    public ChannelFuture writeAndFlush(MqttMessage mqttMessage) {
        return channel.writeAndFlush(mqttMessage);
    }

    /**
     * 功能：执行 `nextMsgId` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    int nextMsgId() {
        return deviceSessionCtx.nextMsgId();
    }

    /**
     * 功能：判断消息载荷。
     * 参数：无。
     * 返回：判断结果。
     */
    protected boolean isJsonPayloadType() {
        return deviceSessionCtx.isJsonPayloadType();
    }

    /**
     * 功能：处理`On Connect`。
     * 参数：
     * - `msg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * - `deviceType`：设备信息或设备标识。
     * 返回：无。
     */
    protected void processOnConnect(MqttPublishMessage msg, String deviceName, String deviceType) {
        log.trace("[{}][{}][{}] onDeviceConnect: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
        Futures.addCallback(onDeviceConnect(deviceName, deviceType), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable T result) {
                ack(msg, ReturnCode.SUCCESS);
                log.trace("[{}][{}][{}] onDeviceConnectOk: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
            }

            @Override
            public void onFailure(Throwable t) {
                logDeviceCreationError(t, deviceName);
            }
        }, context.getExecutor());
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `deviceType`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<T> onDeviceConnect(String deviceName, String deviceType) {
        T result = devices.get(deviceName);
        if (result == null) {
            Lock deviceCreationLock = deviceCreationLockMap.computeIfAbsent(deviceName, s -> new ReentrantLock());
            deviceCreationLock.lock();
            try {
                result = devices.get(deviceName);
                if (result == null) {
                    return getDeviceCreationFuture(deviceName, deviceType);
                } else {
                    return Futures.immediateFuture(result);
                }
            } finally {
                deviceCreationLock.unlock();
            }
        } else {
            return Futures.immediateFuture(result);
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `deviceType`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<T> getDeviceCreationFuture(String deviceName, String deviceType) {
        final SettableFuture<T> futureToSet = SettableFuture.create();
        ListenableFuture<T> future = deviceFutures.putIfAbsent(deviceName, futureToSet);
        if (future != null) {
            return future;
        }
        try {
            transportService.process(gateway.getTenantId(),
                    GetOrCreateDeviceFromGatewayRequestMsg.newBuilder()
                            .setDeviceName(deviceName)
                            .setDeviceType(deviceType)
                            .setGatewayIdMSB(gateway.getDeviceId().getId().getMostSignificantBits())
                            .setGatewayIdLSB(gateway.getDeviceId().getId().getLeastSignificantBits())
                            .build(),
                    new TransportServiceCallback<>() {
                        @Override
                        public void onSuccess(GetOrCreateDeviceFromGatewayResponse msg) {
                            T deviceSessionCtx = newDeviceSessionCtx(msg);
                            if (devices.putIfAbsent(deviceName, deviceSessionCtx) == null) {
                                log.trace("[{}][{}][{}] First got or created device [{}], type [{}] for the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, deviceType);
                                SessionInfoProto deviceSessionInfo = deviceSessionCtx.getSessionInfo();
                                transportService.registerAsyncSession(deviceSessionInfo, deviceSessionCtx);
                                transportService.process(TransportProtos.TransportToDeviceActorMsg.newBuilder()
                                        .setSessionInfo(deviceSessionInfo)
                                        .setSessionEvent(SESSION_EVENT_MSG_OPEN)
                                        .setSubscribeToAttributes(SUBSCRIBE_TO_ATTRIBUTE_UPDATES_ASYNC_MSG)
                                        .setSubscribeToRPC(SUBSCRIBE_TO_RPC_ASYNC_MSG)
                                        .build(), null);
                            }
                            futureToSet.set(devices.get(deviceName));
                            deviceFutures.remove(deviceName);
                        }

                        @Override
                        public void onError(Throwable t) {
                            logDeviceCreationError(t, deviceName);
                            futureToSet.setException(t);
                            deviceFutures.remove(deviceName);
                        }
                    });
            return futureToSet;
        } catch (Throwable e) {
            deviceFutures.remove(deviceName);
            throw e;
        }
    }

    /**
     * 功能：执行 `logDeviceCreationError` 对应的处理。
     * 参数：
     * - `t`：`t` 参数。
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    private void logDeviceCreationError(Throwable t, String deviceName) {
        if (DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED.equals(t.getMessage())) {
            log.info("[{}][{}][{}] Failed to process device connect command: [{}] due to [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName,
                    DataConstants.MAXIMUM_NUMBER_OF_DEVICES_REACHED);
        } else {
            log.warn("[{}][{}][{}] Failed to process device connect command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
        }
    }

    /**
     * 功能：执行 `newDeviceSessionCtx` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract T newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg);

    /**
     * 功能：获取消息。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：数值结果。
     */
    protected int getMsgId(MqttPublishMessage mqttMsg) {
        return mqttMsg.variableHeader().packetId();
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    protected void onDeviceConnectJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        JsonElement json = getJson(mqttMsg);
        String deviceName = checkDeviceName(getDeviceName(json));
        String deviceType = getDeviceType(json);
        processOnConnect(mqttMsg, deviceName, deviceType);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    protected void onDeviceConnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.ConnectMsg connectProto = TransportApiProtos.ConnectMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(connectProto.getDeviceName());
            String deviceType = StringUtils.isEmpty(connectProto.getDeviceType()) ? DEFAULT_DEVICE_TYPE : connectProto.getDeviceType();
            processOnConnect(mqttMsg, deviceName, deviceType);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onDeviceDisconnectJson(MqttPublishMessage msg) throws AdaptorException {
        String deviceName = checkDeviceName(getDeviceName(getJson(msg)));
        processOnDisconnect(msg, deviceName);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    protected void onGatewayDeviceDisconnectProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.DisconnectMsg connectProto = TransportApiProtos.DisconnectMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(connectProto.getDeviceName());
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理`On Disconnect`。
     * 参数：
     * - `msg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    void processOnDisconnect(MqttPublishMessage msg, String deviceName) {
        deregisterSession(deviceName);
        ack(msg, ReturnCode.SUCCESS);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    protected void onDeviceTelemetryJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable T deviceCtx) {
                                if (!deviceEntry.getValue().isJsonArray()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                try {
                                    TransportProtos.PostTelemetryMsg postTelemetryMsg = JsonConverter.convertToTelemetryProto(deviceEntry.getValue().getAsJsonArray());
                                    processPostTelemetryMsg(deviceCtx, postTelemetryMsg, deviceName, msgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}][{}] Failed to convert telemetry: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, deviceEntry.getValue(), e);
                                    channel.close();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}][{}][{}] Failed to process device telemetry command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    protected void onDeviceTelemetryProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayTelemetryMsg telemetryMsgProto = TransportApiProtos.GatewayTelemetryMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.TelemetryMsg> deviceMsgList = telemetryMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(deviceMsgList)) {
                deviceMsgList.forEach(telemetryMsg -> {
                    String deviceName = checkDeviceName(telemetryMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable T deviceCtx) {
                                    TransportProtos.PostTelemetryMsg msg = telemetryMsg.getMsg();
                                    try {
                                        TransportProtos.PostTelemetryMsg postTelemetryMsg = ProtoConverter.validatePostTelemetryMsg(msg.toByteArray());
                                        processPostTelemetryMsg(deviceCtx, postTelemetryMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}][{}] Failed to convert telemetry: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, msg, e);
                                        channel.close();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}][{}][{}] Failed to process device telemetry command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}][{}][{}] Devices telemetry messages is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices telemetry messages is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `deviceCtx`：设备信息或设备标识。
     * - `postTelemetryMsg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    public void processPostTelemetryMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.PostTelemetryMsg postTelemetryMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), postTelemetryMsg, getPubAckCallback(channel, deviceName, msgId, postTelemetryMsg));
    }

    /**
     * 功能：执行 `postTelemetryMsgCreated` 对应的处理。
     * 参数：
     * - `keyValueProto`：键。
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    public TransportProtos.PostTelemetryMsg postTelemetryMsgCreated(TransportProtos.KeyValueProto keyValueProto, long ts) {
        List<TransportProtos.KeyValueProto> result = new ArrayList<>();
        result.add(keyValueProto);
        TransportProtos.PostTelemetryMsg.Builder request = TransportProtos.PostTelemetryMsg.newBuilder();
        TransportProtos.TsKvListProto.Builder builder = TransportProtos.TsKvListProto.newBuilder();
        builder.setTs(ts);
        builder.addAllKv(result);
        request.addTsKvList(builder.build());
        return request.build();
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceClaimJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable T deviceCtx) {
                                if (!deviceEntry.getValue().isJsonObject()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                try {
                                    DeviceId deviceId = deviceCtx.getDeviceId();
                                    TransportProtos.ClaimDeviceMsg claimDeviceMsg = JsonConverter.convertToClaimDeviceProto(deviceId, deviceEntry.getValue());
                                    processClaimDeviceMsg(deviceCtx, claimDeviceMsg, deviceName, msgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}][{}] Failed to convert claim message: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, deviceEntry.getValue(), e);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}][{}][{}] Failed to process device claiming command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceClaimProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayClaimMsg claimMsgProto = TransportApiProtos.GatewayClaimMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.ClaimDeviceMsg> claimMsgList = claimMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(claimMsgList)) {
                claimMsgList.forEach(claimDeviceMsg -> {
                    String deviceName = checkDeviceName(claimDeviceMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable T deviceCtx) {
                                    TransportApiProtos.ClaimDevice claimRequest = claimDeviceMsg.getClaimRequest();
                                    if (claimRequest == null) {
                                        throw new IllegalArgumentException("Claim request for device: " + deviceName + " is null!");
                                    }
                                    try {
                                        DeviceId deviceId = deviceCtx.getDeviceId();
                                        TransportProtos.ClaimDeviceMsg claimDeviceMsg = ProtoConverter.convertToClaimDeviceProto(deviceId, claimRequest.toByteArray());
                                        processClaimDeviceMsg(deviceCtx, claimDeviceMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}][{}] Failed to convert claim message: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, claimRequest, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}][{}][{}] Failed to process device claiming command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}][{}][{}] Devices claim messages is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices claim messages is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceCtx`：设备信息或设备标识。
     * - `claimDeviceMsg`：设备信息或设备标识。
     * - `deviceName`：设备信息或设备标识。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    private void processClaimDeviceMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.ClaimDeviceMsg claimDeviceMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), claimDeviceMsg, getPubAckCallback(channel, deviceName, msgId, claimDeviceMsg));
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceAttributesJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            for (Map.Entry<String, JsonElement> deviceEntry : jsonObj.entrySet()) {
                String deviceName = deviceEntry.getKey();
                Futures.addCallback(checkDeviceConnected(deviceName),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable T deviceCtx) {
                                if (!deviceEntry.getValue().isJsonObject()) {
                                    throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
                                }
                                TransportProtos.PostAttributeMsg postAttributeMsg = JsonConverter.convertToAttributesProto(deviceEntry.getValue().getAsJsonObject());
                                processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}][{}][{}] Failed to process device attributes command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            }
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceAttributesProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayAttributesMsg attributesMsgProto = TransportApiProtos.GatewayAttributesMsg.parseFrom(getBytes(payload));
            List<TransportApiProtos.AttributesMsg> attributesMsgList = attributesMsgProto.getMsgList();
            if (!CollectionUtils.isEmpty(attributesMsgList)) {
                attributesMsgList.forEach(attributesMsg -> {
                    String deviceName = checkDeviceName(attributesMsg.getDeviceName());
                    Futures.addCallback(checkDeviceConnected(deviceName),
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable T deviceCtx) {
                                    TransportProtos.PostAttributeMsg kvListProto = attributesMsg.getMsg();
                                    if (kvListProto == null) {
                                        throw new IllegalArgumentException("Attributes List for device: " + deviceName + " is empty!");
                                    }
                                    try {
                                        TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto);
                                        processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}][{}] Failed to process device attributes command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), deviceName, kvListProto, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}][{}][{}] Failed to process device attributes command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}][{}][{}] Devices attributes keys list is empty", gateway.getTenantId(), gateway.getDeviceId(), sessionId);
                throw new IllegalArgumentException("[" + sessionId + "] Devices attributes keys list is empty for [" + gateway.getDeviceId() + "]");
            }
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `deviceCtx`：设备信息或设备标识。
     * - `postAttributeMsg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    protected void processPostAttributesMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.PostAttributeMsg postAttributeMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), postAttributeMsg, getPubAckCallback(channel, deviceName, msgId, postAttributeMsg));
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onDeviceAttributesRequestJson(MqttPublishMessage msg) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, msg.payload());
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            int requestId = jsonObj.get("id").getAsInt();
            String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
            boolean clientScope = jsonObj.get("client").getAsBoolean();
            Set<String> keys;
            if (jsonObj.has("key")) {
                keys = Collections.singleton(jsonObj.get("key").getAsString());
            } else {
                JsonArray keysArray = jsonObj.get("keys").getAsJsonArray();
                keys = new HashSet<>();
                for (JsonElement keyObj : keysArray) {
                    keys.add(keyObj.getAsString());
                }
            }
            TransportProtos.GetAttributeRequestMsg requestMsg = toGetAttributeRequestMsg(requestId, clientScope, keys);
            processGetAttributeRequestMessage(msg, deviceName, requestMsg);
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    private void onDeviceAttributesRequestProto(MqttPublishMessage mqttMsg) throws AdaptorException {
        try {
            TransportApiProtos.GatewayAttributesRequestMsg gatewayAttributesRequestMsg = TransportApiProtos.GatewayAttributesRequestMsg.parseFrom(getBytes(mqttMsg.payload()));
            String deviceName = checkDeviceName(gatewayAttributesRequestMsg.getDeviceName());
            int requestId = gatewayAttributesRequestMsg.getId();
            boolean clientScope = gatewayAttributesRequestMsg.getClient();
            ProtocolStringList keysList = gatewayAttributesRequestMsg.getKeysList();
            Set<String> keys = new HashSet<>(keysList);
            TransportProtos.GetAttributeRequestMsg requestMsg = toGetAttributeRequestMsg(requestId, clientScope, keys);
            processGetAttributeRequestMessage(mqttMsg, deviceName, requestMsg);
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceRpcResponseJson(int msgId, ByteBuf payload) throws AdaptorException {
        JsonElement json = JsonMqttAdaptor.validateJsonPayload(sessionId, payload);
        if (json.isJsonObject()) {
            JsonObject jsonObj = json.getAsJsonObject();
            String deviceName = jsonObj.get(DEVICE_PROPERTY).getAsString();
            Futures.addCallback(checkDeviceConnected(deviceName),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable T deviceCtx) {
                            Integer requestId = jsonObj.get("id").getAsInt();
                            String data = jsonObj.get("data").toString();
                            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                                    .setRequestId(requestId).setPayload(data).build();
                            processRpcResponseMsg(deviceCtx, rpcResponseMsg, deviceName, msgId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.debug("[{}][{}][{}] Failed to process device Rpc response command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                        }
                    }, context.getExecutor());
        } else {
            throw new JsonSyntaxException(CAN_T_PARSE_VALUE + json);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `msgId`：消息ID。
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    private void onDeviceRpcResponseProto(int msgId, ByteBuf payload) throws AdaptorException {
        try {
            TransportApiProtos.GatewayRpcResponseMsg gatewayRpcResponseMsg = TransportApiProtos.GatewayRpcResponseMsg.parseFrom(getBytes(payload));
            String deviceName = checkDeviceName(gatewayRpcResponseMsg.getDeviceName());
            Futures.addCallback(checkDeviceConnected(deviceName),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable T deviceCtx) {
                            Integer requestId = gatewayRpcResponseMsg.getId();
                            String data = gatewayRpcResponseMsg.getData();
                            TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                                    .setRequestId(requestId).setPayload(data).build();
                            processRpcResponseMsg(deviceCtx, rpcResponseMsg, deviceName, msgId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            log.debug("[{}][{}][{}] Failed to process device Rpc response command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                        }
                    }, context.getExecutor());
        } catch (RuntimeException | InvalidProtocolBufferException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理RPC。
     * 参数：
     * - `deviceCtx`：设备信息或设备标识。
     * - `rpcResponseMsg`：响应对象。
     * - `deviceName`：设备信息或设备标识。
     * - `msgId`：消息ID。
     * 返回：无。
     */
    private void processRpcResponseMsg(MqttDeviceAwareSessionContext deviceCtx, TransportProtos.ToDeviceRpcResponseMsg rpcResponseMsg, String deviceName, int msgId) {
        transportService.process(deviceCtx.getSessionInfo(), rpcResponseMsg, getPubAckCallback(channel, deviceName, msgId, rpcResponseMsg));
    }

    /**
     * 功能：处理属性。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * - `requestMsg`：请求对象。
     * 返回：无。
     */
    private void processGetAttributeRequestMessage(MqttPublishMessage mqttMsg, String deviceName, TransportProtos.GetAttributeRequestMsg requestMsg) {
        int msgId = getMsgId(mqttMsg);
        Futures.addCallback(checkDeviceConnected(deviceName),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable T deviceCtx) {
                        transportService.process(deviceCtx.getSessionInfo(), requestMsg, getPubAckCallback(channel, deviceName, msgId, requestMsg));
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        ack(mqttMsg, ReturnCode.IMPLEMENTATION_SPECIFIC);
                        log.debug("[{}][{}][{}] Failed to process device attributes request command: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, t);
                    }
                }, context.getExecutor());
    }

    /**
     * 功能：执行 `toGetAttributeRequestMsg` 对应的处理。
     * 参数：
     * - `requestId`：请求ID。
     * - `clientScope`：客户端对象。
     * - `keys`：键。
     * 返回：处理结果。
     */
    private TransportProtos.GetAttributeRequestMsg toGetAttributeRequestMsg(int requestId, boolean clientScope, Set<String> keys) {
        TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
        result.setRequestId(requestId);

        if (clientScope) {
            result.addAllClientAttributeNames(keys);
        } else {
            result.addAllSharedAttributeNames(keys);
        }
        return result.build();
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：判断结果。
     */
    protected ListenableFuture<T> checkDeviceConnected(String deviceName) {
        T ctx = devices.get(deviceName);
        if (ctx == null) {
            log.debug("[{}][{}][{}] Missing device [{}] for the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
            return onDeviceConnect(deviceName, DEFAULT_DEVICE_TYPE);
        } else {
            return Futures.immediateFuture(ctx);
        }
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：判断结果。
     */
    protected String checkDeviceName(String deviceName) {
        if (StringUtils.isEmpty(deviceName)) {
            throw new RuntimeException("Device name is empty!");
        } else {
            return deviceName;
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：文本结果。
     */
    private String getDeviceName(JsonElement json) {
        return json.getAsJsonObject().get(DEVICE_PROPERTY).getAsString();
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `json`：`json` 参数。
     * 返回：文本结果。
     */
    private String getDeviceType(JsonElement json) {
        JsonElement type = json.getAsJsonObject().get("type");
        return type == null || type instanceof JsonNull ? DEFAULT_DEVICE_TYPE : type.getAsString();
    }

    /**
     * 功能：获取JSON。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：处理结果。
     */
    private JsonElement getJson(MqttPublishMessage mqttMsg) throws AdaptorException {
        return JsonMqttAdaptor.validateJsonPayload(sessionId, mqttMsg.payload());
    }

    /**
     * 功能：获取`Bytes`。
     * 参数：
     * - `payload`：`payload` 参数。
     * 返回：处理结果。
     */
    protected byte[] getBytes(ByteBuf payload) {
        return ProtoMqttAdaptor.toBytes(payload);
    }

    /**
     * 功能：执行 `ack` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `returnCode`：`returnCode` 参数。
     * 返回：无。
     */
    protected void ack(MqttPublishMessage msg, ReturnCode returnCode) {
        int msgId = getMsgId(msg);
        if (msgId > 0) {
            writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(deviceSessionCtx, msgId, returnCode));
        }
    }

    /**
     * 功能：执行 `deregisterSession` 对应的处理。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `deviceSessionCtx`：设备信息或设备标识。
     * 返回：无。
     */
    private void deregisterSession(String deviceName, MqttDeviceAwareSessionContext deviceSessionCtx) {
        if (this.deviceSessionCtx.isSparkplug()) {
            sendSparkplugStateOnTelemetry(deviceSessionCtx.getSessionInfo(),
                    deviceSessionCtx.getDeviceInfo().getDeviceName(), OFFLINE, new Date().getTime());
        }
        transportService.deregisterSession(deviceSessionCtx.getSessionInfo());
        transportService.process(deviceSessionCtx.getSessionInfo(), SESSION_EVENT_MSG_CLOSED, null);
        log.debug("[{}][{}][{}] Removed device [{}] from the gateway session", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName);
    }

    /**
     * 功能：发送或提交遥测。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `deviceName`：设备信息或设备标识。
     * - `connectionState`：`connectionState` 参数。
     * - `ts`：时间戳。
     * 返回：无。
     */
    public void sendSparkplugStateOnTelemetry(TransportProtos.SessionInfoProto sessionInfo, String deviceName, SparkplugConnectionState connectionState, long ts) {
        TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
        keyValueProtoBuilder.setKey(messageName(STATE));
        keyValueProtoBuilder.setType(TransportProtos.KeyValueType.STRING_V);
        keyValueProtoBuilder.setStringV(connectionState.name());
        TransportProtos.PostTelemetryMsg postTelemetryMsg = postTelemetryMsgCreated(keyValueProtoBuilder.build(), ts);
        transportService.process(sessionInfo, postTelemetryMsg, getPubAckCallback(channel, deviceName, -1, postTelemetryMsg));
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `ctx`：处理上下文。
     * - `deviceName`：设备信息或设备标识。
     * - `msgId`：消息ID。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private <T> TransportServiceCallback<Void> getPubAckCallback(final ChannelHandlerContext ctx, final String deviceName, final int msgId, final T msg) {
        return new TransportServiceCallback<Void>() {
            @Override
            public void onSuccess(Void dummy) {
                log.trace("[{}][{}][{}][{}] Published msg: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, deviceName, msg);
                if (msgId > 0) {
                    ctx.writeAndFlush(MqttTransportHandler.createMqttPubAckMsg(deviceSessionCtx, msgId, ReturnCode.SUCCESS));
                }
            }

            @Override
            public void onError(Throwable e) {
                log.trace("[{}][{}][{}] Failed to publish msg: [{}] for device: [{}]", gateway.getTenantId(), gateway.getDeviceId(), sessionId, msg, deviceName, e);
                ctx.close();
            }
        };
    }

}
