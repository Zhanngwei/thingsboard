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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.downlink.composite.TbLwM2MReadCompositeRequest;
import org.thingsboard.server.transport.lwm2m.server.rpc.composite.RpcWriteCompositeRequest;

import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mDownlinkMsgHandler` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface LwM2mDownlinkMsgHandler {

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendReadRequest(LwM2mClient client, TbLwM2MReadRequest request, DownlinkRequestCallback<ReadRequest, ReadResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * - `contentFormatComposite`：`contentFormatComposite` 参数。
     * 返回：无。
     */
    void sendReadCompositeRequest(LwM2mClient client, TbLwM2MReadCompositeRequest request, DownlinkRequestCallback<ReadCompositeRequest, ReadCompositeResponse> callback, ContentFormat contentFormatComposite);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendObserveRequest(LwM2mClient client, TbLwM2MObserveRequest request, DownlinkRequestCallback<ObserveRequest, ObserveResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendObserveAllRequest(LwM2mClient client, TbLwM2MObserveAllRequest request, DownlinkRequestCallback<TbLwM2MObserveAllRequest, Set<String>> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendExecuteRequest(LwM2mClient client, TbLwM2MExecuteRequest request, DownlinkRequestCallback<ExecuteRequest, ExecuteResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendDeleteRequest(LwM2mClient client, TbLwM2MDeleteRequest request, DownlinkRequestCallback<DeleteRequest, DeleteResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendCancelObserveRequest(LwM2mClient client, TbLwM2MCancelObserveRequest request, DownlinkRequestCallback<TbLwM2MCancelObserveRequest, Integer> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendCancelAllRequest(LwM2mClient client, TbLwM2MCancelAllRequest request, DownlinkRequestCallback<TbLwM2MCancelAllRequest, Integer> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendDiscoverRequest(LwM2mClient client, TbLwM2MDiscoverRequest request, DownlinkRequestCallback<DiscoverRequest, DiscoverResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendDiscoverAllRequest(LwM2mClient client, TbLwM2MDiscoverAllRequest request, DownlinkRequestCallback<TbLwM2MDiscoverAllRequest, List<Link>> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendWriteAttributesRequest(LwM2mClient client, TbLwM2MWriteAttributesRequest request, DownlinkRequestCallback<WriteAttributesRequest, WriteAttributesResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendWriteReplaceRequest(LwM2mClient client, TbLwM2MWriteReplaceRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `nodes`：`nodes` 参数。
     * - `callback`：处理完成后的回调。
     * - `contentFormatComposite`：`contentFormatComposite` 参数。
     * 返回：无。
     */
    void sendWriteCompositeRequest(LwM2mClient client, RpcWriteCompositeRequest nodes, DownlinkRequestCallback<WriteCompositeRequest, WriteCompositeResponse> callback, ContentFormat contentFormatComposite);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendWriteUpdateRequest(LwM2mClient client, TbLwM2MWriteUpdateRequest request, DownlinkRequestCallback<WriteRequest, WriteResponse> callback);

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void sendCreateRequest(LwM2mClient client, TbLwM2MCreateRequest request, DownlinkRequestCallback<CreateRequest, CreateResponse> callback);

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mDownlinkMsgHandler` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
