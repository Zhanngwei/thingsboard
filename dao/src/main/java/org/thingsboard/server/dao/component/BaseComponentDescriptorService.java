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
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `BaseComponentDescriptorService` 是 ThingsBoard DAO 中负责 `Component Descriptor` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `ComponentDescriptorService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
public class BaseComponentDescriptorService implements ComponentDescriptorService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    /**
     * 校验器，封装可复用的处理规则。
     */
    @Autowired
    private DataValidator<ComponentDescriptor> componentValidator;

    /**
     * 功能：保存或创建`Component`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor saveComponent(TenantId tenantId, ComponentDescriptor component) {
        componentValidator.validate(component, data -> TenantId.SYS_TENANT_ID);
        Optional<ComponentDescriptor> result = componentDescriptorDao.saveIfNotExist(tenantId, component);
        return result.orElseGet(() -> componentDescriptorDao.findByClazz(tenantId, component.getClazz()));
    }

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `componentId`：`componentId`ID。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId) {
        Validator.validateId(componentId, "Incorrect component id for search request.");
        return componentDescriptorDao.findById(tenantId, componentId);
    }

    /**
     * 功能：获取`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for search request.");
        return componentDescriptorDao.findByClazz(tenantId, clazz);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByTypeAndPageLink(tenantId, type, pageLink);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scope`：`scope` 参数。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByScopeAndTypeAndPageLink(tenantId, scope, type, pageLink);
    }

    /**
     * 功能：删除或清理`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Override
    public void deleteByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for delete request.");
        componentDescriptorDao.deleteByClazz(tenantId, clazz);
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * - `configuration`：配置对象。
     * 返回：判断结果。
     */
    @Override
    public boolean validate(TenantId tenantId, ComponentDescriptor component, JsonNode configuration) {
        JsonValidator validator = JsonSchemaFactory.byDefault().getValidator();
        try {
            if (!component.getConfigurationDescriptor().has("schema")) {
                throw new DataValidationException("Configuration descriptor doesn't contain schema property!");
            }
            JsonNode configurationSchema = component.getConfigurationDescriptor().get("schema");
            ProcessingReport report = validator.validate(configurationSchema, configuration);
            return report.isSuccess();
        } catch (ProcessingException e) {
            throw new IncorrectParameterException(e.getMessage(), e);
        }
    }
}
