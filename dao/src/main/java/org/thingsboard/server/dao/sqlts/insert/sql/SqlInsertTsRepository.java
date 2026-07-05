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
package org.thingsboard.server.dao.sqlts.insert.sql;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`SqlInsertTsRepository` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@SqlTsDao
@Repository
@Transactional
public class SqlInsertTsRepository extends AbstractInsertRepository implements InsertTsRepository<TsKvEntity> {

    private static final String INSERT_ON_CONFLICT_DO_UPDATE = "INSERT INTO ts_kv (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) VALUES (?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
            "ON CONFLICT (entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json);";

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveOrUpdate(List<TsKvEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_ON_CONFLICT_DO_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TsKvEntity tsKvEntity = entities.get(i);
                ps.setObject(1, tsKvEntity.getEntityId());
                ps.setInt(2, tsKvEntity.getKey());
                ps.setLong(3, tsKvEntity.getTs());

                if (tsKvEntity.getBooleanValue() != null) {
                    ps.setBoolean(4, tsKvEntity.getBooleanValue());
                    ps.setBoolean(9, tsKvEntity.getBooleanValue());
                } else {
                    ps.setNull(4, Types.BOOLEAN);
                    ps.setNull(9, Types.BOOLEAN);
                }

                ps.setString(5, replaceNullChars(tsKvEntity.getStrValue()));
                ps.setString(10, replaceNullChars(tsKvEntity.getStrValue()));


                if (tsKvEntity.getLongValue() != null) {
                    ps.setLong(6, tsKvEntity.getLongValue());
                    ps.setLong(11, tsKvEntity.getLongValue());
                } else {
                    ps.setNull(6, Types.BIGINT);
                    ps.setNull(11, Types.BIGINT);
                }

                if (tsKvEntity.getDoubleValue() != null) {
                    ps.setDouble(7, tsKvEntity.getDoubleValue());
                    ps.setDouble(12, tsKvEntity.getDoubleValue());
                } else {
                    ps.setNull(7, Types.DOUBLE);
                    ps.setNull(12, Types.DOUBLE);
                }

                ps.setString(8, replaceNullChars(tsKvEntity.getJsonValue()));
                ps.setString(13, replaceNullChars(tsKvEntity.getJsonValue()));
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
 * 1. 核心职责：`SqlInsertTsRepository` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
