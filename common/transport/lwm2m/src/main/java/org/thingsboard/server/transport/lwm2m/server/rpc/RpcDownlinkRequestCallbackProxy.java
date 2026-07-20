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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * 中文说明：
 * 1. `RpcDownlinkRequestCallbackProxy` 是 ThingsBoard Common Transport 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `DownlinkRequestCallback`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public abstract class RpcDownlinkRequestCallbackProxy<R, T> implements DownlinkRequestCallback<R, T> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TransportService transportService;
    private final TransportProtos.ToDeviceRpcRequestMsg request;
    /**
     * 回调，用于接收异步处理完成后的结果。
     */
    private final DownlinkRequestCallback<R, T> callback;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected final LwM2mClient client;

    /**
     * 功能：创建 `RpcDownlinkRequestCallbackProxy` 实例，并初始化必要字段。
     * 参数：
     * - `transportService`：服务对象。
     * - `client`：客户端对象。
     * - `requestMsg`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：新创建的对象实例。
     */
    public RpcDownlinkRequestCallbackProxy(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<R, T> callback) {
        this.transportService = transportService;
        this.client = client;
        this.request = requestMsg;
        this.callback = callback;
    }

    /**
     * 功能：处理`on Sent`。
     * 参数：
     * - `request`：请求对象。
     * 返回：判断结果。
     */
    @Override
    public boolean onSent(R request) {
        client.lock();
        try {
            UUID rpcId = new UUID(this.request.getRequestIdMSB(), this.request.getRequestIdLSB());
            if (rpcId.equals(client.getLastSentRpcId())) {
                log.debug("[{}]][{}] Rpc has already sent!", client.getEndpoint(), rpcId);
                return false;
            }
            client.setLastSentRpcId(rpcId);
        } finally {
            client.unlock();
        }
        transportService.process(client.getSession(), this.request, RpcStatus.SENT, TransportServiceCallback.EMPTY);
        return true;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onSuccess(R request, T response) {
        transportService.process(client.getSession(), this.request, RpcStatus.DELIVERED, true, TransportServiceCallback.EMPTY);
        sendRpcReplyOnSuccess(response);
        if (callback != null) {
            callback.onSuccess(request, response);
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onValidationError(String params, String msg) {
        sendRpcReplyOnValidationError(msg);
        if (callback != null) {
            callback.onValidationError(params, msg);
        }
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onError(String params, Exception e) {
        if (e instanceof TimeoutException || e instanceof org.eclipse.leshan.core.request.exception.TimeoutException) {
            client.setLastSentRpcId(null);
            transportService.process(client.getSession(), this.request, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
        } else if (!(e instanceof ClientSleepingException)) {
            sendRpcReplyOnError(e);
        }
        if (callback != null) {
            callback.onError(params, e);
        }
    }

    /**
     * 功能：执行 `reply` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    protected void reply(LwM2MRpcResponseBody response) {
        TransportProtos.ToDeviceRpcResponseMsg.Builder msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(request.getRequestId());
        String responseAsString = JacksonUtil.toString(response);
        if (StringUtils.isEmpty(response.getError())) {
            msg.setPayload(responseAsString);
        } else {
            msg.setError(responseAsString);
        }
        transportService.process(client.getSession(), msg.build(), null);
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    abstract protected void sendRpcReplyOnSuccess(T response);

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    protected void sendRpcReplyOnValidationError(String msg) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.BAD_REQUEST.getName()).error(msg).build());
    }

    /**
     * 功能：发送或提交RPC。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    protected void sendRpcReplyOnError(Exception e) {
        String error = e.getMessage();
        if (error == null) {
            error = e.toString();
        }
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.INTERNAL_SERVER_ERROR.getName()).error(error).build());
    }

}
