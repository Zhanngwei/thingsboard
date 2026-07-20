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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.ConfigurationChecker;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import java.util.Map;

/**
 * 中文说明：
 * 1. `LwM2MConfigurationChecker` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `ConfigurationChecker`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class LwM2MConfigurationChecker extends ConfigurationChecker {

    /**
     * 功能：执行 `verify` 对应的处理。
     * 参数：
     * - `config`：配置对象。
     * 返回：无。
     */
    @Override
    public void verify(BootstrapConfig config) throws InvalidConfigurationException {
        // check security configurations
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> e : config.security.entrySet()) {
            BootstrapConfig.ServerSecurity sec = e.getValue();

            // checks security config
            switch (sec.securityMode) {
                case NO_SEC:
                    checkNoSec(sec);
                    break;
                case PSK:
                    checkPSK(sec);
                    break;
                case RPK:
                    checkRPK(sec);
                    break;
                case X509:
                    checkX509(sec);
                    break;
                case EST:
                    throw new InvalidConfigurationException("EST is not currently supported.", e);
            }

            validateMandatoryField(sec);
        }

        // does each server have a corresponding security entry?
        validateOneSecurityByServer(config);
    }

    /**
     * 功能：校验服务端。
     * 参数：
     * - `config`：配置对象。
     * 返回：无。
     */
    protected void validateOneSecurityByServer(BootstrapConfig config) throws InvalidConfigurationException {
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> e : config.servers.entrySet()) {
            BootstrapConfig.ServerConfig srvCfg = e.getValue();

            // shortId checks
            if (srvCfg.shortId == 0) {
                throw new InvalidConfigurationException("short ID must not be 0");
            }

            // look for security entry
            BootstrapConfig.ServerSecurity security = getSecurityEntry(config, srvCfg.shortId);

            if (security == null) {
                throw new InvalidConfigurationException("no security entry for server instance: " + e.getKey());
            }
        }
    }

    /**
     * 功能：获取`Security Entry`。
     * 参数：
     * - `config`：配置对象。
     * - `shortId`：`shortId`ID。
     * 返回：处理结果。
     */
    protected static BootstrapConfig.ServerSecurity getSecurityEntry(BootstrapConfig config, int shortId) {
        for (Map.Entry<Integer, BootstrapConfig.ServerSecurity> es : config.security.entrySet()) {
            if (es.getValue().serverId == shortId) {
                return es.getValue();
            }
        }
        return null;
    }
}