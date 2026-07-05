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
package org.thingsboard.server.dao.sql.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.dao.ExportableEntityRepository;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.DeviceInfoEntity;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`DeviceRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID>, ExportableEntityRepository<DeviceEntity> {

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Query("SELECT d FROM DeviceInfoEntity d WHERE d.id = :deviceId")
    DeviceInfoEntity findDeviceInfoById(@Param("deviceId") UUID deviceId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") UUID customerId,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `profileId`：配置ID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :profileId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndProfileId(@Param("tenantId") UUID tenantId,
                                                  @Param("profileId") UUID profileId,
                                                  @Param("textSearch") String textSearch,
                                                  Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceInfoEntity> findDeviceInfosByTenantIdAndCustomerId(@Param("tenantId") UUID tenantId,
                                                                  @Param("customerId") UUID customerId,
                                                                  @Param("textSearch") String textSearch,
                                                                  Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantId(@Param("tenantId") UUID tenantId,
                                      @Param("textSearch") String textSearch,
                                      Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndType(@Param("tenantId") UUID tenantId,
                                             @Param("type") String type,
                                             @Param("textSearch") String textSearch,
                                             Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndTypeAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndTypeAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
                                                                @Param("deviceProfileId") UUID deviceProfileId,
                                                                @Param("textSearch") String textSearch,
                                                                Pageable pageable);

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.firmwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndFirmwareIdIsNull(@Param("tenantId") UUID tenantId,
    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.deviceProfileId = :deviceProfileId " +
            "AND d.softwareId = null")
    Long countByTenantIdAndDeviceProfileIdAndSoftwareIdIsNull(@Param("tenantId") UUID tenantId,
    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
                                                              @Param("deviceProfileId") UUID deviceProfileId);

    @Query("SELECT d FROM DeviceEntity d WHERE d.tenantId = :tenantId " +
            "AND d.customerId = :customerId " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndCustomerIdAndType(@Param("tenantId") UUID tenantId,
                                                          @Param("customerId") UUID customerId,
                                                          @Param("type") String type,
                                                          @Param("textSearch") String textSearch,
                                                          Pageable pageable);

    /**
     * 功能：获取设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceInfoEntity d " +
            "WHERE d.tenantId = :tenantId " +
            "AND (:customerId IS NULL OR d.customerId = uuid(:customerId)) " +
            "AND (:edgeId IS NULL OR d.id IN (SELECT re.toId FROM RelationEntity re WHERE re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' AND re.relationType = 'Contains' AND re.fromType = 'EDGE' AND re.fromId = uuid(:edgeId))) " +
            "AND ((:deviceType) IS NULL OR d.type = :deviceType) " +
            "AND (:deviceProfileId IS NULL OR d.deviceProfileId = uuid(:deviceProfileId)) " +
            "AND ((:filterByActive) IS FALSE OR d.active = :deviceActive) " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.type, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.customerTitle, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceInfoEntity> findDeviceInfosByFilter(@Param("tenantId") UUID tenantId,
                                                   @Param("customerId") String customerId,
                                                   @Param("edgeId") String edgeId,
                                                   @Param("deviceType") String type,
                                                   @Param("deviceProfileId") String deviceProfileId,
                                                   @Param("filterByActive") boolean filterByActive,
                                                   @Param("deviceActive") boolean active,
                                                   @Param("textSearch") String textSearch,
                                                   Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    DeviceEntity findByTenantIdAndName(UUID tenantId, String name);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndIdIn(UUID tenantId, UUID customerId, List<UUID> deviceIds);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    List<DeviceEntity> findDevicesByTenantIdAndIdIn(UUID tenantId, List<UUID> deviceIds);

    /**
     * 功能：获取`Devices By Id In`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    List<DeviceEntity> findDevicesByIdIn(List<UUID> deviceIds);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    DeviceEntity findByTenantIdAndId(UUID tenantId, UUID id);

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：数值结果。
     */
    Long countByDeviceProfileId(UUID deviceProfileId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `textSearch`：`textSearch` 参数。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndEdgeId(@Param("tenantId") UUID tenantId,
                                               @Param("edgeId") UUID edgeId,
                                               @Param("textSearch") String textSearch,
                                               Pageable pageable);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `textSearch`：`textSearch` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d FROM DeviceEntity d, RelationEntity re WHERE d.tenantId = :tenantId " +
            "AND d.id = re.toId AND re.toType = 'DEVICE' AND re.relationTypeGroup = 'EDGE' " +
            "AND re.relationType = 'Contains' AND re.fromId = :edgeId AND re.fromType = 'EDGE' " +
            "AND d.type = :type " +
            "AND (:textSearch IS NULL OR ilike(d.name, CONCAT('%', :textSearch, '%')) = true " +
            "OR ilike(d.label, CONCAT('%', :textSearch, '%')) = true)")
    Page<DeviceEntity> findByTenantIdAndEdgeIdAndType(@Param("tenantId") UUID tenantId,
                                                      @Param("edgeId") UUID edgeId,
                                                      @Param("type") String type,
                                                      @Param("textSearch") String textSearch,
                                                      Pageable pageable);

    /**
     * Count devices by tenantId.
     * Custom query applied because default QueryDSL produces slow count(id).
     *
     * There is two way to count devices.
     * OPTIMAL: count(*)
     * - returns _row_count_ and use index-only scan (super fast).
     * SLOW: count(id)
     * - returns _NON_NULL_id_count and performs table scan to verify isNull for each id in filtered rows.
     */
    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Query("SELECT count(*) FROM DeviceEntity d WHERE d.tenantId = :tenantId")
    Long countByTenantId(@Param("tenantId") UUID tenantId);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `transportType`：类型。
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Query("SELECT d.id FROM DeviceEntity d " +
            "INNER JOIN DeviceProfileEntity p ON d.deviceProfileId = p.id " +
            "WHERE p.transportType = :transportType")
    Page<UUID> findIdsByDeviceProfileTransportType(@Param("transportType") DeviceTransportType transportType, Pageable pageable);

    /**
     * 功能：获取`External Id By Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Query("SELECT externalId FROM DeviceEntity WHERE id = :id")
    UUID getExternalIdById(@Param("id") UUID id);

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
