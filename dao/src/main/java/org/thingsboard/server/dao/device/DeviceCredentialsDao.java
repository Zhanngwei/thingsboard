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

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface DeviceCredentialsDao.
 */
/**
 * 中文说明：
 * 1. `DeviceCredentialsDao` 是 ThingsBoard DAO 中定义设备凭据能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface DeviceCredentialsDao extends Dao<DeviceCredentials> {

    /**
     * Save or update device credentials object
     *
     * @param tenantId the device tenant id
     * @param deviceCredentials the device credentials object
     * @return saved device credentials object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceCredentials save(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceCredentials saveAndFlush(TenantId tenantId, DeviceCredentials deviceCredentials);

    /**
     * Find device credentials by device id.
     *
     * @param deviceId the device id
     * @return the device credentials object
     */
    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceCredentials findByDeviceId(TenantId tenantId, UUID deviceId);

    /**
     * Find device credentials by credentials id.
     *
     * @param credentialsId the credentials id
     * @return the device credentials object
     */
    /**
     * 功能：获取凭据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `credentialsId`：凭据ID。
     * 返回：处理结果。
     */
    DeviceCredentials findByCredentialsId(TenantId tenantId, String credentialsId);

    /**
     * 功能：删除或清理设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceCredentials removeByDeviceId(TenantId tenantId, DeviceId deviceId);

}
