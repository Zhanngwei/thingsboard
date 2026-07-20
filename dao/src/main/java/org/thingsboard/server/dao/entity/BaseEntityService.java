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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasEmail;
import org.thingsboard.server.common.data.HasLabel;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.NameLabelAndCustomerDetails;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityFilterType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.dao.exception.IncorrectParameterException;

import java.util.Optional;
import java.util.function.Function;

import static org.thingsboard.server.common.data.id.EntityId.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateEntityDataPageLink;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * Created by ashvayka on 04.05.17.
 */
/**
 * 中文说明：
 * 1. `BaseEntityService` 是 ThingsBoard DAO 中负责实体的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`EntityService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
public class BaseEntityService extends AbstractEntityService implements EntityService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final CustomerId NULL_CUSTOMER_ID = new CustomerId(NULL_UUID);

    /**
     * 实体，用于读取或保存对应领域对象。
     */
    @Autowired
    private EntityQueryDao entityQueryDao;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    EntityServiceRegistry entityServiceRegistry;

    /**
     * 功能：统计查询条件数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：数值结果。
     */
    @Override
    public long countEntitiesByQuery(TenantId tenantId, CustomerId customerId, EntityCountQuery query) {
        log.trace("Executing countEntitiesByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityCountQuery(query);
        return this.entityQueryDao.countEntitiesByQuery(tenantId, customerId, query);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EntityData> findEntityDataByQuery(TenantId tenantId, CustomerId customerId, EntityDataQuery query) {
        log.trace("Executing findEntityDataByQuery, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateEntityDataQuery(query);
        return this.entityQueryDao.findEntityDataByQuery(tenantId, customerId, query);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<String> fetchEntityName(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityName [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getName);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<String> fetchEntityLabel(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityLabel [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getLabel);
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<CustomerId> fetchEntityCustomerId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchEntityCustomerId [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getCustomerId);
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<NameLabelAndCustomerDetails> fetchNameLabelAndCustomerDetails(TenantId tenantId, EntityId entityId) {
        log.trace("Executing fetchNameLabelAndCustomerDetails [{}]", entityId);
        return fetchAndConvert(tenantId, entityId, this::getNameLabelAndCustomerDetails);
    }

    /**
     * 功能：获取`And Convert`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `converter`：`converter` 参数。
     * 返回：可能存在的结果。
     */
    private <T> Optional<T> fetchAndConvert(TenantId tenantId, EntityId entityId, Function<HasId<?>, T> converter) {
        EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityId.getEntityType());
        Optional<HasId<?>> entityOpt = entityDaoService.findEntity(tenantId, entityId);
        return entityOpt.map(converter);
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `entity`：实体对象。
     * 返回：文本结果。
     */
    private String getName(HasId<?> entity) {
        return entity instanceof HasName ? ((HasName) entity).getName() : null;
    }

    /**
     * 功能：获取显示标签。
     * 参数：
     * - `entity`：实体对象。
     * 返回：文本结果。
     */
    private String getLabel(HasId<?> entity) {
        if (entity instanceof HasTitle && StringUtils.isNotEmpty(((HasTitle) entity).getTitle())) {
            return ((HasTitle) entity).getTitle();
        }
        if (entity instanceof HasLabel && StringUtils.isNotEmpty(((HasLabel) entity).getLabel())) {
            return ((HasLabel) entity).getLabel();
        }
        if (entity instanceof HasEmail && StringUtils.isNotEmpty(((HasEmail) entity).getEmail())) {
            return ((HasEmail) entity).getEmail();
        }
        if (entity instanceof HasName && StringUtils.isNotEmpty(((HasName) entity).getName())) {
            return ((HasName) entity).getName();
        }
        return null;
    }

    /**
     * 功能：获取客户ID。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    private CustomerId getCustomerId(HasId<?> entity) {
        if (entity instanceof HasCustomerId) {
            HasCustomerId hasCustomerId = (HasCustomerId) entity;
            CustomerId customerId = hasCustomerId.getCustomerId();
            if (customerId == null) {
                customerId = NULL_CUSTOMER_ID;
            }
            return customerId;
        }
        return NULL_CUSTOMER_ID;
    }

    /**
     * 功能：获取客户。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    private NameLabelAndCustomerDetails getNameLabelAndCustomerDetails(HasId<?> entity) {
        return new NameLabelAndCustomerDetails(getName(entity), getLabel(entity), getCustomerId(entity));
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    private static void validateEntityCountQuery(EntityCountQuery query) {
        if (query == null) {
            throw new IncorrectParameterException("Query must be specified.");
        } else if (query.getEntityFilter() == null) {
            throw new IncorrectParameterException("Query entity filter must be specified.");
        } else if (query.getEntityFilter().getType() == null) {
            throw new IncorrectParameterException("Query entity filter type must be specified.");
        } else if (query.getEntityFilter().getType().equals(EntityFilterType.RELATIONS_QUERY)) {
            validateRelationQuery((RelationsQueryFilter) query.getEntityFilter());
        }
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：无。
     */
    private static void validateEntityDataQuery(EntityDataQuery query) {
        validateEntityCountQuery(query);
        validateEntityDataPageLink(query.getPageLink());
    }

    /**
     * 功能：校验关系。
     * 参数：
     * - `queryFilter`：`queryFilter` 参数。
     * 返回：无。
     */
    private static void validateRelationQuery(RelationsQueryFilter queryFilter) {
        if (queryFilter.isMultiRoot() && queryFilter.getMultiRootEntitiesType() == null) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntitiesType'");
        }
        if (queryFilter.isMultiRoot() && CollectionUtils.isEmpty(queryFilter.getMultiRootEntityIds())) {
            throw new IncorrectParameterException("Multi-root relation query filter should contain 'multiRootEntityIds' array that contains string representation of UUIDs");
        }
        if (!queryFilter.isMultiRoot() && queryFilter.getRootEntity() == null) {
            throw new IncorrectParameterException("Relation query filter root entity should not be blank");
        }
    }
}
