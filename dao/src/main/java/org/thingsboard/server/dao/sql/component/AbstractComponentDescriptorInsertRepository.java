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
 * 1. 类目的：`AbstractComponentDescriptorInsertRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`AbstractComponentDescriptorInsertRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
