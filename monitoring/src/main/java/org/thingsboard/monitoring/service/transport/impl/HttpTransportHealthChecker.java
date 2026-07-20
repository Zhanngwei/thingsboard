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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.monitoring.config.transport.HttpTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.config.transport.TransportType;
import org.thingsboard.monitoring.service.transport.TransportHealthChecker;

import java.time.Duration;

/**
 * 中文说明：
 * 1. `HttpTransportHealthChecker` 是 ThingsBoard Monitoring 中负责 HTTP 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TransportHealthChecker`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class HttpTransportHealthChecker extends TransportHealthChecker<HttpTransportMonitoringConfig> {

    /**
     * HTTP 客户端，用于支撑当前网络或外部服务交互。
     */
    private RestTemplate restTemplate;

    /**
     * 功能：创建 `HttpTransportHealthChecker` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `target`：`target` 参数。
     * 返回：新创建的对象实例。
     */
    protected HttpTransportHealthChecker(HttpTransportMonitoringConfig config, TransportMonitoringTarget target) {
        super(config, target);
    }

    /**
     * 功能：初始化或启动客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void initClient() throws Exception {
        if (restTemplate == null) {
            restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .setReadTimeout(Duration.ofMillis(config.getRequestTimeoutMs()))
                    .build();
            log.debug("Initialized HTTP client");
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
        String accessToken = target.getDevice().getCredentials().getCredentialsId();
        restTemplate.postForObject(target.getBaseUrl() + "/api/v1/" + accessToken + "/telemetry", payload, String.class);
    }

    /**
     * 功能：停止或关闭客户端。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void destroyClient() throws Exception {}

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected TransportType getTransportType() {
        return TransportType.HTTP;
    }

}
