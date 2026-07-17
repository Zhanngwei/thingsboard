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
package org.thingsboard.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `AbstractEntityService` 是 ThingsBoard DAO 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class AbstractEntityService {

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    protected RelationService relationService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    protected AlarmService alarmService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    protected EntityViewService entityViewService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired(required = false)
    protected EdgeService edgeService;

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    protected void createRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    /**
     * 功能：删除或清理关系。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    protected void deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    protected void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        relationService.deleteEntityRelations(tenantId, entityId);
        alarmService.deleteEntityAlarmRelations(tenantId, entityId);
    }

    /**
     * 功能：执行 `extractConstraintViolationException` 对应的处理。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：可能存在的结果。
     */
    protected static Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * 功能：校验`Constraint Violation`。
     * 参数：
     * - `t`：`t` 参数。
     * - `constraintName`：名称。
     * - `constraintMessage`：待处理消息。
     * 返回：无。
     */
    public static final void checkConstraintViolation(Exception t, String constraintName, String constraintMessage) {
        checkConstraintViolation(t, Collections.singletonMap(constraintName, constraintMessage));
    }

    /**
     * 功能：校验`Constraint Violation`。
     * 参数：
     * - `t`：`t` 参数。
     * - `constraintName1`：名称。
     * - `constraintMessage1`：待处理消息。
     * - `constraintName2`：名称。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public static final void checkConstraintViolation(Exception t, String constraintName1, String constraintMessage1, String constraintName2, String constraintMessage2) {
        checkConstraintViolation(t, Map.of(constraintName1, constraintMessage1, constraintName2, constraintMessage2));
    }

    /**
     * 功能：校验`Constraint Violation`。
     * 参数：
     * - `t`：`t` 参数。
     * - `constraints`：键值映射。
     * 返回：无。
     */
    public static final void checkConstraintViolation(Exception t, Map<String, String> constraints) {
        var exOpt = extractConstraintViolationException(t);
        if (exOpt.isPresent()) {
            var ex = exOpt.get();
            if (StringUtils.isNotEmpty(ex.getConstraintName())) {
                var constraintName = ex.getConstraintName();
                for (var constraintMessage : constraints.entrySet()) {
                    if (constraintName.equals(constraintMessage.getKey())) {
                        throw new DataValidationException(constraintMessage.getValue());
                    }
                }
            }
        }
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    protected void checkAssignedEntityViewsToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(tenantId, entityId);
        if (entityViews != null && !entityViews.isEmpty()) {
            EntityView entityView = entityViews.get(0);
            boolean relationExists = relationService.checkRelation(
                    tenantId, edgeId, entityView.getId(),
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE
            );
            if (relationExists) {
                throw new DataValidationException("Can't unassign device/asset from edge that is related to entity view and entity view is assigned to edge!");
            }
        }
    }
}
