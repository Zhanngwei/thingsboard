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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

/**
 * 中文说明：
 * 1. `NotificationsUnsubCmd` 是 ThingsBoard Application 中承载 `Notifications Unsub Cmd` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UnsubscribeCmd`、`WsCmd`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsUnsubCmd implements UnsubscribeCmd, WsCmd {
    /**
     * `cmdId`ID，用于定位对应业务对象。
     */
    private int cmdId;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WsCmdType getType() {
        return WsCmdType.NOTIFICATIONS_UNSUBSCRIBE;
    }
}
