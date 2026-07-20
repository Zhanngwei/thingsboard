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
package org.thingsboard.server.service.edge.rpc.constructor.device;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.MsgConstructor;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `DeviceMsgConstructor` 是 ThingsBoard Application 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `MsgConstructor`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceMsgConstructor extends MsgConstructor {

    /**
     * 功能：执行 `constructDeviceUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device);

    /**
     * 功能：执行 `constructDeviceDeleteMsg` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId);

    /**
     * 功能：执行 `constructDeviceCredentialsUpdatedMsg` 对应的处理。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials);

    /**
     * 功能：执行 `constructDeviceProfileUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile);

    /**
     * 功能：执行 `constructDeviceProfileDeleteMsg` 对应的处理。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    DeviceProfileUpdateMsg constructDeviceProfileDeleteMsg(DeviceProfileId deviceProfileId);

    /**
     * 功能：执行 `constructDeviceRpcCallMsg` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `body`：`body` 参数。
     * 返回：处理结果。
     */
    DeviceRpcCallMsg constructDeviceRpcCallMsg(UUID deviceId, JsonNode body);
}
