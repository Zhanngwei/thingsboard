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
package org.thingsboard.server.dao.sqlts.timescale;

import org.springframework.stereotype.Repository;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AggregationRepository` 是 ThingsBoard DAO 中负责 `Aggregation` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Repository
@TimescaleDBTsOrTsLatestDao
public class AggregationRepository {

    /**
     * `FIND_AVG`常量，用于统一引用固定值。
     */
    public static final String FIND_AVG = "findAvg";
    public static final String FIND_MAX = "findMax";
    /**
     * `FIND_MIN`常量，用于统一引用固定值。
     */
    public static final String FIND_MIN = "findMin";
    public static final String FIND_SUM = "findSum";
    /**
     * 数量常量，用于统一引用固定值。
     */
    public static final String FIND_COUNT = "findCount";

    public static final String FROM_WHERE_CLAUSE = "FROM ts_kv tskv WHERE " +
            "tskv.entity_id = cast(:entityId AS uuid) " +
            "AND tskv.key= cast(:entityKey AS int) " +
            "AND tskv.ts >= :startTs AND tskv.ts < :endTs " +
            "GROUP BY tskv.entity_id, tskv.key, tsBucket " +
            "ORDER BY tskv.entity_id, tskv.key, tsBucket";

    public static final String FIND_AVG_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, " +
            "SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, 'AVG' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MAX_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MAX(COALESCE(tskv.long_v, -9223372036854775807)) AS longValue, " +
            "MAX(COALESCE(tskv.dbl_v, -1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MAX(tskv.str_v) AS strValue, 'MAX' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_MIN_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "MIN(COALESCE(tskv.long_v, 9223372036854775807)) AS longValue, " +
            "MIN(COALESCE(tskv.dbl_v, 1.79769E+308)) as doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "MIN(tskv.str_v) AS strValue, 'MIN' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_SUM_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(COALESCE(tskv.long_v, 0)) AS longValue, SUM(COALESCE(tskv.dbl_v, 0.0)) AS doubleValue, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longCountValue, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleCountValue, " +
            "null AS strValue, null AS jsonValue, 'SUM' AS aggType, MAX(tskv.ts) AS maxAggTs ";

    public static final String FIND_COUNT_QUERY = "SELECT " +
            "time_bucket(:timeBucket, tskv.ts, :startTs) AS tsBucket, :timeBucket AS interval, " +
            "SUM(CASE WHEN tskv.bool_v IS NULL THEN 0 ELSE 1 END) AS booleanValueCount, " +
            "SUM(CASE WHEN tskv.str_v IS NULL THEN 0 ELSE 1 END) AS strValueCount, " +
            "SUM(CASE WHEN tskv.long_v IS NULL THEN 0 ELSE 1 END) AS longValueCount, " +
            "SUM(CASE WHEN tskv.dbl_v IS NULL THEN 0 ELSE 1 END) AS doubleValueCount, " +
            "SUM(CASE WHEN tskv.json_v IS NULL THEN 0 ELSE 1 END) AS jsonValueCount, " +
            "MAX(tskv.ts) AS maxAggTs ";

    /**
     * 实体，负责处理对应任务或消息。
     */
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 功能：获取`Avg`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findAvg(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_AVG);
    }

    /**
     * 功能：获取`Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMax(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MAX);
    }

    /**
     * 功能：获取`Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findMin(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_MIN);
    }

    /**
     * 功能：获取`Sum`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findSum(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_SUM);
    }

    /**
     * 功能：获取数量。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public List<TimescaleTsKvEntity> findCount(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs) {
        return getResultList(entityId, entityKey, timeBucket, startTs, endTs, FIND_COUNT);
    }

    /**
     * 功能：获取`Result List`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `timeBucket`：`timeBucket` 参数。
     * - `startTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private List getResultList(UUID entityId, int entityKey, long timeBucket, long startTs, long endTs, String query) {
        return entityManager.createNamedQuery(query)
                .setParameter("entityId", entityId)
                .setParameter("entityKey", entityKey)
                .setParameter("timeBucket", timeBucket)
                .setParameter("startTs", startTs)
                .setParameter("endTs", endTs)
                .getResultList();
    }

}
