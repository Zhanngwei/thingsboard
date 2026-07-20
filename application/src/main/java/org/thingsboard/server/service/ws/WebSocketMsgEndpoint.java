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
package org.thingsboard.server.service.ws;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. `WebSocketMsgEndpoint` 是 ThingsBoard Application 中定义消息能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface WebSocketMsgEndpoint {

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `subscriptionId`：订阅ID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void send(WebSocketSessionRef sessionRef, int subscriptionId, String msg) throws IOException;

    /**
     * 功能：发送或提交`Ping`。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `currentTime`：`currentTime` 参数。
     * 返回：无。
     */
    void sendPing(WebSocketSessionRef sessionRef, long currentTime) throws IOException;

    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：
     * - `sessionRef`：会话对象。
     * - `withReason`：`withReason` 参数。
     * 返回：无。
     */
    void close(WebSocketSessionRef sessionRef, CloseStatus withReason) throws IOException;
}
