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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.mqtt.SparkplugBProto;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;

/**
 * 中文说明：
 * 1. `SparkplugDeviceSessionContext` 是 ThingsBoard Common Transport 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractGatewayDeviceSessionContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    private final Map<String, SparkplugBProto.Payload.Metric> deviceBirthMetrics = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `SparkplugDeviceSessionContext` 实例，并初始化必要字段。
     * 参数：
     * - `parent`：`parent` 参数。
     * - `deviceInfo`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * - `mqttQoSMap`：键值映射。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                                 Integer> mqttQoSMap,
                                         TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    public  Map<String, SparkplugBProto.Payload.Metric> getDeviceBirthMetrics() {
        return deviceBirthMetrics;
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `metrics`：数据列表。
     * 返回：无。
     */
    public void setDeviceBirthMetrics(java.util.List<org.thingsboard.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.deviceBirthMetrics.putAll(metrics.stream()
                .collect(Collectors.toMap(SparkplugBProto.Payload.Metric::getName, metric -> metric)));
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
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProto -> {
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            }
        });
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
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
            SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
            header.setMessageType(messageType.name());
            TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        messageType, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(payload -> parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo));
            } else {
                parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                        ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                                rpcRequest.getMethodName() + ". This device does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
            }
        } catch (ThingsboardException e) {
            parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                    ThingsboardErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                            rpcRequest.getMethodName() + ". " + e.getMessage());
        }
    }

}
