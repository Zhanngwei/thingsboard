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
package org.thingsboard.server.cluster;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.edge.FromEdgeSyncResponse;
import org.thingsboard.server.common.msg.edge.ToEdgeSyncRequest;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueClusterService;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`TbClusterService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface TbClusterService extends TbQueueClusterService {

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msgKey`：待处理消息。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToCore(TopicPartitionInfo tpi, UUID msgKey, ToCoreMsg msg, TbQueueCallback callback);

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback);

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback);

    /**
     * 功能：执行 `broadcastToCore` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void broadcastToCore(TransportProtos.ToCoreNotificationMsg msg);

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToVersionControl(TenantId tenantId, ToVersionControlServiceMsg msg, TbQueueCallback callback);

    /**
     * 功能：发送或提交通知。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushNotificationToCore(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tpi`：`tpi` 参数。
     * - `msgId`：消息ID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback);

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg msg, TbQueueCallback callback);

    /**
     * 功能：发送或提交规则引擎。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushNotificationToRuleEngine(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    /**
     * 功能：发送或提交传输层。
     * 参数：
     * - `targetServiceId`：服务ID。
     * - `response`：响应对象。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void pushNotificationToTransport(String targetServiceId, ToTransportMsg response, TbQueueCallback callback);

    /**
     * 功能：执行 `broadcastEntityStateChangeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceProfileChange(DeviceProfile deviceProfile, TbQueueCallback callback);

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceProfileDelete(DeviceProfile deviceProfile, TbQueueCallback callback);

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback);

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTenantProfileDelete(TenantProfile tenantProfile, TbQueueCallback callback);

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTenantChange(Tenant tenant, TbQueueCallback callback);

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenant`：租户信息或租户标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onTenantDelete(Tenant tenant, TbQueueCallback callback);

    /**
     * 功能：处理状态。
     * 参数：
     * - `apiUsageState`：`apiUsageState` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback);

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `old`：`old` 参数。
     * 返回：无。
     */
    void onDeviceUpdated(Device device, Device old);

    /**
     * 功能：处理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onDeviceDeleted(TenantId tenantId, Device device, TbQueueCallback callback);

    /**
     * 功能：处理租户。
     * 参数：
     * - `oldTenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：无。
     */
    void onDeviceAssignedToTenant(TenantId oldTenantId, Device device);

    /**
     * 功能：处理`on Resource Change`。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onResourceChange(TbResource resource, TbQueueCallback callback);

    /**
     * 功能：处理`on Resource Deleted`。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void onResourceDeleted(TbResource resource, TbQueueCallback callback);

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：无。
     */
    void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId);

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `toEdgeSyncRequest`：请求对象。
     * 返回：无。
     */
    void pushEdgeSyncRequestToCore(ToEdgeSyncRequest toEdgeSyncRequest);

    /**
     * 功能：发送或提交边缘节点。
     * 参数：
     * - `fromEdgeSyncResponse`：响应对象。
     * 返回：无。
     */
    void pushEdgeSyncResponseToCore(FromEdgeSyncResponse fromEdgeSyncResponse);

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
    void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action, EdgeId sourceEdgeId);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbClusterService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
