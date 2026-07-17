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
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rpc.Rpc;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. `RuleEngineRpcService` 是 ThingsBoard Rule Engine API 中定义 RPC 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface RuleEngineRpcService {

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `serviceId`：服务ID。
     * - `sessionId`：会话ID。
     * - `requestId`：请求ID。
     * - `body`：`body` 参数。
     * 返回：无。
     */
    void sendRpcReplyToDevice(String serviceId, UUID sessionId, int requestId, String body);

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `request`：请求对象。
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    void sendRpcRequestToDevice(RuleEngineDeviceRpcRequest request, Consumer<RuleEngineDeviceRpcResponse> consumer);

    /**
     * 功能：获取RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    Rpc findRpcById(TenantId tenantId, RpcId id);
}
