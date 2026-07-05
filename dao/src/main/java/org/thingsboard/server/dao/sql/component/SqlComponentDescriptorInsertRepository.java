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
 * 1. 类目的：`SqlComponentDescriptorInsertRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
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

/*
 * 本类总结：
 * 1. 核心职责：`SqlComponentDescriptorInsertRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
