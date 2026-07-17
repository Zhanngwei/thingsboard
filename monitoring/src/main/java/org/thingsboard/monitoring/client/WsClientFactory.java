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
package org.thingsboard.monitoring.client;

import lombok.RequiredArgsConstructor;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.service.MonitoringReporter;
import org.thingsboard.monitoring.util.TbStopWatch;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `WsClientFactory` 是 ThingsBoard Monitoring 中创建或提供 `Ws Client` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@RequiredArgsConstructor
public class WsClientFactory {

    /**
     * 监控报告器，表示当前对象的对应属性。
     */
    private final MonitoringReporter monitoringReporter;
    private final TbStopWatch stopWatch;
    /**
     * 基础访问地址，用于定位外部资源或本地资源。
     */
    @Value("${monitoring.ws.base_url}")
    private String baseUrl;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${monitoring.ws.request_timeout_ms}")
    private int requestTimeoutMs;

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    public WsClient createClient(String accessToken) throws Exception {
        URI uri = new URI(baseUrl + "/api/ws/plugins/telemetry?token=" + accessToken);
        stopWatch.start();
        WsClient wsClient = new WsClient(uri, requestTimeoutMs);
        if (baseUrl.startsWith("wss")) {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, (TrustStrategy) (chain, authType) -> true);
            wsClient.setSocketFactory(builder.build().getSocketFactory());
        }
        boolean connected = wsClient.connectBlocking(requestTimeoutMs, TimeUnit.MILLISECONDS);
        if (!connected) {
            throw new IllegalStateException("Failed to establish WS session");
        }
        monitoringReporter.reportLatency(Latencies.WS_CONNECT, stopWatch.getTime());
        return wsClient;
    }

}
