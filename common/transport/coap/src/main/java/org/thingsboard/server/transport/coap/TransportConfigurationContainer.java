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
package org.thingsboard.server.transport.coap;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.Data;

/**
 * 中文说明：
 * 1. `TransportConfigurationContainer` 是 ThingsBoard Common Transport 中负责传输层接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
public class TransportConfigurationContainer {

    /**
     * 是否满足消息载荷条件。
     */
    private boolean jsonPayload;
    private Descriptors.Descriptor telemetryMsgDescriptor;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private Descriptors.Descriptor attributesMsgDescriptor;
    private Descriptors.Descriptor rpcResponseMsgDescriptor;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private DynamicMessage.Builder rpcRequestDynamicMessageBuilder;

    /**
     * 功能：创建 `TransportConfigurationContainer` 实例，并初始化必要字段。
     * 参数：
     * - `jsonPayload`：`jsonPayload` 参数。
     * - `telemetryMsgDescriptor`：待处理消息。
     * - `attributesMsgDescriptor`：待处理消息。
     * - `rpcResponseMsgDescriptor`：响应对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TransportConfigurationContainer(boolean jsonPayload, Descriptors.Descriptor telemetryMsgDescriptor, Descriptors.Descriptor attributesMsgDescriptor, Descriptors.Descriptor rpcResponseMsgDescriptor, DynamicMessage.Builder rpcRequestDynamicMessageBuilder) {
        this.jsonPayload = jsonPayload;
        this.telemetryMsgDescriptor = telemetryMsgDescriptor;
        this.attributesMsgDescriptor = attributesMsgDescriptor;
        this.rpcResponseMsgDescriptor = rpcResponseMsgDescriptor;
        this.rpcRequestDynamicMessageBuilder = rpcRequestDynamicMessageBuilder;
    }

    /**
     * 功能：创建 `TransportConfigurationContainer` 实例，并初始化必要字段。
     * 参数：
     * - `jsonPayload`：`jsonPayload` 参数。
     * 返回：新创建的对象实例。
     */
    public TransportConfigurationContainer(boolean jsonPayload) {
        this.jsonPayload = jsonPayload;
    }
}
