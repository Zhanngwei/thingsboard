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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `TbRpcService` 是 ThingsBoard Application 中负责 RPC 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class TbRpcService {
    /**
     * RPC，提供当前类调用的业务操作。
     */
    private final RpcService rpcService;
    private final TbClusterService tbClusterService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpc`：`rpc` 参数。
     * 返回：处理结果。
     */
    public Rpc save(TenantId tenantId, Rpc rpc) {
        Rpc saved = rpcService.save(rpc);
        pushRpcMsgToRuleEngine(tenantId, saved);
        return saved;
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpcId`：RPCID。
     * - `newStatus`：`newStatus` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    public void save(TenantId tenantId, RpcId rpcId, RpcStatus newStatus, JsonNode response) {
        Rpc foundRpc = rpcService.findById(tenantId, rpcId);
        if (foundRpc != null) {
            foundRpc.setStatus(newStatus);
            if (response != null) {
                foundRpc.setResponse(response);
            }
            Rpc saved = rpcService.save(foundRpc);
            pushRpcMsgToRuleEngine(tenantId, saved);
        } else {
            log.warn("[{}] Failed to update RPC status because RPC was already deleted", rpcId);
        }
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpc`：`rpc` 参数。
     * 返回：无。
     */
    private void pushRpcMsgToRuleEngine(TenantId tenantId, Rpc rpc) {
        TbMsg msg = TbMsg.newMsg(TbMsgType.valueOf("RPC_" + rpc.getStatus().name()), rpc.getDeviceId(), TbMsgMetaData.EMPTY, JacksonUtil.toString(rpc));
        tbClusterService.pushMsgToRuleEngine(tenantId, rpc.getDeviceId(), msg, null);
    }

    /**
     * 功能：获取RPC。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `rpcId`：RPCID。
     * 返回：处理结果。
     */
    public Rpc findRpcById(TenantId tenantId, RpcId rpcId) {
        return rpcService.findById(tenantId, rpcId);
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `rpcStatus`：`rpcStatus` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return rpcService.findAllByDeviceIdAndStatus(tenantId, deviceId, rpcStatus, pageLink);
    }

}
