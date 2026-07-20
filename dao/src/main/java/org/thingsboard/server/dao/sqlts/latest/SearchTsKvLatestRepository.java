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
package org.thingsboard.server.dao.sqlts.latest;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.latest.TsKvLatestEntity;
import org.thingsboard.server.dao.util.SqlTsLatestAnyDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `SearchTsKvLatestRepository` 是 ThingsBoard DAO 中负责 `Search Ts Kv` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@SqlTsLatestAnyDao
@Repository
public class SearchTsKvLatestRepository {

    /**
     * 实体ID常量，用于统一引用固定值。
     */
    public static final String FIND_ALL_BY_ENTITY_ID = "findAllByEntityId";

    public static final String FIND_ALL_BY_ENTITY_ID_QUERY = "SELECT ts_kv_latest.entity_id AS entityId, ts_kv_latest.key AS key, ts_kv_dictionary.key AS strKey, ts_kv_latest.str_v AS strValue," +
            " ts_kv_latest.bool_v AS boolValue, ts_kv_latest.long_v AS longValue, ts_kv_latest.dbl_v AS doubleValue, ts_kv_latest.json_v AS jsonValue, ts_kv_latest.ts AS ts FROM ts_kv_latest " +
            "INNER JOIN ts_kv_dictionary ON ts_kv_latest.key = ts_kv_dictionary.key_id WHERE ts_kv_latest.entity_id = cast(:id AS uuid)";

    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    public List<TsKvLatestEntity> findAllByEntityId(UUID entityId) {
        return entityManager.createNamedQuery(FIND_ALL_BY_ENTITY_ID, TsKvLatestEntity.class)
                .setParameter("id", entityId)
                .getResultList();
    }

}
