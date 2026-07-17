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
package org.thingsboard.server.service.entitiy.entity.relation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

/**
 * 中文说明：
 * 1. `DefaultTbEntityRelationService` 是 ThingsBoard Application 中负责实体关系的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractTbEntityService`、`TbEntityRelationService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityRelationService extends AbstractTbEntityService implements TbEntityRelationService {

    /**
     * 关系，提供当前类调用的业务操作。
     */
    private final RelationService relationService;

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `relation`：`relation` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void save(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws ThingsboardException {
        ActionType actionType = ActionType.RELATION_ADD_OR_UPDATE;
        try {
            relationService.saveRelation(tenantId, relation);
            notificationEntityService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, null, relation);
        } catch (Exception e) {
            notificationEntityService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `relation`：`relation` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void delete(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws ThingsboardException {
        ActionType actionType = ActionType.RELATION_DELETED;
        try {
            boolean found = relationService.deleteRelation(tenantId, relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
            if (!found) {
                throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
            }
            notificationEntityService.logEntityRelationAction(tenantId, customerId, relation, user, actionType, null, relation);
        } catch (Exception e) {
            notificationEntityService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    /**
     * 功能：删除或清理`Common Relations`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `entityId`：实体IDID。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    @Override
    public void deleteCommonRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws ThingsboardException {
        try {
            relationService.deleteEntityCommonRelations(tenantId, entityId);
            notificationEntityService.logEntityAction(tenantId, entityId, null, customerId, ActionType.RELATIONS_DELETED, user);
        } catch (Exception e) {
            notificationEntityService.logEntityAction(tenantId, entityId, null, customerId,
                    ActionType.RELATIONS_DELETED, user, e);
            throw e;
        }
    }
}
