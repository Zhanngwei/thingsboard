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

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

/**
 * 中文说明：
 * 1. `WidgetTypeDataValidator` 是 ThingsBoard DAO 中负责部件存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@AllArgsConstructor
public class WidgetTypeDataValidator extends DataValidator<WidgetTypeDetails> {

    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    private final WidgetTypeDao widgetTypeDao;
    private final WidgetsBundleDao widgetsBundleDao;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeDetails`：类型。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        validateString("Widgets type name", widgetTypeDetails.getName());
        if (widgetTypeDetails.getDescriptor() == null || widgetTypeDetails.getDescriptor().size() == 0) {
            throw new DataValidationException("Widgets type descriptor can't be empty!");
        }
        if (widgetTypeDetails.getTenantId() == null) {
            widgetTypeDetails.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!widgetTypeDetails.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(widgetTypeDetails.getTenantId())) {
                throw new DataValidationException("Widget type is referencing to non-existent tenant!");
            }
        }
    }

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeDetails`：类型。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        String fqn = widgetTypeDetails.getFqn();
        if (fqn == null || fqn.trim().isEmpty()) {
            fqn = widgetTypeDetails.getName().toLowerCase().replaceAll("\\W+", "_");
        }
        String originalFqn = fqn;
        int c = 1;
        WidgetType withSameFqn;
        do {
            withSameFqn = widgetTypeDao.findByTenantIdAndFqn(widgetTypeDetails.getTenantId().getId(), fqn);
            if (withSameFqn != null) {
                fqn = originalFqn + (++c);
            }
        } while (withSameFqn != null);
        widgetTypeDetails.setFqn(fqn);
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeDetails`：类型。
     * 返回：判断结果。
     */
    @Override
    protected WidgetTypeDetails validateUpdate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        WidgetTypeDetails storedWidgetType = widgetTypeDao.findById(tenantId, widgetTypeDetails.getId().getId());
        if (!storedWidgetType.getTenantId().getId().equals(widgetTypeDetails.getTenantId().getId())) {
            throw new DataValidationException("Can't move existing widget type to different tenant!");
        }
        if (!storedWidgetType.getFqn().equals(widgetTypeDetails.getFqn())) {
            throw new DataValidationException("Update of widget type fqn is prohibited!");
        }
        return new WidgetTypeDetails(storedWidgetType);
    }
}
