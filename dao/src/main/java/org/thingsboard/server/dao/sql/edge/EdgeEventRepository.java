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
package org.thingsboard.server.dao.sql.edge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.EdgeEventEntity;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `EdgeEventRepository` 是 ThingsBoard DAO 中定义事件能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EdgeEventRepository extends JpaRepository<EdgeEventEntity, UUID>, JpaSpecificationExecutor<EdgeEventEntity> {

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `textSearch`：`textSearch` 参数。
     * - `startTime`：开始时间戳。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT e FROM EdgeEventEntity e WHERE " +
            "e.tenantId = :tenantId " +
            "AND e.edgeId = :edgeId " +
            "AND (:startTime IS NULL OR e.createdTime >= :startTime) " +
            "AND (:endTime IS NULL OR e.createdTime <= :endTime) " +
            "AND (:seqIdStart IS NULL OR e.seqId > :seqIdStart) " +
            "AND (:seqIdEnd IS NULL OR e.seqId < :seqIdEnd) " +
            "AND (:textSearch IS NULL OR ilike(e.edgeEventType, CONCAT('%', :textSearch, '%')) = true)"
    )
    Page<EdgeEventEntity> findEdgeEventsByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                                            @Param("edgeId") UUID edgeId,
                                                            @Param("textSearch") String textSearch,
                                                            @Param("startTime") Long startTime,
                                                            @Param("endTime") Long endTime,
                                                            @Param("seqIdStart") Long seqIdStart,
                                                            @Param("seqIdEnd") Long seqIdEnd,
                                                            Pageable pageable);
}
