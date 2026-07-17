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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. `BaseRelationProcessor` 是 ThingsBoard Application 中处理实体关系的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class BaseRelationProcessor extends BaseEdgeProcessor {

    /**
     * 功能：处理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relationUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected ListenableFuture<Void> processRelationMsg(TenantId tenantId, RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] processRelationMsg [{}]", tenantId, relationUpdateMsg);
        try {
            EntityRelation entityRelation = constructEntityRelationFromUpdateMsg(relationUpdateMsg);
            if (entityRelation == null) {
                throw new RuntimeException("[{" + tenantId + "}] relationUpdateMsg {" + relationUpdateMsg + "} cannot be converted to entity relation");
            }
            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (isEntityExists(tenantId, entityRelation.getTo())
                            && isEntityExists(tenantId, entityRelation.getFrom())) {
                        relationService.saveRelation(tenantId, entityRelation);
                    } else {
                        log.warn("[{}] Skipping relating update msg because from/to entity doesn't exists on edge, {}", tenantId, relationUpdateMsg);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    relationService.deleteRelation(tenantId, entityRelation);
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(relationUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process relation update msg [{}]", tenantId, relationUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 功能：执行 `constructEntityRelationFromUpdateMsg` 对应的处理。
     * 参数：
     * - `relationUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract EntityRelation constructEntityRelationFromUpdateMsg(RelationUpdateMsg relationUpdateMsg);
}
