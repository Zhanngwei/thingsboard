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

import org.apache.commons.codec.binary.Base64;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import javax.servlet.http.HttpServletResponse;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.rpk;
import static org.eclipse.leshan.client.object.Security.rpkBootstrap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.BOTH;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. 类目的：`RpkLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class RpkLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    //Lwm2m only
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testWithRpkConnectLwm2mSuccess` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testWithRpkConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Security securityBs = rpk(SECURE_URI,
                shortServerId,
                certificate.getPublicKey().getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getPublicKey().getEncoded());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, false);
        this.basicTestConnection(securityBs,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportConfiguration,
                "await on client state (Rpk_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                false,
                ON_REGISTRATION_SUCCESS,
                true);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testWithRpkValidationPublicKeyBase64format_BAD_REQUEST` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testWithRpkValidationPublicKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK + "BadPublicKey";
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Hex.encodeHexString(certificate.getPublicKey().getEncoded()));
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, false);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "LwM2M client RPK key must be in standard [RFC7250] and support only EC algorithm and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testWithRpkValidationPrivateKeyBase64format_BAD_REQUEST` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testWithRpkValidationPrivateKeyBase64format_BAD_REQUEST() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK + "BadPrivateKey";
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, true);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        createDeviceProfile(transportConfiguration);
        MvcResult result = createDeviceWithMvcResult(deviceCredentials, clientEndpoint);
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, result.getResponse().getStatus());
        String msgExpected = "Bootstrap server client RPK secret key must be in PKCS#8 format (DER encoding, standard [RFC5958]) and then encoded to Base64 format!";
        assertTrue(result.getResponse().getContentAsString().contains(msgExpected));
    }

    // Bootstrap + Lwm2m
    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testWithRpkConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess` 对应的传输层测试或适配类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 JUnit 测试生命周期创建，随单个测试方法准备和清理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void testWithRpkConnectBsSuccess_UpdateTwoSectionsBootstrapAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK_BS;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Security securityBs = rpkBootstrap(SECURE_URI_BS,
                certificate.getPublicKey().getEncoded(),
                privateKey.getEncoded(),
                serverX509CertBs.getPublicKey().getEncoded());
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, BOTH));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, clientPrivateKeyFromCertTrust, certificate, RPK, false);
        this.basicTestConnection(securityBs,
                deviceCredentials,
                COAP_CONFIG_BS,
                clientEndpoint,
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                transportConfiguration,
                "await on client state (RpkBS two section)",
                expectedStatusesRegistrationBsSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RpkLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
