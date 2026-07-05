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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.util.SslUtil;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import javax.security.auth.x500.X500Principal;
import java.net.InetSocketAddress;
import java.security.cert.CertPath;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`TbCoapDtlsCertificateVerifier` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Data
public class TbCoapDtlsCertificateVerifier implements NewAdvancedCertificateVerifier {

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private final TbCoapDtlsSessionInMemoryStorage tbCoapDtlsSessionInMemoryStorage;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private TransportService transportService;
    private TbServiceInfoProvider serviceInfoProvider;
    /**
     * 是否满足客户端条件。
     */
    private boolean skipValidityCheckForClientCert;

    /**
     * 功能：创建 `TbCoapDtlsCertificateVerifier` 实例，并初始化必要字段。
     * 参数：
     * - `transportService`：服务对象。
     * - `serviceInfoProvider`：服务对象。
     * - `dtlsSessionInactivityTimeout`：会话对象。
     * - `dtlsSessionReportTimeout`：会话对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbCoapDtlsCertificateVerifier(TransportService transportService, TbServiceInfoProvider serviceInfoProvider, long dtlsSessionInactivityTimeout, long dtlsSessionReportTimeout, boolean skipValidityCheckForClientCert) {
        this.transportService = transportService;
        this.serviceInfoProvider = serviceInfoProvider;
        this.skipValidityCheckForClientCert = skipValidityCheckForClientCert;
        this.tbCoapDtlsSessionInMemoryStorage = new TbCoapDtlsSessionInMemoryStorage(dtlsSessionInactivityTimeout, dtlsSessionReportTimeout);
    }

    /**
     * 功能：获取证书。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<CertificateType> getSupportedCertificateTypes() {
        return Collections.singletonList(CertificateType.X_509);
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
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName, InetSocketAddress remotePeer, boolean clientUsage, boolean verifySubject, boolean truncateCertificatePath, CertificateMessage message) {
        try {
            CertPath certpath = message.getCertificateChain();
            X509Certificate[] chain = certpath.getCertificates().toArray(new X509Certificate[0]);
            for (X509Certificate cert : chain) {
                try {
                    if (!skipValidityCheckForClientCert) {
                        cert.checkValidity();
                    }

                    String strCert = SslUtil.getCertificateString(cert);
                    String sha3Hash = EncryptionUtil.getSha3Hash(strCert);
                    final ValidateDeviceCredentialsResponse[] deviceCredentialsResponse = new ValidateDeviceCredentialsResponse[1];
                    CountDownLatch latch = new CountDownLatch(1);
                    transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceX509CertRequestMsg.newBuilder().setHash(sha3Hash).build(),
                            new TransportServiceCallback<>() {
                                @Override
                                public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                                    if (!StringUtils.isEmpty(msg.getCredentials())) {
                                        deviceCredentialsResponse[0] = msg;
                                    }
                                    latch.countDown();
                                }

                                @Override
                                public void onError(Throwable e) {
                                    log.error(e.getMessage(), e);
                                    latch.countDown();
                                }
                            });
                    latch.await(10, TimeUnit.SECONDS);
                    ValidateDeviceCredentialsResponse msg = deviceCredentialsResponse[0];
                    if (msg != null && strCert.equals(msg.getCredentials())) {
                        DeviceProfile deviceProfile = msg.getDeviceProfile();
                        if (msg.hasDeviceInfo() && deviceProfile != null) {
                            tbCoapDtlsSessionInMemoryStorage.put(remotePeer, new TbCoapDtlsSessionInfo(msg, deviceProfile));
                        }
                        break;
                    }
                } catch (InterruptedException |
                        CertificateEncodingException |
                        CertificateExpiredException |
                        CertificateNotYetValidException e) {
                    log.error(e.getMessage(), e);
                    AlertMessage alert = new AlertMessage(AlertMessage.AlertLevel.FATAL, AlertMessage.AlertDescription.BAD_CERTIFICATE);
                    throw new HandshakeException("Certificate chain could not be validated", alert);
                }
            }
            return new CertificateVerificationResult(cid, certpath, null);
        } catch (HandshakeException e) {
            log.trace("Certificate validation failed!", e);
            return new CertificateVerificationResult(cid, e, null);
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

    /**
     * 功能：获取`Tb Coap Dtls Sessions Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<InetSocketAddress, TbCoapDtlsSessionInfo> getTbCoapDtlsSessionsMap() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionsMap();
    }

    /**
     * 功能：删除或清理超时时间。
     * 参数：无。
     * 返回：无。
     */
    public void evictTimeoutSessions() {
        tbCoapDtlsSessionInMemoryStorage.evictTimeoutSessions();
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getDtlsSessionReportTimeout() {
        return tbCoapDtlsSessionInMemoryStorage.getDtlsSessionReportTimeout();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbCoapDtlsCertificateVerifier` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
