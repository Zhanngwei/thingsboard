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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mEncoder;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MAuthorizer;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MDtlsCertificateVerifier;
import org.thingsboard.server.transport.lwm2m.server.store.TbSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2mValueConverterImpl;

import javax.annotation.PreDestroy;
import java.security.cert.X509Certificate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RECOMMENDED_CURVES_ONLY;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_ROLE;
import static org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole.SERVER_ONLY;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256;
import static org.eclipse.californium.scandium.dtls.cipher.CipherSuite.TLS_PSK_WITH_AES_128_CCM_8;
import static org.thingsboard.server.transport.lwm2m.server.LwM2MNetworkConfig.getCoapConfig;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;

/**
 * 中文说明：
 * 1. `DefaultLwM2mTransportService` 是 ThingsBoard Common Transport 中负责 LwM2M 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `LwM2MTransportService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Component
@DependsOn({"lwM2mDownlinkMsgHandler", "lwM2mUplinkMsgHandler"})
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2mTransportService implements LwM2MTransportService {

    /**
     * `RPK_OR_X509_CIPHER_SUITES`常量，用于统一引用固定值。
     */
    public static final CipherSuite[] RPK_OR_X509_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256, TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8, TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256};
    public static final CipherSuite[] PSK_CIPHER_SUITES = {TLS_PSK_WITH_AES_128_CCM_8, TLS_PSK_WITH_AES_128_CBC_SHA256};

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2mUplinkMsgHandler handler;
    /**
     * 存储组件，表示当前对象的对应属性。
     */
    private final CaliforniumRegistrationStore registrationStore;
    private final TbSecurityStore securityStore;
    /**
     * 证书，用于认证或安全校验。
     */
    private final TbLwM2MDtlsCertificateVerifier certificateVerifier;
    private final TbLwM2MAuthorizer authorizer;
    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    private final LwM2mVersionedModelProvider modelProvider;

    /**
     * 服务端，用于支撑当前网络或外部服务交互。
     */
    private LeshanServer server;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.AFTER_TRANSPORT_SERVICE)
    public void init() {
        this.server = getLhServer();
        /*
         * Add a resource to the server.
         * CoapResource ->
         * path = FW_PACKAGE or SW_PACKAGE
         * nameFile = "BC68JAR01A09_TO_BC68JAR01A10.bin"
         * "coap://host:port/{path}/{token}/{nameFile}"
         */
        LwM2mTransportCoapResource otaCoapResource = new LwM2mTransportCoapResource(otaPackageDataCache, FIRMWARE_UPDATE_COAP_RESOURCE);
        this.server.coap().getServer().add(otaCoapResource);
        this.context.setServer(server);
        this.startLhServer();
    }

    /**
     * 功能：初始化或启动服务端。
     * 参数：无。
     * 返回：无。
     */
    private void startLhServer() {
        log.info("Starting LwM2M transport server...");
        this.server.start();
        LwM2mServerListener lhServerCertListener = new LwM2mServerListener(handler);
        this.server.getRegistrationService().addListener(lhServerCertListener.registrationListener);
        this.server.getPresenceService().addListener(lhServerCertListener.presenceListener);
        this.server.getObservationService().addListener(lhServerCertListener.observationListener);
        this.server.getSendService().addListener(lhServerCertListener.sendListener);
        log.info("Started LwM2M transport server.");
    }

    /**
     * 功能：执行 `shutdown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdown() {
        try {
            log.info("Stopping LwM2M transport server!");
            server.destroy();
            log.info("LwM2M transport server stopped!");
        } catch (Exception e) {
            log.error("Failed to gracefully stop the LwM2M transport server!", e);
        }
    }

    /**
     * 功能：获取服务端。
     * 参数：无。
     * 返回：处理结果。
     */
    private LeshanServer getLhServer() {
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(config.getHost(), config.getPort());
        builder.setLocalSecureAddress(config.getSecureHost(), config.getSecurePort());
        builder.setDecoder(new DefaultLwM2mDecoder());
        /* Use a magic converter to support bad type send by the UI. */
        builder.setEncoder(new DefaultLwM2mEncoder(LwM2mValueConverterImpl.getInstance()));

        /* Create CoAP Config */
        builder.setCoapConfig(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        /* Define model provider (Create Models )*/
        builder.setObjectModelProvider(modelProvider);

        /* Set securityStore with new registrationStore */
        builder.setSecurityStore(securityStore);
        builder.setRegistrationStore(registrationStore);

        /* Create DTLS Config */
        DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(getCoapConfig(config.getPort(), config.getSecurePort(), config));

        dtlsConfig.set(DTLS_RECOMMENDED_CURVES_ONLY, config.isRecommendedSupportedGroups());
        dtlsConfig.set(DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, config.isRecommendedCiphers());
        dtlsConfig.set(DTLS_RETRANSMISSION_TIMEOUT, config.getDtlsRetransmissionTimeout(), MILLISECONDS);
        dtlsConfig.set(DTLS_CONNECTION_ID_LENGTH, config.getDtlsConnectionIdLength());
        dtlsConfig.set(DTLS_ROLE, SERVER_ONLY);

        /*  Create credentials */
        this.setServerWithCredentials(builder, dtlsConfig);

        /* Set DTLS Config */
        builder.setDtlsConfig(dtlsConfig);

        /* Create LWM2M server */
        return builder.build();
    }

    /**
     * 功能：更新凭据。
     * 参数：
     * - `builder`：`builder` 参数。
     * - `dtlsConfig`：配置对象。
     * 返回：无。
     */
    private void setServerWithCredentials(LeshanServerBuilder builder, DtlsConnectorConfig.Builder dtlsConfig) {
        if (this.config.getSslCredentials() != null) {
            SslCredentials sslCredentials = this.config.getSslCredentials();
            builder.setPublicKey(sslCredentials.getPublicKey());
            builder.setPrivateKey(sslCredentials.getPrivateKey());
            builder.setCertificateChain(sslCredentials.getCertificateChain());
            dtlsConfig.setAdvancedCertificateVerifier(certificateVerifier);
            builder.setAuthorizer(authorizer);
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, RPK_OR_X509_CIPHER_SUITES);
        } else {
            /* by default trust all */
            builder.setTrustedCertificates(new X509Certificate[0]);
            log.info("Unable to load X509 files for LWM2MServer");
            dtlsConfig.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, PSK_CIPHER_SUITES);
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return DataConstants.LWM2M_TRANSPORT_NAME;
    }

}
