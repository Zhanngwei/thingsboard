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

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;

/**
 * 中文说明：
 * 1. `MqttConnectResult` 是 ThingsBoard Netty MQTT Client 中承载 MQTT 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MqttConnectResult {

    /**
     * 当前操作是否成功。
     */
    private final boolean success;
    private final MqttConnectReturnCode returnCode;
    /**
     * 异步结果，表示当前对象的对应属性。
     */
    private final ChannelFuture closeFuture;

    /**
     * 功能：创建 `MqttConnectResult` 实例，并初始化必要字段。
     * 参数：
     * - `success`：`success` 参数。
     * - `returnCode`：`returnCode` 参数。
     * - `closeFuture`：`closeFuture` 参数。
     * 返回：新创建的对象实例。
     */
    MqttConnectResult(boolean success, MqttConnectReturnCode returnCode, ChannelFuture closeFuture) {
        this.success = success;
        this.returnCode = returnCode;
        this.closeFuture = closeFuture;
    }

    /**
     * 功能：判断`Success`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 功能：获取编码。
     * 参数：无。
     * 返回：处理结果。
     */
    public MqttConnectReturnCode getReturnCode() {
        return returnCode;
    }

    /**
     * 功能：获取异步结果。
     * 参数：无。
     * 返回：异步处理结果。
     */
    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}
