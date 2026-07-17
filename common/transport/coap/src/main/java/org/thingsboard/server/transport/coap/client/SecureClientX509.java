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
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 中文说明：
 * 1. `SecureClientX509` 是 ThingsBoard Common Transport 中负责 `Secure Client X509` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class SecureClientX509 {

    /**
     * `dtlsConnector` 字段，保存当前对象的对应属性。
     */
    private final DTLSConnector dtlsConnector;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private CoapClient coapClient;

    /**
     * 功能：创建 `SecureClientX509` 实例，并初始化必要字段。
     * 参数：
     * - `dtlsConnector`：`dtlsConnector` 参数。
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `clientKeys`：客户端对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public SecureClientX509(DTLSConnector dtlsConnector, String host, int port, String clientKeys, String sharedKeys) throws URISyntaxException {
        this.dtlsConnector = dtlsConnector;
        this.coapClient = getCoapClient(host, port, clientKeys, sharedKeys);
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
     * 功能：获取客户端。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `clientKeys`：客户端对象。
     * - `sharedKeys`：键。
     * 返回：处理结果。
     */
    private CoapClient getCoapClient(String host, Integer port, String clientKeys, String sharedKeys) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, clientKeys, sharedKeys));
        CoapClient client = new CoapClient(uri);
        CoapEndpoint.Builder builder = new CoapEndpoint.Builder();
        builder.setConnector(dtlsConnector);

        client.setEndpoint(builder.build());
        return client;
    }

    /**
     * 功能：获取异步结果。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `clientKeys`：客户端对象。
     * - `sharedKeys`：键。
     * 返回：文本结果。
     */
    private String getFutureUrl(String host, Integer port, String clientKeys, String sharedKeys) {
        return "coaps://" + host + ":" + port + "/api/v1/attributes?clientKeys=" + clientKeys + "&sharedKeys=" + sharedKeys;
    }

    /**
     * 功能：作为当前类的入口方法，完成参数处理并触发主要逻辑。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：无。
     */
    public static void main(String[] args) throws URISyntaxException {
        System.out.println("Usage: java -cp ... org.thingsboard.server.transport.coap.client.SecureClientX509 " +
                "host port keyStoreUriPath keyStoreAlias trustedAliasPattern clientKeys sharedKeys");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String clientKeys = args[6];
        String sharedKeys = args[7];

        String keyStoreUriPath = args[2];
        String keyStoreAlias = args[3];
        String trustedAliasPattern = args[4];
        String keyStorePassword = args[5];


        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new Configuration());
        setupCredentials(builder, keyStoreUriPath, keyStoreAlias, trustedAliasPattern, keyStorePassword);
        DTLSConnector dtlsConnector = new DTLSConnector(builder.build());
        SecureClientX509 client = new SecureClientX509(dtlsConnector, host, port, clientKeys, sharedKeys);
        client.test();
    }

    /**
     * 功能：执行 `setupCredentials` 对应的处理。
     * 参数：
     * - `config`：配置对象。
     * - `keyStoreUriPath`：键。
     * - `keyStoreAlias`：键。
     * - `trustedAliasPattern`：`trustedAliasPattern` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private static void setupCredentials(DtlsConnectorConfig.Builder config, String keyStoreUriPath, String keyStoreAlias, String trustedAliasPattern, String keyStorePassword) {
        StaticNewAdvancedCertificateVerifier.Builder trustBuilder = StaticNewAdvancedCertificateVerifier.builder();
        try {
            SslContextUtil.Credentials serverCredentials = SslContextUtil.loadCredentials(
                    keyStoreUriPath, keyStoreAlias, keyStorePassword.toCharArray(), keyStorePassword.toCharArray());
            Certificate[] trustedCertificates = SslContextUtil.loadTrustedCertificates(
                    keyStoreUriPath, trustedAliasPattern, keyStorePassword.toCharArray());
            trustBuilder.setTrustedCertificates(trustedCertificates);
            config.setAdvancedCertificateVerifier(trustBuilder.build());
            config.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(), Collections.singletonList(CertificateType.X_509)));
        } catch (GeneralSecurityException e) {
            System.err.println("certificates are invalid!");
            throw new IllegalArgumentException(e.getMessage());
        } catch (IOException e) {
            System.err.println("certificates are missing!");
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
