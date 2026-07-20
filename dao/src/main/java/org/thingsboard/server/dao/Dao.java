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
package org.thingsboard.server.dao;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `Dao` 是 ThingsBoard DAO 中定义 `Dao` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface Dao<T> {

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    List<T> find(TenantId tenantId);

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    T findById(TenantId tenantId, UUID id);

    /**
     * 功能：获取`By Id Async`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<T> findByIdAsync(TenantId tenantId, UUID id);

    /**
     * 功能：判断`By Id`是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    boolean existsById(TenantId tenantId, UUID id);

    /**
     * 功能：判断`By Id Async`是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    ListenableFuture<Boolean> existsByIdAsync(TenantId tenantId, UUID id);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    T save(TenantId tenantId, T t);

    /**
     * 功能：保存或创建`And Flush`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    T saveAndFlush(TenantId tenantId, T t);

    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    boolean removeById(TenantId tenantId, UUID id);

    /**
     * 功能：删除或清理`All By Ids`。
     * 参数：
     * - `ids`：数据列表。
     * 返回：无。
     */
    void removeAllByIds(Collection<UUID> ids);

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    default EntityType getEntityType() { return null; }

}
