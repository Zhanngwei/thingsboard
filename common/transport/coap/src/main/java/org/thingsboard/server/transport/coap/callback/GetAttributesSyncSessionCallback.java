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
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;

/**
 * 中文说明：
 * 1. `GetAttributesSyncSessionCallback` 是 ThingsBoard Common Transport 中处理会话的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractSyncSessionCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class GetAttributesSyncSessionCallback extends AbstractSyncSessionCallback {

    /**
     * 功能：创建 `GetAttributesSyncSessionCallback` 实例，并初始化必要字段。
     * 参数：
     * - `state`：`state` 参数。
     * - `exchange`：`exchange` 参数。
     * - `request`：请求对象。
     * 返回：新创建的对象实例。
     */
    public GetAttributesSyncSessionCallback(TbCoapClientState state, CoapExchange exchange, Request request) {
        super(state, exchange, request);
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg msg) {
        try {
            respond(state.getAdaptor().convertToPublish(msg));
        } catch (AdaptorException e) {
            log.trace("[{}] Failed to reply due to error", state.getDeviceId(), e);
            exchange.respond(new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

}
