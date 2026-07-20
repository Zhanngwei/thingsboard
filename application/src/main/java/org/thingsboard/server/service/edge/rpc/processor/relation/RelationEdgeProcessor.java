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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 中文说明：
 * 1. `RelationEdgeProcessor` 是 ThingsBoard Application 中处理实体关系的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseRelationProcessor`、`RelationProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class RelationEdgeProcessor extends BaseRelationProcessor implements RelationProcessor {

    /**
     * 功能：处理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `relationUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processRelationMsgFromEdge(TenantId tenantId, Edge edge, RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] executing processRelationMsgFromEdge [{}] from edge [{}]", tenantId, relationUpdateMsg, edge.getId());
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());
            return processRelationMsg(tenantId, relationUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：转换关系。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertRelationEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityRelation entityRelation = JacksonUtil.convertValue(edgeEvent.getBody(), EntityRelation.class);
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        RelationUpdateMsg relationUpdateMsg = ((RelationMsgConstructor) relationMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                .constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationUpdateMsg(relationUpdateMsg)
                .build();
    }

    /**
     * 功能：处理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeNotificationMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> processRelationNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EntityRelation relation = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (relation == null || (relation.getFrom().getEntityType().equals(EntityType.EDGE) || relation.getTo().getEntityType().equals(EntityType.EDGE))) {
            return Futures.immediateFuture(null);
        }
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());

        Set<EdgeId> uniqueEdgeIds = new HashSet<>();
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getTo()));
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getFrom()));
        uniqueEdgeIds.remove(originatorEdgeId);
        if (uniqueEdgeIds.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EdgeId edgeId : uniqueEdgeIds) {
            futures.add(saveEdgeEvent(tenantId,
                    edgeId,
                    EdgeEventType.RELATION,
                    EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                    null,
                    JacksonUtil.valueToTree(relation)));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }
}
