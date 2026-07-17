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
package org.thingsboard.server.service.entitiy.customer;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

/**
 * 中文说明：
 * 1. `DefaultTbCustomerService` 是 ThingsBoard Application 中负责客户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbCustomerService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@AllArgsConstructor
public class DefaultTbCustomerService extends AbstractTbEntityService implements TbCustomerService {

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Customer save(Customer customer, User user) throws Exception {
        ActionType actionType = customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = customer.getTenantId();
        try {
            Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));
            autoCommit(user, savedCustomer.getId());
            notificationEntityService.logEntityAction(tenantId, savedCustomer.getId(), savedCustomer, null, actionType, user);
            return savedCustomer;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), customer, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(Customer customer, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = customer.getTenantId();
        CustomerId customerId = customer.getId();
        try {
            customerService.deleteCustomer(tenantId, customerId);
            notificationEntityService.logEntityAction(tenantId, customer.getId(), customer, customerId, actionType,
                    user, customerId.toString());
            tbClusterService.broadcastEntityStateChangeEvent(tenantId, customer.getId(), ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), actionType, user,
                    e, customerId.toString());
            throw e;
        }
    }
}
