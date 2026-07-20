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
package org.thingsboard.server.dao.sql.component;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `JpaBaseComponentDescriptorDao` 是 ThingsBoard DAO 中负责 `Jpa Component Descriptor` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`ComponentDescriptorDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class JpaBaseComponentDescriptorDao extends JpaAbstractDao<ComponentDescriptorEntity, ComponentDescriptor>
        implements ComponentDescriptorDao {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private ComponentDescriptorRepository componentDescriptorRepository;

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    @Autowired
    private ComponentDescriptorInsertRepository componentDescriptorInsertRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<ComponentDescriptorEntity> getEntityClass() {
        return ComponentDescriptorEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<ComponentDescriptorEntity, UUID> getRepository() {
        return componentDescriptorRepository;
    }

    /**
     * 功能：保存或创建`If Not Exist`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(TenantId tenantId, ComponentDescriptor component) {
        if (component.getId() == null) {
            UUID uuid = Uuids.timeBased();
            component.setId(new ComponentDescriptorId(uuid));
            component.setCreatedTime(Uuids.unixTimestamp(uuid));
        }
        if (!componentDescriptorRepository.existsById(component.getId().getId())) {
            ComponentDescriptorEntity componentDescriptorEntity = new ComponentDescriptorEntity(component);
            ComponentDescriptorEntity savedEntity = componentDescriptorInsertRepository.saveOrUpdate(componentDescriptorEntity);
            return Optional.of(savedEntity.toData());
        }
        return Optional.empty();
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
        return findById(tenantId, componentId.getId());
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
        return DaoUtil.getData(componentDescriptorRepository.findByClazz(clazz));
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
        return DaoUtil.toPageData(componentDescriptorRepository
                .findByType(
                        type,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
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
        return DaoUtil.toPageData(componentDescriptorRepository
                .findByScopeAndType(
                        type,
                        scope,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `componentId`：`componentId`ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteById(TenantId tenantId, ComponentDescriptorId componentId) {
        removeById(tenantId, componentId.getId());
    }

    /**
     * 功能：删除或清理`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteByClazz(TenantId tenantId, String clazz) {
        componentDescriptorRepository.deleteByClazz(clazz);
    }
}
