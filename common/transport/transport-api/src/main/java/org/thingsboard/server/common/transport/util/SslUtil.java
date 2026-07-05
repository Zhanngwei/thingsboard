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
package org.thingsboard.server.common.transport.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.msg.EncryptionUtil;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * @author Valerii Sosliuk
 */
/**
 * 中文说明：
 * 1. 类目的：`SslUtil` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Slf4j
public class SslUtil {

    /**
     * 功能：创建 `SslUtil` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private SslUtil() {
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `cert`：`cert` 参数。
     * 返回：文本结果。
     */
    public static String getCertificateString(Certificate cert)
            throws CertificateEncodingException {
        return EncryptionUtil.certTrimNewLines(Base64Utils.encodeToString(cert.getEncoded()));
    }

    /**
     * 功能：获取证书。
     * 参数：
     * - `chain`：`chain` 参数。
     * 返回：文本结果。
     */
    public static String getCertificateChainString(Certificate[] chain)
            throws CertificateEncodingException {
        String begin = "-----BEGIN CERTIFICATE-----";
        String end = "-----END CERTIFICATE-----";
        StringBuilder stringBuilder = new StringBuilder();
        for (Certificate cert: chain) {
            stringBuilder.append(begin).append(EncryptionUtil.certTrimNewLines(Base64Utils.encodeToString(cert.getEncoded()))).append(end).append("\n");
        }
        return stringBuilder.toString();
    }

    /**
     * 功能：执行 `readCertFile` 对应的处理。
     * 参数：
     * - `fileContent`：`fileContent` 参数。
     * 返回：处理结果。
     */
    public static X509Certificate readCertFile(String fileContent) {
        X509Certificate certificate = null;
        try {
            if (fileContent != null && !fileContent.trim().isEmpty()) {
                fileContent = fileContent.replace("-----BEGIN CERTIFICATE-----", "")
                        .replace("-----END CERTIFICATE-----", "")
                        .replaceAll("\\s", "");
                byte[] decoded = Base64.decodeBase64(fileContent);
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                try (InputStream inStream = new ByteArrayInputStream(decoded)) {
                    certificate = (X509Certificate) certFactory.generateCertificate(inStream);
                }
            }
        } catch (Exception ignored) {}
        return certificate;
    }

    /**
     * 功能：解析名称。
     * 参数：
     * - `certificate`：`certificate` 参数。
     * 返回：文本结果。
     */
    public static String parseCommonName(X509Certificate certificate) {
        X500Name x500name;
        try {
            x500name = new JcaX509CertificateHolder(certificate).getSubject();
        } catch (CertificateEncodingException e) {
            log.warn("Cannot parse CN from device certificate");
            throw new RuntimeException(e);
        }
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        return IETFUtils.valueToString(cn.getFirst().getValue());
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SslUtil` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
