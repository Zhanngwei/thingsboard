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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;

/**
 * 中文说明：
 * 1. `LwM2MBootstrapServers` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
public class LwM2MBootstrapServers {
    /**
     * `shortId`ID，用于定位对应业务对象。
     */
    private Integer shortId = 123;
    private Integer lifetime = 300;
    /**
     * `defaultMinPeriod` 字段，保存当前对象的对应属性。
     */
    private Integer defaultMinPeriod = 1;
    private boolean notifIfDisabled = true;
    /**
     * 绑定模式，用于区分当前对象的状态或类别。
     */
    private String binding = "UQ";
}
