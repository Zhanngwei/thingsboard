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

import lombok.Getter;
import org.thingsboard.server.common.data.device.profile.MqttTopics;

/**
 * 中文说明：
 * 1. `TopicType` 是 ThingsBoard Common Transport 中定义 `Topic Type` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum TopicType {

    V1(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_TOPIC),
    V2(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_TOPIC),
    V2_JSON(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_JSON_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_JSON_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_JSON_TOPIC),
    V2_PROTO(MqttTopics.DEVICE_ATTRIBUTES_RESPONSE_SHORT_PROTO_TOPIC_PREFIX, MqttTopics.DEVICE_ATTRIBUTES_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_RPC_REQUESTS_SHORT_PROTO_TOPIC, MqttTopics.DEVICE_RPC_RESPONSE_SHORT_PROTO_TOPIC);

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Getter
    private final String attributesResponseTopicBase;

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    @Getter
    private final String attributesSubTopic;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    private final String rpcRequestTopicBase;

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Getter
    private final String rpcResponseTopicBase;

    TopicType(String attributesRequestTopicBase, String attributesSubTopic, String rpcRequestTopicBase, String rpcResponseTopicBase) {
        this.attributesResponseTopicBase = attributesRequestTopicBase;
        this.attributesSubTopic = attributesSubTopic;
        this.rpcRequestTopicBase = rpcRequestTopicBase;
        this.rpcResponseTopicBase = rpcResponseTopicBase;
    }
}
