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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.AfterStartUp;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportContext;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfigService;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MOtaUpdateService;
import org.thingsboard.server.transport.lwm2m.server.session.LwM2MSessionManager;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MClientStore;
import org.thingsboard.server.transport.lwm2m.server.store.TbMainSecurityStore;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.SecurityMode.NO_SEC;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mClientContextImpl` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class LwM2mClientContextImpl implements LwM2mClientContext {

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final LwM2mTransportContext context;
    private final LwM2MTransportServerConfig config;
    /**
     * 存储组件，表示当前对象的对应属性。
     */
    private final TbMainSecurityStore securityStore;
    private final TbLwM2MClientStore clientStore;
    /**
     * 会话，负责处理对应任务或消息。
     */
    private final LwM2MSessionManager sessionManager;
    private final TransportDeviceProfileCache deviceProfileCache;
    /**
     * 配置，提供当前类调用的业务操作。
     */
    private final LwM2MModelConfigService modelConfigService;

    /**
     * 消息，负责处理对应任务或消息。
     */
    @Autowired
    @Lazy
    private LwM2mUplinkMsgHandler defaultLwM2MUplinkMsgHandler;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private LwM2MOtaUpdateService otaUpdateService;

    private final Map<String, LwM2mClient> lwM2mClientsByEndpoint = new ConcurrentHashMap<>();
    private final Map<String, LwM2mClient> lwM2mClientsByRegistrationId = new ConcurrentHashMap<>();
    private final Map<UUID, Lwm2mDeviceProfileTransportConfiguration> profiles = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterStartUp(order = AfterStartUp.BEFORE_TRANSPORT_SERVICE)
    public void init() {
        String nodeId = context.getNodeId();
        Set<LwM2mClient> fetchedClients = clientStore.getAll();
        log.debug("Fetched clients from store: {}", fetchedClients);
        fetchedClients.forEach(client -> {
            lwM2mClientsByEndpoint.put(client.getEndpoint(), client);
            try {
                client.lock();
                updateFetchedClient(nodeId, client);
            } finally {
                client.unlock();
            }
        });
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2mClient getClientByEndpoint(String endpoint) {
        return lwM2mClientsByEndpoint.computeIfAbsent(endpoint, ep -> {
            LwM2mClient client = clientStore.get(ep);
            String nodeId = context.getNodeId();
            if (client == null) {
                log.info("[{}] initialized new client.", endpoint);
                client = new LwM2mClient(nodeId, ep);
            } else {
                log.debug("[{}] fetched client from store: {}", endpoint, client);
                updateFetchedClient(nodeId, client);
            }
            return client;
        });
    }

    /**
     * 功能：更新客户端。
     * 参数：
     * - `nodeId`：节点实例ID。
     * - `client`：客户端对象。
     * 返回：无。
     */
    private void updateFetchedClient(String nodeId, LwM2mClient client) {
        boolean updated = false;
        if (client.getRegistration() != null) {
            lwM2mClientsByRegistrationId.put(client.getRegistration().getId(), client);
        }
        if (client.getSession() != null) {
            client.refreshSessionId(nodeId);
            sessionManager.register(client.getSession());
            updated = true;
        }
        if (updated) {
            clientStore.put(client);
        }
    }

    /**
     * 功能：执行 `register` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<TransportProtos.SessionInfoProto> register(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        TransportProtos.SessionInfoProto oldSession;
        client.lock();
        try {
            if (LwM2MClientState.UNREGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            oldSession = client.getSession();
            TbLwM2MSecurityInfo securityInfo = securityStore.getTbLwM2MSecurityInfoByEndpoint(client.getEndpoint());
            if (securityInfo.getSecurityMode() != null) {
                if (SecurityMode.X509.equals(securityInfo.getSecurityMode())) {
                    securityStore.registerX509(registration.getEndpoint(), registration.getId());
                }
                if (securityInfo.getDeviceProfile() != null) {
                    profileUpdate(securityInfo.getDeviceProfile());
                    if (securityInfo.getSecurityInfo() != null) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else if (NO_SEC.equals(securityInfo.getSecurityMode())) {
                        client.init(securityInfo.getMsg(), UUID.randomUUID());
                    } else {
                        throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                    }
                } else {
                    throw new RuntimeException(String.format("Registration failed: device %s not found.", client.getEndpoint()));
                }
            } else {
                throw new RuntimeException(String.format("Registration failed: FORBIDDEN, endpointId: %s", client.getEndpoint()));
            }
            client.setRegistration(registration);
            this.lwM2mClientsByRegistrationId.put(registration.getId(), client);
            client.setState(LwM2MClientState.REGISTERED);
            onUplink(client);
            if (!compareAndSetSleepFlag(client, false)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
        return Optional.ofNullable(oldSession);
    }

    /**
     * 功能：执行 `asleep` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    @Override
    public boolean asleep(LwM2mClient client) {
        boolean changed = compareAndSetSleepFlag(client, true);
        if (changed) {
            log.debug("[{}] client is sleeping", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is sleeping!");
        }
        return changed;
    }

    /**
     * 功能：执行 `awake` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    @Override
    public boolean awake(LwM2mClient client) {
        onUplink(client);
        boolean changed = compareAndSetSleepFlag(client, false);
        if (changed) {
            log.debug("[{}] client is awake", client.getEndpoint());
            context.getTransportService().log(client.getSession(), "Info : Client is awake!");
            sendMsgsAfterSleeping(client);
        }
        return changed;
    }

    /**
     * 功能：执行 `compareAndSetSleepFlag` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `sleeping`：`sleeping` 参数。
     * 返回：判断结果。
     */
    private boolean compareAndSetSleepFlag(LwM2mClient client, boolean sleeping) {
        if (sleeping == client.isAsleep()) {
            log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
            return false;
        }
        client.lock();
        try {
            if (sleeping == client.isAsleep()) {
                log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                return false;
            } else {
                PowerMode powerMode = getPowerMode(client);
                if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                    log.trace("[{}] Switch sleeping from: {} to: {}", client.getEndpoint(), client.isAsleep(), sleeping);
                    client.setAsleep(sleeping);
                    update(client);
                    return true;
                } else {
                    return false;
                }
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：更新`Registration`。
     * 参数：
     * - `client`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    @Override
    public void updateRegistration(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            client.setRegistration(registration);
            if (!awake(client)) {
                clientStore.put(client);
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：执行 `unregister` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    @Override
    public void unregister(LwM2mClient client, Registration registration) throws LwM2MClientStateException {
        client.lock();
        try {
            if (!LwM2MClientState.REGISTERED.equals(client.getState())) {
                throw new LwM2MClientStateException(client.getState(), "Client is in invalid state.");
            }
            lwM2mClientsByRegistrationId.remove(registration.getId());
            Registration currentRegistration = client.getRegistration();
            if (currentRegistration.getId().equals(registration.getId())) {
                client.setState(LwM2MClientState.UNREGISTERED);
                lwM2mClientsByEndpoint.remove(client.getEndpoint());
//                TODO: change tests to use new certificate.
//                this.securityStore.remove(client.getEndpoint(), registration.getId());
                clientStore.remove(client.getEndpoint());
                modelConfigService.removeUpdates(client.getEndpoint());
                UUID profileId = client.getProfileId();
                if (profileId != null) {
                    Optional<LwM2mClient> otherClients = lwM2mClientsByRegistrationId.values().stream().filter(e -> e.getProfileId().equals(profileId)).findFirst();
                    if (otherClients.isEmpty()) {
                        profiles.remove(profileId);
                    }
                }
            } else {
                throw new LwM2MClientStateException(client.getState(), "Client has different registration.");
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `sessionInfo`：会话对象。
     * 返回：处理结果。
     */
    @Override
    public LwM2mClient getClientBySessionInfo(TransportProtos.SessionInfoProto sessionInfo) {
        LwM2mClient lwM2mClient = null;
        UUID sessionId = new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
        Predicate<LwM2mClient> isClientFilter =
                c -> c.getSession() != null && sessionId.equals((new UUID(c.getSession().getSessionIdMSB(), c.getSession().getSessionIdLSB())));
        if (this.lwM2mClientsByEndpoint.size() > 0) {
            lwM2mClient = this.lwM2mClientsByEndpoint.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null && this.lwM2mClientsByRegistrationId.size() > 0) {
            lwM2mClient = this.lwM2mClientsByRegistrationId.values().stream().filter(isClientFilter).findAny().orElse(null);
        }
        if (lwM2mClient == null) {
            log.error("[{}] Failed to lookup client by session id.", sessionId);
        }
        return lwM2mClient;
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `client`：客户端对象。
     * - `keyName`：名称。
     * 返回：文本结果。
     */
    @Override
    public String getObjectIdByKeyNameFromProfile(LwM2mClient client, String keyName) {
        Lwm2mDeviceProfileTransportConfiguration profile = getProfile(client.getProfileId());
        for (Map.Entry<String, String> entry : profile.getObserveAttr().getKeyName().entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (v.equals(keyName) && client.isValidObjectVersion(k).isEmpty()) {
                return k;
            }
        }
        throw new IllegalArgumentException(keyName + " is not configured in the device profile!");
    }

    /**
     * 功能：获取`Registration`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：处理结果。
     */
    public Registration getRegistration(String registrationId) {
        return this.lwM2mClientsByRegistrationId.get(registrationId).getRegistration();
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `registration`：`registration` 参数。
     * - `credentials`：`credentials` 参数。
     * 返回：无。
     */
    @Override
    public void registerClient(Registration registration, ValidateDeviceCredentialsResponse credentials) {
        LwM2mClient client = getClientByEndpoint(registration.getEndpoint());
        client.init(credentials, UUID.randomUUID());
        lwM2mClientsByRegistrationId.put(registration.getId(), client);
        profileUpdate(credentials.getDeviceProfile());
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    @Override
    public void update(LwM2mClient client) {
        client.lock();
        try {
            if (client.getState().equals(LwM2MClientState.REGISTERED)) {
                clientStore.put(client);
            } else {
                log.error("[{}] Client is in invalid state: {}!", client.getEndpoint(), client.getState());
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：发送或提交`Msgs After Sleeping`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * 返回：无。
     */
    @Override
    public void sendMsgsAfterSleeping(LwM2mClient lwM2MClient) {
        if (LwM2MClientState.REGISTERED.equals(lwM2MClient.getState())) {
            PowerMode powerMode = getPowerMode(lwM2MClient);
            if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                modelConfigService.sendUpdates(lwM2MClient);
                defaultLwM2MUplinkMsgHandler.initAttributes(lwM2MClient, false);
                TransportProtos.TransportToDeviceActorMsg persistentRpcRequestMsg = TransportProtos.TransportToDeviceActorMsg
                        .newBuilder()
                        .setSessionInfo(lwM2MClient.getSession())
                        .setSendPendingRPC(TransportProtos.SendPendingRPCMsg.newBuilder().build())
                        .build();
                context.getTransportService().process(persistentRpcRequestMsg, TransportServiceCallback.EMPTY);
                otaUpdateService.init(lwM2MClient);
            }
        }
    }

    /**
     * 功能：获取`Power Mode`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * 返回：处理结果。
     */
    private PowerMode getPowerMode(LwM2mClient lwM2MClient) {
        PowerMode powerMode = lwM2MClient.getPowerMode();
        if (powerMode == null) {
            Lwm2mDeviceProfileTransportConfiguration deviceProfile = getProfile(lwM2MClient.getProfileId());
            powerMode = deviceProfile.getClientLwM2mSettings().getPowerMode();
        }
        return powerMode;
    }

    /**
     * 功能：获取`Lw M2m Clients`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Collection<LwM2mClient> getLwM2mClients() {
        return lwM2mClientsByEndpoint.values();
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `profileId`：配置ID。
     * 返回：处理结果。
     */
    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(UUID profileId) {
        return doGetAndCache(profileId);
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    @Override
    public Lwm2mDeviceProfileTransportConfiguration getProfile(Registration registration) {
        UUID profileId = getClientByEndpoint(registration.getEndpoint()).getProfileId();
        return doGetAndCache(profileId);
    }

    /**
     * 功能：执行 `doGetAndCache` 对应的处理。
     * 参数：
     * - `profileId`：配置ID。
     * 返回：处理结果。
     */
    private Lwm2mDeviceProfileTransportConfiguration doGetAndCache(UUID profileId) {

        Lwm2mDeviceProfileTransportConfiguration result = profiles.get(profileId);
        if (result == null) {
            log.debug("Fetching profile [{}]", profileId);
            DeviceProfile deviceProfile = deviceProfileCache.get(new DeviceProfileId(profileId));
            if (deviceProfile != null) {
                result = profileUpdate(deviceProfile);
            } else {
                log.warn("Device profile was not found! Most probably device profile [{}] has been removed from the database.", profileId);
            }
        }
        return result;
    }

    /**
     * 功能：执行 `profileUpdate` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public Lwm2mDeviceProfileTransportConfiguration profileUpdate(DeviceProfile deviceProfile) {
        Lwm2mDeviceProfileTransportConfiguration clientProfile = LwM2MTransportUtil.toLwM2MClientProfile(deviceProfile);
        profiles.put(deviceProfile.getUuidId(), clientProfile);
        return clientProfile;
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `client`：客户端对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<String> getSupportedIdVerInClient(LwM2mClient client) {
        Set<String> clientObjects = ConcurrentHashMap.newKeySet();
        Arrays.stream(client.getRegistration().getObjectLinks()).forEach(link -> {
            LwM2mPath pathIds = new LwM2mPath(link.getUriReference());
            if (!pathIds.isRoot()) {
                clientObjects.add(convertObjectIdToVersionedId(link.getUriReference(), client.getRegistration()));
            }
        });
        return (clientObjects.size() > 0) ? clientObjects : null;
    }

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    @Override
    public LwM2mClient getClientByDeviceId(UUID deviceId) {
        return lwM2mClientsByRegistrationId.values().stream().filter(e -> deviceId.equals(e.getDeviceId())).findFirst().orElse(null);
    }

    /**
     * 功能：判断`Downlink Allowed`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    @Override
    public boolean isDownlinkAllowed(LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        OtherConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode) || otaUpdateService.isOtaDownloading(client)) {
            return true;
        }
        client.lock();
        long timeSinceLastUplink = System.currentTimeMillis() - client.getLastUplinkTime();
        try {
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }
                return timeSinceLastUplink <= psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                boolean allowed = timeSinceLastUplink <= pagingTransmissionWindow;
                if (!allowed) {
                    return client.checkFirstDownlink();
                } else {
                    return true;
                }
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：处理`on Uplink`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    @Override
    public void onUplink(LwM2mClient client) {
        PowerMode powerMode = client.getPowerMode();
        OtherConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            profileSettings = clientProfile.getClientLwM2mSettings();
            powerMode = profileSettings.getPowerMode();
            if (powerMode == null) {
                powerMode = PowerMode.DRX;
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode)) {
            client.updateLastUplinkTime();
            return;
        }
        client.lock();
        try {
            long uplinkTime = client.updateLastUplinkTime();
            long timeout;
            if (PowerMode.PSM.equals(powerMode)) {
                Long psmActivityTimer = client.getPsmActivityTimer();
                if (psmActivityTimer == null && profileSettings != null) {
                    psmActivityTimer = profileSettings.getPsmActivityTimer();

                }
                if (psmActivityTimer == null || psmActivityTimer == 0L) {
                    psmActivityTimer = config.getPsmActivityTimer();
                }

                timeout = psmActivityTimer;
            } else {
                Long pagingTransmissionWindow = client.getPagingTransmissionWindow();
                if (pagingTransmissionWindow == null && profileSettings != null) {
                    pagingTransmissionWindow = profileSettings.getPagingTransmissionWindow();

                }
                if (pagingTransmissionWindow == null || pagingTransmissionWindow == 0L) {
                    pagingTransmissionWindow = config.getPagingTransmissionWindow();
                }
                timeout = pagingTransmissionWindow;
            }
            Future<Void> sleepTask = client.getSleepTask();
            if (sleepTask != null) {
                sleepTask.cancel(false);
            }
            Future<Void> task = context.getScheduler().schedule(() -> {
                if (uplinkTime == client.getLastUplinkTime() && !otaUpdateService.isOtaDownloading(client)) {
                    asleep(client);
                }
                return null;
            }, timeout, TimeUnit.MILLISECONDS);
            client.setSleepTask(task);
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：获取请求。
     * 参数：
     * - `client`：客户端对象。
     * 返回：数值结果。
     */
    @Override
    public Long getRequestTimeout(LwM2mClient client) {
        Long timeout = null;
        if (PowerMode.E_DRX.equals(client.getPowerMode()) && client.getEdrxCycle() != null) {
            timeout = client.getEdrxCycle();
        } else {
            var clientProfile = getProfile(client.getProfileId());
            OtherConfiguration clientLwM2mSettings = clientProfile.getClientLwM2mSettings();
            if (PowerMode.E_DRX.equals(clientLwM2mSettings.getPowerMode())) {
                timeout = clientLwM2mSettings.getEdrxCycle();
            }
        }
        if (timeout == null || timeout == 0L) {
            timeout = this.config.getTimeout();
        }
        return timeout;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mClientContextImpl` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
