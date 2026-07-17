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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;

/**
 * 中文说明：
 * 1. `LwM2MServerBootstrap` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Data
public class LwM2MServerBootstrap {

    /**
     * 公钥ID，用于定位对应业务对象。
     */
    String clientPublicKeyOrId = "";
    String clientSecretKey = "";
    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    String serverPublicKey = "";
    Integer clientHoldOffTime = 1;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    Integer bootstrapServerAccountTimeout = 0;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    String host = "0.0.0.0";
    Integer port = 0;
    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    String securityHost = "0.0.0.0";
    Integer securityPort = 0;

    /**
     * 安全模式，用于区分当前对象的状态或类别。
     */
    SecurityMode securityMode = SecurityMode.NO_SEC;

    /**
     * 服务端ID，用于定位对应业务对象。
     */
    Integer serverId = 123;
    boolean bootstrapServerIs = false;

    /**
     * 功能：创建 `LwM2MServerBootstrap` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public LwM2MServerBootstrap() {
    }

    /**
     * 功能：创建 `LwM2MServerBootstrap` 实例，并初始化必要字段。
     * 参数：
     * - `bootstrapFromCredential`：`bootstrapFromCredential` 参数。
     * - `profileServerBootstrap`：`profileServerBootstrap` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MServerBootstrap(LwM2MServerBootstrap bootstrapFromCredential, LwM2MServerBootstrap profileServerBootstrap) {
        this.clientPublicKeyOrId = bootstrapFromCredential.getClientPublicKeyOrId();
        this.clientSecretKey = bootstrapFromCredential.getClientSecretKey();
        this.serverPublicKey = profileServerBootstrap.getServerPublicKey();
        this.clientHoldOffTime = profileServerBootstrap.getClientHoldOffTime();
        this.bootstrapServerAccountTimeout = profileServerBootstrap.getBootstrapServerAccountTimeout();
        this.host = (profileServerBootstrap.getHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getHost();
        this.port = profileServerBootstrap.getPort();
        this.securityHost = (profileServerBootstrap.getSecurityHost().equals("0.0.0.0")) ? "localhost" : profileServerBootstrap.getSecurityHost();
        this.securityPort = profileServerBootstrap.getSecurityPort();
        this.securityMode = profileServerBootstrap.getSecurityMode();
        this.serverId = profileServerBootstrap.getServerId();
        this.bootstrapServerIs = profileServerBootstrap.bootstrapServerIs;
    }
}
