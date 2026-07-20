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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.timescale.ts.TimescaleTsKvEntity;
import org.thingsboard.server.dao.util.TimescaleDBTsOrTsLatestDao;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TsKvTimescaleRepository` 是 ThingsBoard DAO 中定义 `Ts Kv Timescale` 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@TimescaleDBTsOrTsLatestDao
public interface TsKvTimescaleRepository extends JpaRepository<TimescaleTsKvEntity, TimescaleTsKvCompositeKey> {

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
            "AND key = :entityKey AND ts >= :startTs AND ts < :endTs", nativeQuery = true)
    List<TimescaleTsKvEntity> findAllWithLimit(@Param("entityId") UUID entityId,
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
    @Query("DELETE FROM TimescaleTsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey " +
            "AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    void delete(@Param("entityId") UUID entityId,
                @Param("entityKey") int key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

}
