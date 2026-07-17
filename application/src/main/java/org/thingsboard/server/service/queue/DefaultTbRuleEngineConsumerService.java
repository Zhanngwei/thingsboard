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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.QueueDeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.processing.AbstractConsumerService;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineConsumerContext;
import org.thingsboard.server.service.queue.ruleengine.TbRuleEngineQueueConsumerManager;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.security.auth.jwt.settings.JwtSettingsService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultTbRuleEngineConsumerService` 是 ThingsBoard Application 中负责 `Tb Rule Engine` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractConsumerService`、`TbRuleEngineConsumerService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbRuleEngineConsumerContext ctx;
    private final QueueService queueService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;

    private final ConcurrentMap<QueueKey, TbRuleEngineQueueConsumerManager> consumers = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `DefaultTbRuleEngineConsumerService` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `tbRuleEngineQueueFactory`：队列名称或队列对象。
     * - `actorContext`：处理上下文。
     * - `encodingService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DefaultTbRuleEngineConsumerService(TbRuleEngineConsumerContext ctx,
                                              TbRuleEngineQueueFactory tbRuleEngineQueueFactory,
                                              ActorSystemContext actorContext,
                                              DataDecodingEncodingService encodingService,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              QueueService queueService,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService,
                                              ApplicationEventPublisher eventPublisher,
                                              JwtSettingsService jwtSettingsService) {
        super(actorContext, encodingService, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService,
                eventPublisher, tbRuleEngineQueueFactory.createToRuleEngineNotificationsMsgConsumer(), jwtSettingsService);
        this.ctx = ctx;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.queueService = queueService;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        super.init("tb-rule-engine-notifications-consumer");
        List<Queue> queues = queueService.findAllQueues();
        for (Queue configuration : queues) {
            if (partitionService.isManagedByCurrentService(configuration.getTenantId())) {
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, configuration);
                createConsumer(queueKey, configuration);
            }
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        event.getPartitionsMap().forEach((queueKey, partitions) -> {
            if (partitionService.isManagedByCurrentService(queueKey.getTenantId())) {
                var consumer = getConsumer(queueKey).orElseGet(() -> {
                    Queue config = queueService.findQueueByTenantIdAndName(queueKey.getTenantId(), queueKey.getQueueName());
                    if (config == null) {
                        if (!partitions.isEmpty()) {
                            log.error("[{}] Queue configuration is missing", queueKey, new RuntimeException("stacktrace"));
                        }
                        return null;
                    }
                    return createConsumer(queueKey, config);
                });
                if (consumer != null) {
                    consumer.update(partitions);
                }
            }
        });
        consumers.keySet().stream()
                .collect(Collectors.groupingBy(QueueKey::getTenantId))
                .forEach((tenantId, queueKeys) -> {
                    if (!partitionService.isManagedByCurrentService(tenantId)) {
                        queueKeys.forEach(queueKey -> {
                            removeConsumer(queueKey).ifPresent(TbRuleEngineQueueConsumerManager::stop);
                        });
                    }
                });
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
        ctx.setReady(true);
    }

    /**
     * 功能：执行 `launchMainConsumers` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void launchMainConsumers() {}

    /**
     * 功能：停止或关闭`Consumers`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void stopConsumers() {
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::stop);
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::awaitStop);
        ctx.stop();
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    /**
     * 功能：获取轮询间隔。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getNotificationPollDuration() {
        return ctx.getPollDuration();
    }

    /**
     * 功能：获取版本包处理超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    protected long getNotificationPackProcessingTimeout() {
        return ctx.getPackProcessingTimeout();
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
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.hasComponentLifecycle()) {
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(nfMsg.getComponentLifecycle()));
            callback.onSuccess();
        } else if (nfMsg.hasFromDeviceRpcResponse()) {
            TransportProtos.FromDeviceRPCResponseProto proto = nfMsg.getFromDeviceRpcResponse();
            RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
            FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                    , proto.getResponse(), error);
            tbDeviceRpcService.processRpcResponseFromDevice(response);
            callback.onSuccess();
        } else if (nfMsg.getQueueUpdateMsgsCount() > 0) {
            updateQueues(nfMsg.getQueueUpdateMsgsList());
            callback.onSuccess();
        } else if (nfMsg.getQueueDeleteMsgsCount() > 0) {
            deleteQueues(nfMsg.getQueueDeleteMsgsList());
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
    }

    /**
     * 功能：更新`Queues`。
     * 参数：
     * - `queueUpdateMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    private void updateQueues(List<QueueUpdateMsg> queueUpdateMsgs) {
        for (QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            log.info("Received queue update msg: [{}]", queueUpdateMsg);
            TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
            if (partitionService.isManagedByCurrentService(tenantId)) {
                QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
                String queueName = queueUpdateMsg.getQueueName();
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId);
                Queue queue = queueService.findQueueById(tenantId, queueId);

                getConsumer(queueKey).ifPresentOrElse(consumer -> consumer.update(queue),
                        () -> createConsumer(queueKey, queue));
            }
        }

        partitionService.updateQueues(queueUpdateMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(),
                new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    /**
     * 功能：删除或清理`Queues`。
     * 参数：
     * - `queueDeleteMsgs`：队列名称或队列对象。
     * 返回：无。
     */
    private void deleteQueues(List<QueueDeleteMsg> queueDeleteMsgs) {
        for (QueueDeleteMsg queueDeleteMsg : queueDeleteMsgs) {
            log.info("Received queue delete msg: [{}]", queueDeleteMsg);
            TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
            removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(true));
        }

        partitionService.removeQueues(queueDeleteMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.TENANT) {
            if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                List<QueueKey> toRemove = consumers.keySet().stream()
                        .filter(queueKey -> queueKey.getTenantId().equals(event.getTenantId()))
                        .collect(Collectors.toList());
                toRemove.forEach(queueKey -> {
                    removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(false));
                });
            }
        }
    }

    /**
     * 功能：获取消息消费者。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * 返回：可能存在的结果。
     */
    private Optional<TbRuleEngineQueueConsumerManager> getConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.get(queueKey));
    }

    /**
     * 功能：保存或创建消息消费者。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    private TbRuleEngineQueueConsumerManager createConsumer(QueueKey queueKey, Queue queue) {
        var consumer = new TbRuleEngineQueueConsumerManager(ctx, queueKey);
        consumers.put(queueKey, consumer);
        consumer.init(queue);
        return consumer;
    }

    /**
     * 功能：删除或清理消息消费者。
     * 参数：
     * - `queueKey`：队列名称或队列对象。
     * 返回：可能存在的结果。
     */
    private Optional<TbRuleEngineQueueConsumerManager> removeConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.remove(queueKey));
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (ctx.isStatsEnabled()) {
            long ts = System.currentTimeMillis();
            consumers.values().forEach(manager -> manager.printStats(ts));
        }
    }

}
