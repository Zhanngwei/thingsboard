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

/**
 * Created by Valerii Sosliuk on 12/30/2017.
 */
/**
 * 中文说明：
 * 1. `MqttClientCallback` 是 ThingsBoard Netty MQTT Client 中定义 MQTT 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface MqttClientCallback {

    /**
     * This method is called when the connection to the server is lost.
     *
     * @param cause the reason behind the loss of connection.
     */
    /**
     * 功能：执行 `connectionLost` 对应的处理。
     * 参数：
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    void connectionLost(Throwable cause);

    /**
     * This method is called when the connection to the server is recovered.
     *
     */
    /**
     * 功能：处理`on Successful Reconnect`。
     * 参数：无。
     * 返回：无。
     */
    void onSuccessfulReconnect();
}
