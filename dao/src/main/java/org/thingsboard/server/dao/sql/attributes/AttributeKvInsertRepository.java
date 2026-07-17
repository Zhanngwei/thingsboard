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
 * 1. `AttributeKvInsertRepository` 是 ThingsBoard DAO 中负责属性存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
}