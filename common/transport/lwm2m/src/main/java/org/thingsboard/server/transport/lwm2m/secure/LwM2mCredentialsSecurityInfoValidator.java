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
import org.apache.commons.codec.DecoderException;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.credentials.LwM2MClientCredentials;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.eclipse.leshan.core.SecurityMode.PSK;
import static org.eclipse.leshan.core.SecurityMode.RPK;
import static org.eclipse.leshan.core.SecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mCredentialsSecurityInfoValidator` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
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
public class LwM2mCredentialsSecurityInfoValidator {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;

    /**
     * 功能：获取凭据。
     * 参数：
     * - `credentialsId`：凭据ID。
     * - `keyValue`：键。
     * 返回：处理结果。
     */
    public TbLwM2MSecurityInfo getEndpointSecurityInfoByCredentialsId(String credentialsId, LwM2mTypeServer keyValue) {
        CountDownLatch latch = new CountDownLatch(1);
        final TbLwM2MSecurityInfo[] resultSecurityStore = new TbLwM2MSecurityInfo[1];
        log.trace("Validating credentials [{}]", credentialsId);
        context.getTransportService().process(ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId(credentialsId).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        log.trace("Validated credentials: [{}] [{}]", credentialsId, msg);
                        resultSecurityStore[0] = createSecurityInfo(credentialsId, msg, keyValue);
                        latch.countDown();
                    }

                    @Override
                    public void onError(Throwable e) {
                        log.trace("[{}] [{}] Failed to process credentials ", credentialsId, e);
                        TbLwM2MSecurityInfo result = new TbLwM2MSecurityInfo();
                        result.setEndpoint(credentialsId);
                        resultSecurityStore[0] = result;
                        latch.countDown();
                    }
                });
        try {
            latch.await(config.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Failed to await credentials!", e);
        }

        TbLwM2MSecurityInfo securityInfo = resultSecurityStore[0];
        if (securityInfo.getSecurityMode() == null) {
            throw new LwM2MAuthException();
        }
        return securityInfo;
    }

    /**
     * Create new SecurityInfo
     *
     * @return SecurityInfo
     */
    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `msg`：待处理消息。
     * - `keyValue`：键。
     * 返回：处理结果。
     */
    private TbLwM2MSecurityInfo createSecurityInfo(String endpoint, ValidateDeviceCredentialsResponse msg, LwM2mTypeServer keyValue) {
        TbLwM2MSecurityInfo result = new TbLwM2MSecurityInfo();
        LwM2MClientCredentials credentials = JacksonUtil.fromString(msg.getCredentials(), LwM2MClientCredentials.class);
        if (credentials != null) {
            result.setMsg(msg);
            result.setDeviceProfile(msg.getDeviceProfile());
            result.setEndpoint(credentials.getClient().getEndpoint());
//            if ((keyValue.equals(CLIENT))) {
            switch (credentials.getClient().getSecurityConfigClientMode()) {
                case NO_SEC:
                    createClientSecurityInfoNoSec(result);
                    break;
                case PSK:
                    createClientSecurityInfoPSK(result, endpoint, credentials.getClient());
                    break;
                case RPK:
                    createClientSecurityInfoRPK(result, endpoint, credentials.getClient());
                    break;
                case X509:
                    createClientSecurityInfoX509(result, endpoint);
                    break;
                default:
                    break;
            }
            if (keyValue.equals(BOOTSTRAP)) {
                LwM2MBootstrapConfig bootstrapCredentialConfig = new LwM2MBootstrapConfig(((Lwm2mDeviceProfileTransportConfiguration) msg.getDeviceProfile().getProfileData().getTransportConfiguration()).getBootstrap(),
                        credentials.getBootstrap().getBootstrapServer(), credentials.getBootstrap().getLwm2mServer());
                result.setBootstrapCredentialConfig(bootstrapCredentialConfig);
            }
        }
        return result;
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：无。
     */
    private void createClientSecurityInfoNoSec(TbLwM2MSecurityInfo result) {
        result.setSecurityInfo(null);
        result.setSecurityMode(NO_SEC);
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `result`：`result` 参数。
     * - `endpoint`：`endpoint` 参数。
     * - `clientCredentialsConfig`：客户端对象。
     * 返回：无。
     */
    private void createClientSecurityInfoPSK(TbLwM2MSecurityInfo result, String endpoint, LwM2MClientCredential clientCredentialsConfig) {
        PSKClientCredential pskConfig = (PSKClientCredential) clientCredentialsConfig;
        if (StringUtils.isNotEmpty(pskConfig.getIdentity())) {
            try {
                if (pskConfig.getDecoded() != null && pskConfig.getDecoded().length > 0) {
                    endpoint = StringUtils.isNotEmpty(pskConfig.getEndpoint()) ? pskConfig.getEndpoint() : endpoint;
                    if (endpoint != null && !endpoint.isEmpty()) {
                        result.setSecurityInfo(SecurityInfo.newPreSharedKeyInfo(endpoint, pskConfig.getIdentity(), pskConfig.getDecoded()));
                        result.setSecurityMode(PSK);
                    }
                }
            } catch (IllegalArgumentException | DecoderException e) {
                log.error("Missing PSK key: " + e.getMessage());
            }
        } else {
            log.error("Missing PSK identity");
        }
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `result`：`result` 参数。
     * - `endpoint`：`endpoint` 参数。
     * - `clientCredentialsConfig`：客户端对象。
     * 返回：无。
     */
    private void createClientSecurityInfoRPK(TbLwM2MSecurityInfo result, String endpoint, LwM2MClientCredential clientCredentialsConfig) {
        RPKClientCredential rpkConfig = (RPKClientCredential) clientCredentialsConfig;
        try {
            if (rpkConfig.getDecoded() != null) {
                PublicKey key = SecurityUtil.publicKey.decode(rpkConfig.getDecoded());
                result.setSecurityInfo(SecurityInfo.newRawPublicKeyInfo(endpoint, key));
                result.setSecurityMode(RPK);
            } else {
                log.error("Missing RPK key");
            }
        } catch (IllegalArgumentException | IOException | GeneralSecurityException | DecoderException e) {
            log.error("RPK: Invalid security info content: " + e.getMessage());
        }
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `result`：`result` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    private void createClientSecurityInfoX509(TbLwM2MSecurityInfo result, String endpoint) {
        result.setSecurityInfo(SecurityInfo.newX509CertInfo(endpoint));
        result.setSecurityMode(X509);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mCredentialsSecurityInfoValidator` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
