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
package org.thingsboard.server.common.data.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `ActionType` 是 ThingsBoard Common Data 中定义 `Action Type` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum ActionType {

    ADDED(false, TbMsgType.ENTITY_CREATED), // log entity
    DELETED(false, TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(false, TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(false, TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(false, TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(false, TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(false, TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL(false, null), // log method and params
    CREDENTIALS_UPDATED(false, null), // log new credentials
    ASSIGNED_TO_CUSTOMER(false, TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false, TbMsgType.ENTITY_UNASSIGNED), // log customer name
    ACTIVATED(false, null), // log string id
    SUSPENDED(false, null), // log string id
    CREDENTIALS_READ(true, null), // log device id
    ATTRIBUTES_READ(true, null), // log attributes
    RELATION_ADD_OR_UPDATE(false, TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(false, TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(false, TbMsgType.RELATIONS_DELETED),
    ALARM_ACK(false, TbMsgType.ALARM_ACK),
    ALARM_CLEAR(false, TbMsgType.ALARM_CLEAR),
    ALARM_DELETE(false, TbMsgType.ALARM_DELETE),
    ALARM_ASSIGNED(false, TbMsgType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(false, TbMsgType.ALARM_UNASSIGNED),
    LOGIN(false, null),
    LOGOUT(false, null),
    LOCKOUT(false, null),
    ASSIGNED_FROM_TENANT(false, TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(false, TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(false, TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(false, TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(false, TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(false, TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(false, TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(false, TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT(false, null),
    SMS_SENT(false, null);

    /**
     * 是否为`read`。
     */
    @Getter
    private final boolean isRead;

    /**
     * 规则引擎，用于区分不同处理分支。
     */
    private final TbMsgType ruleEngineMsgType;

    ActionType(boolean isRead, TbMsgType ruleEngineMsgType) {
        this.isRead = isRead;
        this.ruleEngineMsgType = ruleEngineMsgType;
    }

    /**
     * 功能：获取规则引擎。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
