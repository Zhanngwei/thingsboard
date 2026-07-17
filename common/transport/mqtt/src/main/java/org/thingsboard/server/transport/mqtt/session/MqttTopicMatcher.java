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

import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `MqttTopicMatcher` 是 ThingsBoard Common Transport 中负责 MQTT 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class MqttTopicMatcher {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String topic;
    private final Pattern topicRegex;

    /**
     * 功能：创建 `MqttTopicMatcher` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：新创建的对象实例。
     */
    public MqttTopicMatcher(String topic) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        this.topic = topic;
        this.topicRegex = Pattern.compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getTopic() {
        return topic;
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：判断结果。
     */
    public boolean matches(String topic){
        return this.topicRegex.matcher(topic).matches();
    }

    /**
     * 功能：比较当前对象与传入对象是否等价。
     * 参数：
     * - `o`：`o` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MqttTopicMatcher that = (MqttTopicMatcher) o;

        return topic.equals(that.topic);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        return topic.hashCode();
    }
}
