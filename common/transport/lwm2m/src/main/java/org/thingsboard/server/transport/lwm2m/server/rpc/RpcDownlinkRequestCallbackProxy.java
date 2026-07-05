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
 * 1. 类目的：`RpcDownlinkRequestCallbackProxy` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`RpcDownlinkRequestCallbackProxy` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
