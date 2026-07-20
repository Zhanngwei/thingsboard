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
package org.thingsboard.server.common.data.msg;

import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbMsgType` 是 ThingsBoard Common Data 中定义消息固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum TbMsgType {

    POST_ATTRIBUTES_REQUEST("Post attributes"),
    POST_TELEMETRY_REQUEST("Post telemetry"),
    TO_SERVER_RPC_REQUEST("RPC Request from Device"),
    ACTIVITY_EVENT("Activity Event"),
    INACTIVITY_EVENT("Inactivity Event"),
    CONNECT_EVENT("Connect Event"),
    DISCONNECT_EVENT("Disconnect Event"),
    ENTITY_CREATED("Entity Created"),
    ENTITY_UPDATED("Entity Updated"),
    ENTITY_DELETED("Entity Deleted"),
    ENTITY_ASSIGNED("Entity Assigned"),
    ENTITY_UNASSIGNED("Entity Unassigned"),
    ATTRIBUTES_UPDATED("Attributes Updated"),
    ATTRIBUTES_DELETED("Attributes Deleted"),
    ALARM,
    ALARM_ACK("Alarm Acknowledged"),
    ALARM_CLEAR("Alarm Cleared"),
    ALARM_DELETE,
    ALARM_ASSIGNED("Alarm Assigned"),
    ALARM_UNASSIGNED("Alarm Unassigned"),
    COMMENT_CREATED("Comment Created"),
    COMMENT_UPDATED("Comment Updated"),
    RPC_CALL_FROM_SERVER_TO_DEVICE("RPC Request to Device"),
    ENTITY_ASSIGNED_FROM_TENANT("Entity Assigned From Tenant"),
    ENTITY_ASSIGNED_TO_TENANT("Entity Assigned To Tenant"),
    ENTITY_ASSIGNED_TO_EDGE,
    ENTITY_UNASSIGNED_FROM_EDGE,
    TIMESERIES_UPDATED("Timeseries Updated"),
    TIMESERIES_DELETED("Timeseries Deleted"),
    RPC_QUEUED("RPC Queued"),
    RPC_SENT("RPC Sent"),
    RPC_DELIVERED("RPC Delivered"),
    RPC_SUCCESSFUL("RPC Successful"),
    RPC_TIMEOUT("RPC Timeout"),
    RPC_EXPIRED("RPC Expired"),
    RPC_FAILED("RPC Failed"),
    RPC_DELETED("RPC Deleted"),
    RELATION_ADD_OR_UPDATE("Relation Added or Updated"),
    RELATION_DELETED("Relation Deleted"),
    RELATIONS_DELETED("All Relations Deleted"),
    PROVISION_SUCCESS,
    PROVISION_FAILURE,
    SEND_EMAIL,

    // tellSelfOnly types
    GENERATOR_NODE_SELF_MSG(null, true),
    DEVICE_PROFILE_PERIODIC_SELF_MSG(null, true),
    DEVICE_PROFILE_UPDATE_SELF_MSG(null, true),
    DEVICE_UPDATE_SELF_MSG(null, true),
    DEDUPLICATION_TIMEOUT_SELF_MSG(null, true),
    DELAY_TIMEOUT_SELF_MSG(null, true),
    MSG_COUNT_SELF_MSG(null, true),

    // Custom or N/A type:
    NA;

    public static final List<String> NODE_CONNECTIONS = EnumSet.allOf(TbMsgType.class).stream()
            .filter(tbMsgType -> !tbMsgType.isTellSelfOnly())
            .map(TbMsgType::getRuleNodeConnection)
            .filter(connection -> !TbNodeConnectionType.OTHER.equals(connection))
            .collect(Collectors.toUnmodifiableList());

    /**
     * 规则节点对象，用于描述当前业务场景。
     */
    @Getter
    private final String ruleNodeConnection;

    /**
     * 是否满足`tellSelfOnly`条件。
     */
    @Getter
    private final boolean tellSelfOnly;

    TbMsgType(String ruleNodeConnection, boolean tellSelfOnly) {
        this.ruleNodeConnection = StringUtils.isNotEmpty(ruleNodeConnection) ? ruleNodeConnection : TbNodeConnectionType.OTHER;
        this.tellSelfOnly = tellSelfOnly;
    }

    TbMsgType(String ruleNodeConnection) {
        this(ruleNodeConnection, false);
    }

    TbMsgType() {
        this(null, false);
    }

}
