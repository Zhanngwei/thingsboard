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
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.transport.CoapTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.io.IOException;

/**
 * 中文说明：
 * 1. `CoapTransportHealthChecker` 是 ThingsBoard Monitoring 中负责 CoAP 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TransportHealthChecker`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class CoapTransportHealthChecker extends TransportHealthChecker<CoapTransportMonitoringConfig> {

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private CoapClient coapClient;

    /**
     * 功能：创建 `CoapTransportHealthChecker` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `target`：`target` 参数。
     * 返回：新创建的对象实例。
     */
    protected CoapTransportHealthChecker(CoapTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void initClient() throws Exception {
        if (coapClient == null) {
            String accessToken = target.getDevice().getCredentials().getCredentialsId();
            String uri = target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry";
            coapClient = new CoapClient(uri);
            coapClient.setTimeout((long) config.getRequestTimeoutMs());
            log.debug("Initialized CoAP client for URI {}", uri);
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
        CoapResponse response = coapClient.post(payload, MediaTypeRegistry.APPLICATION_JSON);
        CoAP.ResponseCode code = response.getCode();
        if (code.codeClass != CoAP.CodeClass.SUCCESS_RESPONSE.value) {
            throw new IOException("COAP client didn't receive success response from transport");
        }
    }

    /**
     * 功能：停止或关闭客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void destroyClient() throws Exception {
        if (coapClient != null) {
            coapClient.shutdown();
            coapClient = null;
            log.info("Disconnected CoAP client");
        }
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected TransportType getTransportType() {
        return TransportType.COAP;
    }

}
