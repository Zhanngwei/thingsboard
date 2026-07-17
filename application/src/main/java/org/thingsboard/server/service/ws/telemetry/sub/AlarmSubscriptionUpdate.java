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
package org.thingsboard.server.service.ws.telemetry.sub;

import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.service.subscription.SubscriptionErrorCode;

/**
 * 中文说明：
 * 1. `AlarmSubscriptionUpdate` 是 ThingsBoard Application 中围绕告警提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@ToString
public class AlarmSubscriptionUpdate {

    /**
     * 错误码，记录当前处理过程中的失败原因。
     */
    @Getter
    private int errorCode;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Getter
    private String errorMsg;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    @Getter
    private AlarmInfo alarm;
    /**
     * 当前对象是否已经删除。
     */
    @Getter
    private boolean alarmDeleted;

    /**
     * 功能：创建 `AlarmSubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmSubscriptionUpdate(AlarmInfo alarm) {
        this(alarm, false);
    }

    /**
     * 功能：创建 `AlarmSubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * - `alarmDeleted`：`alarmDeleted` 参数。
     * 返回：新创建的对象实例。
     */
    public AlarmSubscriptionUpdate(AlarmInfo alarm, boolean alarmDeleted) {
        super();
        this.alarm = alarm;
        this.alarmDeleted = alarmDeleted;
    }

    /**
     * 功能：创建 `AlarmSubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `errorCode`：错误信息。
     * 返回：新创建的对象实例。
     */
    public AlarmSubscriptionUpdate(SubscriptionErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * 功能：创建 `AlarmSubscriptionUpdate` 实例，并初始化必要字段。
     * 参数：
     * - `errorCode`：错误信息。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    public AlarmSubscriptionUpdate(SubscriptionErrorCode errorCode, String errorMsg) {
        super();
        this.errorCode = errorCode.getCode();
        this.errorMsg = errorMsg != null ? errorMsg : errorCode.getDefaultMsg();
    }
}