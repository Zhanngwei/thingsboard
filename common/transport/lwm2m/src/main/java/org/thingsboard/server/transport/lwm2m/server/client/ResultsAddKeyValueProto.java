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

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. `ResultsAddKeyValueProto` 是 ThingsBoard Common Transport 中负责 `Results Add Key` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
public class ResultsAddKeyValueProto {
    /**
     * `resultAttributes`列表，用于保存一组待处理对象。
     */
    List<TransportProtos.KeyValueProto> resultAttributes;
    List<TransportProtos.KeyValueProto> resultTelemetries;

    /**
     * 功能：创建 `ResultsAddKeyValueProto` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public ResultsAddKeyValueProto() {
        this.resultAttributes = new ArrayList<>();
        this.resultTelemetries = new ArrayList<>();
    }

}
