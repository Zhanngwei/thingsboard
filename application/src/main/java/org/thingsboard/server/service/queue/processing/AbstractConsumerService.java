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
package org.thingsboard.server.service.queue.processing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.TbPackCallback;
import org.thingsboard.server.service.queue.TbPackProcessingContext;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`AbstractConsumerService` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Slf4j
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends TbApplicationEventListener<PartitionChangeEvent> {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected volatile ExecutorService notificationsConsumerExecutor;
    protected volatile boolean stopped = false;
    /**
     * 当前对象是否已准备就绪。
     */
    protected volatile boolean isReady = false;
    protected final ActorSystemContext actorContext;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    protected final DataDecodingEncodingService encodingService;
    protected final TbTenantProfileCache tenantProfileCache;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    protected final TbDeviceProfileCache deviceProfileCache;
    protected final TbAssetProfileCache assetProfileCache;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    protected final TbApiUsageStateService apiUsageStateService;
    protected final PartitionService partitionService;
    /**
     * 事件，表示当前对象的对应属性。
     */
    protected final ApplicationEventPublisher eventPublisher;

    /**
     * 消息消费者，承载当前流程需要传递的内容。
     */
    protected final TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer;
    protected final JwtSettingsService jwtSettingsService;


    /**
     * 功能：创建 `AbstractConsumerService` 实例，并初始化必要字段。
     * 参数：
     * - `actorContext`：处理上下文。
     * - `encodingService`：服务对象。
     * - `tenantProfileCache`：租户信息或租户标识。
     * - `deviceProfileCache`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public AbstractConsumerService(ActorSystemContext actorContext, DataDecodingEncodingService encodingService,
                                   TbTenantProfileCache tenantProfileCache, TbDeviceProfileCache deviceProfileCache,
                                   TbAssetProfileCache assetProfileCache, TbApiUsageStateService apiUsageStateService,
                                   PartitionService partitionService, ApplicationEventPublisher eventPublisher,
                                   TbQueueConsumer<TbProtoQueueMsg<N>> nfConsumer, JwtSettingsService jwtSettingsService) {
        this.actorContext = actorContext;
        this.encodingService = encodingService;
        this.tenantProfileCache = tenantProfileCache;
        this.deviceProfileCache = deviceProfileCache;
        this.assetProfileCache = assetProfileCache;
        this.apiUsageStateService = apiUsageStateService;
        this.partitionService = partitionService;
        this.eventPublisher = eventPublisher;
        this.nfConsumer = nfConsumer;
        this.jwtSettingsService = jwtSettingsService;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `nfConsumerThreadName`：名称。
     * 返回：无。
     */
    public void init(String nfConsumerThreadName) {
        this.notificationsConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(nfConsumerThreadName));
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Subscribing to notifications: {}", nfConsumer.getTopic());
        this.nfConsumer.subscribe();
        this.isReady = true;
        launchNotificationsConsumer();
        launchMainConsumers();
    }

    /**
     * 功能：执行 `filterTbApplicationEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：判断结果。
     */
    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return event.getServiceType() == getServiceType();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract ServiceType getServiceType();

    /**
     * 功能：执行 `launchMainConsumers` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected abstract void launchMainConsumers();

    /**
     * 功能：停止或关闭`Consumers`。
     * 参数：无。
     * 返回：无。
     */
    protected abstract void stopConsumers();

    /**
     * 功能：获取轮询间隔。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getNotificationPollDuration();

    /**
     * 功能：获取版本包处理超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getNotificationPackProcessingTimeout();

    /**
     * 功能：执行 `launchNotificationsConsumer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void launchNotificationsConsumer() {
        notificationsConsumerExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<N>> msgs = nfConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    List<IdMsgPair<N>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<N>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    orderedMsgList.forEach(element -> {
                        UUID id = element.getUuid();
                        TbProtoQueueMsg<N> msg = element.getMsg();
                        log.trace("[{}] Creating notification callback for message: {}", id, msg.getValue());
                        TbCallback callback = new TbPackCallback<>(id, ctx);
                        try {
                            handleNotification(id, msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process notification: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
                    }
                    nfConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain notifications from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new notifications", e2);
                        }
                    }
                }
            }
            log.info("TB Notifications Consumer stopped.");
        });
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `id`：`id`ID。
     * - `componentLifecycleMsg`：待处理消息。
     * 返回：无。
     */
    protected final void handleComponentLifecycleMsg(UUID id, ComponentLifecycleMsg componentLifecycleMsg) {
        TenantId tenantId = componentLifecycleMsg.getTenantId();
        log.debug("[{}][{}][{}] Received Lifecycle event: {}", tenantId, componentLifecycleMsg.getEntityId().getEntityType(),
                componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
        if (EntityType.TENANT_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            TenantProfileId tenantProfileId = new TenantProfileId(componentLifecycleMsg.getEntityId().getId());
            tenantProfileCache.evict(tenantProfileId);
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                apiUsageStateService.onTenantProfileUpdate(tenantProfileId);
            }
        } else if (EntityType.TENANT.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                jwtSettingsService.reloadJwtSettings();
                return;
            } else {
                tenantProfileCache.evict(tenantId);
                partitionService.evictTenantInfo(tenantId);
                if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                    apiUsageStateService.onTenantUpdate(tenantId);
                } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.DELETED)) {
                    apiUsageStateService.onTenantDelete(tenantId);
                    partitionService.removeTenant(tenantId);
                }
            }
        } else if (EntityType.DEVICE_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.DEVICE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ENTITY_VIEW.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            actorContext.getTbEntityViewService().onComponentLifecycleMsg(componentLifecycleMsg);
        } else if (EntityType.API_USAGE_STATE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            apiUsageStateService.onApiUsageStateUpdate(tenantId);
        } else if (EntityType.CUSTOMER.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.DELETED) {
                apiUsageStateService.onCustomerDelete((CustomerId) componentLifecycleMsg.getEntityId());
            }
        }

        eventPublisher.publishEvent(componentLifecycleMsg);
        log.trace("[{}] Forwarding component lifecycle message to App Actor {}", id, componentLifecycleMsg);
        actorContext.tellWithHighPriority(componentLifecycleMsg);
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `id`：`id`ID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        stopped = true;
        stopConsumers();
        if (nfConsumer != null) {
            nfConsumer.unsubscribe();
        }
        if (notificationsConsumerExecutor != null) {
            notificationsConsumerExecutor.shutdownNow();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractConsumerService` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
