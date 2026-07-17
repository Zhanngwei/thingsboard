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
package org.thingsboard.server.service.edge.rpc;

import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edge.EdgeSessionMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `EdgeRpcService` 是 ThingsBoard Application 中定义边缘节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EdgeRpcService {

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg);

    /**
     * 功能：更新边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    void updateEdge(TenantId tenantId, Edge edge);

    /**
     * 功能：删除或清理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    void deleteEdge(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：处理请求。
     * 参数：
     * - `request`：请求对象。
     * - `responseConsumer`：响应对象。
     * 返回：无。
     */
    void processSyncRequest(ToEdgeSyncRequest request, Consumer<FromEdgeSyncResponse> responseConsumer);
}
