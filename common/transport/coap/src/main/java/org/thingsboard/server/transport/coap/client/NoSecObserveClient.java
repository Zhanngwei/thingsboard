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
package org.thingsboard.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. `NoSecObserveClient` 是 ThingsBoard Common Transport 中访问 `No Sec Observe` 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 它直接协作于网络客户端、认证模型和请求响应对象。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Slf4j
public class NoSecObserveClient {

    /**
     * `INFINIT_EXCHANGE_LIFETIME`常量，用于统一引用固定值。
     */
    private static final long INFINIT_EXCHANGE_LIFETIME = 0L;

    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private CoapClient coapClient;
    private CoapObserveRelation observeRelation;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private CountDownLatch latch;

    /**
     * 功能：创建 `NoSecObserveClient` 实例，并初始化必要字段。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * 返回：新创建的对象实例。
     */
    public NoSecObserveClient(String host, int port, String accessToken) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, accessToken));
        this.coapClient = new CoapClient(uri);
        coapClient.setTimeout(INFINIT_EXCHANGE_LIFETIME);
        this.latch = new CountDownLatch(5);
    }

    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void start() {
        executor.submit(() -> {
            try {
                Request request = Request.newGet();
                request.setObserve();
                observeRelation = coapClient.observe(request, new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse response) {
                        String responseText = response.getResponseText();
                        CoAP.ResponseCode code = response.getCode();
                        Integer observe = response.getOptions().getObserve();
                        log.info("CoAP Response received! " +
                                        "responseText: {}, " +
                                        "code: {}, " +
                                        "observe seq number: {}",
                                responseText,
                                code,
                                observe);
                        latch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.error("Ack error!");
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred while sending COAP requests: ");
            }
        });
        try {
            latch.await();
            observeRelation.proactiveCancel();
        } catch (InterruptedException e) {
            log.error("Error occurred: ", e);
        }
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * 返回：文本结果。
     */
    private String getFutureUrl(String host, Integer port, String accessToken) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes";
    }

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws URISyntaxException {
        log.info("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecObserveClient " +
                "host port accessToken");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];

        final NoSecObserveClient client = new NoSecObserveClient(host, port, accessToken);
        client.start();
    }
}
