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
package org.thingsboard.server.dao.entityview;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Victor Basanets on 8/28/2017.
 */
/**
 * 中文说明：
 * 1. `EntityViewDao` 是 ThingsBoard DAO 中定义实体视图能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EntityViewDao extends Dao<EntityView>, ExportableEntityDao<EntityViewId, EntityView> {

    /**
     * Find entity view info by id.
     *
     * @param tenantId the tenant id
     * @param assetId the asset id
     * @return the entity view info object
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityViewId`：实体视图ID。
     * 返回：处理结果。
     */
    EntityViewInfo findEntityViewInfoById(TenantId tenantId, UUID entityViewId);

    /**
     * Save or update device object
     *
     * @param entityView the entity-view object
     * @return saved entity-view object
     */
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityView`：实体对象。
     * 返回：处理结果。
     */
    EntityView save(TenantId tenantId, EntityView entityView);

    /**
     * Find entity views by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity view infos by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantId(UUID tenantId, PageLink pageLink);

    /**
     * Find entity views by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity view infos by tenantId, type and page link.
     *
     * @param tenantId the tenantId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndType(UUID tenantId, String type, PageLink pageLink);

    /**
     * Find entity views by tenantId and entity view name.
     *
     * @param tenantId the tenantId
     * @param name the entity view name
     * @return the optional entity view object
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    Optional<EntityView> findEntityViewByTenantIdAndName(UUID tenantId, String name);

    /**
     * Find entity views by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerId(UUID tenantId,
                                                                UUID customerId,
                                                                PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink);

    /**
     * Find entity views by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndCustomerIdAndType(UUID tenantId,
                                                                       UUID customerId,
                                                                       String type,
                                                                       PageLink pageLink);

    /**
     * Find entity view infos by tenantId, customerId, type and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view info objects
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityViewInfo> findEntityViewInfosByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    List<EntityView> findEntityViewsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * 功能：判断租户ID是否存在。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：判断结果。
     */
    boolean existsByTenantIdAndEntityId(UUID tenantId, UUID entityId);

    /**
     * Find tenants entity view types.
     *
     * @return the list of tenant entity view type objects
     */
    /**
     * 功能：获取实体视图。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<EntitySubtype>> findTenantEntityViewTypesAsync(UUID tenantId);

    /**
     * Find entity views by tenantId, edgeId and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeId(UUID tenantId,
                                                            UUID edgeId,
                                                            PageLink pageLink);

    /**
     * Find entity views by tenantId, edgeId, type and page link.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param type the type
     * @param pageLink the page link
     * @return the list of entity view objects
     */
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<EntityView> findEntityViewsByTenantIdAndEdgeIdAndType(UUID tenantId,
                                                            UUID edgeId,
                                                            String type,
                                                            PageLink pageLink);

}
