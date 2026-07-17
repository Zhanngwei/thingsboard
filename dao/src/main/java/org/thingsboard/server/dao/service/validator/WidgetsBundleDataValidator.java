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
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

/**
 * 中文说明：
 * 1. `WidgetsBundleDataValidator` 是 ThingsBoard DAO 中负责 `Widgets Bundle Validator` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@AllArgsConstructor
public class WidgetsBundleDataValidator extends DataValidator<WidgetsBundle> {

    /**
     * 部件包，用于读取或保存对应领域对象。
     */
    private final WidgetsBundleDao widgetsBundleDao;
    private final TenantService tenantService;

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, WidgetsBundle widgetsBundle) {
        validateString("Widgets bundle title", widgetsBundle.getTitle());
        if (widgetsBundle.getTenantId() == null) {
            widgetsBundle.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(widgetsBundle.getTenantId())) {
                throw new DataValidationException("Widgets bundle is referencing to non-existent tenant!");
            }
        }
    }

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, WidgetsBundle widgetsBundle) {
        String alias = widgetsBundle.getAlias();
        if (alias == null || alias.trim().isEmpty()) {
            alias = widgetsBundle.getTitle().toLowerCase().replaceAll("\\W+", "_");
        }
        String originalAlias = alias;
        int c = 1;
        WidgetsBundle withSameAlias;
        do {
            withSameAlias = widgetsBundleDao.findWidgetsBundleByTenantIdAndAlias(widgetsBundle.getTenantId().getId(), alias);
            if (withSameAlias != null) {
                alias = originalAlias + (++c);
            }
        } while (withSameAlias != null);
        widgetsBundle.setAlias(alias);
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：判断结果。
     */
    @Override
    protected WidgetsBundle validateUpdate(TenantId tenantId, WidgetsBundle widgetsBundle) {
        WidgetsBundle storedWidgetsBundle = widgetsBundleDao.findById(tenantId, widgetsBundle.getId().getId());
        if (!storedWidgetsBundle.getTenantId().getId().equals(widgetsBundle.getTenantId().getId())) {
            throw new DataValidationException("Can't move existing widgets bundle to different tenant!");
        }
        if (!storedWidgetsBundle.getAlias().equals(widgetsBundle.getAlias())) {
            throw new DataValidationException("Update of widgets bundle alias is prohibited!");
        }
        return storedWidgetsBundle;
    }
}
