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

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `RelationEdgeProcessorV1` 是 ThingsBoard Application 中处理实体关系的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `RelationEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@TbCoreComponent
public class RelationEdgeProcessorV1 extends RelationEdgeProcessor {

    /**
     * 功能：执行 `constructEntityRelationFromUpdateMsg` 对应的处理。
     * 参数：
     * - `relationUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    @Override
    protected EntityRelation constructEntityRelationFromUpdateMsg(RelationUpdateMsg relationUpdateMsg) {
        EntityRelation entityRelation = new EntityRelation();

        UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
        EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
        entityRelation.setFrom(fromId);

        UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
        EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
        entityRelation.setTo(toId);

        entityRelation.setType(relationUpdateMsg.getType());
        entityRelation.setTypeGroup(relationUpdateMsg.hasTypeGroup()
                ? RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()) : RelationTypeGroup.COMMON);
        entityRelation.setAdditionalInfo(JacksonUtil.toJsonNode(relationUpdateMsg.getAdditionalInfo()));
        return entityRelation;
    }
}
