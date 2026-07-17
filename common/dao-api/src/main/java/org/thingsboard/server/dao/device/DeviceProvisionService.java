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
package org.thingsboard.server.dao.device;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;

/**
 * 中文说明：
 * 1. `DeviceProvisionService` 是 ThingsBoard Common 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceProvisionService {

    /**
     * 功能：执行 `provisionDevice` 对应的处理。
     * 参数：
     * - `provisionRequest`：请求对象。
     * 返回：处理结果。
     */
    ProvisionResponse provisionDevice(ProvisionRequest provisionRequest) throws ProvisionFailedException;

    /**
     * 功能：执行 `provisionDeviceViaX509Chain` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `provisionRequest`：请求对象。
     * 返回：处理结果。
     */
    ProvisionResponse provisionDeviceViaX509Chain(DeviceProfile deviceProfile, ProvisionRequest provisionRequest) throws ProvisionFailedException;
}
