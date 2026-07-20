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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.ConfigurationChecker;
import org.eclipse.leshan.server.bootstrap.InMemoryBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 中文说明：
 * 1. `LwM2MInMemoryBootstrapConfigStore` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `InMemoryBootstrapConfigStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Component("LwM2MInMemoryBootstrapConfigStore")
@TbLwM2mBootstrapTransportComponent
public class LwM2MInMemoryBootstrapConfigStore extends InMemoryBootstrapConfigStore {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    protected final ConfigurationChecker configChecker = new LwM2MConfigurationChecker();

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `deviceIdentity`：设备信息或设备标识。
     * - `session`：会话对象。
     * 返回：处理结果。
     */
    @Override
    public BootstrapConfig get(String endpoint, Identity deviceIdentity, BootstrapSession session) {
        return bootstrapByEndpoint.get(endpoint);
    }

    /**
     * 功能：获取`All`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Map<String, BootstrapConfig> getAll() {
        readLock.lock();
        try {
            return super.getAll();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `config`：配置对象。
     * 返回：无。
     */
    @Override
    public void add(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            addToStore(endpoint, config);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public BootstrapConfig remove(String endpoint) {
        writeLock.lock();
        try {
            return super.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 功能：保存或创建存储组件。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `config`：配置对象。
     * 返回：无。
     */
    public void addToStore(String endpoint, BootstrapConfig config) throws InvalidConfigurationException {
        configChecker.verify(config);
        // Check PSK identity uniqueness for bootstrap server:
        PskByServer pskToAdd = getBootstrapPskIdentity(config);
        if (pskToAdd != null) {
            BootstrapConfig existingConfig = bootstrapByPskId.get(pskToAdd);
            if (existingConfig != null) {
                // check if this config will be replace by the new one.
                BootstrapConfig previousConfig = bootstrapByEndpoint.get(endpoint);
                if (previousConfig != existingConfig) {
                    throw new InvalidConfigurationException(
                            "Psk identity [%s] already used for this bootstrap server [%s]", pskToAdd.identity,
                            pskToAdd.serverUrl);
                }
            }
        }

        bootstrapByEndpoint.put(endpoint, config);
        if (pskToAdd != null) {
            bootstrapByPskId.put(pskToAdd, config);
        }
    }
}
