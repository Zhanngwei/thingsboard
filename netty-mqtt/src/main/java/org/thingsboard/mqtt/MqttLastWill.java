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
package org.thingsboard.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * 中文说明：
 * 1. `MqttLastWill` 是 ThingsBoard Netty MQTT Client 中围绕 MQTT 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@SuppressWarnings({"WeakerAccess", "unused", "SimplifiableIfStatement", "StringBufferReplaceableByString"})
public final class MqttLastWill {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String topic;
    private final String message;
    /**
     * 是否满足`retain`条件。
     */
    private final boolean retain;
    private final MqttQoS qos;

    /**
     * 功能：创建 `MqttLastWill` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `message`：待处理消息。
     * - `retain`：`retain` 参数。
     * - `qos`：`qos` 参数。
     * 返回：新创建的对象实例。
     */
    public MqttLastWill(String topic, String message, boolean retain, MqttQoS qos) {
        if(topic == null){
            throw new NullPointerException("topic");
        }
        if(message == null){
            throw new NullPointerException("message");
        }
        if(qos == null){
            throw new NullPointerException("qos");
        }
        this.topic = topic;
        this.message = message;
        this.retain = retain;
        this.qos = qos;
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
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getMessage() {
        return message;
    }

    /**
     * 功能：判断`Retain`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isRetain() {
        return retain;
    }

    /**
     * 功能：获取QoS 等级。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttQoS getQos() {
        return qos;
    }

    /**
     * 功能：执行 `builder` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static MqttLastWill.Builder builder(){
        return new MqttLastWill.Builder();
    }

    /**
     * 中文说明：
     * 1. `Builder` 是 ThingsBoard Netty MQTT Client 中创建或提供 `Builder` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 它直接协作于目标接口、具体实现和创建所需配置。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
     */
    public static final class Builder {

        /**
         * 主题，用于匹配或发送对应主题的数据。
         */
        private String topic;
        private String message;
        /**
         * 是否满足`retain`条件。
         */
        private boolean retain;
        private MqttQoS qos;

        /**
         * 功能：获取主题。
         * 参数：无。
         * 返回：文本结果。
         */
        public String getTopic() {
            return topic;
        }

        /**
         * 功能：更新主题。
         * 参数：
         * - `topic`：主题名称或主题对象。
         * 返回：处理结果。
         */
        public Builder setTopic(String topic) {
            if(topic == null){
                throw new NullPointerException("topic");
            }
            this.topic = topic;
            return this;
        }

        /**
         * 功能：获取消息。
         * 参数：无。
         * 返回：文本结果。
         */
        public String getMessage() {
            return message;
        }

        /**
         * 功能：更新消息。
         * 参数：
         * - `message`：待处理消息。
         * 返回：处理结果。
         */
        public Builder setMessage(String message) {
            if(message == null){
                throw new NullPointerException("message");
            }
            this.message = message;
            return this;
        }

        /**
         * 功能：判断`Retain`。
         * 参数：无。
         * 返回：判断结果。
         */
        public boolean isRetain() {
            return retain;
        }

        /**
         * 功能：更新`Retain`。
         * 参数：
         * - `retain`：`retain` 参数。
         * 返回：处理结果。
         */
        public Builder setRetain(boolean retain) {
            this.retain = retain;
            return this;
        }

        /**
         * 功能：获取QoS 等级。
         * 参数：无。
         * 返回：处理结果。
         */
        public MqttQoS getQos() {
            return qos;
        }

        /**
         * 功能：更新QoS 等级。
         * 参数：
         * - `qos`：`qos` 参数。
         * 返回：处理结果。
         */
        public Builder setQos(MqttQoS qos) {
            if(qos == null){
                throw new NullPointerException("qos");
            }
            this.qos = qos;
            return this;
        }

        /**
         * 功能：执行 `build` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        public MqttLastWill build(){
            return new MqttLastWill(topic, message, retain, qos);
        }
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

        MqttLastWill that = (MqttLastWill) o;

        if (retain != that.retain) return false;
        if (!topic.equals(that.topic)) return false;
        if (!message.equals(that.message)) return false;
        return qos == that.qos;

    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + message.hashCode();
        result = 31 * result + (retain ? 1 : 0);
        result = 31 * result + qos.hashCode();
        return result;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MqttLastWill{");
        sb.append("topic='").append(topic).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", retain=").append(retain);
        sb.append(", qos=").append(qos.name());
        sb.append('}');
        return sb.toString();
    }
}
