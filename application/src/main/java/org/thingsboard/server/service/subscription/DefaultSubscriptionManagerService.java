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
package org.thingsboard.server.service.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.event.OtherServiceShutdownEvent;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.state.DefaultDeviceStateService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultSubscriptionManagerService` 是 ThingsBoard Application 中负责 `Subscription` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `TbApplicationEventListener`、`SubscriptionManagerService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@TbCoreComponent
@Service
@RequiredArgsConstructor
public class DefaultSubscriptionManagerService extends TbApplicationEventListener<PartitionChangeEvent> implements SubscriptionManagerService {

    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;
    private final PartitionService partitionService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbQueueProducerProvider producerProvider;
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    private final TbLocalSubscriptionService localSubscriptionService;
    private final DeviceStateService deviceStateService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService clusterService;
    private final SubscriptionSchedulerComponent scheduler;

    private final Lock subsLock = new ReentrantLock();
    private final ConcurrentMap<EntityId, TbEntityRemoteSubsInfo> entitySubscriptions = new ConcurrentHashMap<>();

    private final ConcurrentMap<EntityId, TbEntityUpdatesInfo> entityUpdates = new ConcurrentHashMap<>();

    /**
     * 服务ID，用于定位对应业务对象。
     */
    private String serviceId;
    private TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNotificationsProducer;

    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long initTs;

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        serviceId = serviceInfoProvider.getServiceId();
        initTs = System.currentTimeMillis();
        toCoreNotificationsProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        scheduler.scheduleWithFixedDelay(this::cleanupEntityUpdates, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `serviceId`：服务ID。
     * - `event`：`event` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onSubEvent(String serviceId, TbEntitySubEvent event, TbCallback callback) {
        var tenantId = event.getTenantId();
        var entityId = event.getEntityId();
        log.trace("[{}][{}][{}] Processing subscription event {}", tenantId, entityId, serviceId, event);
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        if (tpi.isMyPartition()) {
            subsLock.lock();
            try {
                var entitySubs = entitySubscriptions.computeIfAbsent(entityId, id -> new TbEntityRemoteSubsInfo(tenantId, entityId));
                boolean empty = entitySubs.updateAndCheckIsEmpty(serviceId, event);
                if (empty) {
                    entitySubscriptions.remove(entityId);
                }
            } finally {
                subsLock.unlock();
            }
            callback.onSuccess();
            if (event.hasTsOrAttrSub()) {
                sendSubEventCallback(serviceId, entityId, event.getSeqNumber());
            }
        } else {
            log.warn("[{}][{}][{}] Event belongs to external partition. Probably re-balancing is in progress. Topic: {}"
                    , tenantId, entityId, serviceId, tpi.getFullTopicName());
            callback.onFailure(new RuntimeException("Entity belongs to external partition " + tpi.getFullTopicName() + "!"));
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    @EventListener(OtherServiceShutdownEvent.class)
    public void onApplicationEvent(OtherServiceShutdownEvent event) {
        if (event.getServiceTypes() != null && event.getServiceTypes().contains(ServiceType.TB_CORE)) {
            subsLock.lock();
            try {
                int sizeBeforeCleanup = entitySubscriptions.size();
                entitySubscriptions.entrySet().removeIf(kv -> kv.getValue().removeAndCheckIsEmpty(event.getServiceId()));
                log.info("[{}][{}] Removed {} entity subscription records due to server shutdown.", serviceId, event.getServiceId(), entitySubscriptions.size() - sizeBeforeCleanup);
            } finally {
                subsLock.unlock();
            }
        }
    }

    /**
     * 功能：发送或提交事件。
     * 参数：
     * - `targetId`：目标对象ID。
     * - `entityId`：实体IDID。
     * - `seqNumber`：`seqNumber` 参数。
     * 返回：无。
     */
    private void sendSubEventCallback(String targetId, EntityId entityId, int seqNumber) {
        var update = getEntityUpdatesInfo(entityId);
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onSubEventCallback(entityId, seqNumber, update, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(entityId.getId(), seqNumber, update));
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `partitionChangeEvent`：分区标识或分区信息。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent partitionChangeEvent) {
        if (ServiceType.TB_CORE.equals(partitionChangeEvent.getServiceType())) {
            entitySubscriptions.values().removeIf(sub ->
                    !partitionService.isMyPartition(ServiceType.TB_CORE, sub.getTenantId(), sub.getEntityId()));
        }
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTimeSeriesUpdate(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, TbCallback callback) {
        onTimeSeriesUpdate(entityId, ts);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            updateDeviceInactivityTimeout(tenantId, entityId, ts);
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTimeSeriesDelete(TenantId tenantId, EntityId entityId, List<String> keys, TbCallback callback) {
        onTimeSeriesUpdate(entityId,
                keys.stream().map(key -> new BasicTsKvEntry(0, new StringDataEntry(key, ""))).collect(Collectors.toList()));
        if (entityId.getEntityType() == EntityType.DEVICE) {
            deleteDeviceInactivityTimeout(tenantId, entityId, keys);
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `update`：数据列表。
     * 返回：无。
     */
    public void onTimeSeriesUpdate(EntityId entityId, List<TsKvEntry> update) {
        getEntityUpdatesInfo(entityId).timeSeriesUpdateTs = System.currentTimeMillis();
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling time-series update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.tsAllKeys) {
                    onTimeSeriesUpdate(serviceId, entityId, update);
                } else if (sub.tsKeys != null) {
                    List<TsKvEntry> tmp = getSubList(update, sub.tsKeys);
                    if (tmp != null) {
                        onTimeSeriesUpdate(serviceId, entityId, tmp);
                    }
                }
            });
        } else {
            log.trace("[{}] No time-series subscriptions for entity.", entityId);
        }
    }

    /**
     * 功能：处理时序数据。
     * 参数：
     * - `targetId`：目标对象ID。
     * - `entityId`：实体IDID。
     * - `update`：数据列表。
     * 返回：无。
     */
    private void onTimeSeriesUpdate(String targetId, EntityId entityId, List<TsKvEntry> update) {
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onTimeSeriesUpdate(entityId, update, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(entityId, update));
        }
    }

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, TbCallback callback) {
        onAttributesUpdate(tenantId, entityId, scope, attributes, true, callback);
    }

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void onAttributesUpdate(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, TbCallback callback) {
        getEntityUpdatesInfo(entityId).attributesUpdateTs = System.currentTimeMillis();
        processAttributesUpdate(entityId, scope, attributes);
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)) {
                updateDeviceInactivityTimeout(tenantId, entityId, attributes);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onUpdate(tenantId,
                                new DeviceId(entityId.getId()), DataConstants.SHARED_SCOPE, new ArrayList<>(attributes))
                        , null);
            }
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理`on Attributes Delete`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void onAttributesDelete(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, TbCallback callback) {
        processAttributesUpdate(entityId, scope,
                keys.stream().map(key -> new BaseAttributeKvEntry(0, new StringDataEntry(key, ""))).collect(Collectors.toList()));
        if (entityId.getEntityType() == EntityType.DEVICE) {
            if (TbAttributeSubscriptionScope.SERVER_SCOPE.name().equalsIgnoreCase(scope)
                    || TbAttributeSubscriptionScope.ANY_SCOPE.name().equalsIgnoreCase(scope)) {
                deleteDeviceInactivityTimeout(tenantId, entityId, keys);
            } else if (TbAttributeSubscriptionScope.SHARED_SCOPE.name().equalsIgnoreCase(scope) && notifyDevice) {
                clusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(tenantId,
                        new DeviceId(entityId.getId()), scope, keys), null);
            }
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理`Attributes Update`。
     * 参数：
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `update`：数据列表。
     * 返回：无。
     */
    public void processAttributesUpdate(EntityId entityId, String scope, List<AttributeKvEntry> update) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}] Handling attributes update: {}", entityId, update);
            subInfo.getSubs().forEach((serviceId, sub) -> {
                if (sub.attrAllKeys) {
                    processAttributesUpdate(serviceId, entityId, scope, update);
                } else if (sub.attrKeys != null) {
                    List<AttributeKvEntry> tmp = getSubList(update, sub.attrKeys);
                    if (tmp != null) {
                        processAttributesUpdate(serviceId, entityId, scope, tmp);
                    }
                }
            });
        } else {
            log.trace("[{}] No attributes subscriptions for entity.", entityId);
        }
    }

    /**
     * 功能：处理`Attributes Update`。
     * 参数：
     * - `targetId`：目标对象ID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `update`：数据列表。
     * 返回：无。
     */
    private void processAttributesUpdate(String targetId, EntityId entityId, String scope, List<AttributeKvEntry> update) {
        List<TsKvEntry> tsKvEntryList = update.stream().map(attr -> new BasicTsKvEntry(attr.getLastUpdateTs(), attr)).collect(Collectors.toList());
        if (serviceId.equals(targetId)) {
            localSubscriptionService.onAttributesUpdate(entityId, scope, tsKvEntryList, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetId, entityId, TbSubscriptionUtils.toProto(scope, entityId, tsKvEntryList));
        }
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `kvEntries`：数据列表。
     * 返回：无。
     */
    private void updateDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<? extends KvEntry> kvEntries) {
        for (KvEntry kvEntry : kvEntries) {
            if (kvEntry.getKey().equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), getLongValue(kvEntry));
            }
        }
    }

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：无。
     */
    private void deleteDeviceInactivityTimeout(TenantId tenantId, EntityId entityId, List<String> keys) {
        for (String key : keys) {
            if (key.equals(DefaultDeviceStateService.INACTIVITY_TIMEOUT)) {
                deviceStateService.onDeviceInactivityTimeoutUpdate(tenantId, new DeviceId(entityId.getId()), 0);
            }
        }
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onAlarmUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback) {
        onAlarmSubUpdate(tenantId, entityId, alarm, false, callback);
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onAlarmDeleted(TenantId tenantId, EntityId entityId, AlarmInfo alarm, TbCallback callback) {
        onAlarmSubUpdate(tenantId, entityId, alarm, true, callback);
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `deleted`：`deleted` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void onAlarmSubUpdate(TenantId tenantId, EntityId entityId, AlarmInfo alarm, boolean deleted, TbCallback callback) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            log.trace("[{}][{}] Handling alarm update {}: {}", tenantId, entityId, alarm, deleted);
            for (Map.Entry<String, TbSubscriptionsInfo> entry : subInfo.getSubs().entrySet()) {
                if (entry.getValue().alarms) {
                    onAlarmSubUpdate(entry.getKey(), entityId, alarm, deleted);
                }
            }
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理告警。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * - `deleted`：`deleted` 参数。
     * 返回：无。
     */
    private void onAlarmSubUpdate(String targetServiceId, EntityId entityId, AlarmInfo alarm, boolean deleted) {
        if (alarm == null) {
            log.warn("[{}] empty alarm update!", entityId);
            return;
        }
        if (serviceId.equals(targetServiceId)) {
            log.trace("[{}] Forwarding to local service: {} deleted: {}", entityId, alarm, deleted);
            localSubscriptionService.onAlarmUpdate(entityId, alarm, deleted, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetServiceId, entityId,
                    TbSubscriptionUtils.toAlarmSubUpdateToProto(entityId, alarm, deleted));
        }
    }

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void sendCoreNotification(String targetServiceId, EntityId entityId, ToCoreNotificationMsg msg) {
        log.trace("[{}] Forwarding to remote service [{}]: {}", entityId, targetServiceId, msg);
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, targetServiceId);
        TbProtoQueueMsg<ToCoreNotificationMsg> queueMsg = new TbProtoQueueMsg<>(entityId.getId(), msg);
        toCoreNotificationsProducer.send(tpi, queueMsg, null);
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `notificationUpdate`：`notificationUpdate` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onNotificationUpdate(TenantId tenantId, UserId entityId, NotificationUpdate notificationUpdate, TbCallback callback) {
        TbEntityRemoteSubsInfo subInfo = entitySubscriptions.get(entityId);
        if (subInfo != null) {
            NotificationsSubscriptionUpdate subscriptionUpdate = new NotificationsSubscriptionUpdate(notificationUpdate);
            log.trace("[{}][{}] Handling notificationUpdate for user {}", tenantId, entityId, notificationUpdate);
            for (Map.Entry<String, TbSubscriptionsInfo> entry : subInfo.getSubs().entrySet()) {
                if (entry.getValue().notifications) {
                    onNotificationsSubUpdate(entry.getKey(), entityId, subscriptionUpdate);
                }
            }
        }
        callback.onSuccess();
    }

    /**
     * 功能：处理`on Notifications Sub Update`。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `entityId`：实体IDID。
     * - `subscriptionUpdate`：`subscriptionUpdate` 参数。
     * 返回：无。
     */
    private void onNotificationsSubUpdate(String targetServiceId, EntityId entityId, NotificationsSubscriptionUpdate subscriptionUpdate) {
        if (serviceId.equals(targetServiceId)) {
            log.trace("[{}] Forwarding to local service: {}", entityId, subscriptionUpdate);
            localSubscriptionService.onNotificationUpdate(entityId, subscriptionUpdate, TbCallback.EMPTY);
        } else {
            sendCoreNotification(targetServiceId, entityId,
                    TbSubscriptionUtils.notificationsSubUpdateToProto(entityId, subscriptionUpdate));
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `kve`：`kve` 参数。
     * 返回：数值结果。
     */
    private static long getLongValue(KvEntry kve) {
        switch (kve.getDataType()) {
            case LONG:
                return kve.getLongValue().orElse(0L);
            case DOUBLE:
                return kve.getDoubleValue().orElse(0.0).longValue();
            case STRING:
                try {
                    return Long.parseLong(kve.getStrValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            case JSON:
                try {
                    return Long.parseLong(kve.getJsonValue().orElse("0"));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            default:
                return 0L;
        }
    }

    /**
     * 功能：获取`Sub List`。
     * 参数：
     * - `ts`：时间戳。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    private static <T extends KvEntry> List<T> getSubList(List<T> ts, Set<String> keys) {
        List<T> update = null;
        for (T entry : ts) {
            if (keys.contains(entry.getKey())) {
                if (update == null) {
                    update = new ArrayList<>(ts.size());
                }
                update.add(entry);
            }
        }
        return update;
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private TbEntityUpdatesInfo getEntityUpdatesInfo(EntityId entityId) {
        return entityUpdates.computeIfAbsent(entityId, id -> new TbEntityUpdatesInfo(initTs));
    }

    /**
     * 功能：删除或清理实体。
     * 参数：无。
     * 返回：无。
     */
    private void cleanupEntityUpdates() {
        initTs = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        int sizeBeforeCleanup = entityUpdates.size();
        entityUpdates.entrySet().removeIf(kv -> {
            var v = kv.getValue();
            return initTs > v.attributesUpdateTs && initTs > v.timeSeriesUpdateTs;
        });
        log.info("Removed {} old entity update records.", entityUpdates.size() - sizeBeforeCleanup);
    }

}
