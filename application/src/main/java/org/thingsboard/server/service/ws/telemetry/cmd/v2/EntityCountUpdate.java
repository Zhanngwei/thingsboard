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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

/**
 * 中文说明：
 * 1. `EntityCountUpdate` 是 ThingsBoard Application 中围绕实体提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `CmdUpdate`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@ToString
public class EntityCountUpdate extends CmdUpdate {

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    @Getter
    private int count;

    /**
     * 功能：创建 `EntityCountUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `count`：`count` 参数。
     * 返回：新创建的对象实例。
     */
    public EntityCountUpdate(int cmdId, int count) {
        super(cmdId, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.count = count;
    }

    /**
     * 功能：创建 `EntityCountUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public EntityCountUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, errorCode, errorMsg);
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.COUNT_DATA;
    }

    /**
     * 功能：创建 `EntityCountUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `count`：`count` 参数。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public EntityCountUpdate(@JsonProperty("cmdId") int cmdId,
                             @JsonProperty("count") int count,
                             @JsonProperty("errorCode") int errorCode,
                             @JsonProperty("errorMsg") String errorMsg) {
        super(cmdId, errorCode, errorMsg);
        this.count = count;
    }

}
