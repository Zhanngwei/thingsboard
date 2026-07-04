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
 * 测试目标：验证 {@code CertPemCredentialsTest} 覆盖的 凭证解析组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code CertPemCredentials}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：内存 fixture、参数化数据源，以及测试体按需创建的 Mockito mock/spy；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class CertPemCredentialsTest {

    /** 测试常量字段：{@code PASS} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String PASS = "test";
    /** 测试常量字段：{@code EMPTY_PASS} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EMPTY_PASS = "";
    /** 测试常量字段：{@code RSA} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String RSA = "RSA";
    /** 测试常量字段：{@code EC} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EC = "EC";

    /**
     * 测试方法：覆盖 {@code testChainOfCertificates} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void testChainOfCertificates() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code testSingleCertificate} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void testSingleCertificate() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        String fileContent = fileContent("pem/tb-cloud.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(1, x509Certificates.size());
        Assert.assertEquals("CN=*.thingsboard.cloud, O=\"ThingsBoard, Inc.\", ST=New York, C=US",
                x509Certificates.get(0).getSubjectDN().getName());
    }

    /**
     * 测试方法：覆盖 {@code testEmptyFileContent} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @Test
    public void testEmptyFileContent() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        String fileContent = fileContent("pem/empty.pem");

        List<X509Certificate> x509Certificates = SslUtil.readCertFile(fileContent);

        Assert.assertEquals(0, x509Certificates.size());
    }

    /** 参数源方法：{@code testLoadKeyStore} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    private static Stream<Arguments> testLoadKeyStore() {
        return Stream.of(
                Arguments.of("pem/rsa_cert.pem", "pem/rsa_key.pem", EMPTY_PASS, RSA),
                Arguments.of("pem/rsa_encrypted_cert.pem", "pem/rsa_encrypted_key.pem", PASS, RSA),
                Arguments.of("pem/rsa_encrypted_traditional_cert.pem", "pem/rsa_encrypted_traditional_key.pem", PASS, RSA),
                Arguments.of("pem/ec_cert.pem", "pem/ec_key.pem", EMPTY_PASS, EC)
        );
    }

    /**
     * 测试方法：覆盖 {@code testLoadKeyStore} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @ParameterizedTest
    @MethodSource
    public void testLoadKeyStore(String certPath, String keyPath, String password, String algorithm) throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 辅助方法：{@code fileContent} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
