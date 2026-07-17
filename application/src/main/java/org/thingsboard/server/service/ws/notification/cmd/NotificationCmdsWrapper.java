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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.service.ws.WsCommandsWrapper;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated Use {@link WsCommandsWrapper}. This class is left for backward compatibility
 * */
/**
 * 中文说明：
 * 1. `NotificationCmdsWrapper` 是 ThingsBoard Application 中承载通知信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Deprecated
public class NotificationCmdsWrapper {

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private NotificationsCountSubCmd unreadCountSubCmd;

    /**
     * `unreadSubCmd` 字段，保存当前对象的对应属性。
     */
    private NotificationsSubCmd unreadSubCmd;

    /**
     * `markAsReadCmd` 字段，保存当前对象的对应属性。
     */
    private MarkNotificationsAsReadCmd markAsReadCmd;

    /**
     * `markAllAsReadCmd` 字段，保存当前对象的对应属性。
     */
    private MarkAllNotificationsAsReadCmd markAllAsReadCmd;

    /**
     * `unsubCmd` 字段，保存当前对象的对应属性。
     */
    private NotificationsUnsubCmd unsubCmd;

    /**
     * 功能：执行 `toCommonCmdsWrapper` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public WsCommandsWrapper toCommonCmdsWrapper() {
        return new WsCommandsWrapper(null, Stream.of(
                        unreadCountSubCmd, unreadSubCmd, markAsReadCmd, markAllAsReadCmd, unsubCmd
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

}
