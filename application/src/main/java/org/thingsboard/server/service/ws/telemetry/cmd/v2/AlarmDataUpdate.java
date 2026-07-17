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
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

import java.util.List;

/**
 * 中文说明：
 * 1. `AlarmDataUpdate` 是 ThingsBoard Application 中围绕告警提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `DataUpdate`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@ToString
public class AlarmDataUpdate extends DataUpdate<AlarmData> {

    /**
     * 允许访问的实体集合对象，用于描述当前业务场景。
     */
    @Getter
    private long allowedEntities;
    /**
     * `totalEntities` 字段，保存当前对象的对应属性。
     */
    @Getter
    private long totalEntities;

    /**
     * 功能：创建 `AlarmDataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `data`：待处理数据。
     * - `update`：数据列表。
     * - `allowedEntities`：`allowedEntities` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AlarmDataUpdate(int cmdId, PageData<AlarmData> data, List<AlarmData> update, long allowedEntities, long totalEntities) {
        super(cmdId, data, update, SubscriptionErrorCode.NO_ERROR.getCode(), null);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }

    /**
     * 功能：创建 `AlarmDataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public AlarmDataUpdate(int cmdId, int errorCode, String errorMsg) {
        super(cmdId, null, null, errorCode, errorMsg);
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CmdUpdateType getCmdUpdateType() {
        return CmdUpdateType.ALARM_DATA;
    }

    /**
     * 功能：创建 `AlarmDataUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `cmdId`：`cmdId`ID。
     * - `data`：待处理数据。
     * - `update`：数据列表。
     * - `errorCode`：错误信息。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public AlarmDataUpdate(@JsonProperty("cmdId") int cmdId,
                           @JsonProperty("data") PageData<AlarmData> data,
                           @JsonProperty("update") List<AlarmData> update,
                           @JsonProperty("errorCode") int errorCode,
                           @JsonProperty("errorMsg") String errorMsg,
                           @JsonProperty("allowedEntities") long allowedEntities,
                           @JsonProperty("totalEntities") long totalEntities) {
        super(cmdId, data, update, errorCode, errorMsg);
        this.allowedEntities = allowedEntities;
        this.totalEntities = totalEntities;
    }
}
