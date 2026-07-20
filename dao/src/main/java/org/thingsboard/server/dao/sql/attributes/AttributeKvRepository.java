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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AttributeKvRepository` 是 ThingsBoard DAO 中定义属性能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `JpaRepository`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface AttributeKvRepository extends JpaRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllByEntityTypeAndEntityIdAndAttributeType(@Param("entityType") EntityType entityType,
    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityId`：实体IDID。
     * - `attributeType`：类型。
     * - `attributeKey`：键。
     * 返回：无。
     */
                                                                           @Param("entityId") UUID entityId,
                                                                           @Param("attributeType") String attributeType);

    @Transactional
    @Modifying
    @Query("DELETE FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType " +
            "AND a.id.attributeKey = :attributeKey")
    void delete(@Param("entityType") EntityType entityType,
    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
                @Param("entityId") UUID entityId,
                @Param("attributeType") String attributeType,
                @Param("attributeKey") String attributeKey);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = 'DEVICE' " +
            "AND entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId and device_profile_id = :deviceProfileId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByDeviceProfileId(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = 'DEVICE' " +
            "AND entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = :entityType " +
            "AND entity_id in :entityIds ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByEntityIds(@Param("entityType") String entityType, @Param("entityIds") List<UUID> entityIds);

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * - `attributeType`：类型。
     * 返回：匹配的数据集合。
     */
    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE entity_type = :entityType " +
            "AND entity_id in :entityIds AND attribute_type = :attributeType ORDER BY attribute_key", nativeQuery = true)
    List<String> findAllKeysByEntityIdsAndAttributeType(@Param("entityType") String entityType,
                                                        @Param("entityIds") List<UUID> entityIds,
                                                        @Param("attributeType") String attributeType);
}
