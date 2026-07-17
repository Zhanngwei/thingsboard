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

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;

import java.util.UUID;

/**
 * Created by nickAS21 on 26.12.22
 */
/**
 * 中文说明：
 * 1. `GatewaySessionHandler` 是 ThingsBoard Common Transport 中处理会话的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractGatewaySessionHandler`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class GatewaySessionHandler extends AbstractGatewaySessionHandler {

    /**
     * 功能：创建 `GatewaySessionHandler` 实例，并初始化必要字段。
     * 参数：
     * - `deviceSessionCtx`：设备信息或设备标识。
     * - `sessionId`：会话ID。
     * 返回：新创建的对象实例。
     */
    public GatewaySessionHandler(DeviceSessionCtx deviceSessionCtx, UUID sessionId) {
        super(deviceSessionCtx, sessionId);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceConnect(MqttPublishMessage mqttMsg) throws AdaptorException {
        if (isJsonPayloadType()) {
            onDeviceConnectJson(mqttMsg);
        } else {
            onDeviceConnectProto(mqttMsg);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `mqttMsg`：待处理消息。
     * 返回：无。
     */
    public void onDeviceTelemetry(MqttPublishMessage mqttMsg) throws AdaptorException {
        int msgId = getMsgId(mqttMsg);
        ByteBuf payload = mqttMsg.payload();
        if (isJsonPayloadType()) {
            onDeviceTelemetryJson(msgId, payload);
        } else {
            onDeviceTelemetryProto(msgId, payload);
        }
    }

    /**
     * 功能：执行 `newDeviceSessionCtx` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected GatewayDeviceSessionContext newDeviceSessionCtx(GetOrCreateDeviceFromGatewayResponse msg) {
         return new GatewayDeviceSessionContext(this, msg.getDeviceInfo(), msg.getDeviceProfile(), mqttQoSMap, transportService);
    }

}
