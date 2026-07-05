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
 * 1. 类目的：`DefaultTbCustomerService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbCustomerService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
