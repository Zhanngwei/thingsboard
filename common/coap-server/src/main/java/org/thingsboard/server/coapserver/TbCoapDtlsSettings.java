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
package org.thingsboard.server.coapserver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.SslContextUtil;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.x509.SingleCertificateProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.elements.config.CertificateAuthenticationMode.WANTED;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CLIENT_AUTHENTICATION_MODE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;

/**
 * 中文说明：
 * 1. 类目的：`TbCoapDtlsSettings` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@ConditionalOnProperty(prefix = "transport.coap.dtls", value = "enabled", havingValue = "true", matchIfMissing = false)
@Component
public class TbCoapDtlsSettings {

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @Value("${transport.coap.dtls.bind_address}")
    private String host;

    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @Value("${transport.coap.dtls.bind_port}")
    private Integer port;

    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    @Value("${transport.coap.dtls.retransmission_timeout:9000}")
    private int dtlsRetransmissionTimeout;

    /**
     * 功能：执行 `coapDtlsCredentials` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    @ConfigurationProperties(prefix = "transport.coap.dtls.credentials")
    public SslCredentialsConfig coapDtlsCredentials() {
        return new SslCredentialsConfig("COAP DTLS Credentials", false);
    }

    /**
     * 凭据，保存当前对象的配置选项。
     */
    @Autowired
    @Qualifier("coapDtlsCredentials")
    private SslCredentialsConfig coapDtlsCredentialsConfig;

    /**
     * 是否满足客户端条件。
     */
    @Value("${transport.coap.dtls.x509.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.coap.dtls.x509.dtls_session_inactivity_timeout:86400000}")
    private long dtlsSessionInactivityTimeout;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Value("${transport.coap.dtls.x509.dtls_session_report_timeout:1800000}")
    private long dtlsSessionReportTimeout;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TransportService transportService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TbServiceInfoProvider serviceInfoProvider;

    /**
     * 功能：执行 `dtlsConnectorConfig` 对应的处理。
     * 参数：
     * - `configuration`：配置对象。
     * 返回：处理结果。
     */
    public DtlsConnectorConfig dtlsConnectorConfig(Configuration configuration) throws UnknownHostException {
        DtlsConnectorConfig.Builder configBuilder = new DtlsConnectorConfig.Builder(configuration);
        configBuilder.setAddress(getInetSocketAddress());
        SslCredentials sslCredentials = this.coapDtlsCredentialsConfig.getCredentials();
        SslContextUtil.Credentials serverCredentials =
                new SslContextUtil.Credentials(sslCredentials.getPrivateKey(), null, sslCredentials.getCertificateChain());
        configBuilder.set(DTLS_CLIENT_AUTHENTICATION_MODE, WANTED);
        configBuilder.set(DTLS_RETRANSMISSION_TIMEOUT, dtlsRetransmissionTimeout, MILLISECONDS);
        configBuilder.set(DTLS_ROLE, SERVER_ONLY);
        configBuilder.setAdvancedCertificateVerifier(
                new TbCoapDtlsCertificateVerifier(
                        transportService,
                        serviceInfoProvider,
                        dtlsSessionInactivityTimeout,
                        dtlsSessionReportTimeout,
                        skipValidityCheckForClientCert
                )
        );
        configBuilder.setCertificateIdentityProvider(new SingleCertificateProvider(serverCredentials.getPrivateKey(), serverCredentials.getCertificateChain(),
                Collections.singletonList(CertificateType.X_509)));
        return configBuilder.build();
    }

    /**
     * 功能：获取`Inet Socket Address`。
     * 参数：无。
     * 返回：处理结果。
     */
    private InetSocketAddress getInetSocketAddress() throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(host);
        return new InetSocketAddress(addr, port);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbCoapDtlsSettings` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
