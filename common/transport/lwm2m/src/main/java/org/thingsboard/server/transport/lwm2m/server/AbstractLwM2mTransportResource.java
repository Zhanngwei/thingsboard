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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.core.californium.LwM2mCoapResource;

/**
 * 中文说明：
 * 1. `AbstractLwM2mTransportResource` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `LwM2mCoapResource`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
public abstract class AbstractLwM2mTransportResource extends LwM2mCoapResource {

    /**
     * 功能：创建 `AbstractLwM2mTransportResource` 实例，并初始化必要字段。
     * 参数：
     * - `name`：名称。
     * 返回：新创建的对象实例。
     */
    public AbstractLwM2mTransportResource(String name) {
        super(name);
    }

    /**
     * 功能：处理`GET`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    public void handleGET(CoapExchange exchange) {
        processHandleGet(exchange);
    }

    /**
     * 功能：处理`POST`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    public void handlePOST(CoapExchange exchange) {
        processHandlePost(exchange);
    }

    /**
     * 功能：处理`Handle Get`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    protected abstract void processHandleGet(CoapExchange exchange);

    /**
     * 功能：处理`Handle Post`。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    protected abstract void processHandlePost(CoapExchange exchange);

}
