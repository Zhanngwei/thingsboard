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
 * 1. `DefaultRuleEngineDeviceStateManager` 是 ThingsBoard Application 中负责设备的协调管理组件。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `RuleEngineDeviceStateManager`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
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
     * 1. `ConnectivityEventInfo` 是 ThingsBoard Application 中承载事件信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
