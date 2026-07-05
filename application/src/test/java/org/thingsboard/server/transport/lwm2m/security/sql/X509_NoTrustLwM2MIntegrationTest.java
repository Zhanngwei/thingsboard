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
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import javax.servlet.http.HttpServletResponse;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.x509;
import static org.eclipse.leshan.client.object.Security.x509Bootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. 类目的：`X509_NoTrustLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class X509_NoTrustLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    /**
     * 功能：验证`With X509 No Trust Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithX509NoTrustConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO;
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Security security = x509(SECURE_URI,
                shortServerId,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509_Trust_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    /**
     * 功能：验证公钥相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithX509NoTrustValidationPublicKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO + "BadPublicKey";
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Hex.encodeHexString(certificate.getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "LwM2M client X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    /**
     * 功能：验证私钥相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithX509NoTrustValidationPrivateKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO + "BadPrivateKey";
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, true);
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Bootstrap server client X509 secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    // Bootstrap + Lwm2m
    /**
     * 功能：验证`With X509 No Trust Connect Bs Success Update Two Sections Bootstrap And Lm2m Connect Lwm2m Success`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testWithX509NoTrustConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO;
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Security security = x509Bootstrap(SECURE_URI_BS,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509CertBs.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        this.basicTestConnection(security,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509NoTrust two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`X509_NoTrustLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
