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
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Descriptors;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.adaptor.ProtoConverter;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.MqttTransportHandler;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugConnectionState.ONLINE;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.DBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType.NBIRTH;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.createMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.fromSparkplugBMetricToKeyValueProto;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.validatedValueByTypeMetric;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopicUtil.parseTopicSubscribe;

/**
 * Created by nickAS21 on 12.12.22
 */
/**
 * 中文说明：
 * 1. `SparkplugNodeSessionHandler` 是 ThingsBoard Common Transport 中处理会话的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractGatewaySessionHandler`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class SparkplugNodeSessionHandler extends AbstractGatewaySessionHandler<SparkplugDeviceSessionContext> {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final SparkplugTopic sparkplugTopicNode;
    private final Map<String, SparkplugBProto.Payload.Metric> nodeBirthMetrics;
    /**
     * `parent` 字段，保存当前对象的对应属性。
     */
    private final MqttTransportHandler parent;

    /**
     * 功能：创建 `SparkplugNodeSessionHandler` 实例，并初始化必要字段。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `deviceSessionCtx`：设备信息或设备标识。
     * - `sessionId`：会话ID。
     * - `sparkplugTopicNode`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public SparkplugNodeSessionHandler(MqttTransportHandler parent, DeviceSessionCtx deviceSessionCtx, UUID sessionId,
                                       SparkplugTopic sparkplugTopicNode) {
        super(deviceSessionCtx, sessionId);
        this.parent = parent;
        this.sparkplugTopicNode = sparkplugTopicNode;
        this.nodeBirthMetrics = new ConcurrentHashMap<>();
    }

    /**
     * 功能：更新节点实例。
     * 参数：
     * - `metrics`：数据列表。
     * 返回：无。
     */
    public void setNodeBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.nodeBirthMetrics.putAll(metrics.stream()
                .collect(Collectors.toMap(SparkplugBProto.Payload.Metric::getName, metric -> metric)));
    }

    /**
     * 功能：获取节点实例。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map<String, SparkplugBProto.Payload.Metric> getNodeBirthMetrics() {
        return this.nodeBirthMetrics;
    }

    /**
     * 功能：转换遥测。
     * 参数：
     * - `ctx`：处理上下文。
     * - `inbound`：`inbound` 参数。
     * 返回：处理结果。
     */
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(MqttDeviceAwareSessionContext ctx, MqttPublishMessage inbound) throws AdaptorException {
        DeviceSessionCtx deviceSessionCtx = (DeviceSessionCtx) ctx;
        byte[] bytes = getBytes(inbound.payload());
        Descriptors.Descriptor telemetryDynamicMsgDescriptor = ProtoConverter.validateDescriptor(deviceSessionCtx.getTelemetryDynamicMsgDescriptor());
        try {
            return JsonConverter.convertToTelemetryProto(new JsonParser().parse(ProtoConverter.dynamicMsgToJson(bytes, telemetryDynamicMsgDescriptor)));
        } catch (Exception e) {
            log.debug("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理遥测。
     * 参数：
     * - `msgId`：消息ID。
     * - `sparkplugBProto`：`sparkplugBProto` 参数。
     * - `topic`：主题名称或主题对象。
     * 返回：无。
     */
    public void onAttributesTelemetryProto(int msgId, SparkplugBProto.Payload sparkplugBProto, SparkplugTopic topic) throws AdaptorException, ThingsboardException {
        String deviceName = topic.getNodeDeviceName();
        checkDeviceName(deviceName);

        ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture;
        if (topic.isNode()) {
            if (topic.isType(NBIRTH)) {
                sendSparkplugStateOnTelemetry(this.deviceSessionCtx.getSessionInfo(), deviceName, ONLINE,
                        sparkplugBProto.getTimestamp());
                setNodeBirthMetrics(sparkplugBProto.getMetricsList());
            }
            contextListenableFuture = Futures.immediateFuture(this.deviceSessionCtx);
        } else {
            ListenableFuture<SparkplugDeviceSessionContext> deviceCtx = onDeviceConnectProto(topic);
            contextListenableFuture = Futures.transform(deviceCtx, ctx -> {
                if (topic.isType(DBIRTH)) {
                    sendSparkplugStateOnTelemetry(ctx.getSessionInfo(), deviceName, ONLINE,
                            sparkplugBProto.getTimestamp());
                    ctx.setDeviceBirthMetrics(sparkplugBProto.getMetricsList());
                }
                return ctx;
            }, MoreExecutors.directExecutor());
        }
        Set<String> attributesMetricNames = ((MqttDeviceProfileTransportConfiguration) deviceSessionCtx
                .getDeviceProfile().getProfileData().getTransportConfiguration()).getSparkplugAttributesMetricNames();
        if (attributesMetricNames != null) {
            List<TransportApiProtos.AttributesMsg> attributesMsgList = convertToPostAttributes(sparkplugBProto, attributesMetricNames, deviceName);
            onDeviceAttributesProto(contextListenableFuture, msgId, attributesMsgList, deviceName);
        }
        List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList = convertToPostTelemetry(sparkplugBProto, attributesMetricNames, topic.getType().name());
        onDeviceTelemetryProto(contextListenableFuture, msgId, postTelemetryMsgList, deviceName);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `contextListenableFuture`：处理上下文。
     * - `msgId`：消息ID。
     * - `postTelemetryMsgList`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceTelemetryProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture,
                                       int msgId, List<TransportProtos.PostTelemetryMsg> postTelemetryMsgList, String deviceName) throws AdaptorException {
        try {
            int finalMsgId = msgId;
            postTelemetryMsgList.forEach(telemetryMsg -> {
                Futures.addCallback(contextListenableFuture,
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                try {
                                    processPostTelemetryMsg(deviceCtx, telemetryMsg, deviceName, finalMsgId);
                                } catch (Throwable e) {
                                    log.warn("[{}][{}] Failed to convert telemetry: {}", gateway.getDeviceId(), deviceName, telemetryMsg, e);
                                    channel.close();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.debug("[{}] Failed to process device telemetry command: {}", sessionId, deviceName, t);
                            }
                        }, context.getExecutor());
            });
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `contextListenableFuture`：处理上下文。
     * - `msgId`：消息ID。
     * - `attributesMsgList`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    private void onDeviceAttributesProto(ListenableFuture<MqttDeviceAwareSessionContext> contextListenableFuture, int msgId,
                                         List<TransportApiProtos.AttributesMsg> attributesMsgList, String deviceName) throws AdaptorException {
        try {
            if (!CollectionUtils.isEmpty(attributesMsgList)) {
                attributesMsgList.forEach(attributesMsg -> {
                    Futures.addCallback(contextListenableFuture,
                            new FutureCallback<>() {
                                @Override
                                public void onSuccess(@Nullable MqttDeviceAwareSessionContext deviceCtx) {
                                    TransportProtos.PostAttributeMsg kvListProto = attributesMsg.getMsg();
                                    try {
                                        TransportProtos.PostAttributeMsg postAttributeMsg = ProtoConverter.validatePostAttributeMsg(kvListProto);
                                        processPostAttributesMsg(deviceCtx, postAttributeMsg, deviceName, msgId);
                                    } catch (Throwable e) {
                                        log.warn("[{}][{}] Failed to process device attributes command: {}", gateway.getDeviceId(), deviceName, kvListProto, e);
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.debug("[{}] Failed to process device attributes command: {}", sessionId, deviceName, t);
                                }
                            }, context.getExecutor());
                });
            } else {
                log.debug("[{}] Devices attributes keys list is empty for: [{}]", sessionId, gateway.getDeviceId());
            }
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `grantedQoSList`：数据列表。
     * - `subscription`：`subscription` 参数。
     * - `reqQoS`：`reqQoS` 参数。
     * 返回：无。
     */
    public void handleSparkplugSubscribeMsg(List<Integer> grantedQoSList, MqttTopicSubscription subscription,
                                            MqttQoS reqQoS) throws ThingsboardException, AdaptorException,
            ExecutionException, InterruptedException {
        SparkplugTopic sparkplugTopic = parseTopicSubscribe(subscription.topicName());
        if (sparkplugTopic.getGroupId() == null) {
            // TODO SUBSCRIBE NameSpace
        } else if (sparkplugTopic.getType() == null) {
            // TODO SUBSCRIBE GroupId
        } else if (sparkplugTopic.isNode()) {
            // SUBSCRIBE Node
            parent.processAttributesRpcSubscribeSparkplugNode(grantedQoSList, reqQoS);
        } else {
            // SUBSCRIBE Device - DO NOTHING, WE HAVE ALREADY SUBSCRIBED.
            // TODO: track that node subscribed to # or to particular device.
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * - `deviceName`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceDisconnect(MqttPublishMessage mqttMsg, String deviceName) throws AdaptorException {
        try {
            processOnDisconnect(mqttMsg, deviceName);
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<SparkplugDeviceSessionContext> onDeviceConnectProto(SparkplugTopic topic) throws ThingsboardException {
        try {
            String deviceType = this.gateway.getDeviceType() + " device";
            return onDeviceConnect(topic.getNodeDeviceName(), deviceType);
        } catch (RuntimeException e) {
            log.error("Failed Sparkplug Device connect proto!", e);
            throw new ThingsboardException(e, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * 功能：转换遥测。
     * 参数：
     * - `sparkplugBProto`：`sparkplugBProto` 参数。
     * - `attributesMetricNames`：名称。
     * - `topicTypeName`：主题名称或主题对象。
     * 返回：匹配的数据集合。
     */
    private List<TransportProtos.PostTelemetryMsg> convertToPostTelemetry(SparkplugBProto.Payload sparkplugBProto, Set<String> attributesMetricNames, String topicTypeName) throws AdaptorException {
        try {
            List<TransportProtos.PostTelemetryMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                if (attributesMetricNames == null || !matches(attributesMetricNames, protoMetric)) {
                    long ts = protoMetric.getTimestamp();
                    String key = "bdSeq".equals(protoMetric.getName()) ?
                            topicTypeName + " " + protoMetric.getName() : protoMetric.getName();
                    Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(key, protoMetric);
                    if (keyValueProtoOpt.isPresent()) {
                        msgs.add(postTelemetryMsgCreated(keyValueProtoOpt.get(), ts));
                    }
                }
            }

            if (DBIRTH.name().equals(topicTypeName)) {
                TransportProtos.KeyValueProto.Builder keyValueProtoBuilder = TransportProtos.KeyValueProto.newBuilder();
                keyValueProtoBuilder.setKey(topicTypeName + " " + "seq");
                keyValueProtoBuilder.setType(TransportProtos.KeyValueType.LONG_V);
                keyValueProtoBuilder.setLongV(sparkplugBProto.getSeq());
                msgs.add(postTelemetryMsgCreated(keyValueProtoBuilder.build(), sparkplugBProto.getTimestamp()));
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `sparkplugBProto`：`sparkplugBProto` 参数。
     * - `attributesMetricNames`：名称。
     * - `deviceName`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private List<TransportApiProtos.AttributesMsg> convertToPostAttributes(SparkplugBProto.Payload sparkplugBProto,
                                                                           Set<String> attributesMetricNames,
                                                                           String deviceName) throws AdaptorException {
        try {
            List<TransportApiProtos.AttributesMsg> msgs = new ArrayList<>();
            for (SparkplugBProto.Payload.Metric protoMetric : sparkplugBProto.getMetricsList()) {
                if (matches(attributesMetricNames, protoMetric)) {
                    TransportApiProtos.AttributesMsg.Builder deviceAttributesMsgBuilder = TransportApiProtos.AttributesMsg.newBuilder();
                    Optional<TransportProtos.PostAttributeMsg> msgOpt = getPostAttributeMsg(protoMetric);
                    if (msgOpt.isPresent()) {
                        deviceAttributesMsgBuilder.setDeviceName(deviceName);
                        deviceAttributesMsgBuilder.setMsg(msgOpt.get());
                        msgs.add(deviceAttributesMsgBuilder.build());
                    }
                }
            }
            return msgs;
        } catch (IllegalStateException | JsonSyntaxException | ThingsboardException e) {
            log.error("Failed to decode post telemetry request", e);
            throw new AdaptorException(e);
        }
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `attributesMetricNames`：名称。
     * - `protoMetric`：`protoMetric` 参数。
     * 返回：判断结果。
     */
    private boolean matches(Set<String> attributesMetricNames, SparkplugBProto.Payload.Metric protoMetric) {
        String metricName = protoMetric.getName();
        for (String attributeMetricFilter : attributesMetricNames) {
            if (metricName.equals(attributeMetricFilter) ||
                    (attributeMetricFilter.endsWith("*") && metricName.startsWith(
                            attributeMetricFilter.substring(0, attributeMetricFilter.length() - 1)))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：获取属性。
     * 参数：
     * - `protoMetric`：`protoMetric` 参数。
     * 返回：可能存在的结果。
     */
    private Optional<TransportProtos.PostAttributeMsg> getPostAttributeMsg(SparkplugBProto.Payload.Metric protoMetric) throws ThingsboardException {
        Optional<TransportProtos.KeyValueProto> keyValueProtoOpt = fromSparkplugBMetricToKeyValueProto(protoMetric.getName(), protoMetric);
        if (keyValueProtoOpt.isPresent()) {
            TransportProtos.PostAttributeMsg.Builder builder = TransportProtos.PostAttributeMsg.newBuilder();
            builder.addKv(keyValueProtoOpt.get());
            builder.setShared(true);
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：处理结果。
     */
    public SparkplugTopic getSparkplugTopicNode() {
        return this.sparkplugTopicNode;
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `tsKvProto`：`tsKvProto` 参数。
     * - `sparkplugTopic`：主题名称或主题对象。
     * - `metricBirth`：`metricBirth` 参数。
     * 返回：可能存在的结果。
     */
    public Optional<MqttPublishMessage> createSparkplugMqttPublishMsg(TransportProtos.TsKvProto tsKvProto,
                                                                      String sparkplugTopic,
                                                                      SparkplugBProto.Payload.Metric metricBirth) {
        try {
            long ts = tsKvProto.getTs();
            MetricDataType metricDataType = MetricDataType.fromInteger(metricBirth.getDatatype());
            Optional value = validatedValueByTypeMetric(tsKvProto.getKv(), metricDataType);
            if (value.isPresent()) {
                SparkplugBProto.Payload.Builder cmdPayload = SparkplugBProto.Payload.newBuilder()
                        .setTimestamp(ts);
                cmdPayload.addMetrics(createMetric(value.get(), ts, tsKvProto.getKv().getKey(), metricDataType));
                byte[] payloadInBytes = cmdPayload.build().toByteArray();
                return Optional.of(getPayloadAdaptor().createMqttPublishMsg(deviceSessionCtx, sparkplugTopic, payloadInBytes));
            } else {
                log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes [{}] response to MQTT sparkplug  msg",
                        deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, tsKvProto.getKv());
            }
        } catch (Exception e) {
            log.trace("DeviceId: [{}] tenantId: [{}] sessionId:[{}] Failed to convert device attributes response to MQTT sparkplug  msg",
                    deviceSessionCtx.getDeviceInfo().getDeviceId(), deviceSessionCtx.getDeviceInfo().getTenantId(), sessionId, e);
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * 功能：执行 `newDeviceSessionCtx` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected SparkplugDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
        return new SparkplugDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `payload`：`payload` 参数。
     * - `rpcRequest`：请求对象。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    protected void sendToDeviceRpcRequest(MqttMessage payload, TransportProtos.ToDeviceRpcRequestMsg rpcRequest, TransportProtos.SessionInfoProto sessionInfo) {
        parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo);
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
    protected void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ThingsboardErrorCode result, String errorMsg) {
        parent.sendErrorRpcResponse(sessionInfo, requestId, result, errorMsg);
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
    protected void sendSuccessRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, ResponseCode result, String successMsg) {
        parent.sendSuccessRpcResponse(sessionInfo, requestId, result, successMsg);
    }


}
