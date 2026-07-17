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
package org.thingsboard.server.dao.sql;

import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * 中文说明：
 * 1. `JpaPartitionedAbstractDao` 是 ThingsBoard DAO 中负责 `Jpa Partitioned` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `BaseEntity`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@SqlDao
public abstract class JpaPartitionedAbstractDao<E extends BaseEntity<D>, D> extends JpaAbstractDao<E, D> {

    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 功能：执行 `doSave` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `isNew`：`isNew` 参数。
     * 返回：处理结果。
     */
    @Override
    protected E doSave(E entity, boolean isNew) {
        createPartition(entity);
        if (isNew) {
            entityManager.persist(entity);
        } else {
            entity = entityManager.merge(entity);
        }
        return entity;
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    public abstract void createPartition(E entity);

}
