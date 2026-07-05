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
package org.thingsboard.server.transport.lwm2m.server.client;

import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mClientContext` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface LwM2mClientContext {

    /**
     * 功能：获取客户端。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    LwM2mClient getClientByEndpoint(String endpoint);

    /**
     * 功能：获取会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo);

    /**
     * 功能：执行 `register` 对应的处理。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：可能存在的结果。
     */
    Optional<TransportProtos.SessionInfoProto> register(LwM2mClient lwM2MClient, Registration registration) throws LwM2MClientStateException;

    /**
     * 功能：更新`Registration`。
     * 参数：
     * - `client`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    /**
     * 功能：执行 `unregister` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException;

    /**
     * 功能：获取`Lw M2m Clients`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    Collection<LwM2mClient> getLwM2mClients();

    //TODO: replace UUID with DeviceProfileId
    /**
     * 功能：获取配置。
     * 参数：
     * - `profileUuId`：配置ID。
     * 返回：处理结果。
     */
    Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileUuId);

    /**
     * 功能：获取配置。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration);

    /**
     * 功能：执行 `profileUpdate` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile);

    /**
     * 功能：获取客户端。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：匹配的数据集合。
     */
    Set<String> getSupportedIdVerInClient(LwM2mClient registration);

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    LwM2mClient getClientByDeviceId(UUID deviceId);

    /**
     * 功能：获取配置。
     * 参数：
     * - `lwM2mClient`：客户端对象。
     * - `keyName`：名称。
     * 返回：文本结果。
     */
    String getObjectIdByKeyNameFromProfile(LwM2mClient lwM2mClient, String keyName);

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `credentials`：`credentials` 参数。
     * 返回：无。
     */
    void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials);

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * 返回：无。
     */
    void update(LwM2mClient lwM2MClient);

    /**
     * 功能：发送或提交`Msgs After Sleeping`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * 返回：无。
     */
    void sendMsgsAfterSleeping(LwM2mClient lwM2MClient);

    /**
     * 功能：处理`on Uplink`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    void onUplink(LwM2mClient client);

    /**
     * 功能：获取请求。
     * 参数：
     * - `client`：客户端对象。
     * 返回：数值结果。
     */
    Long getRequestTimeout(LwM2mClient client);

    /**
     * 功能：执行 `asleep` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean asleep(LwM2mClient client);

    /**
     * 功能：执行 `awake` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean awake(LwM2mClient client);

    /**
     * 功能：判断`Downlink Allowed`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean isDownlinkAllowed(LwM2mClient client);

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mClientContext` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
