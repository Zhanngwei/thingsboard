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
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.auth.RawPublicKeyIdentity;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.dtls.x509.StaticNewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.store.LwM2MBootstrapSecurityStore;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import javax.annotation.PostConstruct;
import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * 中文说明：
 * 1. `TbLwM2MDtlsBootstrapCertificateVerifier` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `NewAdvancedCertificateVerifier`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Component
@TbLwM2mBootstrapTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsBootstrapCertificateVerifier implements NewAdvancedCertificateVerifier {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final LwM2MTransportServerConfig config;
    private final LwM2MBootstrapSecurityStore bsSecurityStore;

    /**
     * 证书，用于认证或安全校验。
     */
    private StaticNewAdvancedCertificateVerifier staticCertificateVerifier;

    /**
     * 是否满足客户端条件。
     */
    @Value("${transport.lwm2m.server.security.skip_validity_check_for_client_cert:false}")
    private boolean skipValidityCheckForClientCert;

    /**
     * 功能：获取证书。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Arrays.asList(CertificateType.X_509, CertificateType.RAW_PUBLIC_KEY);
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @SuppressWarnings("deprecation")
    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("ailed to initialize the LwM2M certificate verifier", e);
        }
    }

    /**
     * 功能：校验证书。
     * 参数：
     * - `cid`：`cid`ID。
     * - `serverName`：名称。
     * - `remotePeer`：`remotePeer` 参数。
     * - `clientUsage`：客户端对象。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer,
                                                           boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath,
                                                           CertificateMessage message) {
        CertPath certChain = message.getCertificateChain();
        if (certChain == null) {
            //We trust all RPK on this layer, and use TbLwM2MAuthorizer
            PublicKey publicKey = message.getPublicKey();
            return new CertificateVerificationResult(cid, publicKey, null);
        } else {
            try {
                boolean x509CredentialsFound = false;
                X509Certificate[] chain = certChain.getCertificates().toArray(new X509Certificate[0]);
                for (X509Certificate cert : chain) {
                    try {
                        if (!skipValidityCheckForClientCert) {
                            cert.checkValidity();
                        }
                        TbLwM2MSecurityInfo securityInfo = null;
                        // verify if trust
                        if (staticCertificateVerifier != null) {
                            HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                            if (exception == null) {
                                String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                                if (StringUtils.isNotEmpty(endpoint)) {
                                    securityInfo = bsSecurityStore.getX509ByEndpoint(endpoint);
                                }
                            } else {
                                log.trace("Certificate validation failed.", exception);
                            }
                        }
                        // if not trust or cert trust securityInfo == null
                        if (securityInfo == null || securityInfo.getMsg() == null) {
                            String strCert = SslUtil.getCertificateString(cert);
                            String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                            try {
                                securityInfo = bsSecurityStore.getX509ByEndpoint(sha3Hash);
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed to find security info: [{}]", sha3Hash, e);
                            }
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                            x509CredentialsFound = true;
                            break;
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.trace("Failed to find security info: [{}]", cert.getSubjectX500Principal().getName(), e);
                    }
                }
                if (!x509CredentialsFound) {
                    AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.INTERNAL_ERROR);
                    throw new HandshakeException("x509 verification not enabled!", alert);
                }
                return new CertificateVerificationResult(cid, certChain, null);
            } catch (HandshakeException e) {
                log.trace("Certificate validation failed!", e);
                return new CertificateVerificationResult(cid, e, null);
            }
        }
    }

    /**
     * 功能：获取`Accepted Issuers`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return CertPathUtil.toSubjects(null);
    }

    /**
     * 功能：更新处理器。
     * 参数：
     * - `resultHandler`：处理器对象。
     * 返回：无。
     */
    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {

    }
}
