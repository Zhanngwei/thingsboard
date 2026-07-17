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
package org.thingsboard.server.service.entitiy.edge;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

/**
 * 中文说明：
 * 1. `DefaultTbEdgeService` 是 ThingsBoard Application 中负责边缘节点的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbEdgeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@AllArgsConstructor
@TbCoreComponent
@Service
@Slf4j
public class DefaultTbEdgeService extends AbstractTbEntityService implements TbEdgeService {

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final EdgeNotificationService edgeNotificationService;
    private final RuleChainService ruleChainService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `edgeTemplateRootRuleChain`：`edgeTemplateRootRuleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, User user) throws Exception {
        ActionType actionType = edge.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = edge.getTenantId();
        try {
            if (actionType == ActionType.ADDED && edge.getRootRuleChainId() == null) {
                edge.setRootRuleChainId(edgeTemplateRootRuleChain.getId());
            }
            Edge savedEdge = checkNotNull(edgeService.saveEdge(edge));
            EdgeId edgeId = savedEdge.getId();

            if (actionType == ActionType.ADDED) {
                ruleChainService.assignRuleChainToEdge(tenantId, edgeTemplateRootRuleChain.getId(), edgeId);
                edgeNotificationService.setEdgeRootRuleChain(tenantId, savedEdge, edgeTemplateRootRuleChain.getId());
                edgeService.assignDefaultRuleChainsToEdge(tenantId, edgeId);
            }

            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, savedEdge.getCustomerId(), savedEdge, actionType, user);

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), edge, actionType, user, e);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(Edge edge, User user) {
        EdgeId edgeId = edge.getId();
        TenantId tenantId = edge.getTenantId();
        try {
            edgeService.deleteEdge(tenantId, edgeId);
            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, edge.getCustomerId(), edge, ActionType.DELETED, user, edgeId.toString());
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE), ActionType.DELETED,
                    user, e, edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignEdgeToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.ASSIGNED_TO_CUSTOMER;
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));
            notificationEntityService.logEntityAction(tenantId, edgeId, savedEdge, customerId, actionType,
                    user, edgeId.toString(), customerId.toString(), customer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, edgeId.toString(), customerId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `unassignEdgeFromCustomer` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge unassignEdgeFromCustomer(Edge edge, Customer customer, User user) throws ThingsboardException {
        ActionType actionType = ActionType.UNASSIGNED_FROM_CUSTOMER;
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        CustomerId customerId = customer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.unassignEdgeFromCustomer(tenantId, edgeId));
            notificationEntityService.logEntityAction(tenantId, edgeId, savedEdge, customerId, actionType,
                    user, edgeId.toString(), customerId.toString(), customer.getName());
            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UNASSIGNED_FROM_CUSTOMER, user, e, edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：执行 `assignEdgeToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge assignEdgeToPublicCustomer(TenantId tenantId, EdgeId edgeId, User user) throws ThingsboardException {
        Customer publicCustomer = customerService.findOrCreatePublicCustomer(tenantId);
        CustomerId customerId = publicCustomer.getId();
        try {
            Edge savedEdge = checkNotNull(edgeService.assignEdgeToCustomer(tenantId, edgeId, customerId));

            notificationEntityService.notifyCreateOrUpdateOrDeleteEdge(tenantId, edgeId, customerId, savedEdge, ActionType.ASSIGNED_TO_CUSTOMER, user,
                    edgeId.toString(), customerId.toString(), publicCustomer.getName());

            return savedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.ASSIGNED_TO_CUSTOMER, user, e, edgeId.toString());
            throw e;
        }
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `ruleChainId`：规则链ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception {
        TenantId tenantId = edge.getTenantId();
        EdgeId edgeId = edge.getId();
        try {
            Edge updatedEdge = edgeNotificationService.setEdgeRootRuleChain(tenantId, edge, ruleChainId);
            notificationEntityService.logEntityAction(tenantId, edgeId, edge, null, ActionType.UPDATED, user);
            return updatedEdge;
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, emptyId(EntityType.EDGE),
                    ActionType.UPDATED, user, e, edgeId.toString());
            throw e;
        }
    }
}
