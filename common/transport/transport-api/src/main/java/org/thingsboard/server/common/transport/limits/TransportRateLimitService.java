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
package org.thingsboard.server.common.transport.limits;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;

import java.net.InetSocketAddress;

/**
 * 中文说明：
 * 1. `TransportRateLimitService` 是 ThingsBoard Common Transport 中定义传输层能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TransportRateLimitService {

    /**
     * 功能：校验`Limits`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `dataPoints`：待处理数据。
     * 返回：判断结果。
     */
    EntityType checkLimits(TenantId tenantId, DeviceId deviceId, int dataPoints);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：无。
     */
    void update(TenantProfileUpdateResult update);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void update(TenantId tenantId);

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void remove(TenantId tenantId);

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    void remove(DeviceId deviceId);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `transportEnabled`：`transportEnabled` 参数。
     * 返回：无。
     */
    void update(TenantId tenantId, boolean transportEnabled);

    /**
     * 功能：校验`Address`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：判断结果。
     */
    boolean checkAddress(InetSocketAddress address);

    /**
     * 功能：处理`on Auth Success`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    void onAuthSuccess(InetSocketAddress address);

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：无。
     */
    void onAuthFailure(InetSocketAddress address);

    /**
     * 功能：执行 `invalidateRateLimitsIpTable` 对应的处理。
     * 参数：
     * - `sessionInactivityTimeout`：会话对象。
     * 返回：无。
     */
    void invalidateRateLimitsIpTable(long sessionInactivityTimeout);

}
