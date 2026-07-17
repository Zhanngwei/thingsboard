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
package org.thingsboard.server.service.ws.notification.cmd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.CmdUpdateType;

/**
 * 中文说明：
 * 1. `UnreadNotificationsCountUpdate` 是 ThingsBoard Application 中围绕 `Unread Notifications Count` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `CmdUpdate`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Getter
@ToString
public class UnreadNotificationsCountUpdate extends CmdUpdate {

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private final int totalUnreadCount;
    private final int sequenceNumber;

    /**
     * 功能：创建 `UnreadNotificationsCountUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * - `totalUnreadCount`：`totalUnreadCount` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    @JsonCreator
    public UnreadNotificationsCountUpdate(@JsonProperty("cmdId") int cmdId, @JsonProperty("errorCode") int errorCode,
                                          @JsonProperty("errorMsg") String errorMsg,
                                          @JsonProperty("totalUnreadCount") int totalUnreadCount,
                                          @JsonProperty("sequenceNumber") int sequenceNumber) {
        super(cmdId, errorCode, errorMsg);
        this.totalUnreadCount = totalUnreadCount;
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.NOTIFICATIONS_COUNT;
    }

}
