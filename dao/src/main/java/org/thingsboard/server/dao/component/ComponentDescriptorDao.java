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

import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.Dao;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `ComponentDescriptorDao` 是 ThingsBoard DAO 中定义 `Component Descriptor` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Dao`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ComponentDescriptorDao extends Dao<ComponentDescriptor> {

    /**
     * 功能：保存或创建`If Not Exist`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `component`：`component` 参数。
     * 返回：可能存在的结果。
     */
    Optional<ComponentDescriptor> saveIfNotExist(TenantId tenantId, ComponentDescriptor component);

    /**
     * 功能：获取`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `componentId`：`componentId`ID。
     * 返回：处理结果。
     */
    ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId);

    /**
     * 功能：获取`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    ComponentDescriptor findByClazz(TenantId tenantId, String clazz);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink);

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scope`：`scope` 参数。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink);

    /**
     * 功能：删除或清理`By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `componentId`：`componentId`ID。
     * 返回：无。
     */
    void deleteById(TenantId tenantId, ComponentDescriptorId componentId);

    /**
     * 功能：删除或清理`By Clazz`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    void deleteByClazz(TenantId tenantId, String clazz);

}
