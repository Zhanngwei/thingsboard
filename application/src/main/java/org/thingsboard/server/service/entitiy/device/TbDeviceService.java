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
package org.thingsboard.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;

/**
 * 中文说明：
 * 1. `TbDeviceService` 是 ThingsBoard Application 中定义设备能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbDeviceService {

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device save(Device device, Device oldDevice, String accessToken, User user) throws Exception;

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void delete(Device device, User user);

    /**
     * 功能：执行 `assignDeviceToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignDeviceFromCustomer` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignDeviceToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws ThingsboardException;

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws ThingsboardException;

    /**
     * 功能：更新设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws ThingsboardException;

    /**
     * 功能：执行 `claimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `customerId`：客户IDID。
     * - `secretKey`：键。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user);

    /**
     * 功能：执行 `reclaimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user);

    /**
     * 功能：执行 `assignDeviceToTenant` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `newTenant`：租户信息或租户标识。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToTenant(Device device, Tenant newTenant, User user);

    /**
     * 功能：执行 `assignDeviceToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignDeviceFromEdge` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws ThingsboardException;
}
