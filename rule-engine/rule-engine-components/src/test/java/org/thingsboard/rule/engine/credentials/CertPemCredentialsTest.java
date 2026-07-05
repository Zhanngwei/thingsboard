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
package org.thingsboard.rule.engine.credentials;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.thingsboard.common.util.SslUtil;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.stream.Stream;

import static org.thingsboard.rule.engine.credentials.CertPemCredentials.CERT_ALIAS_PREFIX;
import static org.thingsboard.rule.engine.credentials.CertPemCredentials.PRIVATE_KEY_ALIAS;

/**
 * `CertPemCredentialsTest` 测试类，用于验证 `CertPemCredentials` 相关行为。
 */
public class CertPemCredentialsTest {

    /**
     * `PASS`常量，用于统一引用固定值。
     */
    private static final String PASS = "test";
    /**
     * `EMPTY_PASS`常量，用于统一引用固定值。
     */
    private static final String EMPTY_PASS = "";
    /**
     * `RSA`常量，用于统一引用固定值。
     */
    private static final String RSA = "RSA";
    /**
     * `EC`常量，用于统一引用固定值。
     */
    private static final String EC = "EC";

    /**
     * 功能：验证`Chain Of Certificates`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testChainOfCertificates() throws Exception {
        String fileContent = fileContent("pem/tb-cloud-chain.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(4, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
        Assert.assertEquals("CN=Sectigo ECC Organization Validation Secure Server CA, O=Sectigo Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(1).getSubjectDN().getName());
        Assert.assertEquals("CN=USERTrust ECC Certification Authority, O=The USERTRUST Network, L=Jersey City, ST=New Jersey, C=US",
                x509Certificates.get(2).getSubjectDN().getName());
        Assert.assertEquals("CN=AAA Certificate Services, O=Comodo CA Limited, L=Salford, ST=Greater Manchester, C=GB",
                x509Certificates.get(3).getSubjectDN().getName());
    }

    /**
     * 功能：验证证书相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSingleCertificate() throws Exception {
        String fileContent = fileContent("pem/tb-cloud.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(1, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
    }

    /**
     * 功能：验证文件相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testEmptyFileContent() throws Exception {
        String fileContent = fileContent("pem/empty.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(0, x509Certificates.size());
    }

    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> testLoadKeyStore() {
        return Stream.of(
                Arguments.of("pem/rsa_cert.pem", "pem/rsa_key.pem", EMPTY_PASS, RSA),
                Arguments.of("pem/rsa_encrypted_cert.pem", "pem/rsa_encrypted_key.pem", PASS, RSA),
                Arguments.of("pem/rsa_encrypted_traditional_cert.pem", "pem/rsa_encrypted_traditional_key.pem", PASS, RSA),
                Arguments.of("pem/ec_cert.pem", "pem/ec_key.pem", EMPTY_PASS, EC)
        );
    }

    /**
     * 功能：验证键相关场景。
     * 参数：
     * - `certPath`：文件或资源路径。
     * - `keyPath`：键。
     * - `password`：`password` 参数。
     * - `algorithm`：`algorithm` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource
    public void testLoadKeyStore(String certPath, String keyPath, String password, String algorithm) throws Exception {
        CertPemCredentials certPemCredentials = new CertPemCredentials();
        String certContent = fileContent(certPath);
        certPemCredentials.setCert(certContent);
        certPemCredentials.setPrivateKey(fileContent(keyPath));
        certPemCredentials.setPassword(password);
        KeyStore keyStore = certPemCredentials.loadKeyStore();
        Assertions.assertNotNull(keyStore);
        Key key = keyStore.getKey(PRIVATE_KEY_ALIAS, password.toCharArray());
        Assertions.assertNotNull(key);
        Assertions.assertEquals(algorithm, key.getAlgorithm());

        List<X509Certificate> certs = SslUtil.readCertFile(certContent);
        for (X509Certificate cert : certs) {
            String alias = CERT_ALIAS_PREFIX + cert.getIssuerDN().getName();
            Certificate certificate = keyStore.getCertificate(alias);
            Assertions.assertNotNull(certificate);
            Assertions.assertEquals(new String(cert.getEncoded()), new String(certificate.getEncoded()));
        }
    }

    /**
     * 功能：执行 `fileContent` 对应的处理。
     * 参数：
     * - `fileName`：名称。
     * 返回：文本结果。
     */
    private String fileContent(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }
}
/*
 * 本类总结：{@code CertPemCredentialsTest} 为 {@code CertPemCredentials} 的 凭证解析组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
