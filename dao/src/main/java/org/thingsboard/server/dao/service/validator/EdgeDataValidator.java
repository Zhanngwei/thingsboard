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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * 中文说明：
 * 1. `EdgeDataValidator` 是 ThingsBoard DAO 中负责边缘节点存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
@AllArgsConstructor
public class EdgeDataValidator extends DataValidator<Edge> {

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    private final EdgeDao edgeDao;
    private final TenantService tenantService;
    /**
     * 客户，用于读取或保存对应领域对象。
     */
    private final CustomerDao customerDao;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, Edge edge) {
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * 返回：判断结果。
     */
    @Override
    protected Edge validateUpdate(TenantId tenantId, Edge edge) {
        return edgeDao.findById(edge.getTenantId(), edge.getId().getId());
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, Edge edge) {
        validateString("Edge name", edge.getName());
        validateString("Edge type", edge.getType());
        if (StringUtils.isEmpty(edge.getSecret())) {
            throw new DataValidationException("Edge secret should be specified!");
        }
        if (StringUtils.isEmpty(edge.getRoutingKey())) {
            throw new DataValidationException("Edge routing key should be specified!");
        }
        if (edge.getTenantId() == null) {
            throw new DataValidationException("Edge should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(edge.getTenantId())) {
                throw new DataValidationException("Edge is referencing to non-existent tenant!");
            }
        }
        if (edge.getCustomerId() == null) {
            edge.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!edge.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(edge.getTenantId(), edge.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign edge to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(edge.getTenantId().getId())) {
                throw new DataValidationException("Can't assign edge to customer from different tenant!");
            }
        }
    }
}
