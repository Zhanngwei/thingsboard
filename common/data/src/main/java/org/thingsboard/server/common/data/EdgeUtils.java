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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 中文说明：
 * 1. `EdgeUtils` 是 ThingsBoard Common Data 中处理边缘节点通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public final class EdgeUtils {

    /**
     * 实体常量，用于统一引用固定值。
     */
    private static final EnumMap<EntityType, EdgeEventType> entityTypeEdgeEventTypeEnumMap;
    private static final EnumMap<ActionType, EdgeEventActionType> actionTypeEdgeEventActionTypeEnumMap;

    static {
        entityTypeEdgeEventTypeEnumMap = new EnumMap<>(EntityType.class);
        for (EdgeEventType edgeEventType : EdgeEventType.values()) {
            if (edgeEventType.getEntityType() != null) {
                entityTypeEdgeEventTypeEnumMap.put(edgeEventType.getEntityType(), edgeEventType);
            }
        }

        actionTypeEdgeEventActionTypeEnumMap = new EnumMap<>(ActionType.class);
        for (EdgeEventActionType edgeEventActionType : EdgeEventActionType.values()) {
            if (edgeEventActionType.getActionType() != null) {
                actionTypeEdgeEventActionTypeEnumMap.put(edgeEventActionType.getActionType(), edgeEventActionType);
            }
        }
    }

    /**
     * 数量限制常量，用于统一引用固定值。
     */
    private static final int STACK_TRACE_LIMIT = 10;

    /**
     * 功能：创建 `EdgeUtils` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private EdgeUtils() {}

    /**
     * 功能：执行 `nextPositiveInt` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public static int nextPositiveInt() {
        return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    public static EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        return entityTypeEdgeEventTypeEnumMap.get(entityType);
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `actionType`：类型。
     * 返回：处理结果。
     */
    public static EdgeEventActionType getEdgeEventActionTypeByActionType(ActionType actionType) {
        return actionTypeEdgeEventActionTypeEnumMap.get(actionType);
    }

    /**
     * 功能：执行 `constructEdgeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `action`：`action` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：文本结果。
     */
    public static String createErrorMsgFromRootCauseAndStackTrace(Throwable t) {
        Throwable rootCause = Throwables.getRootCause(t);
        StringBuilder errorMsg = new StringBuilder(rootCause.getMessage() != null ? rootCause.getMessage() : "");
        if (rootCause.getStackTrace().length > 0) {
            int idx = 0;
            for (StackTraceElement stackTraceElement : rootCause.getStackTrace()) {
                errorMsg.append("\n").append(stackTraceElement.toString());
                idx++;
                if (idx > STACK_TRACE_LIMIT) {
                    break;
                }
            }
        }
        return errorMsg.toString();
    }
}
