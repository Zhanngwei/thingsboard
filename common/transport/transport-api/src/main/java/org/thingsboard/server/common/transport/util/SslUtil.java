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
 * 1. `SslUtil` 是 ThingsBoard Common Transport 中处理 `Ssl Util` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
