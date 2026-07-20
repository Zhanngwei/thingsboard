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
 * 1. `LwM2mDownlinkMsgHandler` 是 ThingsBoard Common Transport 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
