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
 * 1. `LwM2mClientContext` 是 ThingsBoard Common Transport 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
