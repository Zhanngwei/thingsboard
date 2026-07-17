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
package org.thingsboard.server.common.transport;

import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.service.SessionMetaData;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ClaimDeviceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetAttributeRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOtaPackageRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOtaPackageResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.LwM2MResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToAttributeUpdatesMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscribeToRPCMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToDeviceRpcResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToServerRpcRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateBasicMqttCredRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 04.10.18.
 */
/**
 * 中文说明：
 * 1. `TransportService` 是 ThingsBoard Common Transport 中定义传输层能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TransportService {

    /**
     * 功能：获取实体。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    GetEntityProfileResponseMsg getEntityProfile(GetEntityProfileRequestMsg msg);

    /**
     * 功能：获取队列。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    List<TransportProtos.GetQueueRoutingInfoResponseMsg> getQueueRoutingInfo(TransportProtos.GetAllQueueRoutingInfoRequestMsg msg);

    /**
     * 功能：获取`Resource`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    GetResourceResponseMsg getResource(GetResourceRequestMsg msg);

    /**
     * 功能：获取`Snmp Devices Ids`。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：处理结果。
     */
    GetSnmpDevicesResponseMsg getSnmpDevicesIds(GetSnmpDevicesRequestMsg requestMsg);

    /**
     * 功能：获取设备。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：处理结果。
     */
    GetDeviceResponseMsg getDevice(GetDeviceRequestMsg requestMsg);

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：处理结果。
     */
    GetDeviceCredentialsResponseMsg getDeviceCredentials(GetDeviceCredentialsRequestMsg requestMsg);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `transportType`：类型。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(DeviceTransportType transportType, ValidateDeviceTokenRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `transportType`：类型。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(DeviceTransportType transportType, ValidateBasicMqttCredRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `transportType`：类型。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(DeviceTransportType transportType, ValidateDeviceX509CertRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `transportType`：类型。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(DeviceTransportType transportType, ValidateOrCreateDeviceX509CertRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(ValidateDeviceLwM2MCredentialsRequestMsg msg,
                 TransportServiceCallback<ValidateDeviceCredentialsResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(TenantId tenantId, GetOrCreateDeviceFromGatewayRequestMsg msg,
                 TransportServiceCallback<GetOrCreateDeviceFromGatewayResponse> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(ProvisionDeviceRequestMsg msg,
                 TransportServiceCallback<ProvisionDeviceResponseMsg> callback);

    /**
     * 功能：处理配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    void onProfileUpdate(DeviceProfile deviceProfile);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(LwM2MRequestMsg msg,
                 TransportServiceCallback<LwM2MResponseMsg> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, SessionEventMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `md`：`md` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, TbMsgMetaData md, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `md`：`md` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, TbMsgMetaData md, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `rpcStatus`：`rpcStatus` 参数。
     * - `reportActivity`：`reportActivity` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, ToDeviceRpcRequestMsg msg, RpcStatus rpcStatus, boolean reportActivity, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `rpcStatus`：`rpcStatus` 参数。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, ToDeviceRpcRequestMsg msg, RpcStatus rpcStatus, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, SubscriptionInfoProto msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfo, ClaimDeviceMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(TransportToDeviceActorMsg msg, TransportServiceCallback<Void> callback);

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `sessionInfoProto`：会话对象。
     * - `msg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void process(SessionInfoProto sessionInfoProto, GetOtaPackageRequestMsg msg, TransportServiceCallback<GetOtaPackageResponseMsg> callback);

    /**
     * 功能：保存或创建会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `listener`：数据列表。
     * 返回：处理结果。
     */
    SessionMetaData registerAsyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener);

    /**
     * 功能：保存或创建会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `listener`：数据列表。
     * - `timeout`：`timeout` 参数。
     * 返回：处理结果。
     */
    SessionMetaData registerSyncSession(SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout);

    /**
     * 功能：执行 `recordActivity` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    void recordActivity(SessionInfoProto sessionInfo);

    /**
     * 功能：执行 `lifecycleEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `eventType`：类型。
     * - `success`：`success` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void lifecycleEvent(TenantId tenantId, DeviceId deviceId, ComponentLifecycleEvent eventType, boolean success, Throwable error);

    /**
     * 功能：执行 `errorEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `method`：`method` 参数。
     * - `error`：错误信息。
     * 返回：无。
     */
    void errorEvent(TenantId tenantId, DeviceId deviceId, String method, Throwable error);

    /**
     * 功能：执行 `deregisterSession` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    void deregisterSession(SessionInfoProto sessionInfo);

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    void log(SessionInfoProto sessionInfo, String msg);

    /**
     * 功能：通知`About Uplink`。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `build`：`build` 参数。
     * - `empty`：`empty` 参数。
     * 返回：无。
     */
    void notifyAboutUplink(SessionInfoProto sessionInfo, TransportProtos.UplinkNotificationMsg build, TransportServiceCallback<Void> empty);

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：处理结果。
     */
    ExecutorService getCallbackExecutor();

    /**
     * 功能：判断会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：判断结果。
     */
    boolean hasSession(SessionInfoProto sessionInfo);

    /**
     * 功能：保存或创建`Gauge Stats`。
     * 参数：
     * - `openConnections`：`openConnections` 参数。
     * - `connectionsCounter`：`connectionsCounter` 参数。
     * 返回：无。
     */
    void createGaugeStats(String openConnections, AtomicInteger connectionsCounter);
}
