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
package org.thingsboard.server.dao.usagerecord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `ApiUsageStateServiceImpl` 是 ThingsBoard DAO 中负责用量统计的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractEntityService`、`ApiUsageStateService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service("ApiUsageStateDaoService")
@Slf4j
public class ApiUsageStateServiceImpl extends AbstractEntityService implements ApiUsageStateService {
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 状态，用于读取或保存对应领域对象。
     */
    private final ApiUsageStateDao apiUsageStateDao;
    private final TenantProfileDao tenantProfileDao;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final TimeseriesService tsService;
    /**
     * 状态，表示当前对象所处状态。
     */
    private final DataValidator<ApiUsageState> apiUsageStateValidator;

    /**
     * 功能：创建 `ApiUsageStateServiceImpl` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageStateDao`：`apiUsageStateDao` 参数。
     * - `tenantProfileDao`：租户信息或租户标识。
     * - `tenantService`：租户信息或租户标识。
     * - `tsService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public ApiUsageStateServiceImpl(ApiUsageStateDao apiUsageStateDao, TenantProfileDao tenantProfileDao,
                                    TenantService tenantService, @Lazy TimeseriesService tsService,
                                    DataValidator<ApiUsageState> apiUsageStateValidator) {
        this.apiUsageStateDao = apiUsageStateDao;
        this.tenantProfileDao = tenantProfileDao;
        this.tenantService = tenantService;
        this.tsService = tsService;
        this.apiUsageStateValidator = apiUsageStateValidator;
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteApiUsageStateByTenantId(TenantId tenantId) {
        log.trace("Executing deleteUsageRecordsByTenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        apiUsageStateDao.deleteApiUsageStateByTenantId(tenantId);
    }

    /**
     * 功能：删除或清理实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    public void deleteApiUsageStateByEntityId(EntityId entityId) {
        log.trace("Executing deleteApiUsageStateByEntityId [{}]", entityId);
        validateId(entityId.getId(), "Invalid entity id");
        apiUsageStateDao.deleteApiUsageStateByEntityId(entityId);
    }

    /**
     * 功能：保存或创建状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState createDefaultApiUsageState(TenantId tenantId, EntityId entityId) {
        entityId = Objects.requireNonNullElse(entityId, tenantId);
        log.trace("Executing createDefaultUsageRecord [{}]", entityId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ApiUsageState apiUsageState = new ApiUsageState();
        apiUsageState.setTenantId(tenantId);
        apiUsageState.setEntityId(entityId);
        apiUsageState.setTransportState(ApiUsageStateValue.ENABLED);
        apiUsageState.setReExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setJsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setTbelExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setDbStorageState(ApiUsageStateValue.ENABLED);
        apiUsageState.setSmsExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setEmailExecState(ApiUsageStateValue.ENABLED);
        apiUsageState.setAlarmExecState(ApiUsageStateValue.ENABLED);
        apiUsageStateValidator.validate(apiUsageState, ApiUsageState::getTenantId);

        ApiUsageState saved = apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);

        List<TsKvEntry> apiUsageStates = new ArrayList<>();
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.TRANSPORT.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.DB.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.RE.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.JS.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.TBEL.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.EMAIL.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.SMS.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        apiUsageStates.add(new BasicTsKvEntry(saved.getCreatedTime(),
                new StringDataEntry(ApiFeature.ALARM.getApiStateKey(), ApiUsageStateValue.ENABLED.name())));
        tsService.save(tenantId, saved.getId(), apiUsageStates, 0L);

        if (entityId.getEntityType() == EntityType.TENANT && !entityId.equals(TenantId.SYS_TENANT_ID)) {
            tenantId = (TenantId) entityId;
            Tenant tenant = tenantService.findTenantById(tenantId);
            TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenant.getTenantProfileId().getId());
            TenantProfileConfiguration configuration = tenantProfile.getProfileData().getConfiguration();

            List<TsKvEntry> profileThresholds = new ArrayList<>();
            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                if (key.getApiLimitKey() == null) continue;
                profileThresholds.add(new BasicTsKvEntry(saved.getCreatedTime(), new LongDataEntry(key.getApiLimitKey(), configuration.getProfileThreshold(key))));
            }
            tsService.save(tenantId, saved.getId(), profileThresholds, 0L);
        }

        return saved;
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `apiUsageState`：`apiUsageState` 参数。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState update(ApiUsageState apiUsageState) {
        log.trace("Executing save [{}]", apiUsageState.getTenantId());
        validateId(apiUsageState.getTenantId(), INCORRECT_TENANT_ID + apiUsageState.getTenantId());
        validateId(apiUsageState.getId(), "Can't save new usage state. Only update is allowed!");
        return apiUsageStateDao.save(apiUsageState.getTenantId(), apiUsageState);
    }

    /**
     * 功能：获取租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState findTenantApiUsageState(TenantId tenantId) {
        log.trace("Executing findTenantUsageRecord, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return apiUsageStateDao.findTenantApiUsageState(tenantId.getId());
    }

    /**
     * 功能：获取实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState findApiUsageStateByEntityId(EntityId entityId) {
        validateId(entityId.getId(), "Invalid entity id");
        return apiUsageStateDao.findApiUsageStateByEntityId(entityId);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id) {
        log.trace("Executing findApiUsageStateById, tenantId [{}], apiUsageStateId [{}]", tenantId, id);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(id, "Incorrect apiUsageStateId " + id);
        return apiUsageStateDao.findById(tenantId, id.getId());
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
        return Optional.ofNullable(findApiUsageStateById(tenantId, new ApiUsageStateId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
