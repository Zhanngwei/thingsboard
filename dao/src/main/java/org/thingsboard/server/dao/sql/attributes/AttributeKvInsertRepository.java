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
package org.thingsboard.server.dao.sql.attributes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. 类目的：`AttributeKvInsertRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Repository
@Slf4j
@SqlDao
public abstract class AttributeKvInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    /**
     * `EMPTY_STR`常量，用于统一引用固定值。
     */
    private static final String EMPTY_STR = "";

    private static final String BATCH_UPDATE = "UPDATE attribute_kv SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ? " +
            "WHERE entity_type = ? and entity_id = ? and attribute_type =? and attribute_key = ?;";

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, str_v, long_v, dbl_v, bool_v, json_v, last_update_ts) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?,  cast(? AS json), ?) " +
                    "ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) " +
                    "DO UPDATE SET str_v = ?, long_v = ?, dbl_v = ?, bool_v = ?, json_v =  cast(? AS json), last_update_ts = ?;";

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
     * 是否移除对应数据。
     */
    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    public void saveOrUpdate(List<AttributeKvEntity> entities) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                int[] result = jdbcTemplate.batchUpdate(BATCH_UPDATE, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AttributeKvEntity kvEntity = entities.get(i);
                        ps.setString(1, replaceNullChars(kvEntity.getStrValue()));

                        if (kvEntity.getLongValue() != null) {
                            ps.setLong(2, kvEntity.getLongValue());
                        } else {
                            ps.setNull(2, Types.BIGINT);
                        }

                        if (kvEntity.getDoubleValue() != null) {
                            ps.setDouble(3, kvEntity.getDoubleValue());
                        } else {
                            ps.setNull(3, Types.DOUBLE);
                        }

                        if (kvEntity.getBooleanValue() != null) {
                            ps.setBoolean(4, kvEntity.getBooleanValue());
                        } else {
                            ps.setNull(4, Types.BOOLEAN);
                        }

                        ps.setString(5, replaceNullChars(kvEntity.getJsonValue()));

                        ps.setLong(6, kvEntity.getLastUpdateTs());
                        ps.setString(7, kvEntity.getId().getEntityType().name());
                        ps.setObject(8, kvEntity.getId().getEntityId());
                        ps.setString(9, kvEntity.getId().getAttributeType());
                        ps.setString(10, kvEntity.getId().getAttributeKey());
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

                List<AttributeKvEntity> insertEntities = new ArrayList<>(updatedCount);
                for (int i = 0; i < result.length; i++) {
                    if (result[i] == 0) {
                        insertEntities.add(entities.get(i));
                    }
                }

                jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        AttributeKvEntity kvEntity = insertEntities.get(i);
                        ps.setString(1, kvEntity.getId().getEntityType().name());
                        ps.setObject(2, kvEntity.getId().getEntityId());
                        ps.setString(3, kvEntity.getId().getAttributeType());
                        ps.setString(4, kvEntity.getId().getAttributeKey());

                        ps.setString(5, replaceNullChars(kvEntity.getStrValue()));
                        ps.setString(11, replaceNullChars(kvEntity.getStrValue()));

                        if (kvEntity.getLongValue() != null) {
                            ps.setLong(6, kvEntity.getLongValue());
                            ps.setLong(12, kvEntity.getLongValue());
                        } else {
                            ps.setNull(6, Types.BIGINT);
                            ps.setNull(12, Types.BIGINT);
                        }

                        if (kvEntity.getDoubleValue() != null) {
                            ps.setDouble(7, kvEntity.getDoubleValue());
                            ps.setDouble(13, kvEntity.getDoubleValue());
                        } else {
                            ps.setNull(7, Types.DOUBLE);
                            ps.setNull(13, Types.DOUBLE);
                        }

                        if (kvEntity.getBooleanValue() != null) {
                            ps.setBoolean(8, kvEntity.getBooleanValue());
                            ps.setBoolean(14, kvEntity.getBooleanValue());
                        } else {
                            ps.setNull(8, Types.BOOLEAN);
                            ps.setNull(14, Types.BOOLEAN);
                        }

                        ps.setString(9, replaceNullChars(kvEntity.getJsonValue()));
                        ps.setString(15, replaceNullChars(kvEntity.getJsonValue()));

                        ps.setLong(10, kvEntity.getLastUpdateTs());
                        ps.setLong(16, kvEntity.getLastUpdateTs());
                    }

                    @Override
                    public int getBatchSize() {
                        return insertEntities.size();
                    }
                });
            }
        });
    }

    /**
     * 功能：执行 `replaceNullChars` 对应的处理。
     * 参数：
     * - `strValue`：值。
     * 返回：文本结果。
     */
    private String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }

/*
 * 本类总结：
 * 1. 核心职责：`AttributeKvInsertRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
}