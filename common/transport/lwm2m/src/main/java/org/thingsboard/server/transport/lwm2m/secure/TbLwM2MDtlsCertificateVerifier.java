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
package org.thingsboard.server.transport.lwm2m.secure;

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
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

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

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.CLIENT;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2MDtlsCertificateVerifier` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Component
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class TbLwM2MDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final LwM2MTransportServerConfig config;
    /**
     * 信息对象，表示当前对象的对应属性。
     */
    private final LwM2mCredentialsSecurityInfoValidator securityInfoValidator;
    private final TbMainSecurityStore securityStore;

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
    @PostConstruct
    public void init() {
        try {
            /* by default trust all */
            if (config.getTrustSslCredentials() != null) {
                X509Certificate[] trustedCertificates = config.getTrustSslCredentials().getTrustedCertificates();
                staticCertificateVerifier = new StaticNewAdvancedCertificateVerifier(trustedCertificates, new RawPublicKeyIdentity[0], null);
            }
        } catch (Exception e) {
            log.warn("Failed to initialize the LwM2M certificate verifier", e);
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
                        if (staticCertificateVerifier != null) {
                            HandshakeException exception = staticCertificateVerifier.verifyCertificate(cid, serverName, remotePeer, clientUsage, verifySubject, truncateCertificatePath, message).getException();
                            if (exception == null) {
                                try {
                                    String endpoint = config.getTrustSslCredentials().getValueFromSubjectNameByKey(cert.getSubjectX500Principal().getName(), "CN");
                                    if (StringUtils.isNotEmpty(endpoint)) {
                                        securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, CLIENT);
                                    }
                                } catch (LwM2MAuthException e) {
                                    log.trace("Certificate trust validation failed.", e);
                                }
                            } else {
                                log.trace("Certificate trust validation failed.", exception);
                            }
                        }
                        // if not trust or cert trust securityInfo == null
                        String strCert = SslUtil.getCertificateString(cert);
                        String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                        if (securityInfo == null || securityInfo.getMsg() == null) {
                            try {
                                securityInfo = securityInfoValidator.getEndpointSecurityInfoByCredentialsId(sha3Hash, CLIENT);
                            } catch (LwM2MAuthException e) {
                                log.trace("Failed find security info: {}", sha3Hash, e);
                            }
                        }
                        ValidateDeviceCredentialsResponse msg = securityInfo != null ? securityInfo.getMsg() : null;
                        if (msg != null && StringUtils.isNotEmpty(msg.getCredentials())) {
                            LwM2MClientCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MClientCredentials.class);
                            if (!credentials.getClient().getSecurityConfigClientMode().equals(LwM2MSecurityMode.X509)) {
                                continue;
                            }
                            X509ClientCredential config = (X509ClientCredential) credentials.getClient();
                            String certBody = config.getCert();
                            String endpoint = config.getEndpoint();
                            if (StringUtils.isBlank(certBody) || strCert.equals(certBody)) {
                                x509CredentialsFound = true;
                                DeviceProfile deviceProfile = msg.getDeviceProfile();
                                if (msg.hasDeviceInfo() && deviceProfile != null) {
                                    sessionStorage.put(endpoint, new TbX509DtlsSessionInfo(cert.getSubjectX500Principal().getName(), msg));
                                    try {
                                        securityStore.putX509(securityInfo);
                                    } catch (NonUniqueSecurityInfoException e) {
                                        log.trace("Failed to add security info: {}", securityInfo, e);
                                    }
                                    break;
                                }
                            } else {
                                log.trace("[{}][{}] Certificate mismatch. Expected: {}, Actual: {}", endpoint, sha3Hash, strCert, certBody);
                            }
                        }
                    } catch (CertificateEncodingException |
                            CertificateExpiredException |
                            CertificateNotYetValidException e) {
                        log.error(e.getMessage(), e);
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

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2MDtlsCertificateVerifier` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
