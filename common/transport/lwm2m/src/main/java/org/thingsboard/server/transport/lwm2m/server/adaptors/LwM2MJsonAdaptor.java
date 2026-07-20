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
package org.thingsboard.server.transport.lwm2m.server.adaptors;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;

import java.util.Collection;
import java.util.Random;

/**
 * 中文说明：
 * 1. `LwM2MJsonAdaptor` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `LwM2MTransportAdaptor`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Component("LwM2MJsonAdaptor")
@TbLwM2mTransportComponent
public class LwM2MJsonAdaptor implements LwM2MTransportAdaptor  {

    /**
     * 功能：转换遥测。
     * 参数：
     * - `jsonElement`：`jsonElement` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostTelemetryMsg convertToPostTelemetry(JsonElement jsonElement) throws AdaptorException {
        try {
            return JsonConverter.convertToTelemetryProto(jsonElement);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Post Attributes`。
     * 参数：
     * - `jsonElement`：`jsonElement` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.PostAttributeMsg convertToPostAttributes(JsonElement jsonElement) throws AdaptorException {
        try {
            return JsonConverter.convertToAttributesProto(jsonElement);
        } catch (IllegalStateException | JsonSyntaxException ex) {
            throw new AdaptorException(ex);
        }
    }

    /**
     * 功能：转换`To Get Attributes`。
     * 参数：
     * - `clientKeys`：客户端对象。
     * - `sharedKeys`：键。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.GetAttributeRequestMsg convertToGetAttributes(Collection<String> clientKeys, Collection<String> sharedKeys) throws AdaptorException {
        try {
            TransportProtos.GetAttributeRequestMsg.Builder result = TransportProtos.GetAttributeRequestMsg.newBuilder();
            Random random = new Random();
            result.setRequestId(random.nextInt());
            if (clientKeys != null) {
                result.addAllClientAttributeNames(clientKeys);
            }
            if (sharedKeys != null) {
                result.addAllSharedAttributeNames(sharedKeys);
            }
            return result.build();
        } catch (RuntimeException e) {
            throw new AdaptorException(e);
        }
    }
}
