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

import io.netty.handler.codec.mqtt.MqttQoS;
import org.thingsboard.server.common.transport.session.DeviceAwareSessionContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by ashvayka on 30.08.18.
 */
/**
 * 中文说明：
 * 1. `MqttDeviceAwareSessionContext` 是 ThingsBoard Common Transport 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DeviceAwareSessionContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public abstract class MqttDeviceAwareSessionContext extends DeviceAwareSessionContext {

    /**
     * QoS 等级映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap;

    /**
     * 功能：创建 `MqttDeviceAwareSessionContext` 实例，并初始化必要字段。
     * 参数：
     * - `sessionId`：会话ID。
     * - `mqttQoSMap`：键值映射。
     * 返回：新创建的对象实例。
     */
    public MqttDeviceAwareSessionContext(UUID sessionId, ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap) {
        super(sessionId);
        this.mqttQoSMap = mqttQoSMap;
    }

    /**
     * 功能：获取QoS 等级。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<MqttTopicMatcher, Integer> getMqttQoSMap() {
        return mqttQoSMap;
    }

    /**
     * 功能：获取QoS 等级。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    public MqttQoS getQoSForTopic(String topic) {
        List<Integer> qosList = mqttQoSMap.entrySet()
                .stream()
                .filter(entry -> entry.getKey().matches(topic))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        if (!qosList.isEmpty()) {
            return MqttQoS.valueOf(qosList.get(0));
        } else {
            return MqttQoS.AT_LEAST_ONCE;
        }
    }
}
