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

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasRuleEngineProfile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.EdgeEventUpdateMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rule.engine.DeviceEdgeUpdateMsg;
import org.thingsboard.server.common.msg.rule.engine.DeviceNameOrTypeUpdateMsg;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.thingsboard.server.gen.transport.TransportProtos.QueueDeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.QueueUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.MultipleTbQueueCallbackWrapper;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.util.ProtoUtils.toProto;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbClusterService` 是ThingsBoard Application 模块中的队列服务类型，用于封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 生命周期：由 Spring 创建并随应用启动订阅队列，运行期持续处理消息。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Producer-Consumer / Strategy。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultTbClusterService implements TbClusterService {

    /**
     * 是否启用`stats`。
     */
    @Value("${cluster.stats.enabled:false}")
    private boolean statsEnabled;
    /**
     * 是否启用`edges`。
     */
    @Value("${edges.enabled:true}")
    protected boolean edgesEnabled;

    private final AtomicInteger toCoreMsgs = new AtomicInteger(0);
    private final AtomicInteger toCoreNfs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineMsgs = new AtomicInteger(0);
    private final AtomicInteger toRuleEngineNfs = new AtomicInteger(0);
    private final AtomicInteger toTransportNfs = new AtomicInteger(0);

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private PartitionService partitionService;

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private OtaPackageStateService otaPackageStateService;

    /**
     * 主题，提供当前类调用的业务操作。
     */
    private final TopicService topicService;
    private final DataDecodingEncodingService encodingService;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbAssetProfileCache assetProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final GatewayNotificationsService gatewayNotificationsService;
    private final EdgeService edgeService;

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, entityId);
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msgId`：消息ID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToCore(TopicPartitionInfo tpi, UUID msgId, ToCoreMsg msg, TbQueueCallback callback) {
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toCoreMsgs.incrementAndGet();
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, msg.getTenantId(), msg.getDeviceId());
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        ToCoreMsg toCoreMsg = ToCoreMsg.newBuilder().setToDeviceActorNotification(toProto(msg)).build();
        producerProvider.getTbCoreMsgProducer().send(tpi, new TbProtoQueueMsg<>(msg.getDeviceId().getId(), toCoreMsg), callback);
        toCoreMsgs.incrementAndGet();
    }

    /**
     * 功能：执行 `broadcastToCore` 对应的处理。
     * 参数：
     * - `toCoreMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void broadcastToCore(ToCoreNotificationMsg toCoreMsg) {
        UUID msgId = UUID.randomUUID();
        TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        for (String serviceId : tbCoreServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(msgId, toCoreMsg), null);
            toCoreNfs.incrementAndGet();
        }
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToVersionControl(TenantId tenantId, TransportProtos.ToVersionControlServiceMsg msg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_VC_EXECUTOR, tenantId, tenantId);
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getTbVersionControlMsgProducer().send(tpi, new TbProtoQueueMsg<>(tenantId.getId(), msg), callback);
        //TODO: ashvayka
        toCoreMsgs.incrementAndGet();
    }

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `serviceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushNotificationToCore(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toCoreNfs.incrementAndGet();
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msgId`：消息ID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback) {
        log.trace("PUSHING msg: {} to:{}", msg, tpi);
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(msgId, msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg tbMsg, TbQueueCallback callback) {
        if (tenantId == null || tenantId.isNullUid()) {
            if (entityId.getEntityType().equals(EntityType.TENANT)) {
                tenantId = TenantId.fromUUID(entityId.getId());
            } else {
                log.warn("[{}][{}] Received invalid message: {}", tenantId, entityId, tbMsg);
                return;
            }
        } else {
            HasRuleEngineProfile ruleEngineProfile = getRuleEngineProfileForEntityOrElseNull(tenantId, entityId);
            tbMsg = transformMsg(tbMsg, ruleEngineProfile);
        }
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tbMsg.getQueueName(), tenantId, entityId);
        log.trace("PUSHING msg: {} to:{}", tbMsg, tpi);
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTbMsg(TbMsg.toByteString(tbMsg)).build();
        producerProvider.getRuleEngineMsgProducer().send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), callback);
        toRuleEngineMsgs.incrementAndGet();
    }

    /**
     * 功能：获取规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private HasRuleEngineProfile getRuleEngineProfileForEntityOrElseNull(TenantId tenantId, EntityId entityId) {
        if (entityId.getEntityType().equals(EntityType.DEVICE)) {
            return deviceProfileCache.get(tenantId, new DeviceId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.DEVICE_PROFILE)) {
            return deviceProfileCache.get(tenantId, new DeviceProfileId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.ASSET)) {
            return assetProfileCache.get(tenantId, new AssetId(entityId.getId()));
        } else if (entityId.getEntityType().equals(EntityType.ASSET_PROFILE)) {
            return assetProfileCache.get(tenantId, new AssetProfileId(entityId.getId()));
        }
        return null;
    }

    /**
     * 功能：转换消息。
     * 参数：
     * - `tbMsg`：待处理消息。
     * - `ruleEngineProfile`：`ruleEngineProfile` 参数。
     * 返回：处理结果。
     */
    private TbMsg transformMsg(TbMsg tbMsg, HasRuleEngineProfile ruleEngineProfile) {
        if (ruleEngineProfile != null) {
            RuleChainId targetRuleChainId = ruleEngineProfile.getDefaultRuleChainId();
            String targetQueueName = ruleEngineProfile.getDefaultQueueName();

            boolean isRuleChainTransform = targetRuleChainId != null && !targetRuleChainId.equals(tbMsg.getRuleChainId());
            boolean isQueueTransform = targetQueueName != null && !targetQueueName.equals(tbMsg.getQueueName());

            if (isRuleChainTransform && isQueueTransform) {
                tbMsg = TbMsg.transformMsg(tbMsg, targetRuleChainId, targetQueueName);
            } else if (isRuleChainTransform) {
                tbMsg = TbMsg.transformMsgRuleChainId(tbMsg, targetRuleChainId);
            } else if (isQueueTransform) {
                tbMsg = TbMsg.transformMsgQueueName(tbMsg, targetQueueName);
            }
        }
        return tbMsg;
    }

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `serviceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushNotificationToRuleEngine(String serviceId, FromDeviceRpcResponse response, TbQueueCallback callback) {
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        FromDeviceRPCResponseProto.Builder builder = FromDeviceRPCResponseProto.newBuilder()
                .setRequestIdMSB(response.getId().getMostSignificantBits())
                .setRequestIdLSB(response.getId().getLeastSignificantBits())
                .setError(response.getError().isPresent() ? response.getError().get().ordinal() : -1);
        response.getResponse().ifPresent(builder::setResponse);
        ToRuleEngineNotificationMsg msg = ToRuleEngineNotificationMsg.newBuilder().setFromDeviceRpcResponse(builder).build();
        producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(response.getId(), msg), callback);
        toRuleEngineNfs.incrementAndGet();
    }

    /**
     * 功能：发送或提交传输层。
     * 参数：
     * - `serviceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void pushNotificationToTransport(String serviceId, ToTransportMsg response, TbQueueCallback callback) {
        if (serviceId == null || serviceId.isEmpty()) {
            log.trace("pushNotificationToTransport: skipping message without serviceId [{}], (ToTransportMsg) response [{}]", serviceId, response);
            if (callback != null) {
                callback.onSuccess(null); //callback that message already sent, no useful payload expected
            }
            return;
        }
        TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceId);
        log.trace("PUSHING msg: {} to:{}", response, tpi);
        producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), response), callback);
        toTransportNfs.incrementAndGet();
    }

    /**
     * 功能：执行 `broadcastEntityStateChangeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    @Override
    public void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state) {
        log.trace("[{}] Processing {} state change event: {}", tenantId, entityId.getEntityType(), state);
        broadcast(new ComponentLifecycleMsg(tenantId, entityId, state));
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileChange(DeviceProfile deviceProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(deviceProfile.getTenantId(), deviceProfile.getId(), deviceProfile, callback);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenantProfile.getId(), tenantProfile, callback);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTenantChange(Tenant tenant, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(TenantId.SYS_TENANT_ID, tenant.getId(), tenant, callback);
    }

    /**
     * 功能：处理状态。
     * 参数：
     * - `apiUsageState`：`apiUsageState` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback) {
        broadcastEntityChangeToTransport(apiUsageState.getTenantId(), apiUsageState.getId(), apiUsageState, callback);
        broadcast(new ComponentLifecycleMsg(apiUsageState.getTenantId(), apiUsageState.getId(), ComponentLifecycleEvent.UPDATED));
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `entity`：实体对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileDelete(DeviceProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(entity.getTenantId(), entity.getId(), entity.getName(), callback);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `entity`：实体对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTenantProfileDelete(TenantProfile entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `entity`：实体对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onTenantDelete(Tenant entity, TbQueueCallback callback) {
        broadcastEntityDeleteToTransport(TenantId.SYS_TENANT_ID, entity.getId(), entity.getName(), callback);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceDeleted(TenantId tenantId, Device device, TbQueueCallback callback) {
        DeviceId deviceId = device.getId();
        broadcastEntityDeleteToTransport(tenantId, deviceId, device.getName(), callback);
        sendDeviceStateServiceEvent(tenantId, deviceId, false, false, true);
        broadcastEntityStateChangeEvent(tenantId, deviceId, ComponentLifecycleEvent.DELETED);
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `oldTenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceAssignedToTenant(TenantId oldTenantId, Device device) {
        onDeviceDeleted(oldTenantId, device, null);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), true, false, false);
    }

    /**
     * 功能：处理`on Resource Change`。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onResourceChange(TbResource resource, TbQueueCallback callback) {
        TenantId tenantId = resource.getTenantId();
        log.trace("[{}][{}][{}] Processing change resource", tenantId, resource.getResourceType(), resource.getResourceKey());
        TransportProtos.ResourceUpdateMsg resourceUpdateMsg = TransportProtos.ResourceUpdateMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceUpdateMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    /**
     * 功能：处理`on Resource Deleted`。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onResourceDeleted(TbResource resource, TbQueueCallback callback) {
        log.trace("[{}] Processing delete resource", resource);
        TransportProtos.ResourceDeleteMsg resourceUpdateMsg = TransportProtos.ResourceDeleteMsg.newBuilder()
                .setTenantIdMSB(resource.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(resource.getTenantId().getId().getLeastSignificantBits())
                .setResourceType(resource.getResourceType().name())
                .setResourceKey(resource.getResourceKey())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setResourceDeleteMsg(resourceUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    /**
     * 功能：执行 `broadcastEntityChangeToTransport` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityid`：`entityid`ID。
     * - `entity`：实体对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public <T> void broadcastEntityChangeToTransport(TenantId tenantId, EntityId entityid, T entity, TbQueueCallback callback) {
        String entityName = (entity instanceof HasName) ? ((HasName) entity).getName() : entity.getClass().getName();
        log.trace("[{}][{}][{}] Processing [{}] change event", tenantId, entityid.getEntityType(), entityid.getId(), entityName);
        TransportProtos.EntityUpdateMsg entityUpdateMsg = TransportProtos.EntityUpdateMsg.newBuilder()
                .setEntityType(entityid.getEntityType().name())
                .setData(ByteString.copyFrom(encodingService.encode(entity))).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityUpdateMsg(entityUpdateMsg).build();
        broadcast(transportMsg, callback);
    }

    /**
     * 功能：执行 `broadcastEntityDeleteToTransport` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `name`：名称。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void broadcastEntityDeleteToTransport(TenantId tenantId, EntityId entityId, String name, TbQueueCallback callback) {
        log.trace("[{}][{}][{}] Processing [{}] delete event", tenantId, entityId.getEntityType(), entityId.getId(), name);
        TransportProtos.EntityDeleteMsg entityDeleteMsg = TransportProtos.EntityDeleteMsg.newBuilder()
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().setEntityDeleteMsg(entityDeleteMsg).build();
        broadcast(transportMsg, callback);
    }

    /**
     * 功能：执行 `broadcast` 对应的处理。
     * 参数：
     * - `transportMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void broadcast(ToTransportMsg transportMsg, TbQueueCallback callback) {
        TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> toTransportNfProducer = producerProvider.getTransportNotificationsMsgProducer();
        Set<String> tbTransportServices = partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT);
        TbQueueCallback proxyCallback = callback != null ? new MultipleTbQueueCallbackWrapper(tbTransportServices.size(), callback) : null;
        for (String transportServiceId : tbTransportServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportServiceId);
            toTransportNfProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), proxyCallback);
            toTransportNfs.incrementAndGet();
        }
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    @Override
    public void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId) {
        log.trace("[{}] Processing edge {} event update ", tenantId, edgeId);
        EdgeEventUpdateMsg msg = new EdgeEventUpdateMsg(tenantId, edgeId);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setEdgeEventUpdate(toProto(msg)).build();
        pushEdgeSyncMsgToCore(edgeId, toCoreMsg);
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `toEdgeSyncRequest`：请求对象。
     * 返回：无。
     */
    @Override
    public void pushEdgeSyncRequestToCore(ToEdgeSyncRequest toEdgeSyncRequest) {
        log.trace("[{}] Processing edge sync request {} ", toEdgeSyncRequest.getTenantId(), toEdgeSyncRequest);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setToEdgeSyncRequest(toProto(toEdgeSyncRequest)).build();
        pushEdgeSyncMsgToCore(toEdgeSyncRequest.getEdgeId(), toCoreMsg);
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `fromEdgeSyncResponse`：响应对象。
     * 返回：无。
     */
    @Override
    public void pushEdgeSyncResponseToCore(FromEdgeSyncResponse fromEdgeSyncResponse) {
        log.trace("[{}] Processing edge sync response {}", fromEdgeSyncResponse.getTenantId(), fromEdgeSyncResponse);
        ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setFromEdgeSyncResponse(toProto(fromEdgeSyncResponse)).build();
        pushEdgeSyncMsgToCore(fromEdgeSyncResponse.getEdgeId(), toCoreMsg);
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `toCoreMsg`：待处理消息。
     * 返回：无。
     */
    private void pushEdgeSyncMsgToCore(EdgeId edgeId, ToCoreNotificationMsg toCoreMsg) {
        TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        for (String serviceId : tbCoreServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
            toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(edgeId.getId(), toCoreMsg), null);
            toCoreNfs.incrementAndGet();
        }
    }

    /**
     * 功能：执行 `broadcast` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void broadcast(ComponentLifecycleMsg msg) {
        TransportProtos.ComponentLifecycleMsgProto componentLifecycleMsgProto = toProto(msg);
        TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> toRuleEngineProducer = producerProvider.getRuleEngineNotificationsMsgProducer();
        Set<String> tbRuleEngineServices = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        EntityType entityType = msg.getEntityId().getEntityType();
        if (entityType.equals(EntityType.TENANT)
                || entityType.equals(EntityType.TENANT_PROFILE)
                || entityType.equals(EntityType.DEVICE_PROFILE)
                || (entityType.equals(EntityType.ASSET) && msg.getEvent() == ComponentLifecycleEvent.UPDATED)
                || entityType.equals(EntityType.ASSET_PROFILE)
                || entityType.equals(EntityType.API_USAGE_STATE)
                || (entityType.equals(EntityType.DEVICE) && msg.getEvent() == ComponentLifecycleEvent.UPDATED)
                || entityType.equals(EntityType.ENTITY_VIEW)
                || entityType.equals(EntityType.EDGE)
                || entityType.equals(EntityType.NOTIFICATION_RULE)) {
            TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> toCoreNfProducer = producerProvider.getTbCoreNotificationsMsgProducer();
            Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
            for (String serviceId : tbCoreServices) {
                TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                ToCoreNotificationMsg toCoreMsg = ToCoreNotificationMsg.newBuilder().setComponentLifecycle(componentLifecycleMsgProto).build();
                toCoreNfProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toCoreMsg), null);
                toCoreNfs.incrementAndGet();
            }
            // No need to push notifications twice
            tbRuleEngineServices.removeAll(tbCoreServices);
        }
        for (String serviceId : tbRuleEngineServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, serviceId);
            ToRuleEngineNotificationMsg toRuleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().setComponentLifecycle(componentLifecycleMsgProto).build();
            toRuleEngineProducer.send(tpi, new TbProtoQueueMsg<>(msg.getEntityId().getId(), toRuleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${cluster.stats.print_interval_ms}")
    public void printStats() {
        if (statsEnabled) {
            int toCoreMsgCnt = toCoreMsgs.getAndSet(0);
            int toCoreNfsCnt = toCoreNfs.getAndSet(0);
            int toRuleEngineMsgsCnt = toRuleEngineMsgs.getAndSet(0);
            int toRuleEngineNfsCnt = toRuleEngineNfs.getAndSet(0);
            int toTransportNfsCnt = toTransportNfs.getAndSet(0);
            if (toCoreMsgCnt > 0 || toCoreNfsCnt > 0 || toRuleEngineMsgsCnt > 0 || toRuleEngineNfsCnt > 0 || toTransportNfsCnt > 0) {
                log.info("To TbCore: [{}] messages [{}] notifications; To TbRuleEngine: [{}] messages [{}] notifications; To Transport: [{}] notifications",
                        toCoreMsgCnt, toCoreNfsCnt, toRuleEngineMsgsCnt, toRuleEngineNfsCnt, toTransportNfsCnt);
            }
        }
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `added`：`added` 参数。
     * - `updated`：`updated` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void sendDeviceStateServiceEvent(TenantId tenantId, DeviceId deviceId, boolean added, boolean updated, boolean deleted) {
        TransportProtos.DeviceStateServiceMsgProto.Builder builder = TransportProtos.DeviceStateServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setDeviceIdMSB(deviceId.getId().getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getId().getLeastSignificantBits());
        builder.setAdded(added);
        builder.setUpdated(updated);
        builder.setDeleted(deleted);
        TransportProtos.DeviceStateServiceMsgProto msg = builder.build();
        pushMsgToCore(tenantId, deviceId, TransportProtos.ToCoreMsg.newBuilder().setDeviceStateServiceMsg(msg).build(), null);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `old`：`old` 参数。
     * 返回：无。
     */
    @Override
    public void onDeviceUpdated(Device device, Device old) {
        var created = old == null;
        broadcastEntityChangeToTransport(device.getTenantId(), device.getId(), device, null);
        if (old != null) {
            boolean deviceNameChanged = !device.getName().equals(old.getName());
            if (deviceNameChanged) {
                gatewayNotificationsService.onDeviceUpdated(device, old);
            }
            if (deviceNameChanged || !device.getType().equals(old.getType())) {
                pushMsgToCore(new DeviceNameOrTypeUpdateMsg(device.getTenantId(), device.getId(), device.getName(), device.getType()), null);
            }
        }
        broadcastEntityStateChangeEvent(device.getTenantId(), device.getId(), created ? ComponentLifecycleEvent.CREATED : ComponentLifecycleEvent.UPDATED);
        sendDeviceStateServiceEvent(device.getTenantId(), device.getId(), created, !created, false);
        otaPackageStateService.update(device, old);
    }

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `entityId`：实体IDID。
     * - `body`：`body` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    @Override
    public void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action, EdgeId originatorEdgeId) {
        if (!edgesEnabled) {
            return;
        }
        if (type == null) {
            if (entityId != null) {
                type = EdgeUtils.getEdgeEventTypeByEntityType(entityId.getEntityType());
            } else {
                log.trace("[{}] entity id and type are null. Ignoring this notification", tenantId);
                return;
            }
            if (type == null) {
                log.trace("[{}] edge event type is null. Ignoring this notification [{}]", tenantId, entityId);
                return;
            }
        }
        TransportProtos.EdgeNotificationMsgProto.Builder builder = TransportProtos.EdgeNotificationMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setType(type.name());
        builder.setAction(action.name());
        if (entityId != null) {
            builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
            builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
            builder.setEntityType(entityId.getEntityType().name());
        }
        if (edgeId != null) {
            builder.setEdgeIdMSB(edgeId.getId().getMostSignificantBits());
            builder.setEdgeIdLSB(edgeId.getId().getLeastSignificantBits());
        }
        if (body != null) {
            builder.setBody(body);
        }
        if (originatorEdgeId != null) {
            builder.setOriginatorEdgeIdMSB(originatorEdgeId.getId().getMostSignificantBits());
            builder.setOriginatorEdgeIdLSB(originatorEdgeId.getId().getLeastSignificantBits());
        }
        TransportProtos.EdgeNotificationMsgProto msg = builder.build();
        log.trace("[{}] sending notification to edge service {}", tenantId.getId(), msg);
        pushMsgToCore(tenantId, entityId != null ? entityId : tenantId, TransportProtos.ToCoreMsg.newBuilder().setEdgeNotificationMsg(msg).build(), null);

        if (entityId != null && EntityType.DEVICE.equals(entityId.getEntityType())) {
            pushDeviceUpdateMessage(tenantId, edgeId, entityId, action);
        }
    }

    /**
     * 功能：发送或提交设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `entityId`：实体IDID。
     * - `action`：`action` 参数。
     * 返回：无。
     */
    private void pushDeviceUpdateMessage(TenantId tenantId, EdgeId edgeId, EntityId entityId, EdgeEventActionType action) {
        log.trace("{} Going to send edge update notification for device actor, device id {}, edge id {}", tenantId, entityId, edgeId);
        switch (action) {
            case ASSIGNED_TO_EDGE:
                pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), edgeId), null);
                break;
            case UNASSIGNED_FROM_EDGE:
                EdgeId relatedEdgeId = findRelatedEdgeIdIfAny(tenantId, entityId);
                pushMsgToCore(new DeviceEdgeUpdateMsg(tenantId, new DeviceId(entityId.getId()), relatedEdgeId), null);
                break;
        }
    }

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：处理结果。
     */
    private EdgeId findRelatedEdgeIdIfAny(TenantId tenantId, EntityId entityId) {
        PageData<EdgeId> pageData = edgeService.findRelatedEdgeIdsByEntityId(tenantId, entityId, new PageLink(1));
        return Optional.ofNullable(pageData).filter(pd -> pd.getTotalElements() > 0).map(pd -> pd.getData().get(0)).orElse(null);
    }

    /**
     * 功能：处理`on Queues Update`。
     * 参数：
     * - `queues`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    public void onQueuesUpdate(List<Queue> queues) {
        List<QueueUpdateMsg> queueUpdateMsgs = queues.stream()
                .map(queue -> QueueUpdateMsg.newBuilder()
                        .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                        .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                        .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                        .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                        .setQueueName(queue.getName())
                        .setQueueTopic(queue.getTopic())
                        .setPartitions(queue.getPartitions())
                        .build())
                .collect(Collectors.toList());

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().addAllQueueUpdateMsgs(queueUpdateMsgs).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().addAllQueueUpdateMsgs(queueUpdateMsgs).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().addAllQueueUpdateMsgs(queueUpdateMsgs).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    /**
     * 功能：处理`on Queues Delete`。
     * 参数：
     * - `queues`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    public void onQueuesDelete(List<Queue> queues) {
        List<QueueDeleteMsg> queueDeleteMsgs = queues.stream()
                .map(queue -> QueueDeleteMsg.newBuilder()
                        .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                        .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                        .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                        .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                        .setQueueName(queue.getName())
                        .build())
                .collect(Collectors.toList());

        ToRuleEngineNotificationMsg ruleEngineMsg = ToRuleEngineNotificationMsg.newBuilder().addAllQueueDeleteMsgs(queueDeleteMsgs).build();
        ToCoreNotificationMsg coreMsg = ToCoreNotificationMsg.newBuilder().addAllQueueDeleteMsgs(queueDeleteMsgs).build();
        ToTransportMsg transportMsg = ToTransportMsg.newBuilder().addAllQueueDeleteMsgs(queueDeleteMsgs).build();
        doSendQueueNotifications(ruleEngineMsg, coreMsg, transportMsg);
    }

    /**
     * 功能：执行 `doSendQueueNotifications` 对应的处理。
     * 参数：
     * - `ruleEngineMsg`：待处理消息。
     * - `coreMsg`：待处理消息。
     * - `transportMsg`：待处理消息。
     * 返回：无。
     */
    private void doSendQueueNotifications(ToRuleEngineNotificationMsg ruleEngineMsg, ToCoreNotificationMsg coreMsg, ToTransportMsg transportMsg) {
        Set<String> tbRuleEngineServices = partitionService.getAllServiceIds(ServiceType.TB_RULE_ENGINE);
        Set<String> tbCoreServices = partitionService.getAllServiceIds(ServiceType.TB_CORE);
        Set<String> tbTransportServices = partitionService.getAllServiceIds(ServiceType.TB_TRANSPORT);
        // No need to push notifications twice
        tbTransportServices.removeAll(tbCoreServices);
        tbCoreServices.removeAll(tbRuleEngineServices);

        for (String ruleEngineServiceId : tbRuleEngineServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_RULE_ENGINE, ruleEngineServiceId);
            producerProvider.getRuleEngineNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), ruleEngineMsg), null);
            toRuleEngineNfs.incrementAndGet();
        }
        for (String coreServiceId : tbCoreServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, coreServiceId);
            producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), coreMsg), null);
            toCoreNfs.incrementAndGet();
        }
        for (String transportServiceId : tbTransportServices) {
            TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_TRANSPORT, transportServiceId);
            producerProvider.getTransportNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), transportMsg), null);
            toTransportNfs.incrementAndGet();
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbClusterService` 在 ThingsBoard Application 模块 中承担队列服务类型职责，核心目的是封装 ThingsBoard 队列生产、消费、确认和分区处理。
 * 2. 核心流程：接收队列记录后反序列化消息，路由到 Actor 或业务服务并提交确认。
 * 3. 关键依赖：主要依赖或协作对象包括TbQueue、Actor、Rule Engine、Transport、Tenant Profile 和统计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
