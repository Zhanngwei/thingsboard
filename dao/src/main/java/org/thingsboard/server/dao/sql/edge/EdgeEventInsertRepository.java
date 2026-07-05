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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


/**
 * 中文说明：
 * 1. 类目的：`EdgeEventInsertRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
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
public class EdgeEventInsertRepository {

    private static final String INSERT =
            "INSERT INTO edge_event (id, created_time, edge_id, edge_event_type, edge_event_uid, entity_id, edge_event_action, body, tenant_id, ts) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT DO NOTHING;";

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 事务执行模板，表示当前对象的对应属性。
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    protected void save(List<EdgeEventEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        EdgeEventEntity edgeEvent = entities.get(i);
                        ps.setObject(1, edgeEvent.getId());
                        ps.setLong(2, edgeEvent.getCreatedTime());
                        ps.setObject(3, edgeEvent.getEdgeId());
                        ps.setString(4, edgeEvent.getEdgeEventType().name());
                        ps.setString(5, edgeEvent.getEdgeEventUid());
                        ps.setObject(6, edgeEvent.getEntityId());
                        ps.setString(7, edgeEvent.getEdgeEventAction().name());
                        ps.setString(8, edgeEvent.getEntityBody() != null
                                ? edgeEvent.getEntityBody().toString()
                                : null);
                        ps.setObject(9, edgeEvent.getTenantId());
                        ps.setLong(10, edgeEvent.getTs());
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });
            }
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeEventInsertRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
