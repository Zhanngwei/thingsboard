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
package org.thingsboard.server.transport.coap.efento.adaptor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.efento.CoapEfentoTransportResource;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EfentoCoapAdaptor` 是 ThingsBoard Common Transport 中负责 CoAP 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Component
@Slf4j
public class EfentoCoapAdaptor {

    private static final Gson gson = new Gson();

    /**
     * 功能：转换遥测。
     * 参数：
     * - `sessionId`：会话ID。
     * - `telemetryList`：数据列表。
     * 返回：处理结果。
     */
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(UUID sessionId, List<CoapEfentoTransportResource.EfentoTelemetry> telemetryList) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(gson.toJsonTree(telemetryList));
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert EfentoMeasurements to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `sessionId`：会话ID。
     * - `deviceInfo`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public TransportProtos.PostAttributeMsg convertToPostAttributes(UUID sessionId, JsonElement deviceInfo) throws AdaptorException {
        try {
            return JsonConverter.convertToAttributesProto(deviceInfo);
        } catch (Exception ex) {
            log.warn("[{}] Failed to convert JsonObject to PostTelemetry request!", sessionId);
            throw new AdaptorException(ex);
        }
    }


}
