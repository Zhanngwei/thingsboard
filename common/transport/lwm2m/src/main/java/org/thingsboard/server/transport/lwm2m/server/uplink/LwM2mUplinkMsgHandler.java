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
 * 1. `LwM2mUplinkMsgHandler` 是 ThingsBoard Common Transport 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
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
