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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.thingsboard.server.msa.mapper.WsTelemetryResponse;

import javax.net.ssl.SSLParameters;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `WsClient` 是 ThingsBoard Microservices 中访问 `Ws Client` 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 直接依赖的类型边界包括 `WebSocketClient`。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Slf4j
public class WsClient extends WebSocketClient {
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private WsTelemetryResponse message;

    /**
     * 是否满足`firstReplyReceived`条件。
     */
    private volatile boolean firstReplyReceived;
    private final CountDownLatch firstReply = new CountDownLatch(1);
    private final CountDownLatch latch = new CountDownLatch(1);

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private final long timeoutMultiplier;

    /**
     * 功能：创建 `WsClient` 实例，并初始化必要字段。
     * 参数：
     * - `serverUri`：`serverUri` 参数。
     * - `timeoutMultiplier`：`timeoutMultiplier` 参数。
     * 返回：新创建的对象实例。
     */
    WsClient(URI serverUri, long timeoutMultiplier) {
        super(serverUri);
        this.timeoutMultiplier = timeoutMultiplier;
    }

    /**
     * 功能：处理`on Open`。
     * 参数：
     * - `serverHandshake`：`serverHandshake` 参数。
     * 返回：无。
     */
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `message`：待处理消息。
     * 返回：无。
     */
    @Override
    public synchronized void onMessage(String message) {
        log.error("WS onMessage: {}", message);
        if (!firstReplyReceived) {
            firstReplyReceived = true;
            firstReply.countDown();
        } else {
            try {
                WsTelemetryResponse response = mapper.readValue(message, WsTelemetryResponse.class);
                if (!response.getData().isEmpty()) {
                    this.message = response;
                    latch.countDown();
                }
            } catch (IOException e) {
                log.error("ws message can't be read");
            }
        }
    }

    /**
     * 功能：处理`on Close`。
     * 参数：
     * - `code`：`code` 参数。
     * - `reason`：`reason` 参数。
     * - `remote`：`remote` 参数。
     * 返回：无。
     */
    @Override
    public synchronized void onClose(int code, String reason, boolean remote) {
        log.error("WS onClose: [{}]", reason);
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `ex`：`ex` 参数。
     * 返回：无。
     */
    @Override
    public synchronized void onError(Exception ex) {
        log.error("WS onError: ", ex);
        ex.printStackTrace();
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    public WsTelemetryResponse getLastMessage() {
        try {
            boolean result = latch.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (result) {
                return this.message;
            } else {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
        }
        return null;
    }

    /**
     * 功能：执行 `waitForFirstReply` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void waitForFirstReply() {
        try {
            boolean result = firstReply.await(10 * timeoutMultiplier, TimeUnit.SECONDS);
            if (!result) {
                log.error("Timeout, ws message wasn't received");
                throw new RuntimeException("Timeout, ws message wasn't received");
            }
        } catch (InterruptedException e) {
            log.error("Timeout, ws message wasn't received");
            throw new RuntimeException(e);
        }
    }

    /**
     * 功能：处理SSL。
     * 参数：
     * - `sslParameters`：`sslParameters` 参数。
     * 返回：无。
     */
    @Override
    protected void onSetSSLParameters(SSLParameters sslParameters) {
        sslParameters.setEndpointIdentificationAlgorithm(null);
    }
}
