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
package org.thingsboard.server.service.edge.rpc.processor.device;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.rpc.RpcError;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponseActorMsg;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.edge.rpc.constructor.device.DeviceMsgConstructor;

import java.util.UUID;

@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DeviceEdgeProcessor` 是ThingsBoard Application 模块中的Edge 同步服务类型，用于处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 生命周期：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Factory / Strategy / Template Method。
 */
public abstract class DeviceEdgeProcessor extends BaseDeviceProcessor implements DeviceProcessor {

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceMsgFromEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processDeviceMsgFromEdge(TenantId tenantId, Edge edge, DeviceUpdateMsg deviceUpdateMsg) {
        log.trace("[{}] executing processDeviceMsgFromEdge [{}] from edge [{}]", tenantId, deviceUpdateMsg, edge.getId());
        DeviceId deviceId = new DeviceId(new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
            switch (deviceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg, edge);
                    return saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.CREDENTIALS_REQUEST, deviceId, null);
                case ENTITY_DELETED_RPC_MESSAGE:
                    Device deviceToDelete = deviceService.findDeviceById(tenantId, deviceId);
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (deviceToDelete != null) {
                        deviceService.unassignDeviceFromEdge(tenantId, deviceId, edge.getId());
                    }
                    // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                    return Futures.immediateFuture(null);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(deviceUpdateMsg.getMsgType());
            }
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (DataValidationException e) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (e.getMessage().contains("limit reached")) {
                log.warn("[{}] Number of allowed devices violated {}", tenantId, deviceUpdateMsg, e);
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                return Futures.immediateFuture(null);
            } else {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceCredentialsMsgFromEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processDeviceCredentialsMsgFromEdge(TenantId tenantId, EdgeId edgeId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        log.debug("[{}] Executing processDeviceCredentialsMsgFromEdge, deviceCredentialsUpdateMsg [{}]", tenantId, deviceCredentialsUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);

            updateDeviceCredentials(tenantId, deviceCredentialsUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return Futures.immediateFuture(null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `saveOrUpdateDevice` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void saveOrUpdateDevice(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg, Edge edge) {
        Pair<Boolean, Boolean> resultPair = super.saveOrUpdateDevice(tenantId, deviceId, deviceUpdateMsg);
        Boolean created = resultPair.getFirst();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), deviceId);
            pushDeviceCreatedEventToRuleEngine(tenantId, edge, deviceId);
            deviceService.assignDeviceToEdge(tenantId, deviceId, edge.getId());
        }
        Boolean deviceNameUpdated = resultPair.getSecond();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceNameUpdated) {
            saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.DEVICE, EdgeEventActionType.UPDATED, deviceId, null);
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `pushDeviceCreatedEventToRuleEngine` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void pushDeviceCreatedEventToRuleEngine(TenantId tenantId, Edge edge, DeviceId deviceId) {
        try {
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            String deviceAsString = JacksonUtil.toString(device);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, device.getCustomerId());
            pushEntityEventToRuleEngine(tenantId, deviceId, device.getCustomerId(), TbMsgType.ENTITY_CREATED, deviceAsString, msgMetaData);
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push device action to rule engine: {}", tenantId, deviceId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceRpcCallFromEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public ListenableFuture<Void> processDeviceRpcCallFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        log.trace("[{}] processDeviceRpcCallFromEdge [{}]", tenantId, deviceRpcCallMsg);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (deviceRpcCallMsg.hasResponseMsg()) {
            return processDeviceRpcResponseFromEdge(tenantId, deviceRpcCallMsg);
        } else if (deviceRpcCallMsg.hasRequestMsg()) {
            return processDeviceRpcRequestFromEdge(tenantId, edge, deviceRpcCallMsg);
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceRpcResponseFromEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> processDeviceRpcResponseFromEdge(TenantId tenantId, DeviceRpcCallMsg deviceRpcCallMsg) {
        SettableFuture<Void> futureToSet = SettableFuture.create();
        UUID requestUuid = new UUID(deviceRpcCallMsg.getRequestUuidMSB(), deviceRpcCallMsg.getRequestUuidLSB());
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));

        FromDeviceRpcResponse response;
        if (!StringUtils.isEmpty(deviceRpcCallMsg.getResponseMsg().getError())) {
            response = new FromDeviceRpcResponse(requestUuid, null, RpcError.valueOf(deviceRpcCallMsg.getResponseMsg().getError()));
        } else {
            response = new FromDeviceRpcResponse(requestUuid, deviceRpcCallMsg.getResponseMsg().getResponse(), null);
        }
        TbQueueCallback callback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                futureToSet.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Can't process push notification to core [{}]", tenantId, deviceRpcCallMsg, t);
                futureToSet.setException(t);
            }
        };
        FromDeviceRpcResponseActorMsg msg =
                new FromDeviceRpcResponseActorMsg(deviceRpcCallMsg.getRequestId(),
                        tenantId,
                        deviceId, response);
        tbClusterService.pushMsgToCore(msg, callback);
        return futureToSet;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `processDeviceRpcRequestFromEdge` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private ListenableFuture<Void> processDeviceRpcRequestFromEdge(TenantId tenantId, Edge edge, DeviceRpcCallMsg deviceRpcCallMsg) {
        DeviceId deviceId = new DeviceId(new UUID(deviceRpcCallMsg.getDeviceIdMSB(), deviceRpcCallMsg.getDeviceIdLSB()));
        try {
            TbMsgMetaData metaData = new TbMsgMetaData();
            String requestId = Integer.toString(deviceRpcCallMsg.getRequestId());
            metaData.putValue("requestId", requestId);
            metaData.putValue("serviceId", deviceRpcCallMsg.getServiceId());
            metaData.putValue("sessionId", deviceRpcCallMsg.getSessionId());
            metaData.putValue(DataConstants.EDGE_ID, edge.getId().toString());
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            if (device != null) {
                metaData.putValue("deviceName", device.getName());
                metaData.putValue("deviceType", device.getType());
                metaData.putValue(DataConstants.DEVICE_ID, deviceId.getId().toString());
            }
            ObjectNode data = JacksonUtil.newObjectNode();
            data.put("method", deviceRpcCallMsg.getRequestMsg().getMethod());
            data.put("params", deviceRpcCallMsg.getRequestMsg().getParams());
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.TO_SERVER_RPC_REQUEST, deviceId, null, metaData,
                    TbMsgDataType.JSON, JacksonUtil.toString(data));
            tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    log.debug("[{}] Successfully send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg);
                }

                @Override
                public void onFailure(Throwable t) {
                    log.debug("[{}] Failed to send TO_SERVER_RPC_REQUEST to rule engine [{}], deviceRpcCallMsg {}",
                            tenantId, device, deviceRpcCallMsg, t);
                }
            });
        } catch (IllegalArgumentException e) {
            log.warn("[{}][{}] Failed to push TO_SERVER_RPC_REQUEST to rule engine. deviceRpcCallMsg {}", tenantId, deviceId, deviceRpcCallMsg, e);
        }

        return Futures.immediateFuture(null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `convertDeviceEventToDownlink` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DownlinkMsg convertDeviceEventToDownlink(EdgeEvent edgeEvent, EdgeId edgeId, EdgeVersion edgeVersion) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
            case ASSIGNED_TO_EDGE:
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Device device = deviceService.findDeviceById(edgeEvent.getTenantId(), deviceId);
                if (device != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    DeviceUpdateMsg deviceUpdateMsg = ((DeviceMsgConstructor)
                            deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                            .constructDeviceUpdatedMsg(msgType, device);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceUpdateMsg(deviceUpdateMsg);
                    if (UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE.equals(msgType)) {
                        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(edgeEvent.getTenantId(), device.getDeviceProfileId());
                        deviceProfile = checkIfDeviceProfileDefaultFieldsAssignedToEdge(edgeEvent.getTenantId(), edgeId, deviceProfile, edgeVersion);
                        builder.addDeviceProfileUpdateMsg(((DeviceMsgConstructor)
                                deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                                .constructDeviceProfileUpdatedMsg(msgType, deviceProfile));
                    }
                    downlinkMsg = builder.build();
                }
                break;
            case DELETED:
            case UNASSIGNED_FROM_EDGE:
                DeviceUpdateMsg deviceUpdateMsg = ((DeviceMsgConstructor)
                        deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDeviceDeleteMsg(deviceId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceUpdateMsg(deviceUpdateMsg)
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(edgeEvent.getTenantId(), deviceId);
                if (deviceCredentials != null) {
                    DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg = ((DeviceMsgConstructor)
                            deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructDeviceCredentialsUpdatedMsg(deviceCredentials);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addDeviceCredentialsUpdateMsg(deviceCredentialsUpdateMsg)
                            .build();
                }
                break;
            case RPC_CALL:
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addDeviceRpcCallMsg(((DeviceMsgConstructor)
                                deviceMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                                .constructDeviceRpcCallMsg(edgeEvent.getEntityId(), edgeEvent.getBody()))
                        .build();
            case CREDENTIALS_REQUEST:
                return convertCredentialsRequestEventToDownlink(edgeEvent);
        }
        return downlinkMsg;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `convertCredentialsRequestEventToDownlink` 对应的Edge 同步服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 服务和队列消费流程触发，随 Edge 连接和同步任务运行时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private DownlinkMsg convertCredentialsRequestEventToDownlink(EdgeEvent edgeEvent) {
        DeviceId deviceId = new DeviceId(edgeEvent.getEntityId());
        DeviceCredentialsRequestMsg deviceCredentialsRequestMsg = DeviceCredentialsRequestMsg.newBuilder()
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .build();
        DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addDeviceCredentialsRequestMsg(deviceCredentialsRequestMsg);
        return builder.build();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceEdgeProcessor` 在 ThingsBoard Application 模块 中承担Edge 同步服务类型职责，核心目的是处理云端与边缘端之间的实体、事件和 RPC 数据同步。
 * 2. 核心流程：读取实体或事件状态，构造 Edge 消息并发送到边缘同步通道。
 * 3. 关键依赖：主要依赖或协作对象包括EdgeEvent、Edge RPC、DAO、队列、protobuf 消息和版本兼容构造器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
