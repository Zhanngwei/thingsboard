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
 * 1. `SqlRelationInsertRepository` 是 ThingsBoard DAO 中负责实体关系存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `RelationInsertRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
