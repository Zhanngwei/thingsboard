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
 * 1. 类目的：`EdgeDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
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

/*
 * 本类总结：
 * 1. 核心职责：`EdgeDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
