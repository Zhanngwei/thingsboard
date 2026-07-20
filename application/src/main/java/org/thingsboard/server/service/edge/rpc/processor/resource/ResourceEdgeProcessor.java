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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.resource.ResourceMsgConstructor;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `ResourceEdgeProcessor` 是 ThingsBoard Application 中处理边缘节点的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseResourceProcessor`、`ResourceProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class ResourceEdgeProcessor extends BaseResourceProcessor implements ResourceProcessor {

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `resourceUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processResourceMsgFromEdge(TenantId tenantId, Edge edge, ResourceUpdateMsg resourceUpdateMsg) {
        TbResourceId tbResourceId = new TbResourceId(new UUID(resourceUpdateMsg.getIdMSB(), resourceUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (resourceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean resourceKeyUpdated = super.saveOrUpdateTbResource(tenantId, tbResourceId, resourceUpdateMsg);
                    if (resourceKeyUpdated) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.TB_RESOURCE, EdgeEventActionType.UPDATED, tbResourceId, null);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(resourceUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("exceeds the maximum")) {
                log.warn("[{}] Resource data size has been exhausted {}", tenantId, resourceUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 功能：转换事件。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertResourceEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        TbResourceId tbResourceId = new TbResourceId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                TbResource tbResource = resourceService.findResourceById(edgeEvent.getTenantId(), tbResourceId);
                if (tbResource != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    ResourceUpdateMsg resourceUpdateMsg = ((ResourceMsgConstructor)
                            resourceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructResourceUpdatedMsg(msgType, tbResource);
                    downlinkMsg = resourceUpdateMsg != null ? DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addResourceUpdateMsg(resourceUpdateMsg)
                            .build() : null;
                }
                break;
            case DELETED:
                ResourceUpdateMsg resourceUpdateMsg = ((ResourceMsgConstructor)
                        resourceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructResourceDeleteMsg(tbResourceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addResourceUpdateMsg(resourceUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}
