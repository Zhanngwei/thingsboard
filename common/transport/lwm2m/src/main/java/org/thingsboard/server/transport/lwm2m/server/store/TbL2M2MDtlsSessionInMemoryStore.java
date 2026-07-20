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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：
 * 1. `TbL2M2MDtlsSessionInMemoryStore` 是 ThingsBoard Common Transport 中负责会话接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TbLwM2MDtlsSessionStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class TbL2M2MDtlsSessionInMemoryStore implements TbLwM2MDtlsSessionStore {

    private final ConcurrentHashMap<String, TbX509DtlsSessionInfo> store = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void put(String endpoint, TbX509DtlsSessionInfo msg) {
        store.put(endpoint, msg);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbX509DtlsSessionInfo get(String endpoint) {
        return store.get(endpoint);
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void remove(String endpoint) {
        store.remove(endpoint);
    }
}
