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
package org.thingsboard.server.service.asset;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.asset.TbAssetService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.importing.csv.AbstractBulkImportService;

import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `AssetBulkImportService` 是 ThingsBoard Application 中负责资产的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractBulkImportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetBulkImportService extends AbstractBulkImportService<Asset> {
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetService assetService;
    private final TbAssetService tbAssetService;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetProfileService assetProfileService;

    /**
     * 功能：更新实体。
     * 参数：
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：无。
     */
    @Override
    protected void setEntityFields(Asset entity, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
            }
        });
        entity.setAdditionalInfo(additionalInfo);
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：匹配的数据集合。
     */
    @Override
    @SneakyThrows
    protected Asset saveEntity(SecurityUser user, Asset entity, Map<BulkImportColumnType, String> fields) {
        AssetProfile assetProfile;
        if (StringUtils.isNotEmpty(entity.getType())) {
            assetProfile = assetProfileService.findOrCreateAssetProfile(entity.getTenantId(), entity.getType());
        } else {
            assetProfile = assetProfileService.findDefaultAssetProfile(entity.getTenantId());
        }
        entity.setAssetProfileId(assetProfile.getId());
        return tbAssetService.save(entity, user);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Asset findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(assetService.findAssetByTenantIdAndName(tenantId, name))
                .orElseGet(Asset::new);
    }

    /**
     * 功能：更新`Owners`。
     * 参数：
     * - `entity`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    protected void setOwners(Asset entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
