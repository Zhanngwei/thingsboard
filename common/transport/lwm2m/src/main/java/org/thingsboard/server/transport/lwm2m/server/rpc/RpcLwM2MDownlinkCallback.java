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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.eclipse.leshan.core.request.LwM2mRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `RpcLwM2MDownlinkCallback` 是 ThingsBoard Common Transport 中处理 LwM2M 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `LwM2mRequest`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public abstract class RpcLwM2MDownlinkCallback<R extends LwM2mRequest<T>, T extends LwM2mResponse> extends RpcDownlinkRequestCallbackProxy<R, T> {

    /**
     * 功能：创建 `RpcLwM2MDownlinkCallback` 实例，并初始化必要字段。
     * 参数：
     * - `transportService`：服务对象。
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：新创建的对象实例。
     */
    public RpcLwM2MDownlinkCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        super(transportService, client, requestMsg, callback);
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    protected void sendRpcReplyOnSuccess(T response) {
        LwM2MRpcResponseBody.LwM2MRpcResponseBodyBuilder builder = LwM2MRpcResponseBody.builder().result(response.getCode().getName());
        if (response.isSuccess()) {
            Optional<String> responseValue = serializeSuccessfulResponse(response);
            if (responseValue.isPresent() && StringUtils.isNotEmpty(responseValue.get())) {
                builder.value(responseValue.get());
            }
        } else {
            if (StringUtils.isNotEmpty(response.getErrorMessage())) {
                builder.error(response.getErrorMessage());
            }
        }
        reply(builder.build());
    }

    /**
     * 功能：执行 `serializeSuccessfulResponse` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：可能存在的结果。
     */
    protected abstract Optional<String> serializeSuccessfulResponse(T response);
}
