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
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.audit.ActionType;

/**
 * 中文说明：
 * 1. `EdgeEventActionType` 是 ThingsBoard Common Data 中定义事件固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
@Getter
public enum EdgeEventActionType {
    ADDED(ActionType.ADDED),
    UPDATED(ActionType.UPDATED),
    DELETED(ActionType.DELETED),
    POST_ATTRIBUTES(null),
    ATTRIBUTES_UPDATED(ActionType.ATTRIBUTES_UPDATED),
    ATTRIBUTES_DELETED(ActionType.ATTRIBUTES_DELETED),
    TIMESERIES_UPDATED(ActionType.TIMESERIES_UPDATED),
    CREDENTIALS_UPDATED(ActionType.CREDENTIALS_UPDATED),
    ASSIGNED_TO_CUSTOMER(ActionType.ASSIGNED_TO_CUSTOMER),
    UNASSIGNED_FROM_CUSTOMER(ActionType.UNASSIGNED_FROM_CUSTOMER),
    RELATION_ADD_OR_UPDATE(ActionType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(ActionType.RELATION_DELETED),
    RPC_CALL(ActionType.RPC_CALL),
    ALARM_ACK(ActionType.ALARM_ACK),
    ALARM_CLEAR(ActionType.ALARM_CLEAR),
    ALARM_DELETE(ActionType.ALARM_DELETE),
    ALARM_ASSIGNED(ActionType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(ActionType.ALARM_UNASSIGNED),
    ADDED_COMMENT(ActionType.ADDED_COMMENT),
    UPDATED_COMMENT(ActionType.UPDATED_COMMENT),
    DELETED_COMMENT(ActionType.DELETED_COMMENT),
    ASSIGNED_TO_EDGE(ActionType.ASSIGNED_TO_EDGE),
    UNASSIGNED_FROM_EDGE(ActionType.UNASSIGNED_FROM_EDGE),
    CREDENTIALS_REQUEST(null),
    ENTITY_MERGE_REQUEST(null); // deprecated

    /**
     * 类型，用于区分不同处理分支。
     */
    private final ActionType actionType;

    EdgeEventActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
