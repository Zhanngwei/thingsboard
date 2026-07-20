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

import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `MqttSubscription` 是 ThingsBoard Netty MQTT Client 中围绕 MQTT 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
final class MqttSubscription {

    /**
     * 主题，用于匹配或发送对应主题的数据。
     */
    private final String topic;
    private final Pattern topicRegex;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final MqttHandler handler;

    /**
     * 是否满足`once`条件。
     */
    private final boolean once;

    /**
     * 是否满足`called`条件。
     */
    private volatile boolean called;

    /**
     * 功能：创建 `MqttSubscription` 实例，并初始化必要字段。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `handler`：处理器对象。
     * - `once`：`once` 参数。
     * 返回：新创建的对象实例。
     */
    MqttSubscription(String topic, MqttHandler handler, boolean once) {
        if (topic == null) {
            throw new NullPointerException("topic");
        }
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.topic = topic;
        this.handler = handler;
        this.once = once;
        this.topicRegex = Pattern.compile(topic.replace("+", "[^/]+").replace("#", ".+") + "$");
    }

    /**
     * 功能：获取主题。
     * 参数：无。
     * 返回：文本结果。
     */
    String getTopic() {
        return topic;
    }

    /**
     * 功能：获取处理器。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttHandler getHandler() {
        return handler;
    }

    /**
     * 功能：判断`Once`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isOnce() {
        return once;
    }

    /**
     * 功能：判断`Called`。
     * 参数：无。
     * 返回：判断结果。
     */
    boolean isCalled() {
        return called;
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：判断结果。
     */
    boolean matches(String topic) {
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

        MqttSubscription that = (MqttSubscription) o;

        return once == that.once && topic.equals(that.topic) && handler.equals(that.handler);
    }

    /**
     * 功能：计算当前对象的哈希值。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int hashCode() {
        int result = topic.hashCode();
        result = 31 * result + handler.hashCode();
        result = 31 * result + (once ? 1 : 0);
        return result;
    }

    /**
     * 功能：更新`Called`。
     * 参数：
     * - `called`：`called` 参数。
     * 返回：无。
     */
    void setCalled(boolean called) {
        this.called = called;
    }
}
