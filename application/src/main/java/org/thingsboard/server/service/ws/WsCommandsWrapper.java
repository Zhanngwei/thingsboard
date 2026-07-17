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
package org.thingsboard.server.service.ws;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.thingsboard.server.service.ws.notification.cmd.NotificationsUnsubCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUnsubscribeCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUnsubscribeCmd;

import java.util.List;

/**
 * 中文说明：
 * 1. `WsCommandsWrapper` 是 ThingsBoard Application 中承载 `Ws Commands Wrapper` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WsCommandsWrapper {

    /**
     * `authCmd` 字段，保存当前对象的对应属性。
     */
    private AuthCmd authCmd;

    /**
     * `cmds`列表，用于保存一组待处理对象。
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @Type(name = "ATTRIBUTES", value = AttributesSubscriptionCmd.class),
            @Type(name = "TIMESERIES", value = TimeseriesSubscriptionCmd.class),
            @Type(name = "TIMESERIES_HISTORY", value = GetHistoryCmd.class),
            @Type(name = "ENTITY_DATA", value = EntityDataCmd.class),
            @Type(name = "ENTITY_COUNT", value = EntityCountCmd.class),
            @Type(name = "ALARM_DATA", value = AlarmDataCmd.class),
            @Type(name = "ALARM_COUNT", value = AlarmCountCmd.class),
            @Type(name = "NOTIFICATIONS", value = NotificationsSubCmd.class),
            @Type(name = "NOTIFICATIONS_COUNT", value = NotificationsCountSubCmd.class),
            @Type(name = "MARK_NOTIFICATIONS_AS_READ", value = MarkNotificationsAsReadCmd.class),
            @Type(name = "MARK_ALL_NOTIFICATIONS_AS_READ", value = MarkAllNotificationsAsReadCmd.class),
            @Type(name = "ALARM_DATA_UNSUBSCRIBE", value = AlarmDataUnsubscribeCmd.class),
            @Type(name = "ALARM_COUNT_UNSUBSCRIBE", value = AlarmCountUnsubscribeCmd.class),
            @Type(name = "ENTITY_DATA_UNSUBSCRIBE", value = EntityDataUnsubscribeCmd.class),
            @Type(name = "ENTITY_COUNT_UNSUBSCRIBE", value = EntityCountUnsubscribeCmd.class),
            @Type(name = "NOTIFICATIONS_UNSUBSCRIBE", value = NotificationsUnsubCmd.class),
    })
    private List<WsCmd> cmds;

}
