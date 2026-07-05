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
package org.thingsboard.server.dao.sql.relation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.dao.model.sql.RelationEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`SqlRelationInsertRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Repository
@Transactional
public class SqlRelationInsertRepository implements RelationInsertRepository {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JPA = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
            " VALUES (:fromId, :fromType, :toId, :toType, :relationTypeGroup, :relationType, :additionalInfo) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = :additionalInfo returning *";

    private static final String INSERT_ON_CONFLICT_DO_UPDATE_JDBC = "INSERT INTO relation (from_id, from_type, to_id, to_type, relation_type_group, relation_type, additional_info)" +
            " VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (from_id, from_type, relation_type_group, relation_type, to_id, to_type) DO UPDATE SET additional_info = ?";


    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `entity`：实体对象。
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    protected Query getQuery(RelationEntity entity, String query) {
        Query nativeQuery = entityManager.createNativeQuery(query, RelationEntity.class);
        if (entity.getAdditionalInfo() == null) {
            nativeQuery.setParameter("additionalInfo", null);
        } else {
            nativeQuery.setParameter("additionalInfo", JacksonUtil.toString(entity.getAdditionalInfo()));
        }
        return nativeQuery
                .setParameter("fromId", entity.getFromId())
                .setParameter("fromType", entity.getFromType())
                .setParameter("toId", entity.getToId())
                .setParameter("toType", entity.getToType())
                .setParameter("relationTypeGroup", entity.getRelationTypeGroup())
                .setParameter("relationType", entity.getRelationType());
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public RelationEntity saveOrUpdate(RelationEntity entity) {
        return (RelationEntity) getQuery(entity, INSERT_ON_CONFLICT_DO_UPDATE_JPA).getSingleResult();
    }

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveOrUpdate(List<RelationEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_ON_CONFLICT_DO_UPDATE_JDBC, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RelationEntity relation = entities.get(i);
                ps.setObject(1, relation.getFromId());
                ps.setString(2, relation.getFromType());
                ps.setObject(3, relation.getToId());
                ps.setString(4, relation.getToType());

                ps.setString(5, relation.getRelationTypeGroup());
                ps.setString(6, relation.getRelationType());

                if (relation.getAdditionalInfo() == null) {
                    ps.setString(7, null);
                    ps.setString(8, null);
                } else {
                    String json = JacksonUtil.toString(relation.getAdditionalInfo());
                    ps.setString(7, json);
                    ps.setString(8, json);
                }
            }

            @Override
            public int getBatchSize() {
                return entities.size();
            }
        });
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SqlRelationInsertRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
