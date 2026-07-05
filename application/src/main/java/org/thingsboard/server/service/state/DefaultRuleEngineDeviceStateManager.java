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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineDeviceStateManager;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.SimpleTbQueueCallback;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`DefaultRuleEngineDeviceStateManager` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@TbRuleEngineComponent
public class DefaultRuleEngineDeviceStateManager implements RuleEngineDeviceStateManager {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final PartitionService partitionService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final Optional<DeviceStateService> deviceStateService;
    private final TbClusterService clusterService;

    /**
     * 功能：创建 `DefaultRuleEngineDeviceStateManager` 实例，并初始化必要字段。
     * 参数：
     * - `serviceInfoProvider`：服务对象。
     * - `partitionService`：服务对象。
     * - `deviceStateServiceOptional`：设备信息或设备标识。
     * - `clusterService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public DefaultRuleEngineDeviceStateManager(
            TbServiceInfoProvider serviceInfoProvider, PartitionService partitionService,
            Optional<DeviceStateService> deviceStateServiceOptional, TbClusterService clusterService
    ) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.partitionService = partitionService;
        this.deviceStateService = deviceStateServiceOptional;
        this.clusterService = clusterService;
    }

    /**
     * 中文说明：
     * 1. 类目的：`ConnectivityEventInfo` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    @Getter
    private abstract static class ConnectivityEventInfo {

        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final DeviceId deviceId;
        /**
         * 事件，表示当前对象的对应属性。
         */
        private final long eventTime;

        /**
         * 功能：创建 `DefaultRuleEngineDeviceStateManager` 实例，并初始化必要字段。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `deviceId`：设备IDID。
         * - `eventTime`：`eventTime` 参数。
         * 返回：新创建的对象实例。
         */
        private ConnectivityEventInfo(TenantId tenantId, DeviceId deviceId, long eventTime) {
            this.tenantId = tenantId;
            this.deviceId = deviceId;
            this.eventTime = eventTime;
        }

        /**
         * 功能：执行 `forwardToLocalService` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        abstract void forwardToLocalService();

        /**
         * 功能：执行 `toQueueMsg` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        abstract TransportProtos.ToCoreMsg toQueueMsg();

    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `connectTime`：`connectTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceConnect(TenantId tenantId, DeviceId deviceId, long connectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, connectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceConnect(tenantId, deviceId, connectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
                var deviceConnectMsg = TransportProtos.DeviceConnectProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastConnectTime(connectTime)
                        .build();
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceConnectMsg(deviceConnectMsg)
                        .build();
            }
        }, callback);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `activityTime`：`activityTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceActivity(TenantId tenantId, DeviceId deviceId, long activityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, activityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceActivity(tenantId, deviceId, activityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
                var deviceActivityMsg = TransportProtos.DeviceActivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastActivityTime(activityTime)
                        .build();
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceActivityMsg(deviceActivityMsg)
                        .build();
            }
        }, callback);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `disconnectTime`：`disconnectTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceDisconnect(TenantId tenantId, DeviceId deviceId, long disconnectTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, disconnectTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceDisconnect(tenantId, deviceId, disconnectTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
                var deviceDisconnectMsg = TransportProtos.DeviceDisconnectProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastDisconnectTime(disconnectTime)
                        .build();
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceDisconnectMsg(deviceDisconnectMsg)
                        .build();
            }
        }, callback);
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `inactivityTime`：`inactivityTime` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    @Override
    public void onDeviceInactivity(TenantId tenantId, DeviceId deviceId, long inactivityTime, TbCallback callback) {
        routeEvent(new ConnectivityEventInfo(tenantId, deviceId, inactivityTime) {
            @Override
            void forwardToLocalService() {
                deviceStateService.ifPresent(service -> service.onDeviceInactivity(tenantId, deviceId, inactivityTime));
            }

            @Override
            TransportProtos.ToCoreMsg toQueueMsg() {
                var deviceInactivityMsg = TransportProtos.DeviceInactivityProto.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .setLastInactivityTime(inactivityTime)
                        .build();
                return TransportProtos.ToCoreMsg.newBuilder()
                        .setDeviceInactivityMsg(deviceInactivityMsg)
                        .build();
            }
        }, callback);
    }

    /**
     * 功能：执行 `routeEvent` 对应的处理。
     * 参数：
     * - `eventInfo`：`eventInfo` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void routeEvent(ConnectivityEventInfo eventInfo, TbCallback callback) {
        var tenantId = eventInfo.getTenantId();
        var deviceId = eventInfo.getDeviceId();
        long eventTime = eventInfo.getEventTime();

        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, deviceId);
        if (serviceInfoProvider.isService(ServiceType.TB_CORE) && tpi.isMyPartition() && deviceStateService.isPresent()) {
            log.debug("[{}][{}] Forwarding device connectivity event to local service. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            try {
                eventInfo.forwardToLocalService();
            } catch (Exception e) {
                log.error("[{}][{}] Failed to process device connectivity event. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime, e);
                callback.onFailure(e);
                return;
            }
            callback.onSuccess();
        } else {
            TransportProtos.ToCoreMsg msg = eventInfo.toQueueMsg();
            log.debug("[{}][{}] Sending device connectivity message to core. Event time: [{}].", tenantId.getId(), deviceId.getId(), eventTime);
            clusterService.pushMsgToCore(tpi, UUID.randomUUID(), msg, new SimpleTbQueueCallback(__ -> callback.onSuccess(), callback::onFailure));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultRuleEngineDeviceStateManager` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
