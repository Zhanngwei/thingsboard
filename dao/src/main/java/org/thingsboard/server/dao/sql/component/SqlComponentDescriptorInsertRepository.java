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

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

/**
 * 中文说明：
 * 1. `SqlComponentDescriptorInsertRepository` 是 ThingsBoard DAO 中负责 `Sql Component Descriptor` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `AbstractComponentDescriptorInsertRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Repository
public class SqlComponentDescriptorInsertRepository extends AbstractComponentDescriptorInsertRepository {

    /**
     * `ID`常量，用于统一引用固定值。
     */
    private static final String ID = "id = :id";
    private static final String CLAZZ_CLAZZ = "clazz = :clazz";

    /**
     * 键常量，用于统一引用固定值。
     */
    private static final String P_KEY_CONFLICT_STATEMENT = "(id)";
    private static final String UNQ_KEY_CONFLICT_STATEMENT = "(clazz)";

    private static final String ON_P_KEY_CONFLICT_UPDATE_STATEMENT = getUpdateStatement(CLAZZ_CLAZZ);
    private static final String ON_UNQ_KEY_CONFLICT_UPDATE_STATEMENT = getUpdateStatement(ID);

    private static final String INSERT_OR_UPDATE_ON_P_KEY_CONFLICT = getInsertOrUpdateStatement(P_KEY_CONFLICT_STATEMENT, ON_P_KEY_CONFLICT_UPDATE_STATEMENT);
    private static final String INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT = getInsertOrUpdateStatement(UNQ_KEY_CONFLICT_STATEMENT, ON_UNQ_KEY_CONFLICT_UPDATE_STATEMENT);

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public ComponentDescriptorEntity saveOrUpdate(ComponentDescriptorEntity entity) {
        return saveAndGet(entity, INSERT_OR_UPDATE_ON_P_KEY_CONFLICT, INSERT_OR_UPDATE_ON_UNQ_KEY_CONFLICT);
    }

    /**
     * 功能：执行 `doProcessSaveOrUpdate` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    @Override
    protected ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        return (ComponentDescriptorEntity) getQuery(entity, query).getSingleResult();
    }

    /**
     * 功能：获取语句。
     * 参数：
     * - `conflictKeyStatement`：键。
     * - `updateKeyStatement`：键。
     * 返回：文本结果。
     */
    private static String getInsertOrUpdateStatement(String conflictKeyStatement, String updateKeyStatement) {
        return "INSERT INTO component_descriptor (id, created_time, actions, clazz, configuration_descriptor, configuration_version, name, scope, type, clustering_mode, has_queue_name) VALUES (:id, :created_time, :actions, :clazz, :configuration_descriptor, :configuration_version, :name, :scope, :type, :clustering_mode, :has_queue_name) ON CONFLICT " + conflictKeyStatement + " DO UPDATE SET " + updateKeyStatement + " returning *";
    }

    /**
     * 功能：获取语句。
     * 参数：
     * - `id`：`id`ID。
     * 返回：文本结果。
     */
    private static String getUpdateStatement(String id) {
        return "actions = :actions, " + id + ",created_time = :created_time, configuration_descriptor = :configuration_descriptor, configuration_version = :configuration_version, name = :name, scope = :scope, type = :type, clustering_mode = :clustering_mode, has_queue_name = :has_queue_name";
    }
}
