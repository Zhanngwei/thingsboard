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
package org.thingsboard.monitoring.service.transport.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.thingsboard.monitoring.client.Lwm2mClient;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

/**
 * 中文说明：
 * 1. `Lwm2mTransportHealthChecker` 是 ThingsBoard Monitoring 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TransportHealthChecker`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class Lwm2mTransportHealthChecker extends TransportHealthChecker<Lwm2mTransportMonitoringConfig> {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private Lwm2mClient lwm2mClient;

    /**
     * 功能：创建 `Lwm2mTransportHealthChecker` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `target`：`target` 参数。
     * 返回：新创建的对象实例。
     */
    protected Lwm2mTransportHealthChecker(Lwm2mTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void initClient() throws Exception {
        if (lwm2mClient == null || lwm2mClient.getLeshanClient() == null || lwm2mClient.isDestroyed()) {
            String endpoint = target.getDevice().getCredentials().getCredentialsId();
            lwm2mClient = new Lwm2mClient(target.getBaseUrl(), endpoint);
            lwm2mClient.initClient();
            log.debug("Initialized LwM2M client for endpoint '{}'", endpoint);
        }
    }

    /**
     * 功能：发送或提交消息载荷。
     * 参数：
     * - `payload`：`payload` 参数。
     * 返回：无。
     */
    @Override
    protected void sendTestPayload(String payload) throws Exception {
        lwm2mClient.send(payload, 0);
    }

    /**
     * 功能：保存或创建消息载荷。
     * 参数：
     * - `testValue`：值。
     * 返回：文本结果。
     */
    @Override
    protected String createTestPayload(String testValue) {
        return testValue;
    }

    /**
     * 功能：停止或关闭客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void destroyClient() throws Exception {
        if (lwm2mClient != null) {
            lwm2mClient.destroy();
            lwm2mClient = null;
        }
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected TransportType getTransportType() {
        return TransportType.LWM2M;
    }

}
