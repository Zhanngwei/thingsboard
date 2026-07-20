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
package org.thingsboard.server.common.data.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.validation.NoXss;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * 中文说明：
 * 1. `AlarmUpdateRequest` 是 ThingsBoard Common Data 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AlarmModificationRequest`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Builder
public class AlarmUpdateRequest implements AlarmModificationRequest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @NotNull
    @ApiModelProperty(position = 1, value = "JSON object with Tenant Id", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 告警ID，用于定位对应业务对象。
     */
    @NotNull
    @ApiModelProperty(position = 2, value = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.")
    private AlarmId alarmId;
    /**
     * `severity` 字段，保存当前对象的对应属性。
     */
    @NotNull
    @ApiModelProperty(position = 3, required = true, value = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 4, value = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @ApiModelProperty(position = 5, value = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    /**
     * `details` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @ApiModelProperty(position = 6, value = "JSON object with alarm details")
    private JsonNode details;
    /**
     * `propagation` 字段，保存当前对象的对应属性。
     */
    @Valid
    @ApiModelProperty(position = 7, value = "JSON object with propagation details")
    private AlarmPropagationInfo propagation;

    /**
     * 用户ID，用于定位对应业务对象。
     */
    private UserId userId;

    /**
     * 功能：执行 `fromAlarm` 对应的处理。
     * 参数：
     * - `a`：`a` 参数。
     * 返回：处理结果。
     */
    public static AlarmUpdateRequest fromAlarm(Alarm a) {
        return fromAlarm(a, null);
    }

    /**
     * 功能：执行 `fromAlarm` 对应的处理。
     * 参数：
     * - `a`：`a` 参数。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    public static AlarmUpdateRequest fromAlarm(Alarm a, UserId userId) {
        return AlarmUpdateRequest.builder()
                .tenantId(a.getTenantId())
                .alarmId(a.getId())
                .severity((a.getSeverity()))
                .startTs(a.getStartTs())
                .endTs(a.getEndTs())
                .details(a.getDetails())
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(a.isPropagate())
                        .propagateToOwner(a.isPropagateToOwner())
                        .propagateToTenant(a.isPropagateToTenant())
                        .propagateRelationTypes(a.getPropagateRelationTypes()).build())
                .userId(userId)
                .build();
    }
}
