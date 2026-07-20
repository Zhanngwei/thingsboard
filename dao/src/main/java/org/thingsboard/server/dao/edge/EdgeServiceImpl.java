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
package org.thingsboard.server.dao.edge;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.edge.EdgeSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.user.UserService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

/**
 * 中文说明：
 * 1. `EdgeServiceImpl` 是 ThingsBoard DAO 中负责边缘节点的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedEntityService`、`EdgeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("EdgeDaoService")
@Slf4j
public class EdgeServiceImpl extends AbstractCachedEntityService<EdgeCacheKey, Edge, EdgeCacheEvictEvent> implements EdgeService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    /**
     * `DEFAULT_PAGE_SIZE`常量，用于统一引用固定值。
     */
    private static final int DEFAULT_PAGE_SIZE = 1000;

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private EdgeDao edgeDao;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    private UserService userService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    private RuleChainService ruleChainService;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    private RelationService relationService;

    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private TimeseriesService timeseriesService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    @Autowired
    private DataValidator<Edge> edgeValidator;

    /**
     * 是否启用`edges`。
     */
    @Value("${edges.enabled}")
    @Getter
    private boolean edgesEnabled;
    /**
     * 是否满足遥测条件。
     */
    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = EdgeCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(EdgeCacheEvictEvent event) {
        List<EdgeCacheKey> keys = new ArrayList<>(2);
        keys.add(new EdgeCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new EdgeCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public Edge findEdgeById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findById(tenantId, edgeId.getId());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public EdgeInfo findEdgeInfoById(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeInfoById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findEdgeInfoById(tenantId, edgeId.getId());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Edge> findEdgeByIdAsync(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing findEdgeById [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        return edgeDao.findByIdAsync(tenantId, edgeId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public Edge findEdgeByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new EdgeCacheKey(tenantId, name),
                () -> edgeDao.findEdgeByTenantIdAndName(tenantId.getId(), name)
                        .orElse(null), true);
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `routingKey`：键。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<Edge> findEdgeByRoutingKey(TenantId tenantId, String routingKey) {
        log.trace("Executing findEdgeByRoutingKey [{}]", routingKey);
        Validator.validateString(routingKey, "Incorrect edge routingKey for search request.");
        return edgeDao.findByRoutingKey(tenantId.getId(), routingKey);
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：
     * - `edge`：`edge` 参数。
     * 返回：处理结果。
     */
    @Override
    public Edge saveEdge(Edge edge) {
        log.trace("Executing saveEdge [{}]", edge);
        Edge oldEdge = edgeValidator.validate(edge, Edge::getTenantId);
        EdgeCacheEvictEvent evictEvent = new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), oldEdge != null ? oldEdge.getName() : null);
        try {
            var savedEdge = edgeDao.save(edge.getTenantId(), edge);
            publishEvictEvent(evictEvent);
            return savedEdge;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && e.getConstraintName().equalsIgnoreCase("edge_name_unq_key")) {
                throw new DataValidationException("Edge with such name already exists!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：执行 `assignEdgeToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    @Override
    public Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, CustomerId customerId) {
        log.trace("[{}] Executing assignEdgeToCustomer [{}][{}]", tenantId, edgeId, customerId);
        Edge edge = findEdgeById(tenantId, edgeId);
        edge.setCustomerId(customerId);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(edgeId)
                .body(JacksonUtil.toString(customerId)).actionType(ActionType.ASSIGNED_TO_CUSTOMER).build());
        return saveEdge(edge);
    }

    /**
     * 功能：执行 `unassignEdgeFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public Edge unassignEdgeFromCustomer(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}] Executing unassignEdgeFromCustomer [{}]", tenantId, edgeId);
        Edge edge = findEdgeById(tenantId, edgeId);
        var customerId = edge.getCustomerId();
        edge.setCustomerId(null);
        Edge result = saveEdge(edge);
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entityId(edgeId)
                .body(JacksonUtil.toString(customerId)).actionType(ActionType.UNASSIGNED_FROM_CUSTOMER).build());
        return result;
    }

    /**
     * 功能：删除或清理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing deleteEdge [{}]", edgeId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);

        Edge edge = edgeDao.findById(tenantId, edgeId.getId());

        deleteEntityRelations(tenantId, edgeId);

        edgeDao.removeById(tenantId, edgeId.getId());

        publishEvictEvent(new EdgeCacheEvictEvent(edge.getTenantId(), edge.getName(), null));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdAndIdsAsync(TenantId tenantId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdAndIdsAsync, tenantId [{}], edgeIds [{}]", tenantId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(edgeIds));
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteEdgesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantEdgesRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeInfo> findEdgeInfosByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findEdgeInfosByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return edgeDao.findEdgeInfosByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `edgeIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Edge>> findEdgesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<EdgeId> edgeIds) {
        log.trace("Executing findEdgesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], edgeIds [{}]", tenantId, customerId, edgeIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(edgeIds, "Incorrect edgeIds " + edgeIds);
        return edgeDao.findEdgesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(edgeIds));
    }

    /**
     * 功能：执行 `unassignCustomerEdges` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void unassignCustomerEdges(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerEdges, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerEdgeUnassigner.removeEntities(tenantId, customerId);
    }

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<Edge>> findEdgesByQuery(TenantId tenantId, EdgeSearchQuery query) {
        log.trace("[{}] Executing findEdgesByQuery [{}]", tenantId, query);
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Edge>> edges = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Edge>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.EDGE) {
                    futures.add(findEdgeByIdAsync(tenantId, new EdgeId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        }, MoreExecutors.directExecutor());

        edges = Futures.transform(edges, new Function<List<Edge>, List<Edge>>() {
            @Nullable
            @Override
            public List<Edge> apply(@Nullable List<Edge> edgeList) {
                return edgeList == null ?
                        Collections.emptyList() :
                        edgeList.stream().filter(edge -> query.getEdgeTypes().contains(edge.getType())).collect(Collectors.toList());
            }
        }, MoreExecutors.directExecutor());

        return edges;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<EntitySubtype>> findEdgeTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findEdgeTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantEdgeTypes = edgeDao.findTenantEdgeTypesAsync(tenantId.getId());
        return Futures.transform(tenantEdgeTypes,
                edgeTypes -> {
                    edgeTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return edgeTypes;
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `assignDefaultRuleChainsToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    @Override
    public void assignDefaultRuleChainsToEdge(TenantId tenantId, EdgeId edgeId) {
        log.trace("Executing assignDefaultRuleChainsToEdge, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findAutoAssignToEdgeRuleChainsByTenantId(tenantId, pageLink);
            if (pageData.getData().size() > 0) {
                for (RuleChain ruleChain : pageData.getData()) {
                    ruleChainService.assignRuleChainToEdge(tenantId, ruleChain.getId(), edgeId);
                }
            }
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantIdAndEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantIdAndEntityId, tenantId [{}], entityId [{}], pageLink [{}]", tenantId, entityId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), entityId.getEntityType(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Edge> findEdgesByTenantProfileId(TenantProfileId tenantProfileId, PageLink pageLink) {
        log.trace("Executing findEdgesByTenantProfileId, tenantProfileId [{}], pageLink [{}]", tenantProfileId, pageLink);
        Validator.validateId(tenantProfileId, "Incorrect tenantProfileId " + tenantProfileId);
        validatePageLink(pageLink);
        return edgeDao.findEdgesByTenantProfileId(tenantProfileId.getId(), pageLink);
    }

    private PaginatedRemover<TenantId, Edge> tenantEdgesRemover =
            new PaginatedRemover<TenantId, Edge>() {

                @Override
                protected PageData<Edge> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return edgeDao.findEdgesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Edge entity) {
                    deleteEdge(tenantId, new EdgeId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Edge> customerEdgeUnassigner = new PaginatedRemover<CustomerId, Edge>() {

        @Override
        protected PageData<Edge> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return edgeDao.findEdgesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Edge entity) {
            unassignEdgeFromCustomer(tenantId, new EdgeId(entity.getUuidId()));
        }
    };

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<EdgeId> findAllRelatedEdgeIds(TenantId tenantId, EntityId entityId) {
        if (!edgesEnabled) {
            return null;
        }
        if (EntityType.EDGE.equals(entityId.getEntityType())) {
            return Collections.singletonList(new EdgeId(entityId.getId()));
        }
        PageDataIterableByTenantIdEntityId<EdgeId> relatedEdgeIdsIterator =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        List<EdgeId> result = new ArrayList<>();
        for (EdgeId edgeId : relatedEdgeIdsIterator) {
            result.add(edgeId);
        }
        return result;
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<EdgeId> findRelatedEdgeIdsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("[{}] Executing findRelatedEdgeIdsByEntityId [{}] [{}]", tenantId, entityId, pageLink);
        switch (entityId.getEntityType()) {
            case TENANT:
            case DEVICE_PROFILE:
            case ASSET_PROFILE:
            case OTA_PACKAGE:
                return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
            case CUSTOMER:
                return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, new CustomerId(entityId.getId()), pageLink));
            case EDGE:
                List<EdgeId> edgeIds = Collections.singletonList(new EdgeId(entityId.getId()));
                return new PageData<>(edgeIds, 1, 1, false);
            case DEVICE:
            case ASSET:
            case ENTITY_VIEW:
            case DASHBOARD:
            case RULE_CHAIN:
                return convertToEdgeIds(findEdgesByTenantIdAndEntityId(tenantId, entityId, pageLink));
            case USER:
                User userById = userService.findUserById(tenantId, new UserId(entityId.getId()));
                if (userById == null) {
                    return createEmptyEdgeIdPageData();
                }
                if (userById.getCustomerId() == null || userById.getCustomerId().isNullUid()) {
                    return convertToEdgeIds(findEdgesByTenantId(tenantId, pageLink));
                } else {
                    return convertToEdgeIds(findEdgesByTenantIdAndCustomerId(tenantId, userById.getCustomerId(), pageLink));
                }
            case TENANT_PROFILE:
                return convertToEdgeIds(findEdgesByTenantProfileId(new TenantProfileId(entityId.getId()), pageLink));
            default:
                log.warn("[{}] Unsupported entity type {}", tenantId, entityId.getEntityType());
                return createEmptyEdgeIdPageData();
        }
    }

    /**
     * 功能：保存或创建边缘节点。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private PageData<EdgeId> createEmptyEdgeIdPageData() {
        return new PageData<>(new ArrayList<>(), 0, 0, false);
    }

    /**
     * 功能：转换边缘节点。
     * 参数：
     * - `pageData`：待处理数据。
     * 返回：匹配的数据集合。
     */
    private PageData<EdgeId> convertToEdgeIds(PageData<Edge> pageData) {
        if (pageData == null) {
            return createEmptyEdgeIdPageData();
        }
        List<EdgeId> edgeIds = new ArrayList<>();
        if (pageData.getData() != null && !pageData.getData().isEmpty()) {
            edgeIds = pageData.getData().stream().map(IdBased::getId).collect(Collectors.toList());
        }
        return new PageData<>(edgeIds, pageData.getTotalPages(), pageData.getTotalElements(), pageData.hasNext());
    }

    /**
     * 功能：获取`Missing To Related Rule Chains`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `tbRuleChainInputNodeClassName`：名称。
     * 返回：文本结果。
     */
    @Override
    public String findMissingToRelatedRuleChains(TenantId tenantId, EdgeId edgeId, String tbRuleChainInputNodeClassName) {
        List<RuleChain> edgeRuleChains = findEdgeRuleChains(tenantId, edgeId);
        List<RuleChainId> edgeRuleChainIds = edgeRuleChains.stream().map(IdBased::getId).collect(Collectors.toList());
        ObjectNode result = JacksonUtil.newObjectNode();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            List<RuleNode> ruleNodes =
                    ruleChainService.loadRuleChainMetaData(edgeRuleChain.getTenantId(), edgeRuleChain.getId()).getNodes();
            if (ruleNodes != null && !ruleNodes.isEmpty()) {
                List<RuleChainId> connectedRuleChains =
                        ruleNodes.stream()
                                .filter(rn -> rn.getType().equals(tbRuleChainInputNodeClassName))
                                .map(rn -> new RuleChainId(UUID.fromString(rn.getConfiguration().get("ruleChainId").asText())))
                                .collect(Collectors.toList());
                List<String> missingRuleChains = new ArrayList<>();
                for (RuleChainId connectedRuleChain : connectedRuleChains) {
                    if (!edgeRuleChainIds.contains(connectedRuleChain)) {
                        RuleChain ruleChainById = ruleChainService.findRuleChainById(tenantId, connectedRuleChain);
                        missingRuleChains.add(ruleChainById.getName());
                    }
                }
                if (!missingRuleChains.isEmpty()) {
                    ArrayNode array = JacksonUtil.newArrayNode();
                    for (String missingRuleChain : missingRuleChains) {
                        array.add(missingRuleChain);
                    }
                    result.set(edgeRuleChain.getName(), array);
                }
            }
        }
        return result.toString();
    }

    /**
     * 功能：判断边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public ListenableFuture<Boolean> isEdgeActiveAsync(TenantId tenantId, EdgeId edgeId, String key) {
        ListenableFuture<? extends Optional<? extends KvEntry>> futureKvEntry;
        if (persistToTelemetry) {
            futureKvEntry = timeseriesService.findLatest(tenantId, edgeId, key);
        } else {
            futureKvEntry = attributesService.find(tenantId, edgeId, DataConstants.SERVER_SCOPE, key);
        }
        return Futures.transformAsync(futureKvEntry, kvEntryOpt ->
                Futures.immediateFuture(kvEntryOpt.flatMap(KvEntry::getBooleanValue).orElse(false)), MoreExecutors.directExecutor());
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：匹配的数据集合。
     */
    private List<RuleChain> findEdgeRuleChains(TenantId tenantId, EdgeId edgeId) {
        List<RuleChain> result = new ArrayList<>();
        PageLink pageLink = new PageLink(DEFAULT_PAGE_SIZE);
        PageData<RuleChain> pageData;
        do {
            pageData = ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, pageLink);
            if (pageData != null && pageData.getData() != null && !pageData.getData().isEmpty()) {
                result.addAll(pageData.getData());
                if (pageData.hasNext()) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } while (pageData != null && pageData.hasNext());
        return result;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEdgeById(tenantId, new EdgeId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE;
    }

}
