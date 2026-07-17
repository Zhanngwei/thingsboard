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

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. `NoSecClient` 是 ThingsBoard Common Transport 中访问 `No Sec Client` 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 它直接协作于网络客户端、认证模型和请求响应对象。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
public class NoSecClient {

    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private CoapClient coapClient;

    /**
     * 功能：创建 `NoSecClient` 实例，并初始化必要字段。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * - `clientKeys`：客户端对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public NoSecClient(String host, int port, String accessToken, String clientKeys, String sharedKeys) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, accessToken, clientKeys, sharedKeys));
        this.coapClient = new CoapClient(uri);
    }

    /**
     * 功能：执行 `test` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void test() {
        executor.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    CoapResponse response = null;
                    try {
                        response = coapClient.get();
                    } catch (ConnectorException | IOException e) {
                        System.err.println("Error occurred while sending request: " + e);
                        System.exit(-1);
                    }
                    if (response != null) {

                        System.out.println(response.getCode() + " - " + response.getCode().name());
                        System.out.println(response.getOptions());
                        System.out.println(response.getResponseText());
                        System.out.println();
                        System.out.println("ADVANCED:");
                        EndpointContext context = response.advanced().getSourceContext();
                        Principal identity = context.getPeerIdentity();
                        if (identity != null) {
                            System.out.println(context.getPeerIdentity());
                        } else {
                            System.out.println("anonymous");
                        }
                        System.out.println(context.get(DtlsEndpointContext.KEY_CIPHER));
                        System.out.println(Utils.prettyPrint(response));
                    } else {
                        System.out.println("No response received.");
                    }
                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.out.println("Error occurred while sending COAP requests.");
            }
        });
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `accessToken`：`accessToken` 参数。
     * - `clientKeys`：客户端对象。
     * - 其余参数：补充处理条件。
     * 返回：文本结果。
     */
    private String getFutureUrl(String host, Integer port, String accessToken, String clientKeys, String sharedKeys) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes?clientKeys=" + clientKeys + "&sharedKeys=" + sharedKeys;
    }

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws URISyntaxException {
        System.out.println("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecClient " +
                "host port accessToken clientKeys sharedKeys");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];
        String clientKeys = args[3];
        String sharedKeys = args[4];

        NoSecClient client = new NoSecClient(host, port, accessToken, clientKeys, sharedKeys);
        client.test();
    }
}
