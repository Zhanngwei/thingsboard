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
package org.thingsboard.server.dao.dashboard;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.edge.EdgeDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `DashboardServiceImpl` 是 ThingsBoard DAO 中负责仪表盘的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`DashboardService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("DashboardDaoService")
@Slf4j
@RequiredArgsConstructor
public class DashboardServiceImpl extends AbstractEntityService implements DashboardService {

    /**
     * 仪表盘ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_DASHBOARD_ID = "Incorrect dashboardId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    @Autowired
    private DashboardDao dashboardDao;

    /**
     * 仪表盘，用于读取或保存对应领域对象。
     */
    @Autowired
    private DashboardInfoDao dashboardInfoDao;

    /**
     * 客户，用于读取或保存对应领域对象。
     */
    @Autowired
    private CustomerDao customerDao;

    /**
     * 边缘节点，用于读取或保存对应领域对象。
     */
    @Autowired
    private EdgeDao edgeDao;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 仪表盘对象，用于描述当前业务场景。
     */
    @Autowired
    private DataValidator<Dashboard> dashboardValidator;

    /**
     * `cache` 字段，保存当前对象的对应属性。
     */
    @Autowired
    protected TbTransactionalCache<DashboardId, String> cache;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityCountService countService;

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 功能：发送或提交事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    protected void publishEvictEvent(DashboardTitleEvictEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            eventPublisher.publishEvent(event);
        } else {
            handleEvictEvent(event);
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = DashboardTitleEvictEvent.class)
    public void handleEvictEvent(DashboardTitleEvictEvent event) {
        cache.evict(event.getKey());
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard findDashboardById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findById(tenantId, dashboardId.getId());
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Dashboard> findDashboardByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：处理结果。
     */
    @Override
    public DashboardInfo findDashboardInfoById(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoById [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findById(tenantId, dashboardId.getId());
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：文本结果。
     */
    @Override
    public String findDashboardTitleById(TenantId tenantId, DashboardId dashboardId) {
        return cache.getAndPutInTransaction(dashboardId,
                () -> dashboardInfoDao.findTitleById(tenantId.getId(), dashboardId.getId()), true);
    }

    /**
     * 功能：获取仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<DashboardInfo> findDashboardInfoByIdAsync(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing findDashboardInfoByIdAsync [{}]", dashboardId);
        validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        return dashboardInfoDao.findByIdAsync(tenantId, dashboardId.getId());
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    @Override
    public Dashboard saveDashboard(Dashboard dashboard, boolean doValidate) {
        return doSaveDashboard(dashboard, doValidate);
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * 返回：处理结果。
     */
    @Override
    public Dashboard saveDashboard(Dashboard dashboard) {
        return doSaveDashboard(dashboard, true);
    }

    /**
     * 功能：执行 `doSaveDashboard` 对应的处理。
     * 参数：
     * - `dashboard`：`dashboard` 参数。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    private Dashboard doSaveDashboard(Dashboard dashboard, boolean doValidate) {
        log.trace("Executing saveDashboard [{}]", dashboard);
        if (doValidate) {
            dashboardValidator.validate(dashboard, DashboardInfo::getTenantId);
        }
        try {
            imageService.replaceBase64WithImageUrl(dashboard);
            var saved = dashboardDao.save(dashboard.getTenantId(), dashboard);
            publishEvictEvent(new DashboardTitleEvictEvent(saved.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).created(dashboard.getId() == null).build());
            if (dashboard.getId() == null) {
                countService.publishCountEntityEvictEvent(saved.getTenantId(), EntityType.DASHBOARD);
            }
            return saved;
        } catch (Exception e) {
            if (dashboard.getId() != null) {
                publishEvictEvent(new DashboardTitleEvictEvent(dashboard.getId()));
            }
            checkConstraintViolation(e, "dashboard_external_id_unq_key", "Dashboard with such external id already exists!");
            throw e;
        }
    }



    /**
     * 功能：执行 `assignDashboardToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard assignDashboardToCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent customer!");
        }
        if (!customer.getTenantId().getId().equals(dashboard.getTenantId().getId())) {
            throw new DataValidationException("Can't assign dashboard to customer from different tenant!");
        }
        if (dashboard.addAssignedCustomer(customer)) {
            try {
                createRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to create dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    /**
     * 功能：执行 `unassignDashboardFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard unassignDashboardFromCustomer(TenantId tenantId, DashboardId dashboardId, CustomerId customerId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent customer!");
        }
        if (dashboard.removeAssignedCustomer(customer)) {
            try {
                deleteRelation(tenantId, new EntityRelation(customerId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DASHBOARD));
            } catch (Exception e) {
                log.warn("[{}] Failed to delete dashboard relation. Customer Id: [{}]", dashboardId, customerId);
                throw new RuntimeException(e);
            }
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    private Dashboard updateAssignedCustomer(TenantId tenantId, DashboardId dashboardId, Customer customer) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        if (dashboard.updateAssignedCustomer(customer)) {
            return saveDashboard(dashboard);
        } else {
            return dashboard;
        }
    }

    /**
     * 功能：删除或清理仪表盘。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteDashboard(TenantId tenantId, DashboardId dashboardId) {
        log.trace("Executing deleteDashboard [{}]", dashboardId);
        Validator.validateId(dashboardId, INCORRECT_DASHBOARD_ID + dashboardId);
        deleteEntityRelations(tenantId, dashboardId);
        try {
            dashboardDao.removeById(tenantId, dashboardId.getId());
            publishEvictEvent(new DashboardTitleEvictEvent(dashboardId));
            countService.publishCountEntityEvictEvent(tenantId, EntityType.DASHBOARD);
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(dashboardId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_default_dashboard_device_profile")) {
                throw new DataValidationException("The dashboard referenced by the device profiles cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findMobileDashboardsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantId(tenantId.getId(), pageLink);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteDashboardsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDashboardsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDashboardsRemover.removeEntities(tenantId, tenantId);
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
    public PageData<DashboardInfo> findDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
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
    public PageData<DashboardInfo> findMobileDashboardsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findMobileDashboardsByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findMobileDashboardsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    /**
     * 功能：执行 `unassignCustomerDashboards` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void unassignCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't unassign dashboards from non-existent customer!");
        }
        new CustomerDashboardsUnassigner(customer).removeEntities(tenantId, customer);
    }

    /**
     * 功能：更新客户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void updateCustomerDashboards(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing updateCustomerDashboards, customerId [{}]", customerId);
        Validator.validateId(customerId, "Incorrect customerId " + customerId);
        Customer customer = customerDao.findById(tenantId, customerId.getId());
        if (customer == null) {
            throw new DataValidationException("Can't update dashboards for non-existent customer!");
        }
        new CustomerDashboardsUpdater(customer).removeEntities(tenantId, customer);
    }

    /**
     * 功能：执行 `assignDashboardToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard assignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't assign dashboard to non-existent edge!");
        }
        if (!edge.getTenantId().equals(dashboard.getTenantId())) {
            throw new DataValidationException("Can't assign dashboard to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return dashboard;
    }

    /**
     * 功能：执行 `unassignDashboardFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    @Override
    public Dashboard unassignDashboardFromEdge(TenantId tenantId, DashboardId dashboardId, EdgeId edgeId) {
        Dashboard dashboard = findDashboardById(tenantId, dashboardId);
        Edge edge = edgeDao.findById(tenantId, edgeId.getId());
        if (edge == null) {
            throw new DataValidationException("Can't unassign dashboard from non-existent edge!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, dashboardId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete dashboard relation. Edge Id: [{}]", dashboardId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(dashboardId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return dashboard;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<DashboardInfo> findDashboardsByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDashboardsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        Validator.validatePageLink(pageLink);
        return dashboardInfoDao.findDashboardsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public DashboardInfo findFirstDashboardInfoByTenantIdAndName(TenantId tenantId, String name) {
        return dashboardInfoDao.findFirstByTenantIdAndName(tenantId.getId(), name);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `title`：`title` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Dashboard> findTenantDashboardsByTitle(TenantId tenantId, String title) {
        return dashboardDao.findByTenantIdAndTitle(tenantId.getId(), title);
    }

    private final PaginatedRemover<TenantId, DashboardId> tenantDashboardsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<DashboardId> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return dashboardDao.findIdsByTenantId(id, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, DashboardId dashboardId) {
            deleteDashboard(tenantId, dashboardId);
        }
    };

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDashboardById(tenantId, new DashboardId(entityId.getId())));
    }

    /**
     * 功能：统计租户ID数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long countByTenantId(TenantId tenantId) {
        return dashboardDao.countByTenantId(tenantId);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：无。
     */
    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteDashboard(tenantId, (DashboardId) id);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.DASHBOARD;
    }

    /**
     * 中文说明：
     * 1. `CustomerDashboardsUnassigner` 是 ThingsBoard DAO 中负责客户存取的访问组件。
     * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
     * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
     * 4. 直接依赖的类型边界包括 `PaginatedRemover`。
     * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
     * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
     */
    private class CustomerDashboardsUnassigner extends PaginatedRemover<Customer, DashboardInfo> {

        /**
         * 客户对象，用于描述当前业务场景。
         */
        private Customer customer;

        /**
         * 功能：创建 `DashboardServiceImpl` 实例，并初始化必要字段。
         * 参数：
         * - `customer`：`customer` 参数。
         * 返回：新创建的对象实例。
         */
        CustomerDashboardsUnassigner(Customer customer) {
            this.customer = customer;
        }

        /**
         * 功能：获取`Entities`。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `customer`：`customer` 参数。
         * - `pageLink`：`pageLink` 参数。
         * 返回：匹配的数据集合。
         */
        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        /**
         * 功能：删除或清理实体。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `entity`：实体对象。
         * 返回：无。
         */
        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            unassignDashboardFromCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer.getId());
        }

    }

    /**
     * 中文说明：
     * 1. `CustomerDashboardsUpdater` 是 ThingsBoard DAO 中处理客户的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `PaginatedRemover`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    private class CustomerDashboardsUpdater extends PaginatedRemover<Customer, DashboardInfo> {

        /**
         * 客户对象，用于描述当前业务场景。
         */
        private Customer customer;

        /**
         * 功能：创建 `DashboardServiceImpl` 实例，并初始化必要字段。
         * 参数：
         * - `customer`：`customer` 参数。
         * 返回：新创建的对象实例。
         */
        CustomerDashboardsUpdater(Customer customer) {
            this.customer = customer;
        }

        /**
         * 功能：获取`Entities`。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `customer`：`customer` 参数。
         * - `pageLink`：`pageLink` 参数。
         * 返回：匹配的数据集合。
         */
        @Override
        protected PageData<DashboardInfo> findEntities(TenantId tenantId, Customer customer, PageLink pageLink) {
            return dashboardInfoDao.findDashboardsByTenantIdAndCustomerId(customer.getTenantId().getId(), customer.getId().getId(), pageLink);
        }

        /**
         * 功能：删除或清理实体。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `entity`：实体对象。
         * 返回：无。
         */
        @Override
        protected void removeEntity(TenantId tenantId, DashboardInfo entity) {
            updateAssignedCustomer(customer.getTenantId(), new DashboardId(entity.getUuidId()), this.customer);
        }

    }

}
