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

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;

/**
 * 中文说明：
 * 1. `DeviceCredentialsService` 是 ThingsBoard Common 中定义设备凭据能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceCredentialsService {

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `credentialsId`：凭据ID。
     * 返回：处理结果。
     */
    DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId);

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * 功能：执行 `formatCredentials` 对应的处理。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：无。
     */
    void formatCredentials(DeviceCredentials deviceCredentials);

    /**
     * 功能：执行 `toCredentialsInfo` 对应的处理。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    JsonNode toCredentialsInfo(DeviceCredentials deviceCredentials);

    /**
     * 功能：删除或清理设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：无。
     */
    void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * 功能：删除或清理设备凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    void deleteDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId);

}
