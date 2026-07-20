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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.BaseAssetService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. `AssetDataValidator` 是 ThingsBoard DAO 中负责资产存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class AssetDataValidator extends DataValidator<Asset> {

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetDao assetDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private TenantService tenantService;

    /**
     * 客户，用于读取或保存对应领域对象。
     */
    @Autowired
    private CustomerDao customerDao;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, Asset asset) {
        if (!BaseAssetService.TB_SERVICE_QUEUE.equals(asset.getType())) {
            validateNumberOfEntitiesPerTenant(tenantId, EntityType.ASSET);
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * 返回：判断结果。
     */
    @Override
    protected Asset validateUpdate(TenantId tenantId, Asset asset) {
        Asset old = assetDao.findById(asset.getTenantId(), asset.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset!");
        }
        return old;
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `asset`：`asset` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, Asset asset) {
        validateString("Asset name", asset.getName());
        if (asset.getTenantId() == null) {
            throw new DataValidationException("Asset should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(asset.getTenantId())) {
                throw new DataValidationException("Asset is referencing to non-existent tenant!");
            }
        }
        if (asset.getCustomerId() == null) {
            asset.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign asset to non-existent customer!");
            }
            if (!customer.getTenantId().equals(asset.getTenantId())) {
                throw new DataValidationException("Can't assign asset to customer from different tenant!");
            }
        }
    }
}
