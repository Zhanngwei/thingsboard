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
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.resource.ImageCacheKey;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceStateServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.EdgeNotificationMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ErrorEventProto;
import org.thingsboard.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.LifecycleEventProto;
import org.thingsboard.server.gen.transport.TransportProtos.LocalSubscriptionServiceMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbEntitySubEventProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.edge.EdgeNotificationService;
import org.thingsboard.server.service.notification.NotificationSchedulerService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.processing.IdMsgPair;
import org.thingsboard.server.service.resource.TbImageService;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbLocalSubscriptionService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;
import org.thingsboard.server.service.sync.vc.GitVersionControlQueueService;
import org.thingsboard.server.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbCoreConsumerService` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Service
@TbCoreComponent
@Slf4j
public class DefaultTbCoreConsumerService extends AbstractConsumerService<ToCoreNotificationMsg> implements TbCoreConsumerService {

    /**
     * 轮询间隔，用于控制时间范围或等待时长。
     */
    @Value("${queue.core.poll-interval}")
    private long pollDuration;
    /**
     * 版本包处理超时时间，用于控制时间范围或等待时长。
     */
    @Value("${queue.core.pack-processing-timeout}")
    private long packProcessingTimeout;
    /**
     * 是否启用`stats`。
     */
    @Value("${queue.core.stats.enabled:false}")
    private boolean statsEnabled;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Value("${queue.core.ota.pack-interval-ms:60000}")
    private long firmwarePackInterval;
    /**
     * `firmwarePackSize` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.core.ota.pack-size:100}")
    private int firmwarePackSize;

    /**
     * 消息消费者，承载当前流程需要传递的内容。
     */
    private final TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> mainConsumer;
    private final DeviceStateService stateService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbApiUsageStateService statsService;
    private final TbLocalSubscriptionService localSubscriptionService;
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    private final SubscriptionManagerService subscriptionManagerService;
    private final TbCoreDeviceRpcService tbCoreDeviceRpcService;
    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    private final EdgeNotificationService edgeNotificationService;
    private final OtaPackageStateService firmwareStateService;
    /**
     * 队列，提供当前类调用的业务操作。
     */
    private final GitVersionControlQueueService vcQueueService;
    private final NotificationSchedulerService notificationSchedulerService;
    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final TbCoreConsumerStats stats;
    /**
     * 消息消费者，承载当前流程需要传递的内容。
     */
    protected final TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> usageStatsConsumer;
    private final TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> firmwareStatesConsumer;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbImageService imageService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected volatile ExecutorService consumersExecutor;
    protected volatile ExecutorService usageStatsExecutor;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private volatile ExecutorService firmwareStatesExecutor;
    private volatile ListeningExecutorService deviceActivityEventsExecutor;

    /**
     * 功能：创建 `DefaultTbCoreConsumerService` 实例，并初始化必要字段。
     * 参数：
     * - `tbCoreQueueFactory`：队列名称或队列对象。
     * - `actorContext`：处理上下文。
     * - `stateService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DefaultTbCoreConsumerService(TbCoreQueueFactory tbCoreQueueFactory,
                                        ActorSystemContext actorContext,
                                        DeviceStateService stateService,
                                        TbLocalSubscriptionService localSubscriptionService,
                                        SubscriptionManagerService subscriptionManagerService,
                                        DataDecodingEncodingService encodingService,
                                        TbCoreDeviceRpcService tbCoreDeviceRpcService,
                                        StatsFactory statsFactory,
                                        TbDeviceProfileCache deviceProfileCache,
                                        TbAssetProfileCache assetProfileCache,
                                        TbApiUsageStateService statsService,
                                        TbTenantProfileCache tenantProfileCache,
                                        TbApiUsageStateService apiUsageStateService,
                                        EdgeNotificationService edgeNotificationService,
                                        OtaPackageStateService firmwareStateService,
                                        GitVersionControlQueueService vcQueueService,
                                        PartitionService partitionService,
                                        ApplicationEventPublisher eventPublisher,
                                        JwtSettingsService jwtSettingsService,
                                        NotificationSchedulerService notificationSchedulerService,
                                        NotificationRuleProcessor notificationRuleProcessor,
                                        TbImageService imageService) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService, eventPublisher, tbCoreQueueFactory.createToCoreNotificationsMsgConsumer(), jwtSettingsService);
        this.mainConsumer = tbCoreQueueFactory.createToCoreMsgConsumer();
        this.usageStatsConsumer = tbCoreQueueFactory.createToUsageStatsServiceMsgConsumer();
        this.firmwareStatesConsumer = tbCoreQueueFactory.createToOtaPackageStateServiceMsgConsumer();
        this.stateService = stateService;
        this.localSubscriptionService = localSubscriptionService;
        this.subscriptionManagerService = subscriptionManagerService;
        this.tbCoreDeviceRpcService = tbCoreDeviceRpcService;
        this.edgeNotificationService = edgeNotificationService;
        this.stats = new TbCoreConsumerStats(statsFactory);
        this.statsService = statsService;
        this.firmwareStateService = firmwareStateService;
        this.vcQueueService = vcQueueService;
        this.notificationSchedulerService = notificationSchedulerService;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.imageService = imageService;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init("tb-core-notifications-consumer");
        this.consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("tb-core-consumer"));
        this.usageStatsExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-usage-stats-consumer"));
        this.firmwareStatesExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-firmware-notifications-consumer"));
        this.deviceActivityEventsExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-core-device-activity-events-executor")));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        super.destroy();
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (usageStatsExecutor != null) {
            usageStatsExecutor.shutdownNow();
        }
        if (firmwareStatesExecutor != null) {
            firmwareStatesExecutor.shutdownNow();
        }
        if (deviceActivityEventsExecutor != null) {
            deviceActivityEventsExecutor.shutdownNow();
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        super.onApplicationEvent(event);
        launchUsageStatsConsumer();
        launchOtaPackageUpdateNotificationConsumer();
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.info("Subscribing to partitions: {}", event.getPartitions());
        this.mainConsumer.subscribe(event.getPartitions());
        this.usageStatsConsumer.subscribe(
                event
                        .getPartitions()
                        .stream()
                        .map(tpi -> tpi.newByTopic(usageStatsConsumer.getTopic()))
                        .collect(Collectors.toSet()));
        this.firmwareStatesConsumer.subscribe();
    }

    /**
     * 功能：执行 `launchMainConsumers` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void launchMainConsumers() {
        consumersExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToCoreMsg>> msgs = mainConsumer.poll(pollDuration);
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    List<IdMsgPair<ToCoreMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> pendingMap = orderedMsgList.stream().collect(
                            Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToCoreMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
                    Future<?> packSubmitFuture = consumersExecutor.submit(() -> {
                        orderedMsgList.forEach((element) -> {
                            UUID id = element.getUuid();
                            TbProtoQueueMsg<ToCoreMsg> msg = element.getMsg();
                            log.trace("[{}] Creating main callback for message: {}", id, msg.getValue());
                            TbCallback callback = new TbPackCallback<>(id, ctx);
                            try {
                                ToCoreMsg toCoreMsg = msg.getValue();
                                pendingMsgHolder.setToCoreMsg(toCoreMsg);
                                if (toCoreMsg.hasToSubscriptionMgrMsg()) {
                                    log.trace("[{}] Forwarding message to subscription manager service {}", id, toCoreMsg.getToSubscriptionMgrMsg());
                                    forwardToSubMgrService(toCoreMsg.getToSubscriptionMgrMsg(), callback);
                                } else if (toCoreMsg.hasToDeviceActorMsg()) {
                                    log.trace("[{}] Forwarding message to device actor {}", id, toCoreMsg.getToDeviceActorMsg());
                                    forwardToDeviceActor(toCoreMsg.getToDeviceActorMsg(), callback);
                                } else if (toCoreMsg.hasDeviceStateServiceMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceStateServiceMsg());
                                    forwardToStateService(toCoreMsg.getDeviceStateServiceMsg(), callback);
                                } else if (toCoreMsg.hasEdgeNotificationMsg()) {
                                    log.trace("[{}] Forwarding message to edge service {}", id, toCoreMsg.getEdgeNotificationMsg());
                                    forwardToEdgeNotificationService(toCoreMsg.getEdgeNotificationMsg(), callback);
                                } else if (toCoreMsg.hasDeviceConnectMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceConnectMsg());
                                    forwardToStateService(toCoreMsg.getDeviceConnectMsg(), callback);
                                } else if (toCoreMsg.hasDeviceActivityMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceActivityMsg());
                                    forwardToStateService(toCoreMsg.getDeviceActivityMsg(), callback);
                                } else if (toCoreMsg.hasDeviceDisconnectMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceDisconnectMsg());
                                    forwardToStateService(toCoreMsg.getDeviceDisconnectMsg(), callback);
                                } else if (toCoreMsg.hasDeviceInactivityMsg()) {
                                    log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceInactivityMsg());
                                    forwardToStateService(toCoreMsg.getDeviceInactivityMsg(), callback);
                                } else if (toCoreMsg.hasToDeviceActorNotification()) {
                                    TbActorMsg actorMsg = ProtoUtils.fromProto(toCoreMsg.getToDeviceActorNotification());
                                    if (actorMsg != null) {
                                        if (actorMsg.getMsgType().equals(MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG)) {
                                            tbCoreDeviceRpcService.forwardRpcRequestToDeviceActor((ToDeviceRpcRequestActorMsg) actorMsg);
                                        } else {
                                            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
                                            actorContext.tell(actorMsg);
                                        }
                                    }
                                    callback.onSuccess();
                                } else if (!toCoreMsg.getToDeviceActorNotificationMsg().isEmpty()) {
                                    // will be removed in 3.6.1 in favour of hasToDeviceActorNotification()
                                    Optional<TbActorMsg> actorMsg = encodingService.decode(toCoreMsg.getToDeviceActorNotificationMsg().toByteArray());
                                    if (actorMsg.isPresent()) {
                                        TbActorMsg tbActorMsg = actorMsg.get();
                                        if (tbActorMsg.getMsgType().equals(MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG)) {
                                            tbCoreDeviceRpcService.forwardRpcRequestToDeviceActor((ToDeviceRpcRequestActorMsg) tbActorMsg);
                                        } else {
                                            log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg.get());
                                            actorContext.tell(actorMsg.get());
                                        }
                                    }
                                    callback.onSuccess();
                                } else if (toCoreMsg.hasNotificationSchedulerServiceMsg()) {
                                    TransportProtos.NotificationSchedulerServiceMsg notificationSchedulerServiceMsg = toCoreMsg.getNotificationSchedulerServiceMsg();
                                    log.trace("[{}] Forwarding message to notification scheduler service {}", id, toCoreMsg.getNotificationSchedulerServiceMsg());
                                    forwardToNotificationSchedulerService(notificationSchedulerServiceMsg, callback);
                                } else if (toCoreMsg.hasErrorEventMsg()) {
                                    forwardToEventService(toCoreMsg.getErrorEventMsg(), callback);
                                } else if (toCoreMsg.hasLifecycleEventMsg()) {
                                    forwardToEventService(toCoreMsg.getLifecycleEventMsg(), callback);
                                }
                            } catch (Throwable e) {
                                log.warn("[{}] Failed to process message: {}", id, msg, e);
                                callback.onFailure(e);
                            }
                        });
                    });
                    if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
                        if (!packSubmitFuture.isDone()) {
                            packSubmitFuture.cancel(true);
                            ToCoreMsg lastSubmitMsg = pendingMsgHolder.getToCoreMsg();
                            log.info("Timeout to process message: {}", lastSubmitMsg);
                        }
                        ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
                    }
                    mainConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(pollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
            log.info("TB Core Consumer stopped.");
        });
    }

    /**
     * 中文说明：
     * 1. 类目的：`PendingMsgHolder` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
     * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
     */
    private static class PendingMsgHolder {
        /**
         * 消息，承载当前步骤需要处理的内容。
         */
        @Getter
        @Setter
        private volatile ToCoreMsg toCoreMsg;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    /**
     * 功能：获取轮询间隔。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getNotificationPollDuration() {
        return pollDuration;
    }

    /**
     * 功能：获取版本包处理超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    /**
     * 功能：处理通知。
     * 参数：
     * - `id`：`id`ID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToCoreNotificationMsg> msg, TbCallback callback) {
        ToCoreNotificationMsg toCoreNotification = msg.getValue();
        if (toCoreNotification.hasToLocalSubscriptionServiceMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getToLocalSubscriptionServiceMsg());
            forwardToLocalSubMgrService(toCoreNotification.getToLocalSubscriptionServiceMsg(), callback);
        } else if (toCoreNotification.hasCoreStartupMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getCoreStartupMsg());
            forwardCoreStartupMsg(toCoreNotification.getCoreStartupMsg(), callback);
        } else if (toCoreNotification.hasFromDeviceRpcResponse()) {
            log.trace("[{}] Forwarding message to RPC service {}", id, toCoreNotification.getFromDeviceRpcResponse());
            forwardToCoreRpcService(toCoreNotification.getFromDeviceRpcResponse(), callback);
        } else if (toCoreNotification.hasComponentLifecycle()) {
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(toCoreNotification.getComponentLifecycle()));
            callback.onSuccess();
        } else if (toCoreNotification.hasEdgeEventUpdate()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getEdgeEventUpdate()));
            callback.onSuccess();
        } else if (!toCoreNotification.getEdgeEventUpdateMsg().isEmpty()) {
            //will be removed in 3.6.1 in favour of hasEdgeEventUpdate()
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getEdgeEventUpdateMsg().toByteArray()), callback);
        } else if (toCoreNotification.hasToEdgeSyncRequest()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getToEdgeSyncRequest()));
            callback.onSuccess();
        } else if (!toCoreNotification.getToEdgeSyncRequestMsg().isEmpty()) {
            //will be removed in 3.6.1 in favour of hasToEdgeSyncRequest()
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getToEdgeSyncRequestMsg().toByteArray()), callback);
        } else if (toCoreNotification.hasFromEdgeSyncResponse()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getFromEdgeSyncResponse()));
            callback.onSuccess();
        } else if (!toCoreNotification.getFromEdgeSyncResponseMsg().isEmpty()) {
            //will be removed in 3.6.1 in favour of hasFromEdgeSyncResponse()
            forwardToAppActor(id, encodingService.decode(toCoreNotification.getFromEdgeSyncResponseMsg().toByteArray()), callback);
        } else if (toCoreNotification.getQueueUpdateMsgsCount() > 0) {
            partitionService.updateQueues(toCoreNotification.getQueueUpdateMsgsList());
            callback.onSuccess();
        } else if (toCoreNotification.getQueueDeleteMsgsCount() > 0) {
            partitionService.removeQueues(toCoreNotification.getQueueDeleteMsgsList());
            callback.onSuccess();
        } else if (toCoreNotification.hasVcResponseMsg()) {
            vcQueueService.processResponse(toCoreNotification.getVcResponseMsg());
            callback.onSuccess();
        } else if (toCoreNotification.hasToSubscriptionMgrMsg()) {
            forwardToSubMgrService(toCoreNotification.getToSubscriptionMgrMsg(), callback);
        } else if (toCoreNotification.hasNotificationRuleProcessorMsg()) {
            Optional<NotificationRuleTrigger> notificationRuleTrigger = encodingService.decode(toCoreNotification
                    .getNotificationRuleProcessorMsg().getTrigger().toByteArray());
            notificationRuleTrigger.ifPresent(notificationRuleProcessor::process);
            callback.onSuccess();
        } else if (toCoreNotification.hasResourceCacheInvalidateMsg()) {
            forwardToResourceService(toCoreNotification.getResourceCacheInvalidateMsg(), callback);
        }
        if (statsEnabled) {
            stats.log(toCoreNotification);
        }
    }

    /**
     * 功能：执行 `launchUsageStatsConsumer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void launchUsageStatsConsumer() {
        usageStatsExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgs = usageStatsConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    ConcurrentMap<UUID, TbProtoQueueMsg<ToUsageStatsServiceMsg>> pendingMap = msgs.stream().collect(
                            Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
                    CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
                    TbPackProcessingContext<TbProtoQueueMsg<ToUsageStatsServiceMsg>> ctx = new TbPackProcessingContext<>(
                            processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
                    pendingMap.forEach((id, msg) -> {
                        log.trace("[{}] Creating usage stats callback for message: {}", id, msg.getValue());
                        TbCallback callback = new TbPackCallback<>(id, ctx);
                        try {
                            handleUsageStats(msg, callback);
                        } catch (Throwable e) {
                            log.warn("[{}] Failed to process usage stats: {}", id, msg, e);
                            callback.onFailure(e);
                        }
                    });
                    if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
                        ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process usage stats: {}", id, msg.getValue()));
                        ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process usage stats: {}", id, msg.getValue()));
                    }
                    usageStatsConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new usage stats", e2);
                        }
                    }
                }
            }
            log.info("TB Usage Stats Consumer stopped.");
        });
    }

    /**
     * 功能：执行 `launchOtaPackageUpdateNotificationConsumer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void launchOtaPackageUpdateNotificationConsumer() {
        long maxProcessingTimeoutPerRecord = firmwarePackInterval / firmwarePackSize;
        firmwareStatesExecutor.submit(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> msgs = firmwareStatesConsumer.poll(getNotificationPollDuration());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    long timeToSleep = maxProcessingTimeoutPerRecord;
                    for (TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg : msgs) {
                        try {
                            long startTime = System.currentTimeMillis();
                            boolean isSuccessUpdate = handleOtaPackageUpdates(msg);
                            long endTime = System.currentTimeMillis();
                            long spentTime = endTime - startTime;
                            timeToSleep = timeToSleep - spentTime;
                            if (isSuccessUpdate) {
                                if (timeToSleep > 0) {
                                    log.debug("Spent time per record is: [{}]!", spentTime);
                                    Thread.sleep(timeToSleep);
                                    timeToSleep = 0;
                                }
                                timeToSleep += maxProcessingTimeoutPerRecord;
                            }
                        } catch (Throwable e) {
                            log.warn("Failed to process firmware update msg: {}", msg, e);
                        }
                    }
                    firmwareStatesConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain usage stats from queue.", e);
                        try {
                            Thread.sleep(getNotificationPollDuration());
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new firmware updates", e2);
                        }
                    }
                }
            }
            log.info("TB Ota Package States Consumer stopped.");
        });
    }

    /**
     * 功能：处理`Usage Stats`。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void handleUsageStats(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        statsService.process(msg, callback);
    }

    /**
     * 功能：处理`Ota Package Updates`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    private boolean handleOtaPackageUpdates(TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg) {
        return firmwareStateService.process(msg.getValue());
    }

    /**
     * 功能：执行 `forwardToCoreRpcService` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToCoreRpcService(FromDeviceRPCResponseProto proto, TbCallback callback) {
        RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
        FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                , proto.getResponse(), error);
        tbCoreDeviceRpcService.processRpcResponseFromRuleEngine(response);
        callback.onSuccess();
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${queue.core.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    /**
     * 功能：执行 `forwardToLocalSubMgrService` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToLocalSubMgrService(LocalSubscriptionServiceMsgProto msg, TbCallback callback) {
        if (msg.hasSubEventCallback()) {
            localSubscriptionService.onSubEventCallback(msg.getSubEventCallback(), callback);
        } else if (msg.hasTsUpdate()) {
            localSubscriptionService.onTimeSeriesUpdate(msg.getTsUpdate(), callback);
        } else if (msg.hasAttrUpdate()) {
            localSubscriptionService.onAttributesUpdate(msg.getAttrUpdate(), callback);
        } else if (msg.hasAlarmUpdate()) {
            localSubscriptionService.onAlarmUpdate(msg.getAlarmUpdate(), callback);
        } else if (msg.hasNotificationsUpdate()) {
            localSubscriptionService.onNotificationUpdate(msg.getNotificationsUpdate(), callback);
        } else if (msg.hasSubUpdate() || msg.hasAlarmSubUpdate() || msg.hasNotificationsSubUpdate()) {
            //OLD CODE -> Do NOTHING.
            callback.onSuccess();
        } else {
            throwNotHandled(msg, callback);
        }
    }

    /**
     * 功能：执行 `forwardCoreStartupMsg` 对应的处理。
     * 参数：
     * - `coreStartupMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg, TbCallback callback) {
        log.info("[{}] Processing core startup with partitions: {}", coreStartupMsg.getServiceId(), coreStartupMsg.getPartitionsList());
        localSubscriptionService.onCoreStartupMsg(coreStartupMsg);
        callback.onSuccess();
    }

    /**
     * 功能：执行 `forwardToResourceService` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToResourceService(TransportProtos.ResourceCacheInvalidateMsg msg, TbCallback callback) {
        var tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        msg.getKeysList().stream().map(cacheKeyProto -> {
            if (cacheKeyProto.hasResourceKey()) {
                return ImageCacheKey.forImage(tenantId, cacheKeyProto.getResourceKey());
            } else {
                return ImageCacheKey.forPublicImage(cacheKeyProto.getPublicResourceKey());
            }
        }).forEach(imageService::evictETags);
        callback.onSuccess();
    }

    /**
     * 功能：执行 `forwardToSubMgrService` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToSubMgrService(SubscriptionMgrMsgProto msg, TbCallback callback) {
        if (msg.hasSubEvent()) {
            TbEntitySubEventProto subEvent = msg.getSubEvent();
            subscriptionManagerService.onSubEvent(subEvent.getServiceId(), TbSubscriptionUtils.fromProto(subEvent), callback);
        } else if (msg.hasTelemetrySub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasAlarmSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasNotificationsSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasNotificationsCountSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasSubClose()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasTsUpdate()) {
            TbTimeSeriesUpdateProto proto = msg.getTsUpdate();
            long tenantIdMSB = proto.getTenantIdMSB();
            long tenantIdLSB = proto.getTenantIdLSB();
            subscriptionManagerService.onTimeSeriesUpdate(
                    toTenantId(tenantIdMSB, tenantIdLSB),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    TbSubscriptionUtils.toTsKvEntityList(proto.getDataList()), callback);
        } else if (msg.hasAttrUpdate()) {
            TbAttributeUpdateProto proto = msg.getAttrUpdate();
            subscriptionManagerService.onAttributesUpdate(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), TbSubscriptionUtils.toAttributeKvList(proto.getDataList()), callback);
        } else if (msg.hasAttrDelete()) {
            TbAttributeDeleteProto proto = msg.getAttrDelete();
            subscriptionManagerService.onAttributesDelete(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), proto.getKeysList(), proto.getNotifyDevice(), callback);
        } else if (msg.hasTsDelete()) {
            TbTimeSeriesDeleteProto proto = msg.getTsDelete();
            subscriptionManagerService.onTimeSeriesDelete(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getKeysList(), callback);
        } else if (msg.hasAlarmUpdate()) {
            TbAlarmUpdateProto proto = msg.getAlarmUpdate();
            subscriptionManagerService.onAlarmUpdate(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), AlarmInfo.class),
                    callback);
        } else if (msg.hasAlarmDelete()) {
            TbAlarmDeleteProto proto = msg.getAlarmDelete();
            subscriptionManagerService.onAlarmDeleted(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), AlarmInfo.class), callback);
        } else if (msg.hasNotificationUpdate()) {
            TransportProtos.NotificationUpdateProto updateProto = msg.getNotificationUpdate();
            TenantId tenantId = toTenantId(updateProto.getTenantIdMSB(), updateProto.getTenantIdLSB());
            UserId recipientId = new UserId(new UUID(updateProto.getRecipientIdMSB(), updateProto.getRecipientIdLSB()));
            NotificationUpdate update = JacksonUtil.fromString(updateProto.getUpdate(), NotificationUpdate.class);
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, callback);
        } else if (msg.hasNotificationRequestUpdate()) {
            TransportProtos.NotificationRequestUpdateProto updateProto = msg.getNotificationRequestUpdate();
            TenantId tenantId = toTenantId(updateProto.getTenantIdMSB(), updateProto.getTenantIdLSB());
            NotificationRequestUpdate update = JacksonUtil.fromString(updateProto.getUpdate(), NotificationRequestUpdate.class);
            localSubscriptionService.onNotificationRequestUpdate(tenantId, update, callback);
        } else {
            throwNotHandled(msg, callback);
        }
        if (statsEnabled) {
            stats.log(msg);
        }
    }

    /**
     * 功能：执行 `forwardToStateService` 对应的处理。
     * 参数：
     * - `deviceStateServiceMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void forwardToStateService(DeviceStateServiceMsgProto deviceStateServiceMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceStateServiceMsg);
        }
        stateService.onQueueMsg(deviceStateServiceMsg, callback);
    }

    /**
     * 功能：执行 `forwardToStateService` 对应的处理。
     * 参数：
     * - `deviceConnectMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void forwardToStateService(TransportProtos.DeviceConnectProto deviceConnectMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceConnectMsg);
        }
        var tenantId = toTenantId(deviceConnectMsg.getTenantIdMSB(), deviceConnectMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceConnectMsg.getDeviceIdMSB(), deviceConnectMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceConnect(tenantId, deviceId, deviceConnectMsg.getLastConnectTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device connect message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    /**
     * 功能：执行 `forwardToStateService` 对应的处理。
     * 参数：
     * - `deviceActivityMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void forwardToStateService(TransportProtos.DeviceActivityProto deviceActivityMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceActivityMsg);
        }
        var tenantId = toTenantId(deviceActivityMsg.getTenantIdMSB(), deviceActivityMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceActivityMsg.getDeviceIdMSB(), deviceActivityMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceActivity(tenantId, deviceId, deviceActivityMsg.getLastActivityTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device activity message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(new RuntimeException("Failed to update device activity for device [" + deviceId.getId() + "]!", t));
                });
    }

    /**
     * 功能：执行 `forwardToStateService` 对应的处理。
     * 参数：
     * - `deviceDisconnectMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void forwardToStateService(TransportProtos.DeviceDisconnectProto deviceDisconnectMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceDisconnectMsg);
        }
        var tenantId = toTenantId(deviceDisconnectMsg.getTenantIdMSB(), deviceDisconnectMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceDisconnectMsg.getDeviceIdMSB(), deviceDisconnectMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceDisconnect(tenantId, deviceId, deviceDisconnectMsg.getLastDisconnectTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device disconnect message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    /**
     * 功能：执行 `forwardToStateService` 对应的处理。
     * 参数：
     * - `deviceInactivityMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void forwardToStateService(TransportProtos.DeviceInactivityProto deviceInactivityMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceInactivityMsg);
        }
        var tenantId = toTenantId(deviceInactivityMsg.getTenantIdMSB(), deviceInactivityMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceInactivityMsg.getDeviceIdMSB(), deviceInactivityMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceInactivity(tenantId, deviceId, deviceInactivityMsg.getLastInactivityTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device inactivity message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    /**
     * 功能：执行 `forwardToNotificationSchedulerService` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToNotificationSchedulerService(TransportProtos.NotificationSchedulerServiceMsg msg, TbCallback callback) {
        TenantId tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        NotificationRequestId notificationRequestId = new NotificationRequestId(new UUID(msg.getRequestIdMSB(), msg.getRequestIdLSB()));
        try {
            notificationSchedulerService.scheduleNotificationRequest(tenantId, notificationRequestId, msg.getTs());
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(new RuntimeException("Failed to schedule notification request", e));
        }
    }

    /**
     * 功能：执行 `forwardToEdgeNotificationService` 对应的处理。
     * 参数：
     * - `edgeNotificationMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToEdgeNotificationService(EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(edgeNotificationMsg);
        }
        edgeNotificationService.pushNotificationToEdge(edgeNotificationMsg, callback);
    }

    /**
     * 功能：执行 `forwardToDeviceActor` 对应的处理。
     * 参数：
     * - `toDeviceActorMsg`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToDeviceActor(TransportToDeviceActorMsg toDeviceActorMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(toDeviceActorMsg);
        }
        actorContext.tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback));
    }

    /**
     * 功能：执行 `forwardToAppActor` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `actorMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToAppActor(UUID id, Optional<TbActorMsg> actorMsg, TbCallback callback) {
        if (actorMsg.isPresent()) {
            forwardToAppActor(id, actorMsg.get());
        }
        callback.onSuccess();
    }

    /**
     * 功能：执行 `forwardToAppActor` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `actorMsg`：待处理消息。
     * 返回：无。
     */
    private void forwardToAppActor(UUID id, TbActorMsg actorMsg) {
        log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
        actorContext.tell(actorMsg);
    }

    /**
     * 功能：执行 `forwardToEventService` 对应的处理。
     * 参数：
     * - `eventProto`：`eventProto` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToEventService(ErrorEventProto eventProto, TbCallback callback) {
        Event event = ErrorEvent.builder()
                .tenantId(toTenantId(eventProto.getTenantIdMSB(), eventProto.getTenantIdLSB()))
                .entityId(new UUID(eventProto.getEntityIdMSB(), eventProto.getEntityIdLSB()))
                .serviceId(eventProto.getServiceId())
                .ts(System.currentTimeMillis())
                .method(eventProto.getMethod())
                .error(eventProto.getError())
                .build();
        forwardToEventService(event, callback);
    }

    /**
     * 功能：执行 `forwardToEventService` 对应的处理。
     * 参数：
     * - `eventProto`：`eventProto` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToEventService(LifecycleEventProto eventProto, TbCallback callback) {
        Event event = LifecycleEvent.builder()
                .tenantId(toTenantId(eventProto.getTenantIdMSB(), eventProto.getTenantIdLSB()))
                .entityId(new UUID(eventProto.getEntityIdMSB(), eventProto.getEntityIdLSB()))
                .serviceId(eventProto.getServiceId())
                .ts(System.currentTimeMillis())
                .lcEventType(eventProto.getLcEventType())
                .success(eventProto.getSuccess())
                .error(StringUtils.isNotEmpty(eventProto.getError()) ? eventProto.getError() : null)
                .build();
        forwardToEventService(event, callback);
    }

    /**
     * 功能：执行 `forwardToEventService` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToEventService(Event event, TbCallback callback) {
        DonAsynchron.withCallback(actorContext.getEventService().saveAsync(event),
                result -> callback.onSuccess(),
                callback::onFailure,
                actorContext.getDbCallbackExecutor());
    }

    /**
     * 功能：执行 `throwNotHandled` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void throwNotHandled(Object msg, TbCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }

    /**
     * 功能：执行 `toTenantId` 对应的处理。
     * 参数：
     * - `tenantIdMSB`：租户信息或租户标识。
     * - `tenantIdLSB`：租户信息或租户标识。
     * 返回：处理结果。
     */
    private TenantId toTenantId(long tenantIdMSB, long tenantIdLSB) {
        return TenantId.fromUUID(new UUID(tenantIdMSB, tenantIdLSB));
    }

    /**
     * 功能：停止或关闭`Consumers`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void stopConsumers() {
        if (mainConsumer != null) {
            mainConsumer.unsubscribe();
        }
        if (usageStatsConsumer != null) {
            usageStatsConsumer.unsubscribe();
        }
        if (firmwareStatesConsumer != null) {
            firmwareStatesConsumer.unsubscribe();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbCoreConsumerService` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
