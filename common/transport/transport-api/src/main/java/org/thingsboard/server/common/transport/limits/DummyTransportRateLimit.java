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
package org.thingsboard.server.common.transport.limits;

/**
 * 中文说明：
 * 1. `DummyTransportRateLimit` 是 ThingsBoard Common Transport 中负责传输层接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TransportRateLimit`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class DummyTransportRateLimit implements TransportRateLimit {

    /**
     * 功能：获取`Configuration`。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getConfiguration() {
        return "";
    }

    /**
     * 功能：执行 `tryConsume` 对应的处理。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean tryConsume(long number) {
        return true;
    }

    /**
     * 功能：执行 `tryConsume` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean tryConsume() {
        return true;
    }

}
