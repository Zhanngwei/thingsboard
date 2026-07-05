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
package org.thingsboard.server.transport.mqtt;

import io.netty.handler.ssl.SslHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by valerii.sosliuk on 11/6/16.
 */
/**
 * 中文说明：
 * 1. 类目的：`MqttSslHandlerProvider` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Component("MqttSslHandlerProvider")
@ConditionalOnProperty(prefix = "transport.mqtt.ssl", value = "enabled", havingValue = "true", matchIfMissing = false)
public class MqttSslHandlerProvider {

    /**
     * SSL，表示当前对象的对应属性。
     */
    @Value("${transport.mqtt.ssl.protocol}")
    private String sslProtocol;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TransportService transportService;

    /**
     * 功能：执行 `mqttSslCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "transport.mqtt.ssl.credentials")
    public SslCredentialsConfig mqttSslCredentials() {
        return new SslCredentialsConfig("MQTT SSL Credentials", false);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("mqttSslCredentials")
    private SslCredentialsConfig mqttSslCredentialsConfig;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private SSLContext sslContext;

    /**
     * 功能：获取处理器。
     * 参数：无。
     * 返回：处理结果。
     */
    public SslHandler getSslHandler() {
        if (sslContext == null) {
            sslContext = createSslContext();
        }
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        sslEngine.setNeedClientAuth(false);
        sslEngine.setWantClientAuth(true);
        sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols());
        sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
        sslEngine.setEnableSessionCreation(true);
        return new SslHandler(sslEngine);
    }

    /**
     * 功能：保存或创建上下文。
     * 参数：无。
     * 返回：处理结果。
     */
    private SSLContext createSslContext() {
        try {
            SslCredentials sslCredentials = this.mqttSslCredentialsConfig.getCredentials();
            TrustManagerFactory tmFactory = sslCredentials.createTrustManagerFactory();
            KeyManagerFactory kmf = sslCredentials.createKeyManagerFactory();

            KeyManager[] km = kmf.getKeyManagers();
            TrustManager x509wrapped = getX509TrustManager(tmFactory);
            TrustManager[] tm = {x509wrapped};
            if (StringUtils.isEmpty(sslProtocol)) {
                sslProtocol = "TLS";
            }
            SSLContext sslContext = SSLContext.getInstance(sslProtocol);
            sslContext.init(km, tm, null);
            return sslContext;
        } catch (Exception e) {
            log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get SSL context", e);
        }
    }

    /**
     * 功能：获取管理器。
     * 参数：
     * - `tmf`：`tmf` 参数。
     * 返回：处理结果。
     */
    private TrustManager getX509TrustManager(TrustManagerFactory tmf) throws Exception {
        X509TrustManager x509Tm = null;
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                x509Tm = (X509TrustManager) tm;
                break;
            }
        }
        return new ThingsboardMqttX509TrustManager(x509Tm, transportService);
    }

    /**
     * 中文说明：
     * 1. 类目的：`ThingsboardMqttX509TrustManager` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    static class ThingsboardMqttX509TrustManager implements X509TrustManager {

        /**
         * 管理器，负责处理对应任务或消息。
         */
        private final X509TrustManager trustManager;
        private final TransportService transportService;

        ThingsboardMqttX509TrustManager(X509TrustManager trustManager, TransportService transportService) {
            this.trustManager = trustManager;
            this.transportService = transportService;
        }

        /**
         * 功能：获取`Accepted Issuers`。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }

        /**
         * 功能：校验服务端。
         * 参数：
         * - `chain`：`chain` 参数。
         * - `authType`：类型。
         * 返回：无。
         */
        @Override
        public void checkServerTrusted(X509Certificate[] chain,
                                       String authType) throws CertificateException {
            trustManager.checkServerTrusted(chain, authType);
        }

        /**
         * 功能：校验客户端。
         * 参数：
         * - `chain`：`chain` 参数。
         * - `authType`：类型。
         * 返回：无。
         */
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            if (!validateCertificateChain(chain)) {
                throw new CertificateException("Invalid Chain of X509 Certificates. ");
            }
            String clientDeviceCertValue = SslUtil.getCertificateString(chain[0]);
            final String[] credentialsBodyHolder = new String[1];
            CountDownLatch latch = new CountDownLatch(1);
            try {
                String certificateChain = SslUtil.getCertificateChainString(chain);
                transportService.process(DeviceTransportType.MQTT, TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg
                                .newBuilder().setCertificateChain(certificateChain).build(),
                        new TransportServiceCallback<>() {
                            @Override
                            public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                                if (!StringUtils.isEmpty(msg.getCredentials())) {
                                    credentialsBodyHolder[0] = msg.getCredentials();
                                }
                                latch.countDown();
                            }

                            @Override
                            public void onError(Throwable e) {
                                log.trace("Failed to process certificate chain: {}", certificateChain, e);
                                latch.countDown();
                            }
                        });
                latch.await(10, TimeUnit.SECONDS);
                if (!clientDeviceCertValue.equals(credentialsBodyHolder[0])) {
                    log.debug("Failed to find credentials for device certificate chain: {}", chain);
                    if (chain.length == 1) {
                        throw new CertificateException("Invalid Device Certificate");
                    } else {
                        throw new CertificateException("Invalid Chain of X509 Certificates");
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        /**
         * 功能：校验证书。
         * 参数：
         * - `chain`：`chain` 参数。
         * 返回：判断结果。
         */
        private boolean validateCertificateChain(X509Certificate[] chain) {
            try {
                if (chain.length > 1) {
                    X509Certificate leafCert = chain[0];
                    for (int i = 1; i < chain.length; i++) {
                        X509Certificate intermediateCert = chain[i];
                        leafCert.verify(intermediateCert.getPublicKey());
                        leafCert = intermediateCert;
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`MqttSslHandlerProvider` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
