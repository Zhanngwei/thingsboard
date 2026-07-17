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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * 中文说明：
 * 1. `AbstractComponentDescriptorInsertRepository` 是 ThingsBoard DAO 中负责 `Component Descriptor Insert` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `ComponentDescriptorInsertRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
public abstract class AbstractComponentDescriptorInsertRepository implements ComponentDescriptorInsertRepository {

    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    protected PlatformTransactionManager transactionManager;

    /**
     * 功能：保存或创建`And Get`。
     * 参数：
     * - `entity`：实体对象。
     * - `insertOrUpdateOnPrimaryKeyConflict`：键。
     * - `insertOrUpdateOnUniqueKeyConflict`：键。
     * 返回：处理结果。
     */
    protected ComponentDescriptorEntity saveAndGet(ComponentDescriptorEntity entity, String insertOrUpdateOnPrimaryKeyConflict, String insertOrUpdateOnUniqueKeyConflict) {
        ComponentDescriptorEntity componentDescriptorEntity = null;
        TransactionStatus insertTransaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRED);
        try {
            componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnPrimaryKeyConflict);
            transactionManager.commit(insertTransaction);
        } catch (Throwable throwable) {
            transactionManager.rollback(insertTransaction);
            if (throwable.getCause() instanceof ConstraintViolationException) {
                log.trace("Insert request leaded in a violation of a defined integrity constraint {} for Component Descriptor with id {}, name {} and entityType {}", throwable.getMessage(), entity.getUuid(), entity.getName(), entity.getType());
                TransactionStatus transaction = getTransactionStatus(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                try {
                    componentDescriptorEntity = processSaveOrUpdate(entity, insertOrUpdateOnUniqueKeyConflict);
                    transactionManager.commit(transaction);
                } catch (Throwable th) {
                    log.trace("Could not execute the update statement for Component Descriptor with id {}, name {} and entityType {}", entity.getUuid(), entity.getName(), entity.getType());
                    transactionManager.rollback(transaction);
                }
            } else {
                log.trace("Could not execute the insert statement for Component Descriptor with id {}, name {} and entityType {}", entity.getUuid(), entity.getName(), entity.getType());
            }
        }
        return componentDescriptorEntity;
    }

    /**
     * 功能：执行 `doProcessSaveOrUpdate` 对应的处理。
     * 参数：
     * - `entity`：实体对象。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    @Modifying
    protected abstract ComponentDescriptorEntity doProcessSaveOrUpdate(ComponentDescriptorEntity entity, String query);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `entity`：实体对象。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    protected Query getQuery(ComponentDescriptorEntity entity, String query) {
        return entityManager.createNativeQuery(query, ComponentDescriptorEntity.class)
                .setParameter("id", entity.getUuid())
                .setParameter("created_time", entity.getCreatedTime())
                .setParameter("actions", entity.getActions())
                .setParameter("clazz", entity.getClazz())
                .setParameter("configuration_descriptor", entity.getConfigurationDescriptor().toString())
                .setParameter("configuration_version", entity.getConfigurationVersion())
                .setParameter("name", entity.getName())
                .setParameter("scope", entity.getScope().name())
                .setParameter("type", entity.getType().name())
                .setParameter("clustering_mode", entity.getClusteringMode().name())
                .setParameter("has_queue_name", entity.isHasQueueName());
    }

    /**
     * 功能：处理`Save Or Update`。
     * 参数：
     * - `entity`：实体对象。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    private ComponentDescriptorEntity processSaveOrUpdate(ComponentDescriptorEntity entity, String query) {
        return doProcessSaveOrUpdate(entity, query);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `propagationRequired`：`propagationRequired` 参数。
     * 返回：处理结果。
     */
    private TransactionStatus getTransactionStatus(int propagationRequired) {
        DefaultTransactionDefinition insertDefinition = new DefaultTransactionDefinition();
        insertDefinition.setPropagationBehavior(propagationRequired);
        return transactionManager.getTransaction(insertDefinition);
    }
}
