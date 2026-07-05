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
package org.thingsboard.server.dao.sqlts.insert.latest.sql;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.latest.InsertLatestTsRepository;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


/**
 * 中文说明：
 * 1. 类目的：`SqlLatestInsertTsRepository` 是 ThingsBoard DAO 模块 中的时序数据持久化类型，用于处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 生命周期：由遥测写入、历史查询、聚合查询或测试流程按请求调用，并受数据库连接池和事务管理约束。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Strategy / Template。
 */
@SqlTsLatestAnyDao
@Repository
@Transactional
@SqlDao
public class SqlLatestInsertTsRepository extends AbstractInsertRepository implements InsertLatestTsRepository {

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Value("${sql.ts_latest.update_by_latest_ts:true}")
    private Boolean updateByLatestTs;

    private static final String BATCH_UPDATE =
            "UPDATE ts_kv_latest SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json) WHERE entity_id = ? AND key = ?";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO ts_kv_latest (entity_id, key, ts, bool_v, str_v, long_v, dbl_v,  json_v) VALUES(?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
                    "ON CONFLICT (entity_id, key) DO UPDATE SET ts = ?, bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json)";

    /**
     * 时间戳常量，用于统一引用固定值。
     */
    private static final String BATCH_UPDATE_BY_LATEST_TS = BATCH_UPDATE + " AND ts_kv_latest.ts <= ?";

    /**
     * 时间戳常量，用于统一引用固定值。
     */
    private static final String INSERT_OR_UPDATE_BY_LATEST_TS = INSERT_OR_UPDATE + " WHERE ts_kv_latest.ts <= ?";

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveOrUpdate(List<TsKvLatestEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                String batchUpdateQuery = updateByLatestTs ? BATCH_UPDATE_BY_LATEST_TS : BATCH_UPDATE;
                String insertOrUpdateQuery = updateByLatestTs ? INSERT_OR_UPDATE_BY_LATEST_TS : INSERT_OR_UPDATE;

                int[] result = jdbcTemplate.batchUpdate(batchUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TsKvLatestEntity tsKvLatestEntity = entities.get(i);
                        ps.setLong(1, tsKvLatestEntity.getTs());

                        if (tsKvLatestEntity.getBooleanValue() != null) {
                            ps.setBoolean(2, tsKvLatestEntity.getBooleanValue());
                        } else {
                            ps.setNull(2, Types.BOOLEAN);
                        }

                        ps.setString(3, replaceNullChars(tsKvLatestEntity.getStrValue()));

                        if (tsKvLatestEntity.getLongValue() != null) {
                            ps.setLong(4, tsKvLatestEntity.getLongValue());
                        } else {
                            ps.setNull(4, Types.BIGINT);
                        }

                        if (tsKvLatestEntity.getDoubleValue() != null) {
                            ps.setDouble(5, tsKvLatestEntity.getDoubleValue());
                        } else {
                            ps.setNull(5, Types.DOUBLE);
                        }

                        ps.setString(6, replaceNullChars(tsKvLatestEntity.getJsonValue()));

                        ps.setObject(7, tsKvLatestEntity.getEntityId());
                        ps.setInt(8, tsKvLatestEntity.getKey());
                        if (updateByLatestTs) {
                            ps.setLong(9, tsKvLatestEntity.getTs());
                        }
                    }

                    @Override
                    public int getBatchSize() {
                        return entities.size();
                    }
                });

                int updatedCount = 0;
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        updatedCount++;
                    }
                }

                List<TsKvLatestEntity> insertEntities = new ArrayList<>(updatedCount);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        insertEntities.add(entities.get(i));
                    }
                }

                jdbcTemplate.batchUpdate(insertOrUpdateQuery, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        TsKvLatestEntity tsKvLatestEntity = insertEntities.get(i);
                        ps.setObject(1, tsKvLatestEntity.getEntityId());
                        ps.setInt(2, tsKvLatestEntity.getKey());

                        ps.setLong(3, tsKvLatestEntity.getTs());
                        ps.setLong(9, tsKvLatestEntity.getTs());
                        if (updateByLatestTs) {
                            ps.setLong(15, tsKvLatestEntity.getTs());
                        }

                        if (tsKvLatestEntity.getBooleanValue() != null) {
                            ps.setBoolean(4, tsKvLatestEntity.getBooleanValue());
                            ps.setBoolean(10, tsKvLatestEntity.getBooleanValue());
                        } else {
                            ps.setNull(4, Types.BOOLEAN);
                            ps.setNull(10, Types.BOOLEAN);
                        }

                        ps.setString(5, replaceNullChars(tsKvLatestEntity.getStrValue()));
                        ps.setString(11, replaceNullChars(tsKvLatestEntity.getStrValue()));

                        if (tsKvLatestEntity.getLongValue() != null) {
                            ps.setLong(6, tsKvLatestEntity.getLongValue());
                            ps.setLong(12, tsKvLatestEntity.getLongValue());
                        } else {
                            ps.setNull(6, Types.BIGINT);
                            ps.setNull(12, Types.BIGINT);
                        }

                        if (tsKvLatestEntity.getDoubleValue() != null) {
                            ps.setDouble(7, tsKvLatestEntity.getDoubleValue());
                            ps.setDouble(13, tsKvLatestEntity.getDoubleValue());
                        } else {
                            ps.setNull(7, Types.DOUBLE);
                            ps.setNull(13, Types.DOUBLE);
                        }

                        ps.setString(8, replaceNullChars(tsKvLatestEntity.getJsonValue()));
                        ps.setString(14, replaceNullChars(tsKvLatestEntity.getJsonValue()));
                    }

                    @Override
                    public int getBatchSize() {
                        return insertEntities.size();
                    }
                });
            }
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SqlLatestInsertTsRepository` 在 ThingsBoard DAO 模块 中承担时序数据持久化类型职责，核心目的是处理遥测键值、最新值、历史分区、聚合查询和 Timescale/PostgreSQL 时序读写。
 * 2. 核心流程：根据实体、键、时间窗口和聚合参数选择存储路径，执行批量写入或查询后返回时序数据。
 * 3. 关键依赖：主要依赖或协作对象包括TimeseriesService、SQL/Timescale DAO、Cassandra DAO、Queue、Rule Engine 和 Transport 上报链路。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
