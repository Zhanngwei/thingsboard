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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructor;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
@Slf4j
public abstract class DeviceProfileEdgeProcessor extends BaseDeviceProfileProcessor implements DeviceProfileProcessor {

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> processDeviceProfileMsgFromEdge(TenantId tenantId, Edge edge, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        log.trace("[{}] executing processDeviceProfileMsgFromEdge [{}] from edge [{}]", tenantId, deviceProfileUpdateMsg, edge.getId());
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(deviceProfileUpdateMsg.getIdMSB(), deviceProfileUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (deviceProfileUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg, edge);
                    return Futures.immediateFuture(null);
                case ENTITY_DELETED_RPC_MESSAGE:
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(deviceProfileUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process DeviceProfileUpdateMsg from Edge [{}]", tenantId, deviceProfileUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * - `edge`：`edge` 参数。
     * 返回：无。
     */
    private void saveOrUpdateDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDeviceProfile(tenantId, deviceProfileId, deviceProfileUpdateMsg);
        Boolean created = resultPair.getFirst();
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), deviceProfileId);
            pushDeviceProfileCreatedEventToRuleEngine(tenantId, edge, deviceProfileId);
        }
        Boolean deviceProfileNameUpdated = resultPair.getSecond();
        if (deviceProfileNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE_PROFILE, EdgeEventActionType.UPDATED, deviceProfileId, null);
        }
    }

    /**
     * 功能：发送或提交设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edge`：`edge` 参数。
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    private void pushDeviceProfileCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DeviceProfileId deviceProfileId) {
        try {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);
            String deviceProfileAsString = JacksonUtil.toString(deviceProfile);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, deviceProfileId, null, TbMsgType.ENTITY_CREATED, deviceProfileAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device profile action to rule engine: {}", tenantId, deviceProfileId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    /**
     * 功能：转换设备配置。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * - `edgeId`：边缘节点ID。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public DownlinkMsg convertDeviceProfileEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), deviceProfileId);
                if (deviceProfile != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    deviceProfile = checkIfDeviceProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, deviceProfile, edgeVersion);
                    DeviceProfileUpdateMsg deviceProfileUpdateMsg = ((DeviceMsgConstructor)
                            deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDeviceProfileUpdatedMsg(msgType, deviceProfile);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                            .build();
                }
                break;
            case DELETED:
                DeviceProfileUpdateMsg deviceProfileUpdateMsg = ((DeviceMsgConstructor)
                        deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDeviceProfileDeleteMsg(deviceProfileId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceProfileUpdateMsg(deviceProfileUpdateMsg)
                        .build();
                break;
        }
        return downlinkMsg;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
