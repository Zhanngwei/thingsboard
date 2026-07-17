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
package org.thingsboard.server.coapserver;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;

/**
 * 中文说明：
 * 1. `TbCoapDtlsSessionInfo` 是 ThingsBoard Common 中承载会话信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class TbCoapDtlsSessionInfo {

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private ValidateDeviceCredentialsResponse msg;
    private DeviceProfile deviceProfile;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    private long lastActivityTime;


    /**
     * 功能：创建 `TbCoapDtlsSessionInfo` 实例，并初始化必要字段。
     * 参数：
     * - `msg`：待处理消息。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public TbCoapDtlsSessionInfo(ValidateDeviceCredentialsResponse msg, DeviceProfile deviceProfile) {
        this.msg = msg;
        this.deviceProfile = deviceProfile;
        this.lastActivityTime = System.currentTimeMillis();
    }
}