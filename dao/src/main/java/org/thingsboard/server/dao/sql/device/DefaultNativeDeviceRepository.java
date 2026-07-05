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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultNativeDeviceRepository` 是 ThingsBoard DAO 模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@RequiredArgsConstructor
@Repository
@Slf4j
public class DefaultNativeDeviceRepository implements NativeDeviceRepository {

    /**
     * 数量常量，用于统一引用固定值。
     */
    private final String COUNT_QUERY = "SELECT count(id) FROM device;";
    private final String QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `pageable`：`pageable` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(COUNT_QUERY, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(QUERY, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            var data = rows.stream().map(row -> {
                UUID id = (UUID) row.get("id");
                var tenantIdObj = row.get("tenantId");
                var customerIdObj = row.get("customerId");
                return new DeviceIdInfo(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), customerIdObj != null ? (UUID) customerIdObj : null, id);
            }).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultNativeDeviceRepository` 在 ThingsBoard DAO 模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
