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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.CLIENT;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mSecurityStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbLwM2mSecurityStore implements TbMainSecurityStore {

    /**
     * 存储组件，表示当前对象的对应属性。
     */
    private final TbEditableSecurityStore securityStore;
    private final LwM2mCredentialsSecurityInfoValidator validator;
    private final ConcurrentMap<String, Set<String>> endpointRegistrations = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `TbLwM2mSecurityStore` 实例，并初始化必要字段。
     * 参数：
     * - `securityStore`：`securityStore` 参数。
     * - `validator`：`validator` 参数。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mSecurityStore(TbEditableSecurityStore securityStore, LwM2mCredentialsSecurityInfoValidator validator) {
        this.securityStore = securityStore;
        this.validator = validator;
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        return securityStore.getTbLwM2MSecurityInfoByEndpoint(endpoint);
    }

    /**
     * @param endpoint
     * @return : If SecurityMode == NO_SEC:
     * return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
     * SecurityMode.NO_SEC.toString().getBytes());
     */
    /**
     * 功能：获取`By Endpoint`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        SecurityInfo securityInfo = securityStore.getByEndpoint(endpoint);
        if (securityInfo == null) {
            securityInfo = fetchAndPutSecurityInfo(endpoint);
        } else if (securityInfo.usePSK() && securityInfo.getEndpoint().equals(SecurityMode.NO_SEC.toString())
                && securityInfo.getIdentity().equals(SecurityMode.NO_SEC.toString())
                && Arrays.equals(SecurityMode.NO_SEC.toString().getBytes(), securityInfo.getPreSharedKey())) {
            return null;
        }
        return securityInfo;
    }

    /**
     * 功能：获取`By Identity`。
     * 参数：
     * - `pskIdentity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        SecurityInfo securityInfo = securityStore.getByIdentity(pskIdentity);
        if (securityInfo == null) {
            try {
                securityInfo = fetchAndPutSecurityInfo(pskIdentity);
            } catch (LwM2MAuthException e) {
                log.trace("Registration failed: No pre-shared key found for [identity: {}]", pskIdentity);
                return null;
            }
        }
        return securityInfo;
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `credentialsId`：凭据ID。
     * 返回：处理结果。
     */
    public SecurityInfo fetchAndPutSecurityInfo(String credentialsId) {
        TbLwM2MSecurityInfo securityInfo = validator.getEndpointSecurityInfoByCredentialsId(credentialsId, CLIENT);
        doPut(securityInfo);
        return securityInfo != null ? securityInfo.getSecurityInfo() : null;
    }

    /**
     * 功能：执行 `doPut` 对应的处理。
     * 参数：
     * - `securityInfo`：`securityInfo` 参数。
     * 返回：无。
     */
    private void doPut(TbLwM2MSecurityInfo securityInfo) {
        if (securityInfo != null) {
            try {
                securityStore.put(securityInfo);
            } catch (NonUniqueSecurityInfoException e) {
                log.trace("Failed to add security info: {}", securityInfo, e);
            }
        }
    }

    /**
     * 功能：执行 `putX509` 对应的处理。
     * 参数：
     * - `securityInfo`：`securityInfo` 参数。
     * 返回：无。
     */
    @Override
    public void putX509(TbLwM2MSecurityInfo securityInfo) throws NonUniqueSecurityInfoException {
        securityStore.put(securityInfo);
    }

    /**
     * 功能：保存或创建`X509`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `registrationId`：`registrationId`ID。
     * 返回：无。
     */
    @Override
    public void registerX509(String endpoint, String registrationId) {
        endpointRegistrations.computeIfAbsent(endpoint, ep -> new HashSet<>()).add(registrationId);
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * - `registrationId`：`registrationId`ID。
     * 返回：无。
     */
    @Override
    public void remove(String endpoint, String registrationId) {
        Set<String> epRegistrationIds = endpointRegistrations.get(endpoint);
        boolean shouldRemove;
        if (epRegistrationIds == null) {
            shouldRemove = true;
        } else {
            epRegistrationIds.remove(registrationId);
            shouldRemove = epRegistrationIds.isEmpty();
        }
        if (shouldRemove) {
            securityStore.remove(endpoint);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mSecurityStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
