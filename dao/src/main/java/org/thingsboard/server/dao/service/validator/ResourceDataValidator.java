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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.TbResourceDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;

import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;

/**
 * 中文说明：
 * 1. `ResourceDataValidator` 是 ThingsBoard DAO 中负责资源存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class ResourceDataValidator extends DataValidator<TbResource> {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private TbResourceDao resourceDao;

    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetTypeDao widgetTypeDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantService tenantService;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    @Lazy
    private TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resource`：`resource` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, TbResource resource) {
        if (resource.getData() == null || resource.getData().length == 0) {
            throw new DataValidationException("Resource data should be specified");
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resource`：`resource` 参数。
     * 返回：判断结果。
     */
    @Override
    protected TbResource validateUpdate(TenantId tenantId, TbResource resource) {
        if (resource.getData() != null && !resource.getResourceType().isUpdatable() &&
                tenantId != null && !tenantId.isSysTenantId()) {
            throw new DataValidationException("This type of resource can't be updated");
        }
        return resource;
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resource`：`resource` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, TbResource resource) {
        validateString("Resource title", resource.getTitle());
        if (resource.getTenantId() == null) {
            resource.setTenantId(TenantId.SYS_TENANT_ID);
        }
        if (!resource.getTenantId().isSysTenantId()) {
            if (!tenantService.tenantExists(resource.getTenantId())) {
                throw new DataValidationException("Resource is referencing to non-existent tenant!");
            }
        }
        if (resource.getResourceType() == null) {
            throw new DataValidationException("Resource type should be specified!");
        }
        if (resource.getData() != null) {
            validateResourceSize(resource.getTenantId(), resource.getId(), resource.getData().length);
        }
        if (StringUtils.isEmpty(resource.getFileName())) {
            throw new DataValidationException("Resource file name should be specified!");
        }
        if (StringUtils.containsAny(resource.getFileName(), "/", "\\")) {
            throw new DataValidationException("File name contains forbidden symbols");
        }
        if (StringUtils.isEmpty(resource.getResourceKey())) {
            throw new DataValidationException("Resource key should be specified!");
        }
    }

    /**
     * 功能：校验`Resource Size`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * - `dataSize`：待处理数据。
     * 返回：无。
     */
    public void validateResourceSize(TenantId tenantId, TbResourceId resourceId, long dataSize) {
        if (!tenantId.isSysTenantId()) {
            DefaultTenantProfileConfiguration profileConfiguration = tenantProfileCache.get(tenantId).getDefaultProfileConfiguration();
            long maxResourceSize = profileConfiguration.getMaxResourceSize();
            if (maxResourceSize > 0 && dataSize > maxResourceSize) {
                throw new IllegalArgumentException("Resource exceeds the maximum size of " + FileUtils.byteCountToDisplaySize(maxResourceSize));
            }
            long maxSumResourcesDataInBytes = profileConfiguration.getMaxResourcesInBytes();
            if (resourceId != null) {
                long prevSize = resourceDao.getResourceSize(tenantId, resourceId);
                dataSize -= prevSize;
            }
            validateMaxSumDataSizePerTenant(tenantId, resourceDao, maxSumResourcesDataInBytes, dataSize, TB_RESOURCE);
        }
    }

    /**
     * 功能：校验`Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `resourceId`：`resourceId`ID。
     * 返回：无。
     */
    @Override
    public void validateDelete(TenantId tenantId, EntityId resourceId) {
        List<WidgetTypeDetails> widgets = widgetTypeDao.findWidgetTypesInfosByTenantIdAndResourceId(tenantId.getId(),
                resourceId.getId());
        if (!widgets.isEmpty()) {
            List<String> widgetNames = widgets.stream().map(BaseWidgetType::getName).collect(Collectors.toList());
            throw new DataValidationException(String.format("Following widget types uses current resource: %s", widgetNames));
        }
    }

}
