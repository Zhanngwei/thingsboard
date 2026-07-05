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
package org.thingsboard.server.service.apiusage;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageRecordState;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.notification.rule.trigger.ApiUsageLimitTrigger;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.UsageStatsKVProto;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.service.apiusage.BaseApiUsageState.StatsCalculationResult;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.InternalTelemetryService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbApiUsageStateService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTbApiUsageStateService extends AbstractPartitionBasedService<EntityId> implements TbApiUsageStateService {

    /**
     * `HOURLY`常量，用于统一引用固定值。
     */
    public static final String HOURLY = "Hourly";
    public static final FutureCallback<Integer> VOID_CALLBACK = new FutureCallback<Integer>() {
        @Override
        public void onSuccess(@Nullable Integer result) {
        }

        @Override
        public void onFailure(Throwable t) {
        }
    };
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService clusterService;
    private final PartitionService partitionService;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    private final TenantService tenantService;
    private final TimeseriesService tsService;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    private final ApiUsageStateService apiUsageStateService;
    private final TbTenantProfileCache tenantProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final MailService mailService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final DbCallbackExecutorService dbExecutor;
    private final MailExecutorService mailExecutor;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Lazy
    @Autowired
    private InternalTelemetryService tsWsService;

    // Entities that should be processed on this server
    final Map<EntityId, BaseApiUsageState> myUsageStates = new ConcurrentHashMap<>();
    // Entities that should be processed on other servers
    final Map<EntityId, ApiUsageState> otherUsageStates = new ConcurrentHashMap<>();

    final Set<EntityId> deletedEntities = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * 是否启用`enabled`。
     */
    @Value("${usage.stats.report.enabled:true}")
    private boolean enabled;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${usage.stats.check.cycle:60000}")
    private long nextCycleCheckInterval;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${usage.stats.gauge_report_interval:180000}")
    private long gaugeReportInterval;

    private final Lock updateLock = new ReentrantLock();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init();
        if (enabled) {
            log.info("Starting api usage service.");
            scheduledExecutor.scheduleAtFixedRate(this::checkStartOfNextCycle, nextCycleCheckInterval, nextCycleCheckInterval, TimeUnit.MILLISECONDS);
            log.info("Started api usage service.");
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getServiceName() {
        return "API Usage";
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getSchedulerExecutorName() {
        return "api-usage-scheduled";
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void process(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        ToUsageStatsServiceMsg statsMsg = msg.getValue();

        TenantId tenantId = TenantId.fromUUID(new UUID(statsMsg.getTenantIdMSB(), statsMsg.getTenantIdLSB()));
        EntityId ownerId;
        if (statsMsg.getCustomerIdMSB() != 0 && statsMsg.getCustomerIdLSB() != 0) {
            ownerId = new CustomerId(new UUID(statsMsg.getCustomerIdMSB(), statsMsg.getCustomerIdLSB()));
        } else {
            ownerId = tenantId;
        }

        processEntityUsageStats(tenantId, ownerId, statsMsg.getValuesList(), statsMsg.getServiceId());
        callback.onSuccess();
    }

    /**
     * 功能：处理实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ownerId`：`ownerId`ID。
     * - `values`：值。
     * - `serviceId`：服务ID。
     * 返回：无。
     */
    private void processEntityUsageStats(TenantId tenantId, EntityId ownerId, List<UsageStatsKVProto> values, String serviceId) {
        if (deletedEntities.contains(ownerId)) return;

        BaseApiUsageState usageState;
        List<TsKvEntry> updatedEntries;
        Map<ApiFeature, ApiUsageStateValue> result;

        updateLock.lock();
        try {
            usageState = getOrFetchState(tenantId, ownerId);
            long ts = usageState.getCurrentCycleTs();
            long hourTs = usageState.getCurrentHourTs();
            long newHourTs = SchedulerUtils.getStartOfCurrentHour();
            if (newHourTs != hourTs) {
                usageState.setHour(newHourTs);
            }
            updatedEntries = new ArrayList<>(ApiUsageRecordKey.values().length);
            Set<ApiFeature> apiFeatures = new HashSet<>();
            for (UsageStatsKVProto statsItem : values) {
                ApiUsageRecordKey recordKey = ApiUsageRecordKey.valueOf(statsItem.getKey());

                StatsCalculationResult calculationResult = usageState.calculate(recordKey, statsItem.getValue(), serviceId);
                if (calculationResult.isValueChanged()) {
                    long newValue = calculationResult.getNewValue();
                    updatedEntries.add(new BasicTsKvEntry(ts, new LongDataEntry(recordKey.getApiCountKey(), newValue)));
                }
                if (calculationResult.isHourlyValueChanged()) {
                    long newHourlyValue = calculationResult.getNewHourlyValue();
                    updatedEntries.add(new BasicTsKvEntry(newHourTs, new LongDataEntry(recordKey.getApiCountKey() + HOURLY, newHourlyValue)));
                }
                if (recordKey.getApiFeature() != null) {
                    apiFeatures.add(recordKey.getApiFeature());
                }
            }
            if (usageState.getEntityType() == EntityType.TENANT && !usageState.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
                result = ((TenantApiUsageState) usageState).checkStateUpdatedDueToThreshold(apiFeatures);
            } else {
                result = Collections.emptyMap();
            }
        } finally {
            updateLock.unlock();
        }
        tsWsService.saveAndNotifyInternal(tenantId, usageState.getApiUsageState().getId(), updatedEntries, VOID_CALLBACK);
        if (!result.isEmpty()) {
            persistAndNotify(usageState, result);
        }
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：处理结果。
     */
    @Override
    public ApiUsageState getApiUsageState(TenantId tenantId) {
        TenantApiUsageState tenantState = (TenantApiUsageState) myUsageStates.get(tenantId);
        if (tenantState != null) {
            return tenantState.getApiUsageState();
        } else {
            ApiUsageState state = otherUsageStates.get(tenantId);
            if (state != null) {
                return state;
            } else {
                if (partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
                    return getOrFetchState(tenantId, tenantId).getApiUsageState();
                } else {
                    state = otherUsageStates.get(tenantId);
                    if (state == null) {
                        state = apiUsageStateService.findTenantApiUsageState(tenantId);
                        if (state != null) {
                            otherUsageStates.put(tenantId, state);
                        }
                    }
                    return state;
                }
            }
        }
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void onApiUsageStateUpdate(TenantId tenantId) {
        otherUsageStates.remove(tenantId);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void onTenantProfileUpdate(TenantProfileId tenantProfileId) {
        log.info("[{}] On Tenant Profile Update", tenantProfileId);
        TenantProfile tenantProfile = tenantProfileCache.get(tenantProfileId);
        updateLock.lock();
        try {
            myUsageStates.values().stream()
                    .filter(state -> state.getEntityType() == EntityType.TENANT)
                    .map(state -> (TenantApiUsageState) state)
                    .forEach(state -> {
                        if (tenantProfile.getId().equals(state.getTenantProfileId())) {
                            updateTenantState(state, tenantProfile);
                        }
                    });
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void onTenantUpdate(TenantId tenantId) {
        log.info("[{}] On Tenant Update.", tenantId);
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        updateLock.lock();
        try {
            TenantApiUsageState state = (TenantApiUsageState) myUsageStates.get(tenantId);
            if (state != null && !state.getTenantProfileId().equals(tenantProfile.getId())) {
                updateTenantState(state, tenantProfile);
            }
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 功能：更新租户。
     * 参数：
     * - `state`：`state` 参数。
     * - `profile`：`profile` 参数。
     * 返回：无。
     */
    private void updateTenantState(TenantApiUsageState state, TenantProfile profile) {
        TenantProfileData oldProfileData = state.getTenantProfileData();
        state.setTenantProfileId(profile.getId());
        state.setTenantProfileData(profile.getProfileData());
        Map<ApiFeature, ApiUsageStateValue> result = state.checkStateUpdatedDueToThresholds();
        if (!result.isEmpty()) {
            persistAndNotify(state, result);
        }
        updateProfileThresholds(state.getTenantId(), state.getApiUsageState().getId(),
                oldProfileData.getConfiguration(), profile.getProfileData().getConfiguration());
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    private void addEntityState(TopicPartitionInfo tpi, BaseApiUsageState state) {
        EntityId entityId = state.getEntityId();
        Set<EntityId> entityIds = partitionedEntities.get(tpi);
        if (entityIds != null) {
            entityIds.add(entityId);
            myUsageStates.put(entityId, state);
        } else {
            log.debug("[{}] belongs to external partition {}", entityId, tpi.getFullTopicName());
            throw new RuntimeException(entityId.getEntityType() + " belongs to external partition " + tpi.getFullTopicName() + "!");
        }
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `oldData`：待处理数据。
     * - `newData`：待处理数据。
     * 返回：无。
     */
    private void updateProfileThresholds(TenantId tenantId, ApiUsageStateId id,
                                         TenantProfileConfiguration oldData, TenantProfileConfiguration newData) {
        long ts = System.currentTimeMillis();
        List<TsKvEntry> profileThresholds = new ArrayList<>();
        for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
            long newProfileThreshold = newData.getProfileThreshold(key);
            if (oldData == null || oldData.getProfileThreshold(key) != newProfileThreshold) {
                log.info("[{}] Updating profile threshold [{}]:[{}]", tenantId, key, newProfileThreshold);
                profileThresholds.add(new BasicTsKvEntry(ts, new LongDataEntry(key.getApiLimitKey(), newProfileThreshold)));
            }
        }
        if (!profileThresholds.isEmpty()) {
            tsWsService.saveAndNotifyInternal(tenantId, id, profileThresholds, VOID_CALLBACK);
        }
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void onTenantDelete(TenantId tenantId) {
        deletedEntities.add(tenantId);
        myUsageStates.remove(tenantId);
        otherUsageStates.remove(tenantId);
    }

    /**
     * 功能：处理客户。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    public void onCustomerDelete(CustomerId customerId) {
        deletedEntities.add(customerId);
        myUsageStates.remove(customerId);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：无。
     */
    @Override
    protected void cleanupEntityOnPartitionRemoval(EntityId entityId) {
        myUsageStates.remove(entityId);
    }

    /**
     * 功能：执行 `persistAndNotify` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * - `result`：键值映射。
     * 返回：无。
     */
    private void persistAndNotify(BaseApiUsageState state, Map<ApiFeature, ApiUsageStateValue> result) {
        log.info("[{}] Detected update of the API state for {}: {}", state.getEntityId(), state.getEntityType(), result);
        apiUsageStateService.update(state.getApiUsageState());
        clusterService.onApiStateChange(state.getApiUsageState(), null);
        long ts = System.currentTimeMillis();
        List<TsKvEntry> stateTelemetry = new ArrayList<>();
        result.forEach((apiFeature, aState) -> stateTelemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(apiFeature.getApiStateKey(), aState.name()))));
        tsWsService.saveAndNotifyInternal(state.getTenantId(), state.getApiUsageState().getId(), stateTelemetry, VOID_CALLBACK);

        if (state.getEntityType() == EntityType.TENANT && !state.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
            String email = tenantService.findTenantById(state.getTenantId()).getEmail();
            result.forEach((apiFeature, stateValue) -> {
                ApiUsageRecordState recordState = createApiUsageRecordState((TenantApiUsageState) state, apiFeature, stateValue);
                if (recordState == null) {
                    return;
                }
                notificationRuleProcessor.process(ApiUsageLimitTrigger.builder()
                        .tenantId(state.getTenantId())
                        .state(recordState)
                        .status(stateValue)
                        .build());
                if (StringUtils.isNotEmpty(email)) {
                    mailExecutor.submit(() -> {
                        try {
                            mailService.sendApiFeatureStateEmail(apiFeature, stateValue, email, recordState);
                        } catch (ThingsboardException e) {
                            log.warn("[{}] Can't send update of the API state to tenant with provided email [{}]", state.getTenantId(), email, e);
                        }
                    });
                }
            });
        }
    }

    /**
     * 功能：保存或创建状态。
     * 参数：
     * - `state`：`state` 参数。
     * - `apiFeature`：`apiFeature` 参数。
     * - `stateValue`：值。
     * 返回：处理结果。
     */
    private ApiUsageRecordState createApiUsageRecordState(TenantApiUsageState state, ApiFeature apiFeature, ApiUsageStateValue stateValue) {
        StateChecker checker = getStateChecker(stateValue);
        for (ApiUsageRecordKey apiUsageRecordKey : ApiUsageRecordKey.getKeys(apiFeature)) {
            long threshold = state.getProfileThreshold(apiUsageRecordKey);
            long warnThreshold = state.getProfileWarnThreshold(apiUsageRecordKey);
            long value = state.get(apiUsageRecordKey);
            if (checker.check(threshold, warnThreshold, value)) {
                return new ApiUsageRecordState(apiFeature, apiUsageRecordKey, threshold, value);
            }
        }
        return null;
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `stateValue`：值。
     * 返回：处理结果。
     */
    private StateChecker getStateChecker(ApiUsageStateValue stateValue) {
        if (ApiUsageStateValue.ENABLED.equals(stateValue)) {
            return (t, wt, v) -> true;
        } else if (ApiUsageStateValue.WARNING.equals(stateValue)) {
            return (t, wt, v) -> v < t && v >= wt;
        } else {
            return (t, wt, v) -> t > 0 && v >= t;
        }
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
        return apiUsageStateService.findApiUsageStateById(tenantId, id);
    }

    /**
     * 中文说明：
     * 1. 类目的：`StateChecker` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    private interface StateChecker {
        /**
         * 功能：执行 `check` 对应的处理。
         * 参数：
         * - `threshold`：`threshold` 参数。
         * - `warnThreshold`：`warnThreshold` 参数。
         * - `value`：值。
         * 返回：判断结果。
         */
        boolean check(long threshold, long warnThreshold, long value);
    }

    /**
     * 功能：校验`Start Of Next Cycle`。
     * 参数：无。
     * 返回：无。
     */
    private void checkStartOfNextCycle() {
        updateLock.lock();
        try {
            long now = System.currentTimeMillis();
            myUsageStates.values().forEach(state -> {
                if ((state.getNextCycleTs() < now) && (now - state.getNextCycleTs() < TimeUnit.HOURS.toMillis(1))) {
                    state.setCycles(state.getNextCycleTs(), SchedulerUtils.getStartOfNextNextMonth());
                    saveNewCounts(state, Arrays.asList(ApiUsageRecordKey.values()));
                    if (state.getEntityType() == EntityType.TENANT && !state.getEntityId().equals(TenantId.SYS_TENANT_ID)) {
                        TenantId tenantId = state.getTenantId();
                        updateTenantState((TenantApiUsageState) state, tenantProfileCache.get(tenantId));
                    }
                }
            });
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 功能：保存或创建`New Counts`。
     * 参数：
     * - `state`：`state` 参数。
     * - `keys`：键。
     * 返回：无。
     */
    private void saveNewCounts(BaseApiUsageState state, List<ApiUsageRecordKey> keys) {
        List<TsKvEntry> counts = keys.stream()
                .map(key -> new BasicTsKvEntry(state.getCurrentCycleTs(), new LongDataEntry(key.getApiCountKey(), 0L)))
                .collect(Collectors.toList());

        tsWsService.saveAndNotifyInternal(state.getTenantId(), state.getApiUsageState().getId(), counts, VOID_CALLBACK);
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `ownerId`：`ownerId`ID。
     * 返回：处理结果。
     */
    BaseApiUsageState getOrFetchState(TenantId tenantId, EntityId ownerId) {
        if (ownerId == null || ownerId.isNullUid()) {
            ownerId = tenantId;
        }
        BaseApiUsageState state = myUsageStates.get(ownerId);
        if (state != null) {
            return state;
        }

        ApiUsageState storedState = apiUsageStateService.findApiUsageStateByEntityId(ownerId);
        if (storedState == null) {
            try {
                storedState = apiUsageStateService.createDefaultApiUsageState(tenantId, ownerId);
            } catch (Exception e) {
                storedState = apiUsageStateService.findApiUsageStateByEntityId(ownerId);
            }
        }
        if (ownerId.getEntityType() == EntityType.TENANT) {
            if (!ownerId.equals(TenantId.SYS_TENANT_ID)) {
                state = new TenantApiUsageState(tenantProfileCache.get((TenantId) ownerId), storedState);
            } else {
                state = new TenantApiUsageState(storedState);
            }
        } else {
            state = new CustomerApiUsageState(storedState);
        }

        List<ApiUsageRecordKey> newCounts = new ArrayList<>();
        try {
            List<TsKvEntry> dbValues = tsService.findAllLatest(tenantId, storedState.getId()).get();
            for (ApiUsageRecordKey key : ApiUsageRecordKey.values()) {
                boolean cycleEntryFound = false;
                boolean hourlyEntryFound = false;
                for (TsKvEntry tsKvEntry : dbValues) {
                    if (tsKvEntry.getKey().equals(key.getApiCountKey())) {
                        cycleEntryFound = true;

                        boolean oldCount = tsKvEntry.getTs() == state.getCurrentCycleTs();
                        state.set(key, oldCount ? tsKvEntry.getLongValue().get() : 0L);

                        if (!oldCount) {
                            newCounts.add(key);
                        }
                    } else if (tsKvEntry.getKey().equals(key.getApiCountKey() + HOURLY)) {
                        hourlyEntryFound = true;
                        state.setHourly(key, tsKvEntry.getTs() == state.getCurrentHourTs() ? tsKvEntry.getLongValue().get() : 0L);
                    }
                    if (cycleEntryFound && hourlyEntryFound) {
                        break;
                    }
                }
            }
            state.setGaugeReportInterval(gaugeReportInterval);
            log.debug("[{}] Initialized state: {}", ownerId, storedState);
            TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, ownerId);
            if (tpi.isMyPartition()) {
                addEntityState(tpi, state);
            } else {
                otherUsageStates.put(ownerId, state.getApiUsageState());
            }
            saveNewCounts(state, newCounts);
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch api usage state from db.", tenantId, e);
        }

        return state;
    }

    /**
     * 功能：处理事件。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void onRepartitionEvent() {
        otherUsageStates.entrySet().removeIf(entry ->
                partitionService.resolve(ServiceType.TB_CORE, entry.getValue().getTenantId(), entry.getKey()).isMyPartition());
        updateLock.lock();
        try {
            myUsageStates.values().forEach(BaseApiUsageState::onRepartitionEvent);
        } finally {
            updateLock.unlock();
        }
    }

    /**
     * 功能：处理`on Added Partitions`。
     * 参数：
     * - `addedPartitions`：分区标识或分区信息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Map<TopicPartitionInfo, List<ListenableFuture<?>>> onAddedPartitions(Set<TopicPartitionInfo> addedPartitions) {
        var result = new HashMap<TopicPartitionInfo, List<ListenableFuture<?>>>();
        try {
            log.info("Initializing tenant states.");
            updateLock.lock();
            try {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, 1024);
                for (Tenant tenant : tenantIterator) {
                    TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenant.getId(), tenant.getId());
                    if (addedPartitions.contains(tpi)) {
                        if (!myUsageStates.containsKey(tenant.getId()) && tpi.isMyPartition()) {
                            log.debug("[{}] Initializing tenant state.", tenant.getId());
                            result.computeIfAbsent(tpi, tmp -> new ArrayList<>()).add(dbExecutor.submit(() -> {
                                try {
                                    updateTenantState((TenantApiUsageState) getOrFetchState(tenant.getId(), tenant.getId()), tenantProfileCache.get(tenant.getTenantProfileId()));
                                    log.debug("[{}] Initialized tenant state.", tenant.getId());
                                } catch (Exception e) {
                                    log.warn("[{}] Failed to initialize tenant API state", tenant.getId(), e);
                                }
                                return null;
                            }));
                        }
                    } else {
                        log.debug("[{}][{}] Tenant doesn't belong to current partition. tpi [{}]", tenant.getName(), tenant.getId(), tpi);
                    }
                }
            } finally {
                updateLock.unlock();
            }
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
        return result;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void destroy() {
        super.stop();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbApiUsageStateService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
