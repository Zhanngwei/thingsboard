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
package org.thingsboard.server.service.entitiy.asset.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

import static org.thingsboard.server.dao.asset.BaseAssetService.TB_SERVICE_QUEUE;

/**
 * 中文说明：
 * 1. `DefaultTbAssetProfileService` 是 ThingsBoard Application 中负责资产配置的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbAssetProfileService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbAssetProfileService extends AbstractTbEntityService implements TbAssetProfileService {

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetProfileService assetProfileService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile save(AssetProfile assetProfile, User user) throws Exception {
        ActionType actionType = assetProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = assetProfile.getTenantId();
        try {
            if (TB_SERVICE_QUEUE.equals(assetProfile.getName())) {
                throw new ThingsboardException("Unable to save asset profile with name " + TB_SERVICE_QUEUE, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else if (assetProfile.getId() != null) {
                AssetProfile foundAssetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfile.getId());
                if (foundAssetProfile != null && TB_SERVICE_QUEUE.equals(foundAssetProfile.getName())) {
                    throw new ThingsboardException("Updating asset profile with name " + TB_SERVICE_QUEUE + " is prohibited!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            AssetProfile savedAssetProfile = checkNotNull(assetProfileService.saveAssetProfile(assetProfile));
            autoCommit(user, savedAssetProfile.getId());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, savedAssetProfile.getId(),
                    actionType.equals(ActionType.ADDED) ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);

            notificationEntityService.logEntityAction(tenantId, savedAssetProfile.getId(), savedAssetProfile,
                    null, actionType, user);
            return savedAssetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), assetProfile, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(AssetProfile assetProfile, User user) {
        ActionType actionType = ActionType.DELETED;
        AssetProfileId assetProfileId = assetProfile.getId();
        TenantId tenantId = assetProfile.getTenantId();
        try {
            assetProfileService.deleteAssetProfile(tenantId, assetProfileId);

            tbClusterService.broadcastEntityStateChangeEvent(tenantId, assetProfileId, ComponentLifecycleEvent.DELETED);
            notificationEntityService.logEntityAction(tenantId, assetProfileId, assetProfile, null,
                    actionType, user, assetProfileId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), actionType,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `previousDefaultAssetProfile`：`previousDefaultAssetProfile` 参数。
     * - `user`：`user` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfile setDefaultAssetProfile(AssetProfile assetProfile, AssetProfile previousDefaultAssetProfile, User user) throws ThingsboardException {
        TenantId tenantId = assetProfile.getTenantId();
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            if (assetProfileService.setDefaultAssetProfile(tenantId, assetProfileId)) {
                if (previousDefaultAssetProfile != null) {
                    previousDefaultAssetProfile = assetProfileService.findAssetProfileById(tenantId, previousDefaultAssetProfile.getId());
                    notificationEntityService.logEntityAction(tenantId, previousDefaultAssetProfile.getId(), previousDefaultAssetProfile,
                            ActionType.UPDATED, user);
                }
                assetProfile = assetProfileService.findAssetProfileById(tenantId, assetProfileId);

                notificationEntityService.logEntityAction(tenantId, assetProfileId, assetProfile, ActionType.UPDATED, user);
            }
            return assetProfile;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.ASSET_PROFILE), ActionType.UPDATED,
                    user, e, assetProfileId.toString());
            throw e;
        }
    }
}
