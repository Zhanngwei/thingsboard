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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.EditableBootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.eclipse.leshan.server.security.BootstrapSecurityStore;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mBootstrapTransportComponent;
import org.thingsboard.server.transport.lwm2m.bootstrap.secure.LwM2MBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.secure.LwM2mCredentialsSecurityInfoValidator;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mSessionMsgListener;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MAuthException;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mTypeServer.BOOTSTRAP;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

/**
 * 中文说明：
 * 1. `LwM2MBootstrapSecurityStore` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BootstrapSecurityStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Service("LwM2MBootstrapSecurityStore")
@TbLwM2mBootstrapTransportComponent
public class LwM2MBootstrapSecurityStore implements BootstrapSecurityStore {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final EditableBootstrapConfigStore bootstrapConfigStore;

    /**
     * 凭据，用于认证或安全校验。
     */
    private final LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final LwM2mTransportServerHelper helper;
    private final Map<String /* endpoint */, TransportProtos.SessionInfoProto> bsSessions = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `LwM2MBootstrapSecurityStore` 实例，并初始化必要字段。
     * 参数：
     * - `bootstrapConfigStore`：配置对象。
     * - `lwM2MCredentialsSecurityInfoValidator`：`lwM2MCredentialsSecurityInfoValidator` 参数。
     * - `context`：处理上下文。
     * - `helper`：`helper` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MBootstrapSecurityStore(EditableBootstrapConfigStore bootstrapConfigStore, LwM2mCredentialsSecurityInfoValidator lwM2MCredentialsSecurityInfoValidator, LwM2mTransportContext context, LwM2mTransportServerHelper helper) {
        this.bootstrapConfigStore = bootstrapConfigStore;
        this.lwM2MCredentialsSecurityInfoValidator = lwM2MCredentialsSecurityInfoValidator;
        this.context = context;
        this.helper = helper;
    }

    /**
     * 功能：获取`All By Endpoint`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public Iterator<SecurityInfo> getAllByEndpoint(String endpoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endpoint, BOOTSTRAP);
            SecurityInfo securityInfo = this.addValueToStore(store, endpoint);
            return securityInfo == null ? null : Collections.singletonList(store.getSecurityInfo()).iterator();
    }

    /**
     * 功能：获取`By Identity`。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByIdentity(String identity) {
        try {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(identity, BOOTSTRAP);
            if (store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
                /* add value to store  from BootstrapJson */
                this.setBootstrapConfigSecurityInfo(store);
                BootstrapConfig bsConfig = store.getBootstrapConfig();
                if (bsConfig.security != null) {
                    try {
                        bootstrapConfigStore.add(store.getEndpoint(), bsConfig);
                    } catch (InvalidConfigurationException e) {
                        log.trace("Invalid Bootstrap Configuration", e);
                        return null;
                    }
                }
            }
            return store.getSecurityInfo();
        } catch (LwM2MAuthException e) {
            log.trace("Bootstrap Registration failed: No pre-shared key found for [identity: {}]", identity);
            return null;
        }
    }

    /**
     * 功能：获取`X509 By Endpoint`。
     * 参数：
     * - `endPoint`：`endPoint` 参数。
     * 返回：处理结果。
     */
    public TbLwM2MSecurityInfo getX509ByEndpoint(String endPoint) {
            TbLwM2MSecurityInfo store = lwM2MCredentialsSecurityInfoValidator.getEndpointSecurityInfoByCredentialsId(endPoint, BOOTSTRAP);
            this.addValueToStore(store, store.getEndpoint());
            return store;
    }


    /**
     * 功能：更新配置。
     * 参数：
     * - `store`：`store` 参数。
     * 返回：无。
     */
    private void setBootstrapConfigSecurityInfo(TbLwM2MSecurityInfo store) {
        /* BootstrapConfig */
        LwM2MBootstrapConfig lwM2MBootstrapConfig = this.getParametersBootstrap(store);
        if (lwM2MBootstrapConfig != null) {
            BootstrapConfig bootstrapConfig = lwM2MBootstrapConfig.getLwM2MBootstrapConfig();
            store.setBootstrapConfig(bootstrapConfig);
        }
    }

    /**
     * 功能：获取参数集合。
     * 参数：
     * - `store`：`store` 参数。
     * 返回：处理结果。
     */
    private LwM2MBootstrapConfig getParametersBootstrap(TbLwM2MSecurityInfo store) {
        LwM2MBootstrapConfig lwM2MBootstrapConfig = store.getBootstrapCredentialConfig();
        if (lwM2MBootstrapConfig != null) {
            UUID sessionUUiD = UUID.randomUUID();
            TransportProtos.SessionInfoProto sessionInfo = helper.getValidateSessionInfo(store.getMsg(), sessionUUiD.getMostSignificantBits(), sessionUUiD.getLeastSignificantBits());
            bsSessions.put(store.getEndpoint(), sessionInfo);
            context.getTransportService().registerAsyncSession(sessionInfo, new LwM2mSessionMsgListener(null, null, null, sessionInfo, context.getTransportService()));
            if (this.getValidatedSecurityMode(lwM2MBootstrapConfig)) {
                return lwM2MBootstrapConfig;
            } else {
                log.error(" [{}] Different values SecurityMode between of client and profile.", store.getEndpoint());
                log.error("{} getParametersBootstrap: [{}] Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR, store.getEndpoint());
                String logMsg = String.format("%s: Different values SecurityMode between of client and profile.", LOG_LWM2M_ERROR);
                helper.sendParametersOnThingsboardTelemetry(helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), sessionInfo);
                return null;
            }
        }
        log.error("Unable to decode Json or Certificate for [{}]", store.getEndpoint());
        return null;
    }

    /**
     * Bootstrap security have to sync between (bootstrapServer in credential and  bootstrapServer in profile)
     * and (lwm2mServer  in credential and lwm2mServer  in profile
     *
     * @return false if not sync between SecurityMode of Bootstrap credential and profile
     */
    /**
     * 功能：获取安全模式。
     * 参数：
     * - `lwM2MBootstrapConfig`：配置对象。
     * 返回：判断结果。
     */
    private boolean getValidatedSecurityMode(LwM2MBootstrapConfig lwM2MBootstrapConfig) {
        LwM2MSecurityMode bootstrapServerSecurityMode = lwM2MBootstrapConfig.getBootstrapServer().getSecurityMode();
        LwM2MSecurityMode lwm2mServerSecurityMode = lwM2MBootstrapConfig.getLwm2mServer().getSecurityMode();
        AtomicBoolean validBs = new AtomicBoolean(true);
        AtomicBoolean validLw = new AtomicBoolean(true);
        lwM2MBootstrapConfig.getServerConfiguration().forEach(serverCredential -> {
            if (((AbstractLwM2MBootstrapServerCredential) serverCredential).isBootstrapServerIs()) {
                if (!bootstrapServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validBs.set(false);
                }
            } else {
                if (!lwm2mServerSecurityMode.equals(serverCredential.getSecurityMode())) {
                    validLw.set(false);
                }
            }
        });
        return validBs.get() && validLw.get();
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public TransportProtos.SessionInfoProto getSessionByEndpoint(String endpoint) {
        return bsSessions.get(endpoint);
    }

    /**
     * 功能：删除或清理会话。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public TransportProtos.SessionInfoProto removeSessionByEndpoint(String endpoint) {
        return bsSessions.remove(endpoint);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public BootstrapConfig getBootstrapConfigByEndpoint(String endpoint) {
        return bootstrapConfigStore.getAll().get(endpoint);
    }

    /**
     * 功能：保存或创建值。
     * 参数：
     * - `store`：`store` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public SecurityInfo addValueToStore(TbLwM2MSecurityInfo store, String endpoint) {
        /* add value to store  from BootstrapJson */
        SecurityInfo securityInfo = null;
        if (store != null && store.getBootstrapCredentialConfig() != null && store.getSecurityMode() != null) {
            securityInfo = store.getSecurityInfo();
            this.setBootstrapConfigSecurityInfo(store);
            BootstrapConfig bsConfigNew = store.getBootstrapConfig();
            if (bsConfigNew != null) {
                try {
                    boolean bootstrapServerUpdateEnable = ((Lwm2mDeviceProfileTransportConfiguration) store.getDeviceProfile().getProfileData().getTransportConfiguration()).isBootstrapServerUpdateEnable();
                    if (!bootstrapServerUpdateEnable) {
                        Optional<Map.Entry<Integer, BootstrapConfig.ServerSecurity>> securities = bsConfigNew.security.entrySet().stream().filter(sec -> sec.getValue().bootstrapServer).findAny();
                        if (securities.isPresent()) {
                            bsConfigNew.security.entrySet().remove(securities.get());
                            int serverSortId = securities.get().getValue().serverId;
                            Optional<Map.Entry<Integer, BootstrapConfig.ServerConfig>> serverConfigs = bsConfigNew.servers.entrySet().stream().filter(serv -> (serv.getValue()).shortId == serverSortId).findAny();
                            if (serverConfigs.isPresent()) {
                                bsConfigNew.servers.entrySet().remove(serverConfigs.get());
                            }
                        }
                    }
                    for (String config : bootstrapConfigStore.getAll().keySet()) {
                        if (config.equals(endpoint)) {
                            bootstrapConfigStore.remove(config);
                        }
                    }
                    bootstrapConfigStore.add(endpoint, bsConfigNew);
                } catch (InvalidConfigurationException e) {
                    if (e.getMessage().contains("Psk identity") && e.getMessage().contains("already used for this bootstrap server")) {
                        log.trace("Invalid Bootstrap Configuration", e);
                    } else {
                        log.error("Invalid Bootstrap Configuration", e);
                    }
                }
            }
        }
        return securityInfo;
    }
}