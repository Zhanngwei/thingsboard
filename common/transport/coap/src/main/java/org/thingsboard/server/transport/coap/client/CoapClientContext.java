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
package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.transport.coap.CoapSessionMsgType;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `CoapClientContext` 是 ThingsBoard Common Transport 中定义 CoAP 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface CoapClientContext {

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：判断结果。
     */
    boolean registerAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    /**
     * 功能：保存或创建RPC。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：判断结果。
     */
    boolean registerRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    /**
     * 功能：获取通知。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：处理结果。
     */
    AtomicInteger getNotificationCounterByToken(String token);

    /**
     * 功能：获取客户端。
     * 参数：
     * - `type`：类型。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    TbCoapClientState getOrCreateClient(CoapSessionMsgType type, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) throws AdaptorException;

    /**
     * 功能：获取会话。
     * 参数：
     * - `clientState`：客户端对象。
     * 返回：处理结果。
     */
    TransportProtos.SessionInfoProto getNewSyncSession(TbCoapClientState clientState);

    /**
     * 功能：执行 `deregisterAttributeObservation` 对应的处理。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    void deregisterAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    /**
     * 功能：执行 `deregisterRpcObservation` 对应的处理。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    void deregisterRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange);

    /**
     * 功能：上报`Activity`。
     * 参数：无。
     * 返回：无。
     */
    void reportActivity();

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `token`：`token` 参数。
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    void registerObserveRelation(String token, ObserveRelation relation);

    /**
     * 功能：执行 `deregisterObserveRelation` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：无。
     */
    void deregisterObserveRelation(String token);

    /**
     * 功能：执行 `awake` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean awake(TbCoapClientState client);
}
