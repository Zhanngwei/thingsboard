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
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `TenantServiceImpl` 是 ThingsBoard DAO 中负责租户的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCachedEntityService`、`TenantService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("TenantDaoService")
@Slf4j
public class TenantServiceImpl extends AbstractCachedEntityService<TenantId, Tenant, TenantEvictEvent> implements TenantService {

    /**
     * 租户常量，用于统一引用固定值。
     */
    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantDao tenantDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    private TenantProfileService tenantProfileService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private UserService userService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    private CustomerService customerService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetService assetService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AssetProfileService assetProfileService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceService deviceService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceProfileService deviceProfileService;

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private ApiUsageStateService apiUsageStateService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Autowired
    private WidgetTypeService widgetTypeService;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    private DashboardService dashboardService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    private RuleChainService ruleChainService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ResourceService resourceService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private OtaPackageService otaPackageService;

    /**
     * RPC，提供当前类调用的业务操作。
     */
    @Autowired
    private RpcService rpcService;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    private DataValidator<Tenant> tenantValidator;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private QueueService queueService;

    /**
     * 配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private AdminSettingsService adminSettingsService;

    /**
     * 通知服务集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    private NotificationSettingsService notificationSettingsService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    private NotificationRequestService notificationRequestService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    private NotificationRuleService notificationRuleService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    private NotificationTemplateService notificationTemplateService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    private NotificationTargetService notificationTargetService;

    /**
     * 是否满足租户条件。
     */
    @Autowired
    protected TbTransactionalCache<TenantId, Boolean> existsTenantCache;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = TenantEvictEvent.class)
    @Override
    public void handleEvictEvent(TenantEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
        if (event.isInvalidateExists()) {
            existsTenantCache.evict(tenantId);
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);

        return cache.getAndPutInTransaction(tenantId, () -> tenantDao.findById(tenantId, tenantId.getId()), true);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId) {
        log.trace("Executing findTenantInfoById [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findTenantInfoById(tenantId, tenantId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `callerId`：`callerId`ID。
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId) {
        log.trace("Executing findTenantByIdAsync [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    /**
     * 功能：保存或创建租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @Override
    @Transactional
    public Tenant saveTenant(Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        if (tenant.getTenantProfileId() == null) {
            TenantProfile tenantProfile = this.tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);
            tenant.setTenantProfileId(tenantProfile.getId());
        }
        tenantValidator.validate(tenant, Tenant::getId);
        boolean create = tenant.getId() == null;
        Tenant savedTenant = tenantDao.save(tenant.getId(), tenant);
        publishEvictEvent(new TenantEvictEvent(savedTenant.getId(), create));
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedTenant.getId()).entityId(savedTenant.getId()).created(create).build());
        if (tenant.getId() == null) {
            deviceProfileService.createDefaultDeviceProfile(savedTenant.getId());
            assetProfileService.createDefaultAssetProfile(savedTenant.getId());
            apiUsageStateService.createDefaultApiUsageState(savedTenant.getId(), null);
            try {
                notificationSettingsService.createDefaultNotificationConfigs(savedTenant.getId());
            } catch (Throwable e) {
                log.error("Failed to create default notification configs for tenant {}", savedTenant.getId(), e);
            }
        }
        return savedTenant;
    }

    /**
     * We intentionally leave this method without "Transactional" annotation due to complexity of the method.
     * Ideally we should delete related entites without "paginatedRemover" logic. But in such a case we can't clear cache and send events.
     * We will create separate task to make "deleteTenant" transactional.
     */
    /**
     * 功能：删除或清理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        widgetTypeService.deleteWidgetTypesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        assetProfileService.deleteAssetProfilesByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        deviceProfileService.deleteDeviceProfilesByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        edgeService.deleteEdgesByTenantId(tenantId);
        userService.deleteTenantAdmins(tenantId);
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        apiUsageStateService.deleteApiUsageStateByTenantId(tenantId);
        resourceService.deleteResourcesByTenantId(tenantId);
        otaPackageService.deleteOtaPackagesByTenantId(tenantId);
        rpcService.deleteAllRpcByTenantId(tenantId);
        queueService.deleteQueuesByTenantId(tenantId);
        notificationRequestService.deleteNotificationRequestsByTenantId(tenantId);
        notificationRuleService.deleteNotificationRulesByTenantId(tenantId);
        notificationTemplateService.deleteNotificationTemplatesByTenantId(tenantId);
        notificationTargetService.deleteNotificationTargetsByTenantId(tenantId);
        notificationSettingsService.deleteNotificationSettings(tenantId);
        adminSettingsService.deleteAdminSettingsByTenantId(tenantId);
        tenantDao.removeById(tenantId, tenantId.getId());
        publishEvictEvent(new TenantEvictEvent(tenantId, true));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(tenantId).build());
        relationService.deleteEntityRelations(tenantId, tenantId);
        alarmService.deleteEntityAlarmRecordsByTenantId(tenantId);
    }

    /**
     * 功能：获取`Tenants`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Tenant> findTenants(PageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenants(TenantId.SYS_TENANT_ID, pageLink);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TenantInfo> findTenantInfos(PageLink pageLink) {
        log.trace("Executing findTenantInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantInfos(TenantId.SYS_TENANT_ID, pageLink);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantsByTenantProfileId [{}]", tenantProfileId);
        return tenantDao.findTenantIdsByTenantProfileId(tenantProfileId);
    }

    /**
     * 功能：删除或清理`Tenants`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
    }

    /**
     * 功能：获取`Tenants Ids`。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        log.trace("Executing findTenantsIds");
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantsIds(pageLink);
    }

    /**
     * 功能：执行 `tenantExists` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    public boolean tenantExists(TenantId tenantId) {
        return existsTenantCache.getAndPutInTransaction(tenantId, () -> tenantDao.existsById(tenantId, tenantId.getId()), false);
    }

    private PaginatedRemover<TenantId, Tenant> tenantsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Tenant> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return tenantDao.findTenants(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Tenant entity) {
            deleteTenant(TenantId.fromUUID(entity.getUuidId()));
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
        return Optional.ofNullable(findTenantById(new TenantId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
