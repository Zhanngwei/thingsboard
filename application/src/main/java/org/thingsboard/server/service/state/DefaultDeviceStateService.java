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
package org.thingsboard.server.service.state;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.TenantNotFoundException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.notification.rule.trigger.DeviceActivityTrigger;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityListFilter;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.sql.query.EntityQueryRepository;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.util.DbTypeInfoComponent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.partition.AbstractPartitionBasedService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.DataConstants.SERVER_SCOPE;

/**
 * Created by ashvayka on 01.05.18.
 */
/**
 * 中文说明：
 * 1. `DefaultDeviceStateService` 是 ThingsBoard Application 中负责设备的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractPartitionBasedService`、`DeviceStateService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultDeviceStateService extends AbstractPartitionBasedService<DeviceId> implements DeviceStateService {

    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String ACTIVITY_STATE = "active";
    public static final String LAST_CONNECT_TIME = "lastConnectTime";
    /**
     * 时间常量，用于统一引用固定值。
     */
    public static final String LAST_DISCONNECT_TIME = "lastDisconnectTime";
    public static final String LAST_ACTIVITY_TIME = "lastActivityTime";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String INACTIVITY_ALARM_TIME = "inactivityAlarmTime";
    public static final String INACTIVITY_TIMEOUT = "inactivityTimeout";

    private static final List<EntityKey> PERSISTENT_TELEMETRY_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.TIME_SERIES, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.TIME_SERIES, LAST_DISCONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT));

    private static final List<EntityKey> PERSISTENT_ATTRIBUTE_KEYS = Arrays.asList(
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_ALARM_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, ACTIVITY_STATE),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_CONNECT_TIME),
            new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_DISCONNECT_TIME));

    public static final List<String> PERSISTENT_ATTRIBUTES = Arrays.asList(ACTIVITY_STATE, LAST_CONNECT_TIME,
            LAST_DISCONNECT_TIME, LAST_ACTIVITY_TIME, INACTIVITY_ALARM_TIME, INACTIVITY_TIMEOUT);
    private static final List<EntityKey> PERSISTENT_ENTITY_FIELDS = Arrays.asList(
            new EntityKey(EntityKeyType.ENTITY_FIELD, "name"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "type"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "label"),
            new EntityKey(EntityKeyType.ENTITY_FIELD, "createdTime"));

    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceService deviceService;
    private final AttributesService attributesService;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    private final TimeseriesService tsService;
    private final TbClusterService clusterService;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final EntityQueryRepository entityQueryRepository;
    /**
     * 类型，用于区分不同处理分支。
     */
    private final DbTypeInfoComponent dbTypeInfoComponent;
    private final TbApiUsageReportClient apiUsageReportClient;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final NotificationRuleProcessor notificationRuleProcessor;
    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired @Lazy
    private TelemetrySubscriptionService tsSubService;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${state.defaultInactivityTimeoutInSec}")
    @Getter
    @Setter
    private long defaultInactivityTimeoutInSec;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("#{${state.defaultInactivityTimeoutInSec} * 1000}")
    @Getter
    @Setter
    private long defaultInactivityTimeoutMs;

    /**
     * 状态，表示当前对象所处状态。
     */
    @Value("${state.defaultStateCheckIntervalInSec}")
    @Getter
    private int defaultStateCheckIntervalInSec;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${usage.stats.devices.report_interval:60}")
    @Getter
    private int defaultActivityStatsIntervalInSec;

    /**
     * 是否满足遥测条件。
     */
    @Value("${state.persistToTelemetry:false}")
    @Getter
    @Setter
    private boolean persistToTelemetry;

    /**
     * `initFetchPackSize` 字段，保存当前对象的对应属性。
     */
    @Value("${state.initFetchPackSize:50000}")
    @Getter
    private int initFetchPackSize;

    /**
     * 遥测，表示当前对象的对应属性。
     */
    @Value("${state.telemetryTtl:0}")
    @Getter
    private int telemetryTtl;

    /**
     * 设备列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService deviceStateExecutor;
    private ListeningExecutorService deviceStateCallbackExecutor;

    final ConcurrentMap<DeviceId, DeviceStateData> deviceStates = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init();
        deviceStateExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state"));
        deviceStateCallbackExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()), "device-state-callback"));
        scheduledExecutor.scheduleWithFixedDelay(this::checkStates, new Random().nextInt(defaultStateCheckIntervalInSec), defaultStateCheckIntervalInSec, TimeUnit.SECONDS);
        scheduledExecutor.scheduleWithFixedDelay(this::reportActivityStats, defaultActivityStatsIntervalInSec, defaultActivityStatsIntervalInSec, TimeUnit.SECONDS);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void stop() {
        super.stop();
        if (deviceStateExecutor != null) {
            deviceStateExecutor.shutdownNow();
        }
        if (deviceStateCallbackExecutor != null) {
            deviceStateCallbackExecutor.shutdownNow();
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getServiceName() {
        return "Device State";
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getSchedulerExecutorName() {
        return "device-state-scheduled";
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastConnectTime`：`lastConnectTime` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long lastConnectTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastConnectTime < 0) {
            log.trace("[{}][{}] On device connect: received negative last connect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastConnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastConnectTime = stateData.getState().getLastConnectTime();
        if (lastConnectTime <= currentLastConnectTime) {
            log.trace("[{}][{}] On device connect: received outdated last connect ts [{}]. Skipping this event. Current last connect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastConnectTime, currentLastConnectTime);
            return;
        }
        log.trace("[{}][{}] On device connect: processing connect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastConnectTime);
        stateData.getState().setLastConnectTime(lastConnectTime);
        save(deviceId, LAST_CONNECT_TIME, lastConnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.CONNECT_EVENT);
        checkAndUpdateState(deviceId, stateData);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastReportedActivity`：`lastReportedActivity` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long lastReportedActivity) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        log.trace("[{}] on Device Activity [{}], lastReportedActivity [{}]", tenantId.getId(), deviceId.getId(), lastReportedActivity);
        final DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        if (lastReportedActivity > 0 && lastReportedActivity > stateData.getState().getLastActivityTime()) {
            updateActivityState(deviceId, stateData, lastReportedActivity);
        }
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `stateData`：待处理数据。
     * - `lastReportedActivity`：`lastReportedActivity` 参数。
     * 返回：无。
     */
    void updateActivityState(DeviceId deviceId, DeviceStateData stateData, long lastReportedActivity) {
        log.trace("updateActivityState - fetched state {} for device {}, lastReportedActivity {}", stateData, deviceId, lastReportedActivity);
        if (stateData != null) {
            save(deviceId, LAST_ACTIVITY_TIME, lastReportedActivity);
            DeviceState state = stateData.getState();
            state.setLastActivityTime(lastReportedActivity);
            if (!state.isActive()) {
                state.setActive(true);
                if (lastReportedActivity <= state.getLastInactivityAlarmTime()) {
                    state.setLastInactivityAlarmTime(0);
                    save(deviceId, INACTIVITY_ALARM_TIME, 0);
                }
                onDeviceActivityStatusChange(deviceId, true, stateData);
            }
        } else {
            log.debug("updateActivityState - fetched state IS NULL for device {}, lastReportedActivity {}", deviceId, lastReportedActivity);
            cleanupEntity(deviceId);
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastDisconnectTime`：`lastDisconnectTime` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long lastDisconnectTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastDisconnectTime < 0) {
            log.trace("[{}][{}] On device disconnect: received negative last disconnect ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastDisconnectTime = stateData.getState().getLastDisconnectTime();
        if (lastDisconnectTime <= currentLastDisconnectTime) {
            log.trace("[{}][{}] On device disconnect: received outdated last disconnect ts [{}]. Skipping this event. Current last disconnect ts [{}].",
                    tenantId.getId(), deviceId.getId(), lastDisconnectTime, currentLastDisconnectTime);
            return;
        }
        log.trace("[{}][{}] On device disconnect: processing disconnect event with ts [{}].", tenantId.getId(), deviceId.getId(), lastDisconnectTime);
        stateData.getState().setLastDisconnectTime(lastDisconnectTime);
        save(deviceId, LAST_DISCONNECT_TIME, lastDisconnectTime);
        pushRuleEngineMessage(stateData, TbMsgType.DISCONNECT_EVENT);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `inactivityTimeout`：`inactivityTimeout` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceInactivityTimeoutUpdate(TenantId tenantId, DeviceId deviceId, long inactivityTimeout) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (inactivityTimeout <= 0L) {
            inactivityTimeout = defaultInactivityTimeoutMs;
        }
        log.trace("[{}] on Device Activity Timeout Update device id {} inactivityTimeout {}", tenantId.getId(), deviceId.getId(), inactivityTimeout);
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        stateData.getState().setInactivityTimeout(inactivityTimeout);
        checkAndUpdateState(deviceId, stateData);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `lastInactivityTime`：`lastInactivityTime` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long lastInactivityTime) {
        if (cleanDeviceStateIfBelongsToExternalPartition(tenantId, deviceId)) {
            return;
        }
        if (lastInactivityTime < 0) {
            log.trace("[{}][{}] On device inactivity: received negative last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime);
            return;
        }
        DeviceStateData stateData = getOrFetchDeviceStateData(deviceId);
        long currentLastInactivityAlarmTime = stateData.getState().getLastInactivityAlarmTime();
        if (lastInactivityTime <= currentLastInactivityAlarmTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less than current last inactivity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastInactivityAlarmTime);
            return;
        }
        long currentLastActivityTime = stateData.getState().getLastActivityTime();
        if (lastInactivityTime <= currentLastActivityTime) {
            log.trace("[{}][{}] On device inactivity: received last inactivity ts [{}] is less or equal to current last activity ts [{}]. Skipping this event.",
                    tenantId.getId(), deviceId.getId(), lastInactivityTime, currentLastActivityTime);
            return;
        }
        log.trace("[{}][{}] On device inactivity: processing inactivity event with ts [{}].", tenantId.getId(), deviceId.getId(), lastInactivityTime);
        reportInactivity(lastInactivityTime, deviceId, stateData);
    }

    /**
     * 功能：处理队列。
     * 参数：
     * - `proto`：`proto` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onQueueMsg(TransportProtos.DeviceStateServiceMsgProto proto, TbCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(proto.getDeviceIdMSB(), proto.getDeviceIdLSB()));
            if (proto.getDeleted()) {
                onDeviceDeleted(tenantId, deviceId);
                callback.onSuccess();
            } else {
                Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
                if (device != null) {
                    if (proto.getAdded()) {
                        Futures.addCallback(fetchDeviceState(device), new FutureCallback<>() {
                            @Override
                            public void onSuccess(@Nullable DeviceStateData state) {
                                TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, device.getId());
                                if (addDeviceUsingState(tpi, state)) {
                                    save(deviceId, ACTIVITY_STATE, false);
                                    callback.onSuccess();
                                } else {
                                    log.debug("[{}][{}] Device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                                            , tenantId, deviceId, tpi.getFullTopicName());
                                    callback.onFailure(new RuntimeException("Device belongs to external partition " + tpi.getFullTopicName() + "!"));
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                log.warn("Failed to register device to the state service", t);
                                callback.onFailure(t);
                            }
                        }, deviceStateCallbackExecutor);
                    } else if (proto.getUpdated()) {
                        DeviceStateData stateData = getOrFetchDeviceStateData(device.getId());
                        TbMsgMetaData md = new TbMsgMetaData();
                        md.putValue("deviceName", device.getName());
                        md.putValue("deviceLabel", device.getLabel());
                        md.putValue("deviceType", device.getType());
                        stateData.setMetaData(md);
                        callback.onSuccess();
                    }
                } else {
                    //Device was probably deleted while message was in queue;
                    callback.onSuccess();
                }
            }
        } catch (Exception e) {
            log.trace("Failed to process queue msg: [{}]", proto, e);
            callback.onFailure(e);
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
        PageDataIterable<DeviceIdInfo> deviceIdInfos = new PageDataIterable<>(deviceService::findDeviceIdInfos, initFetchPackSize);
        Map<TopicPartitionInfo, List<DeviceIdInfo>> tpiDeviceMap = new HashMap<>();

        for (DeviceIdInfo idInfo : deviceIdInfos) {
            TopicPartitionInfo tpi;
            try {
                tpi = partitionService.resolve(ServiceType.TB_CORE, idInfo.getTenantId(), idInfo.getDeviceId());
            } catch (Exception e) {
                log.warn("Failed to resolve partition for device with id [{}], tenant id [{}], customer id [{}]. Reason: {}",
                        idInfo.getDeviceId(), idInfo.getTenantId(), idInfo.getCustomerId(), e.getMessage());
                continue;
            }
            if (addedPartitions.contains(tpi) && !deviceStates.containsKey(idInfo.getDeviceId())) {
                tpiDeviceMap.computeIfAbsent(tpi, tmp -> new ArrayList<>()).add(idInfo);
            }
        }

        for (var entry : tpiDeviceMap.entrySet()) {
            AtomicInteger counter = new AtomicInteger(0);
            // hard-coded limit of 1000 is due to the Entity Data Query limitations and should not be changed.
            for (List<DeviceIdInfo> partition : Lists.partition(entry.getValue(), 1000)) {
                log.info("[{}] Submit task for device states: {}", entry.getKey(), partition.size());
                DevicePackFutureHolder devicePackFutureHolder = new DevicePackFutureHolder();
                var devicePackFuture = deviceStateExecutor.submit(() -> {
                    try {
                        List<DeviceStateData> states;
                        if (persistToTelemetry && !dbTypeInfoComponent.isLatestTsDaoStoredToSql()) {
                            states = fetchDeviceStateDataUsingSeparateRequests(partition);
                        } else {
                            states = fetchDeviceStateDataUsingEntityDataQuery(partition);
                        }
                        if (devicePackFutureHolder.future == null || !devicePackFutureHolder.future.isCancelled()) {
                            for (var state : states) {
                                if (!addDeviceUsingState(entry.getKey(), state)) {
                                    return;
                                }
                                checkAndUpdateState(state.getDeviceId(), state);
                            }
                            log.info("[{}] Initialized {} out of {} device states", entry.getKey().getPartition().orElse(0), counter.addAndGet(states.size()), entry.getValue().size());
                        }
                    } catch (Throwable t) {
                        log.error("Unexpected exception while device pack fetching", t);
                        throw t;
                    }
                });
                devicePackFutureHolder.future = devicePackFuture;
                result.computeIfAbsent(entry.getKey(), tmp -> new ArrayList<>()).add(devicePackFuture);
            }
        }
        return result;
    }

    /**
     * 中文说明：
     * 1. `DevicePackFutureHolder` 是 ThingsBoard Application 中围绕设备提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    private static class DevicePackFutureHolder {
        /**
         * 异步结果列表，用于保存一组待处理对象。
         */
        private volatile ListenableFuture<?> future;
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    void checkAndUpdateState(@Nonnull DeviceId deviceId, @Nonnull DeviceStateData state) {
        var deviceState = state.getState();
        if (deviceState.isActive()) {
            updateInactivityStateIfExpired(getCurrentTimeMillis(), deviceId, state);
        } else {
            //trying to fix activity state
            if (isActive(getCurrentTimeMillis(), deviceState)) {
                updateActivityState(deviceId, state, deviceState.getLastActivityTime());
                if (deviceState.getLastInactivityAlarmTime() != 0L && deviceState.getLastInactivityAlarmTime() >= deviceState.getLastActivityTime()) {
                    deviceState.setLastInactivityAlarmTime(0L);
                    save(deviceId, INACTIVITY_ALARM_TIME, 0L);
                }
            }
        }
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `state`：`state` 参数。
     * 返回：判断结果。
     */
    private boolean addDeviceUsingState(TopicPartitionInfo tpi, DeviceStateData state) {
        Set<DeviceId> deviceIds = partitionedEntities.get(tpi);
        if (deviceIds != null) {
            deviceIds.add(state.getDeviceId());
            deviceStates.putIfAbsent(state.getDeviceId(), state);
            return true;
        } else {
            log.debug("[{}] Device belongs to external partition {}", state.getDeviceId(), tpi.getFullTopicName());
            return false;
        }
    }

    /**
     * 功能：校验`States`。
     * 参数：无。
     * 返回：无。
     */
    void checkStates() {
        try {
            final long ts = getCurrentTimeMillis();
            partitionedEntities.forEach((tpi, deviceIds) -> {
                log.debug("Calculating state updates. tpi {} for {} devices", tpi.getFullTopicName(), deviceIds.size());
                Set<DeviceId> idsFromRemovedTenant = new HashSet<>();
                for (DeviceId deviceId : deviceIds) {
                    DeviceStateData stateData;
                    try {
                        stateData = getOrFetchDeviceStateData(deviceId);
                    } catch (Exception e) {
                        log.error("[{}] Failed to get or fetch device state data", deviceId, e);
                        continue;
                    }
                    try {
                        updateInactivityStateIfExpired(ts, deviceId, stateData);
                    } catch (Exception e) {
                        if (e instanceof TenantNotFoundException) {
                            idsFromRemovedTenant.add(deviceId);
                        } else {
                            log.warn("[{}] Failed to update inactivity state [{}]", deviceId, e.getMessage());
                        }
                    }
                }
                deviceIds.removeAll(idsFromRemovedTenant);
            });
        } catch (Throwable t) {
            log.warn("Failed to check devices states", t);
        }
    }

    /**
     * 功能：上报`Activity Stats`。
     * 参数：无。
     * 返回：无。
     */
    void reportActivityStats() {
        try {
            Map<TenantId, Pair<AtomicInteger, AtomicInteger>> stats = new HashMap<>();
            for (DeviceStateData stateData : deviceStates.values()) {
                Pair<AtomicInteger, AtomicInteger> tenantDevicesActivity = stats.computeIfAbsent(stateData.getTenantId(),
                        tenantId -> Pair.of(new AtomicInteger(), new AtomicInteger()));
                if (stateData.getState().isActive()) {
                    tenantDevicesActivity.getLeft().incrementAndGet();
                } else {
                    tenantDevicesActivity.getRight().incrementAndGet();
                }
            }

            stats.forEach((tenantId, tenantDevicesActivity) -> {
                int active = tenantDevicesActivity.getLeft().get();
                int inactive = tenantDevicesActivity.getRight().get();
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.ACTIVE_DEVICES, active);
                apiUsageReportClient.report(tenantId, null, ApiUsageRecordKey.INACTIVE_DEVICES, inactive);
                if (active > 0) {
                    log.debug("[{}] Active devices: {}, inactive devices: {}", tenantId, active, inactive);
                }
            });
        } catch (Throwable t) {
            log.warn("Failed to report activity states", t);
        }
    }

    /**
     * 功能：更新状态。
     * 参数：
     * - `ts`：时间戳。
     * - `deviceId`：设备IDID。
     * - `stateData`：待处理数据。
     * 返回：无。
     */
    void updateInactivityStateIfExpired(long ts, DeviceId deviceId, DeviceStateData stateData) {
        log.trace("Processing state {} for device {}", stateData, deviceId);
        if (stateData != null) {
            DeviceState state = stateData.getState();
            if (!isActive(ts, state)
                    && (state.getLastInactivityAlarmTime() == 0L || state.getLastInactivityAlarmTime() <= state.getLastActivityTime())
                    && stateData.getDeviceCreationTime() + state.getInactivityTimeout() <= ts) {
                if (partitionService.resolve(ServiceType.TB_CORE, stateData.getTenantId(), deviceId).isMyPartition()) {
                    reportInactivity(ts, deviceId, stateData);
                } else {
                    cleanupEntity(deviceId);
                }
            }
        } else {
            log.debug("[{}] Device that belongs to other server is detected and removed.", deviceId);
            cleanupEntity(deviceId);
        }
    }

    /**
     * 功能：上报`Inactivity`。
     * 参数：
     * - `ts`：时间戳。
     * - `deviceId`：设备IDID。
     * - `stateData`：待处理数据。
     * 返回：无。
     */
    private void reportInactivity(long ts, DeviceId deviceId, DeviceStateData stateData) {
        DeviceState state = stateData.getState();
        state.setActive(false);
        state.setLastInactivityAlarmTime(ts);
        save(deviceId, INACTIVITY_ALARM_TIME, ts);
        onDeviceActivityStatusChange(deviceId, false, stateData);
    }

    /**
     * 功能：判断`Active`。
     * 参数：
     * - `ts`：时间戳。
     * - `state`：`state` 参数。
     * 返回：判断结果。
     */
    boolean isActive(long ts, DeviceState state) {
        return ts < state.getLastActivityTime() + state.getInactivityTimeout();
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Nonnull
    DeviceStateData getOrFetchDeviceStateData(DeviceId deviceId) {
        return deviceStates.computeIfAbsent(deviceId, this::fetchDeviceStateDataUsingSeparateRequests);
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceStateData fetchDeviceStateDataUsingSeparateRequests(final DeviceId deviceId) {
        final Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);
        if (device == null) {
            log.warn("[{}] Failed to fetch device by Id!", deviceId);
            throw new RuntimeException("Failed to fetch device by id [" + deviceId + "]!");
        }
        try {
            return fetchDeviceState(device).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("[{}] Failed to fetch device state!", deviceId, e);
            throw new RuntimeException("Failed to fetch device state for device [" + deviceId + "]");
        }
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `active`：`active` 参数。
     * - `stateData`：待处理数据。
     * 返回：无。
     */
    private void onDeviceActivityStatusChange(DeviceId deviceId, boolean active, DeviceStateData stateData) {
        save(deviceId, ACTIVITY_STATE, active);
        pushRuleEngineMessage(stateData, active ? TbMsgType.ACTIVITY_EVENT : TbMsgType.INACTIVITY_EVENT);
        TbMsgMetaData metaData = stateData.getMetaData();
        notificationRuleProcessor.process(DeviceActivityTrigger.builder()
                .tenantId(stateData.getTenantId()).customerId(stateData.getCustomerId())
                .deviceId(deviceId).active(active)
                .deviceName(metaData.getValue("deviceName"))
                .deviceType(metaData.getValue("deviceType"))
                .deviceLabel(metaData.getValue("deviceLabel"))
                .build());
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：判断结果。
     */
    boolean cleanDeviceStateIfBelongsToExternalPartition(TenantId tenantId, final DeviceId deviceId) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        boolean cleanup = !partitionedEntities.containsKey(tpi);
        if (cleanup) {
            cleanupEntity(deviceId);
            log.debug("[{}][{}] device belongs to external partition. Probably rebalancing is in progress. Topic: {}"
                    , tenantId, deviceId, tpi.getFullTopicName());
        }
        return cleanup;
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    private void onDeviceDeleted(TenantId tenantId, DeviceId deviceId) {
        cleanupEntity(deviceId);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        Set<DeviceId> deviceIdSet = partitionedEntities.get(tpi);
        if (deviceIdSet != null) {
            deviceIdSet.remove(deviceId);
        }
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    @Override
    protected void cleanupEntityOnPartitionRemoval(DeviceId deviceId) {
        cleanupEntity(deviceId);
    }

    /**
     * 功能：删除或清理实体。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    private void cleanupEntity(DeviceId deviceId) {
        deviceStates.remove(deviceId);
    }


    /**
     * 功能：获取设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<DeviceStateData> fetchDeviceState(Device device) {
        ListenableFuture<DeviceStateData> future;
        if (persistToTelemetry) {
            ListenableFuture<List<TsKvEntry>> tsData = tsService.findLatest(TenantId.SYS_TENANT_ID, device.getId(), PERSISTENT_ATTRIBUTES);
            future = Futures.transform(tsData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        } else {
            ListenableFuture<List<AttributeKvEntry>> attrData = attributesService.find(TenantId.SYS_TENANT_ID, device.getId(), SERVER_SCOPE, PERSISTENT_ATTRIBUTES);
            future = Futures.transform(attrData, extractDeviceStateData(device), MoreExecutors.directExecutor());
        }
        return transformInactivityTimeout(future);
    }

    /**
     * 功能：转换超时时间。
     * 参数：
     * - `future`：数据列表。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<DeviceStateData> transformInactivityTimeout(ListenableFuture<DeviceStateData> future) {
        return Futures.transformAsync(future, deviceStateData -> {
            if (!persistToTelemetry || deviceStateData.getState().getInactivityTimeout() != defaultInactivityTimeoutMs) {
                return future; //fail fast
            }
            var attributesFuture = attributesService.find(TenantId.SYS_TENANT_ID, deviceStateData.getDeviceId(), SERVER_SCOPE, INACTIVITY_TIMEOUT);
            return Futures.transform(attributesFuture, attributes -> {
                attributes.flatMap(KvEntry::getLongValue).ifPresent((inactivityTimeout) -> {
                    if (inactivityTimeout > 0) {
                        deviceStateData.getState().setInactivityTimeout(inactivityTimeout);
                    }
                });
                return deviceStateData;
            }, MoreExecutors.directExecutor());
        }, deviceStateCallbackExecutor);
    }

    /**
     * 功能：执行 `extractDeviceStateData` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private <T extends KvEntry> Function<List<T>, DeviceStateData> extractDeviceStateData(Device device) {
        return new Function<>() {
            @Nonnull
            @Override
            public DeviceStateData apply(@Nullable List<T> data) {
                try {
                    long lastActivityTime = getEntryValue(data, LAST_ACTIVITY_TIME, 0L);
                    long inactivityAlarmTime = getEntryValue(data, INACTIVITY_ALARM_TIME, 0L);
                    long inactivityTimeout = getEntryValue(data, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
                    // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
                    final boolean active = getEntryValue(data, ACTIVITY_STATE, false);
                    DeviceState deviceState = DeviceState.builder()
                            .active(active)
                            .lastConnectTime(getEntryValue(data, LAST_CONNECT_TIME, 0L))
                            .lastDisconnectTime(getEntryValue(data, LAST_DISCONNECT_TIME, 0L))
                            .lastActivityTime(lastActivityTime)
                            .lastInactivityAlarmTime(inactivityAlarmTime)
                            .inactivityTimeout(inactivityTimeout)
                            .build();
                    TbMsgMetaData md = new TbMsgMetaData();
                    md.putValue("deviceName", device.getName());
                    md.putValue("deviceLabel", device.getLabel());
                    md.putValue("deviceType", device.getType());
                    DeviceStateData deviceStateData = DeviceStateData.builder()
                            .customerId(device.getCustomerId())
                            .tenantId(device.getTenantId())
                            .deviceId(device.getId())
                            .deviceCreationTime(device.getCreatedTime())
                            .metaData(md)
                            .state(deviceState).build();
                    log.debug("[{}] Fetched device state from the DB {}", device.getId(), deviceStateData);
                    return deviceStateData;
                } catch (Exception e) {
                    log.warn("[{}] Failed to fetch device state data", device.getId(), e);
                    throw new RuntimeException("Failed to fetch device state data for device [" + device.getId() + "]", e);
                }
            }
        };
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private List<DeviceStateData> fetchDeviceStateDataUsingSeparateRequests(List<DeviceIdInfo> deviceIds) {
        List<Device> devices = deviceService.findDevicesByIds(deviceIds.stream().map(DeviceIdInfo::getDeviceId).collect(Collectors.toList()));
        List<ListenableFuture<DeviceStateData>> deviceStateFutures = new ArrayList<>();
        for (Device device : devices) {
            deviceStateFutures.add(fetchDeviceState(device));
        }
        try {
            List<DeviceStateData> result = Futures.successfulAsList(deviceStateFutures).get(5, TimeUnit.MINUTES);
            boolean success = true;
            for (int i = 0; i < result.size(); i++) {
                success = false;
                if (result.get(i) == null) {
                    DeviceIdInfo deviceIdInfo = deviceIds.get(i);
                    log.warn("[{}][{}] Failed to initialized device state due to:", deviceIdInfo.getTenantId(), deviceIdInfo.getDeviceId());
                }
            }
            return success ? result : result.stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String deviceIdsStr = deviceIds.stream()
                    .map(DeviceIdInfo::getDeviceId)
                    .map(UUIDBased::getId)
                    .map(UUID::toString)
                    .collect(Collectors.joining(", "));
            log.warn("Failed to initialized device state futures for ids [{}] due to:", deviceIdsStr, e);
            throw new RuntimeException("Failed to initialized device state futures for ids [" + deviceIdsStr + "]!", e);
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private List<DeviceStateData> fetchDeviceStateDataUsingEntityDataQuery(List<DeviceIdInfo> deviceIds) {
        EntityListFilter ef = new EntityListFilter();
        ef.setEntityType(EntityType.DEVICE);
        ef.setEntityList(deviceIds.stream().map(DeviceIdInfo::getDeviceId).map(DeviceId::getId).map(UUID::toString).collect(Collectors.toList()));

        EntityDataQuery query = new EntityDataQuery(ef,
                new EntityDataPageLink(deviceIds.size(), 0, null, null),
                PERSISTENT_ENTITY_FIELDS,
                persistToTelemetry ? PERSISTENT_TELEMETRY_KEYS : PERSISTENT_ATTRIBUTE_KEYS, Collections.emptyList());
        PageData<EntityData> queryResult = entityQueryRepository.findEntityDataByQueryInternal(query);

        Map<EntityId, DeviceIdInfo> deviceIdInfos = deviceIds.stream().collect(Collectors.toMap(DeviceIdInfo::getDeviceId, java.util.function.Function.identity()));

        return queryResult.getData().stream().map(ed -> toDeviceStateData(ed, deviceIdInfos.get(ed.getEntityId()))).collect(Collectors.toList());

    }

    /**
     * 功能：执行 `toDeviceStateData` 对应的处理。
     * 参数：
     * - `ed`：`ed` 参数。
     * - `deviceIdInfo`：设备信息或设备标识。
     * 返回：处理结果。
     */
    DeviceStateData toDeviceStateData(EntityData ed, DeviceIdInfo deviceIdInfo) {
        long lastActivityTime = getEntryValue(ed, getKeyType(), LAST_ACTIVITY_TIME, 0L);
        long inactivityAlarmTime = getEntryValue(ed, getKeyType(), INACTIVITY_ALARM_TIME, 0L);
        long inactivityTimeout = getEntryValue(ed, getKeyType(), INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        if (persistToTelemetry && inactivityTimeout == defaultInactivityTimeoutMs) {
            log.trace("[{}] default value for inactivity timeout fetched {}, going to fetch inactivity timeout from attributes",
                    deviceIdInfo.getDeviceId(), inactivityTimeout);
            inactivityTimeout = getEntryValue(ed, EntityKeyType.SERVER_ATTRIBUTE, INACTIVITY_TIMEOUT, defaultInactivityTimeoutMs);
        }
        // Actual active state by wall-clock will be updated outside this method. This method is only for fetching persistent state
        final boolean active = getEntryValue(ed, getKeyType(), ACTIVITY_STATE, false);
        DeviceState deviceState = DeviceState.builder()
                .active(active)
                .lastConnectTime(getEntryValue(ed, getKeyType(), LAST_CONNECT_TIME, 0L))
                .lastDisconnectTime(getEntryValue(ed, getKeyType(), LAST_DISCONNECT_TIME, 0L))
                .lastActivityTime(lastActivityTime)
                .lastInactivityAlarmTime(inactivityAlarmTime)
                .inactivityTimeout(inactivityTimeout)
                .build();
        TbMsgMetaData md = new TbMsgMetaData();
        md.putValue("deviceName", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "name", ""));
        md.putValue("deviceLabel", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "label", ""));
        md.putValue("deviceType", getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "type", ""));
        return DeviceStateData.builder()
                .customerId(deviceIdInfo.getCustomerId())
                .tenantId(deviceIdInfo.getTenantId())
                .deviceId(deviceIdInfo.getDeviceId())
                .deviceCreationTime(getEntryValue(ed, EntityKeyType.ENTITY_FIELD, "createdTime", 0L))
                .metaData(md)
                .state(deviceState).build();
    }

    /**
     * 功能：获取键。
     * 参数：无。
     * 返回：处理结果。
     */
    private EntityKeyType getKeyType() {
        return persistToTelemetry ? EntityKeyType.TIME_SERIES : EntityKeyType.SERVER_ATTRIBUTE;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `ed`：`ed` 参数。
     * - `keyType`：类型。
     * - `keyName`：名称。
     * - `defaultValue`：值。
     * 返回：文本结果。
     */
    private String getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, String defaultValue) {
        return getEntryValue(ed, keyType, keyName, s -> s, defaultValue);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `ed`：`ed` 参数。
     * - `keyType`：类型。
     * - `keyName`：名称。
     * - `defaultValue`：值。
     * 返回：数值结果。
     */
    private long getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, long defaultValue) {
        return getEntryValue(ed, keyType, keyName, Long::parseLong, defaultValue);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `ed`：`ed` 参数。
     * - `keyType`：类型。
     * - `keyName`：名称。
     * - `defaultValue`：值。
     * 返回：判断结果。
     */
    private boolean getEntryValue(EntityData ed, EntityKeyType keyType, String keyName, boolean defaultValue) {
        return getEntryValue(ed, keyType, keyName, Boolean::parseBoolean, defaultValue);
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `ed`：`ed` 参数。
     * - `entityKeyType`：实体对象。
     * - `attributeName`：名称。
     * - `converter`：`converter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    private <T> T getEntryValue(EntityData ed, EntityKeyType entityKeyType, String attributeName, Function<String, T> converter, T defaultValue) {
        if (ed != null && ed.getLatest() != null) {
            var map = ed.getLatest().get(entityKeyType);
            if (map != null) {
                var value = map.get(attributeName);
                if (value != null && !StringUtils.isEmpty(value.getValue())) {
                    try {
                        return converter.apply(value.getValue());
                    } catch (Exception e) {
                        return defaultValue;
                    }
                }
            }
        }
        return defaultValue;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `kvEntries`：数据列表。
     * - `attributeName`：名称。
     * - `defaultValue`：值。
     * 返回：数值结果。
     */
    private long getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, long defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getLongValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `kvEntries`：数据列表。
     * - `attributeName`：名称。
     * - `defaultValue`：值。
     * 返回：判断结果。
     */
    private boolean getEntryValue(List<? extends KvEntry> kvEntries, String attributeName, boolean defaultValue) {
        if (kvEntries != null) {
            for (KvEntry entry : kvEntries) {
                if (entry != null && !StringUtils.isEmpty(entry.getKey()) && entry.getKey().equals(attributeName)) {
                    return entry.getBooleanValue().orElse(defaultValue);
                }
            }
        }
        return defaultValue;
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `stateData`：待处理数据。
     * - `msgType`：待处理消息。
     * 返回：无。
     */
    private void pushRuleEngineMessage(DeviceStateData stateData, TbMsgType msgType) {
        DeviceState state = stateData.getState();
        try {
            String data;
            if (msgType.equals(TbMsgType.CONNECT_EVENT)) {
                ObjectNode stateNode = JacksonUtil.convertValue(state, ObjectNode.class);
                stateNode.remove(ACTIVITY_STATE);
                data = JacksonUtil.toString(stateNode);
            } else {
                data = JacksonUtil.toString(state);
            }
            TbMsgMetaData md = stateData.getMetaData().copy();
            if (!persistToTelemetry) {
                md.putValue(SCOPE, SERVER_SCOPE);
            }
            TbMsg tbMsg = TbMsg.newMsg(msgType, stateData.getDeviceId(), stateData.getCustomerId(), md, TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(stateData.getTenantId(), stateData.getDeviceId(), tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}] Failed to push inactivity alarm: {}", stateData.getDeviceId(), state, e);
        }
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    private void save(DeviceId deviceId, String key, long value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(getCurrentTimeMillis(), new LongDataEntry(key, value))),
                    telemetryTtl, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    private void save(DeviceId deviceId, String key, boolean value) {
        if (persistToTelemetry) {
            tsSubService.saveAndNotifyInternal(
                    TenantId.SYS_TENANT_ID, deviceId,
                    Collections.singletonList(new BasicTsKvEntry(getCurrentTimeMillis(), new BooleanDataEntry(key, value))),
                    telemetryTtl, new TelemetrySaveCallback<>(deviceId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(TenantId.SYS_TENANT_ID, deviceId, SERVER_SCOPE, key, value, new TelemetrySaveCallback<>(deviceId, key, value));
        }
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 中文说明：
     * 1. `TelemetrySaveCallback` 是 ThingsBoard Application 中处理遥测数据的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `FutureCallback`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    private static class TelemetrySaveCallback<T> implements FutureCallback<T> {
        /**
         * 设备ID，用于定位对应业务对象。
         */
        private final DeviceId deviceId;
        private final String key;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final Object value;

        TelemetrySaveCallback(DeviceId deviceId, String key, Object value) {
            this.deviceId = deviceId;
            this.key = key;
            this.value = value;
        }

        /**
         * 功能：处理`on Success`。
         * 参数：
         * - `result`：`result` 参数。
         * 返回：无。
         */
        @Override
        public void onSuccess(@Nullable T result) {
            log.trace("[{}] Successfully updated attribute [{}] with value [{}]", deviceId, key, value);
        }

        /**
         * 功能：处理失败信息。
         * 参数：
         * - `t`：`t` 参数。
         * 返回：无。
         */
        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}] Failed to update attribute [{}] with value [{}]", deviceId, key, value, t);
        }
    }
}
