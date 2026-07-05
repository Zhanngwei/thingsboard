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
package org.thingsboard.server.transport.lwm2m.server.uplink;

import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.SendRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collection;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mUplinkMsgHandler` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface LwM2mUplinkMsgHandler {

    /**
     * 功能：处理`on Registered`。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `previousObsersations`：数据列表。
     * 返回：无。
     */
    void onRegistered(Registration registration, Collection<Observation> previousObsersations);

    /**
     * 功能：执行 `updatedReg` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    void updatedReg(Registration registration);

    /**
     * 功能：执行 `unReg` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `observations`：数据列表。
     * 返回：无。
     */
    void unReg(Registration registration, Collection<Observation> observations);

    /**
     * 功能：处理`on Sleeping Dev`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    void onSleepingDev(Registration registration);

    /**
     * 功能：处理响应。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `path`：文件或资源路径。
     * - `response`：响应对象。
     * 返回：无。
     */
    void onUpdateValueAfterReadResponse(Registration registration, String path, ReadResponse response);

    /**
     * 功能：处理响应。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    void onUpdateValueAfterReadCompositeResponse(Registration registration, ReadCompositeResponse response);

    /**
     * 功能：处理请求。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `sendRequest`：请求对象。
     * 返回：无。
     */
    void onUpdateValueWithSendRequest(Registration registration, SendRequest sendRequest);

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile);

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `device`：设备信息或设备标识。
     * - `deviceProfileOpt`：设备信息或设备标识。
     * 返回：无。
     */
    void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt);

    /**
     * 功能：处理设备。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    void onDeviceDelete(DeviceId deviceId);

    /**
     * 功能：处理`on Resource Update`。
     * 参数：
     * - `resourceUpdateMsgOpt`：待处理消息。
     * 返回：无。
     */
    void onResourceUpdate(TransportProtos.ResourceUpdateMsg resourceUpdateMsgOpt);

    /**
     * 功能：处理`on Resource Delete`。
     * 参数：
     * - `resourceDeleteMsgOpt`：待处理消息。
     * 返回：无。
     */
    void onResourceDelete(TransportProtos.ResourceDeleteMsg resourceDeleteMsgOpt);

    /**
     * 功能：处理`on Awake Dev`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    void onAwakeDev(Registration registration);

    /**
     * 功能：处理响应。
     * 参数：
     * - `client`：客户端对象。
     * - `path`：文件或资源路径。
     * - `request`：请求对象。
     * - `code`：`code` 参数。
     * 返回：无。
     */
    void onWriteResponseOk(LwM2mClient client, String path, WriteRequest request, int code);

    /**
     * 功能：处理响应。
     * 参数：
     * - `client`：客户端对象。
     * - `path`：文件或资源路径。
     * - `request`：请求对象。
     * 返回：无。
     */
    void onCreateResponseOk(LwM2mClient client, String path, CreateRequest request);

    /**
     * 功能：处理响应。
     * 参数：
     * - `client`：客户端对象。
     * - `request`：请求对象。
     * - `code`：`code` 参数。
     * 返回：无。
     */
    void onWriteCompositeResponseOk(LwM2mClient client, WriteCompositeRequest request, int code);

    /**
     * 功能：处理凭据。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `updateCredentials`：`updateCredentials` 参数。
     * 返回：无。
     */
    void onToTransportUpdateCredentials(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToTransportUpdateCredentialsProto updateCredentials);

    /**
     * 功能：初始化或启动`Attributes`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `logFailedUpdateOfNonChangedValue`：值。
     * 返回：无。
     */
    void initAttributes(LwM2mClient lwM2MClient, boolean logFailedUpdateOfNonChangedValue);

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    LwM2MTransportServerConfig getConfig();

    /**
     * 功能：获取转换器。
     * 参数：无。
     * 返回：处理结果。
     */
    LwM2mValueConverter getConverter();

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mUplinkMsgHandler` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
