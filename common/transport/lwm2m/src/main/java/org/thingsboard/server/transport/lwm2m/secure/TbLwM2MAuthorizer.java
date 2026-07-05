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
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.Authorizer;
import org.eclipse.leshan.server.security.SecurityChecker;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MDtlsSessionStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;

import java.util.Arrays;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2MAuthorizer` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@RequiredArgsConstructor
@TbLwM2mTransportComponent
@Slf4j
public class TbLwM2MAuthorizer implements Authorizer {

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    private final TbLwM2MDtlsSessionStore sessionStorage;
    private final TbMainSecurityStore securityStore;
    private final SecurityChecker securityChecker = new SecurityChecker();
    /**
     * 上下文，用于发起外部调用或协议交互。
     */
    private final LwM2mClientContext clientContext;

    /**
     * 功能：判断`Authorized`。
     * 参数：
     * - `request`：请求对象。
     * - `registration`：`registration` 参数。
     * - `senderIdentity`：实体对象。
     * 返回：判断结果。
     */
    @Override
    public Registration isAuthorized(UplinkRequest<?> request, Registration registration, Identity senderIdentity) {
        if (senderIdentity.isX509()) {
            TbX509DtlsSessionInfo sessionInfo = sessionStorage.get(registration.getEndpoint());
            if (sessionInfo != null) {
                if (sessionInfo.getX509CommonName().endsWith(senderIdentity.getX509CommonName())) {
                    clientContext.registerClient(registration, sessionInfo.getCredentials());
                    // X509 certificate is valid and matches endpoint.
                    return registration;
                } else {
                    // X509 certificate is not valid.
                    return null;
                }
            }
            // If session info is not found, this may be the trusted certificate, so we still need to check all other options below.
        }
        SecurityInfo expectedSecurityInfo;
            try {
                expectedSecurityInfo = securityStore.getByEndpoint(registration.getEndpoint());
                if (expectedSecurityInfo != null && expectedSecurityInfo.usePSK() && expectedSecurityInfo.getEndpoint().equals(SecurityMode.NO_SEC.toString())
                        && expectedSecurityInfo.getIdentity().equals(SecurityMode.NO_SEC.toString())
                        && Arrays.equals(SecurityMode.NO_SEC.toString().getBytes(), expectedSecurityInfo.getPreSharedKey())) {
                    expectedSecurityInfo = null;
                }
            } catch (LwM2MAuthException e) {
                log.info("Registration failed: FORBIDDEN, endpointId: [{}]", registration.getEndpoint());
                return null;
            }
        if (securityChecker.checkSecurityInfo(registration.getEndpoint(), senderIdentity, expectedSecurityInfo)) {
            return registration;
        } else {
            securityStore.remove(registration.getEndpoint(), registration.getId());
            return null;
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2MAuthorizer` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
