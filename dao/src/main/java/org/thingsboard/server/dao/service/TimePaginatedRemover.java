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
package org.thingsboard.server.dao.service;

import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;

/**
 * 中文说明：
 * 1. `TimePaginatedRemover` 是 ThingsBoard DAO 中负责 `Time Paginated Remover` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `IdBased`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public abstract class TimePaginatedRemover<I, D extends IdBased<?>> {

    /**
     * 数量限制常量，用于统一引用固定值。
     */
    private static final int DEFAULT_LIMIT = 100;

    /**
     * 功能：删除或清理`Entities`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    public void removeEntities(TenantId tenantId, I id) {
        TimePageLink pageLink = new TimePageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            PageData<D> entities = findEntities(tenantId, id, pageLink);
            for (D entity : entities.getData()) {
                removeEntity(tenantId, entity);
            }
            hasNext = entities.hasNext();
        }
    }

    /**
     * 功能：获取`Entities`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract PageData<D> findEntities(TenantId tenantId, I id, TimePageLink pageLink);

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entity`：实体对象。
     * 返回：无。
     */
    protected abstract void removeEntity(TenantId tenantId, D entity);

}
