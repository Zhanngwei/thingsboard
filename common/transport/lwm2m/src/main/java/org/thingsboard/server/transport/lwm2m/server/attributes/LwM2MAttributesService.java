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
package org.thingsboard.server.transport.lwm2m.server.attributes;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Collection;
import java.util.List;

/**
 * 中文说明：
 * 1. `LwM2MAttributesService` 是 ThingsBoard Common Transport 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface LwM2MAttributesService {

    /**
     * 功能：获取`Shared Attributes`。
     * 参数：
     * - `client`：客户端对象。
     * - `keys`：键。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<TransportProtos.TsKvProto>> getSharedAttributes(LwM2mClient client, Collection<String> keys);

    /**
     * 功能：处理响应。
     * 参数：
     * - `getAttributesResponse`：响应对象。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg getAttributesResponse, TransportProtos.SessionInfoProto sessionInfo);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `attributeUpdateNotification`：`attributeUpdateNotification` 参数。
     * - `sessionInfo`：会话对象。
     * 返回：无。
     */
    void onAttributesUpdate(TransportProtos.AttributeUpdateNotificationMsg attributeUpdateNotification, TransportProtos.SessionInfoProto sessionInfo);

    /**
     * 功能：处理`on Attributes Update`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `tsKvProtos`：数据列表。
     * - `logFailedUpdateOfNonChangedValue`：值。
     * 返回：无。
     */
    void onAttributesUpdate(LwM2mClient lwM2MClient, List<TransportProtos.TsKvProto> tsKvProtos, boolean logFailedUpdateOfNonChangedValue);
}
