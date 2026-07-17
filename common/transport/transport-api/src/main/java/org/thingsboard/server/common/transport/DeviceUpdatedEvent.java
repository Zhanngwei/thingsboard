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
package org.thingsboard.server.common.transport;

import lombok.Getter;
import org.thingsboard.server.common.data.Device;

/**
 * 中文说明：
 * 1. `DeviceUpdatedEvent` 是 ThingsBoard Common Transport 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Getter
public class DeviceUpdatedEvent {
    /**
     * 设备对象，用于描述当前业务场景。
     */
    private final Device device;

    /**
     * 功能：创建 `DeviceUpdatedEvent` 实例，并初始化必要字段。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DeviceUpdatedEvent(Device device) {
        this.device = device;
    }
}
