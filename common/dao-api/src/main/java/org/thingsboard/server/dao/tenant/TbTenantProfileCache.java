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
package org.thingsboard.server.dao.tenant;

import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `TbTenantProfileCache` 是 ThingsBoard Common 中定义租户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbTenantProfileCache {

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    TenantProfile get(TenantId tenantId);

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    TenantProfile get(TenantProfileId tenantProfileId);

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    void put(TenantProfile profile);

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    void evict(TenantProfileId id);

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    void evict(TenantId id);

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * - `profileListener`：`profileListener` 参数。
     * 返回：无。
     */
    void addListener(TenantId tenantId, EntityId listenerId, Consumer<TenantProfile> profileListener);

    /**
     * 功能：删除或清理监听器。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `listenerId`：监听器ID。
     * 返回：无。
     */
    void removeListener(TenantId tenantId, EntityId listenerId);

}
