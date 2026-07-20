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
package org.thingsboard.server.service.rpc;

import org.thingsboard.server.common.msg.rpc.RemoveRpcActorMsg;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.function.Consumer;

/**
 * Handles REST API calls that contain RPC requests to Device.
 */
/**
 * 中文说明：
 * 1. `TbCoreDeviceRpcService` 是 ThingsBoard Application 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbCoreDeviceRpcService {

    /**
     * Handles REST API calls that contain RPC requests to Device and pushes them to Rule Engine.
     * Schedules the timeout for the RPC call based on the {@link ToDeviceRpcRequest}
     *  @param request          the RPC request
     * @param responseConsumer the consumer of the RPC response
     * @param currentUser
     */
    /**
     * 功能：处理RPC。
     * 参数：
     * - `request`：请求对象。
     * - `responseConsumer`：响应对象。
     * - `currentUser`：`currentUser` 参数。
     * 返回：无。
     */
    void processRestApiRpcRequest(ToDeviceRpcRequest request, Consumer<FromDeviceRpcResponse> responseConsumer, SecurityUser currentUser);

    /**
     * Handles the RPC response from the Rule Engine.
     *
     * @param response the RPC response
     */
    /**
     * 功能：处理规则引擎。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    void processRpcResponseFromRuleEngine(FromDeviceRpcResponse response);

    /**
     * Forwards the RPC request from Rule Engine to Device Actor
     *
     * @param request the RPC request message
     */
    /**
     * 功能：执行 `forwardRpcRequestToDeviceActor` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * 返回：无。
     */
    void forwardRpcRequestToDeviceActor(ToDeviceRpcRequestActorMsg request);

    /**
     * Handles the RPC response from the Device Actor (Transport).
     *
     * @param response the RPC response
     */
    /**
     * 功能：处理设备。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    void processRpcResponseFromDeviceActor(FromDeviceRpcResponse response);

    /**
     * 功能：处理RPC。
     * 参数：
     * - `removeRpcMsg`：待处理消息。
     * 返回：无。
     */
    void processRemoveRpc(RemoveRpcActorMsg removeRpcMsg);

}
