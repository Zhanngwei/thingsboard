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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
/**
 * 中文说明：
 * 1. `ComponentDescriptorRepository` 是 ThingsBoard DAO 中定义 `Component Descriptor` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface ComponentDescriptorRepository extends JpaRepository<ComponentDescriptorEntity, UUID> {

    /**
     * 功能：获取`By Clazz`。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    ComponentDescriptorEntity findByClazz(String clazz);

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT cd FROM ComponentDescriptorEntity cd WHERE cd.type = :type " +
            "AND (:textSearch IS NULL OR ilike(cd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<ComponentDescriptorEntity> findByType(@Param("type") ComponentType type,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    /**
     * 功能：获取类型。
     * 参数：
     * - `type`：类型。
     * - `scope`：`scope` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT cd FROM ComponentDescriptorEntity cd WHERE cd.type = :type AND cd.scope = :scope " +
            "AND (:textSearch IS NULL OR ilike(cd.name, CONCAT('%', :textSearch, '%')) = true)")
    Page<ComponentDescriptorEntity> findByScopeAndType(@Param("type") ComponentType type,
                                                       @Param("scope") ComponentScope scope,
                                                       @Param("textSearch") String textSearch,
                                                       Pageable pageable);

    /**
     * 功能：删除或清理`By Clazz`。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM ComponentDescriptorEntity cd where cd.clazz = :clazz")
    void deleteByClazz(@Param("clazz") String clazz);
}
