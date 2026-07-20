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

import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TenantProfileService` 是 ThingsBoard Common 中定义租户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `EntityDaoService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TenantProfileService extends EntityDaoService {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：处理结果。
     */
    EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId);

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：处理结果。
     */
    TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile);

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：无。
     */
    void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    TenantProfile findDefaultTenantProfile(TenantId tenantId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    EntityInfo findDefaultTenantProfileInfo(TenantId tenantId);

    /**
     * 功能：更新租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfileId`：租户IDID。
     * 返回：判断结果。
     */
    boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId);

    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteTenantProfiles(TenantId tenantId);

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ids`：`ids` 参数。
     * 返回：匹配的数据集合。
     */
    List<TenantProfile> findTenantProfilesByIds(TenantId tenantId, UUID[] ids);

}
