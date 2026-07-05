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
 * 1. 类目的：`MqttLastWill` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 State Machine / Command / Callback。
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
     * 1. 类目的：`Builder` 是 ThingsBoard Netty MQTT 模块 中的Netty MQTT 客户端协议类型，用于封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
     * 2. 所属模块：位于 netty-mqtt 模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
     * 3. 协作对象：主要协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
     * 4. 生命周期：由客户端构造、Netty 通道建立、MQTT 会话保持、断线关闭和测试 broker 生命周期驱动。
     * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
     * 6. 事务与缓存：本模块不直接涉及数据库事务或缓存；它通过 MQTT 协议与 Transport 交互，后续数据才可能进入 Actor、Rule Engine 和 DAO。
     * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
     * 8. 设计模式：主要体现 State Machine / Command / Callback。
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

/*
 * 本类总结：
 * 1. 核心职责：`MqttLastWill` 在 ThingsBoard Netty MQTT 模块 中承担Netty MQTT 客户端协议类型职责，核心目的是封装基于 Netty 的 MQTT 客户端连接、订阅、发布、QoS、重传、心跳和集成测试服务端逻辑。
 * 2. 核心流程：建立 TCP/MQTT 连接后处理 CONNECT、SUBSCRIBE、PUBLISH、PING 和 DISCONNECT 状态，并通过回调通知调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Netty Channel、MQTT codec、ThingsBoard Transport、回调接口、集成测试 broker 和异步调度器。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
