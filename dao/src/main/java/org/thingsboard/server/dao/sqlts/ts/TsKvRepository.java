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
package org.thingsboard.server.dao.sqlts.ts;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TsKvRepository` 是 ThingsBoard DAO 中定义 `Ts Kv` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TsKvRepository extends JpaRepository<TsKvEntity, TsKvCompositeKey> {

    /*
    * Using native query to avoid adding 'nulls first' or 'nulls last' (ignoring spring.jpa.properties.hibernate.order_by.default_null_ordering)
    * to the order so that index scan is done instead of full scan.
    *
    * Note: even when setting custom NullHandling for the Sort.Order for non-native queries,
    * it will be ignored and default_null_ordering will be used
    * */
    /**
     * 功能：获取数量限制。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT * FROM ts_kv WHERE entity_id = :entityId " +
            "AND key = :entityKey AND ts >= :startTs AND ts < :endTs ", nativeQuery = true)
    List<TsKvEntity> findAllWithLimit(@Param("entityId") UUID entityId,
                                      @Param("entityKey") int key,
                                      @Param("startTs") long startTs,
                                      @Param("endTs") long endTs,
                                      Pageable pageable);

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：无。
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM TsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    void delete(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`String Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                @Param("entityKey") int key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMax(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Numeric Max`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                                @Param("entityKey") int entityKey,
                                                @Param("startTs") long startTs,
                                                @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(COALESCE(tskv.longValue, -9223372036854775807)), " +
            "MAX(COALESCE(tskv.doubleValue, -1.79769E+308)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MAX', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMax(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`String Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);


    @Query("SELECT new TsKvEntity(MIN(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMin(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Numeric Min`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MIN(COALESCE(tskv.longValue, 9223372036854775807)), " +
            "MIN(COALESCE(tskv.doubleValue, 1.79769E+308)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MIN', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMin(
    /**
     * 功能：获取数量。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityId") UUID entityId,
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(CASE WHEN tskv.booleanValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.strValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.jsonValue IS NULL THEN 0 ELSE 1 END), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findCount(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Avg`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                            @Param("entityKey") int entityKey,
                                            @Param("startTs") long startTs,
                                            @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'AVG', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findAvg(@Param("entityId") UUID entityId,
    /**
     * 功能：获取`Sum`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entityKey`：实体对象。
     * - `startTs`：时间戳。
     * - `endTs`：时间戳。
     * 返回：处理结果。
     */
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'SUM', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findSum(@Param("entityId") UUID entityId,
                                          @Param("entityKey") int entityKey,
                                          @Param("startTs") long startTs,
                                          @Param("endTs") long endTs);

}
