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
package org.thingsboard.server.transport.mqtt.mqttv3;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * 中文说明：
 * 1. `MqttTestSubscribeOnTopicCallback` 是 ThingsBoard Application 中处理 MQTT 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `MqttTestCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class MqttTestSubscribeOnTopicCallback extends MqttTestCallback {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    protected final String awaitSubTopic;

    /**
     * 功能：创建 `MqttTestSubscribeOnTopicCallback` 实例，并初始化必要字段。
     * 参数：
     * - `awaitSubTopic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public MqttTestSubscribeOnTopicCallback(String awaitSubTopic) {
        super();
        this.awaitSubTopic = awaitSubTopic;
    }

    /**
     * 功能：执行 `messageArrived` 对应的处理。
     * 参数：
     * - `requestTopic`：请求对象。
     * - `mqttMessage`：待处理消息。
     * 返回：无。
     */
    @Override
    public void messageArrived(String requestTopic, MqttMessage mqttMessage) {
        log.warn("messageArrived on topic: {}, awaitSubTopic: {}", requestTopic, awaitSubTopic);
        if (awaitSubTopic.equals(requestTopic)) {
            messageArrivedQoS = mqttMessage.getQos();
            payloadBytes = mqttMessage.getPayload();
            subscribeLatch.countDown();
        }
    }

}
