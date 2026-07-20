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
 * 1. `TbLwM2mSecurityStore` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TbMainSecurityStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
