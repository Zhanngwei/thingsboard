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
package org.thingsboard.server.transport.mqtt.util;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;

/**
 * 中文说明：
 * 1. `ReturnCodeResolver` 是 ThingsBoard Common Transport 中处理 `Return Code Resolver` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class ReturnCodeResolver {

    /**
     * 功能：获取编码。
     * 参数：
     * - `mqttVersion`：`mqttVersion` 参数。
     * - `returnCode`：`returnCode` 参数。
     * 返回：处理结果。
     */
    public static MqttConnectReturnCode getConnectionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            switch (returnCode) {
                case BAD_USERNAME_OR_PASSWORD:
                case NOT_AUTHORIZED_5:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
                case SERVER_UNAVAILABLE_5:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;
                case CLIENT_IDENTIFIER_NOT_VALID:
                    return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
                default:
                    log.warn("Unknown return code for conversion: {}", returnCode.name());
            }
        }
        return MqttConnectReturnCode.valueOf(returnCode.byteValue());
    }

    /**
     * 功能：获取订阅。
     * 参数：
     * - `mqttVersion`：`mqttVersion` 参数。
     * - `returnCode`：`returnCode` 参数。
     * 返回：数值结果。
     */
    public static int getSubscriptionReturnCode(MqttVersion mqttVersion, ReturnCode returnCode) {
        if (!MqttVersion.MQTT_5.equals(mqttVersion) && !ReturnCode.SUCCESS.equals(returnCode)) {
            switch (returnCode) {
                case UNSPECIFIED_ERROR:
                case TOPIC_FILTER_INVALID:
                case IMPLEMENTATION_SPECIFIC:
                case NOT_AUTHORIZED_5:
                case PACKET_IDENTIFIER_IN_USE:
                case QUOTA_EXCEEDED:
                case SHARED_SUBSCRIPTION_NOT_SUPPORTED:
                case SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED:
                case WILDCARD_SUBSCRIPTION_NOT_SUPPORTED:
                    return MqttQoS.FAILURE.value();
            }
        }
        return returnCode.byteValue();
    }
}
