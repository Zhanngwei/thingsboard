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
 * 1. `DefaultNativeDeviceRepository` 是 ThingsBoard DAO 中负责设备存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `NativeDeviceRepository`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
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
