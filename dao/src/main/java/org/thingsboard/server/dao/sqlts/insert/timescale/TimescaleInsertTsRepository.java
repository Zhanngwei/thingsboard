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
package org.thingsboard.server.dao.sqlts.insert.timescale;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.sqlts.insert.AbstractInsertRepository;
import org.thingsboard.server.dao.sqlts.insert.InsertTsRepository;
import org.thingsboard.server.dao.util.TimescaleDBTsDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * 中文说明：
 * 1. `TimescaleInsertTsRepository` 是 ThingsBoard DAO 中负责 `Timescale Insert Ts` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `AbstractInsertRepository`、`InsertTsRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@TimescaleDBTsDao
@Repository
@Transactional
public class TimescaleInsertTsRepository extends AbstractInsertRepository implements InsertTsRepository<TimescaleTsKvEntity> {

    private static final String INSERT_OR_UPDATE =
            "INSERT INTO ts_kv (entity_id, key, ts, bool_v, str_v, long_v, dbl_v, json_v) VALUES(?, ?, ?, ?, ?, ?, ?, cast(? AS json)) " +
                    "ON CONFLICT (entity_id, key, ts) DO UPDATE SET bool_v = ?, str_v = ?, long_v = ?, dbl_v = ?, json_v = cast(? AS json);";

    /**
     * 功能：保存或创建`Or Update`。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    @Override
    public void saveOrUpdate(List<TimescaleTsKvEntity> entities) {
        jdbcTemplate.batchUpdate(INSERT_OR_UPDATE, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                TimescaleTsKvEntity tsKvEntity = entities.get(i);
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
