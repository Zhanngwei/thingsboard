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
package org.thingsboard.server.transport.coap.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.server.coapserver.CoapServerContext;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.transport.coap.CoapSessionMsgType;
import org.thingsboard.server.common.transport.DeviceDeletedEvent;
import org.thingsboard.server.common.transport.DeviceProfileUpdatedEvent;
import org.thingsboard.server.common.transport.DeviceUpdatedEvent;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.transport.coap.CoapTransportContext;
import org.thingsboard.server.transport.coap.TbCoapMessageObserver;
import org.thingsboard.server.transport.coap.TransportConfigurationContainer;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;
import org.thingsboard.server.transport.coap.callback.AbstractSyncSessionCallback;
import org.thingsboard.server.transport.coap.callback.CoapNoOpCallback;
import org.thingsboard.server.transport.coap.callback.CoapOkCallback;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.californium.core.coap.Message.MAX_MID;
import static org.eclipse.californium.core.coap.Message.NONE;

/**
 * 中文说明：
 * 1. `DefaultCoapClientContext` 是 ThingsBoard Common Transport 中承载 CoAP 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `CoapClientContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.coap.enabled}'=='true')")
public class DefaultCoapClientContext implements CoapClientContext {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private final CoapServerContext config;
    private final CoapTransportContext transportContext;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TransportService transportService;
    private final TransportDeviceProfileCache profileCache;
    /**
     * 分区，提供当前类调用的业务操作。
     */
    private final PartitionService partitionService;
    private final ConcurrentMap<DeviceId, TbCoapClientState> clients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TbCoapClientState> clientsByToken = new ConcurrentHashMap<>();

    /**
     * 功能：创建 `DefaultCoapClientContext` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * - `transportContext`：处理上下文。
     * - `transportService`：服务对象。
     * - `profileCache`：`profileCache` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DefaultCoapClientContext(CoapServerContext config, @Lazy CoapTransportContext transportContext,
                                    TransportService transportService, TransportDeviceProfileCache profileCache,
                                    PartitionService partitionService) {
        this.config = config;
        this.transportContext = transportContext;
        this.transportService = transportService;
        this.profileCache = profileCache;
        this.partitionService = partitionService;
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(DeviceProfileUpdatedEvent.class)
    public void onApplicationEvent(DeviceProfileUpdatedEvent event) {
        var deviceProfile = event.getDeviceProfile();
        clients.values().stream().filter(state -> state.getSession() == null).forEach(state -> {
            state.lock();
            try {
                if (deviceProfile.getId().equals(state.getProfileId())) {
                    initStateAdaptor(deviceProfile, state);
                }
            } catch (AdaptorException e) {
                log.trace("[{}] Failed to update client state due to: ", state.getDeviceId(), e);
            } finally {
                state.unlock();
            }
        });
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(DeviceUpdatedEvent.class)
    public void onApplicationEvent(DeviceUpdatedEvent event) {
        var device = event.getDevice();
        var state = clients.get(device.getId());
        if (state == null) {
            return;
        }
        state.lock();
        try {
            if (state.getSession() == null) {
                clients.remove(device.getId());
            }
        } finally {
            state.unlock();
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @EventListener(DeviceDeletedEvent.class)
    public void onApplicationEvent(DeviceDeletedEvent event) {
        clients.remove(event.getDeviceId());
    }

    /**
     * 功能：保存或创建属性。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean registerAttributeObservation(TbCoapClientState clientState, String token, CoapExchange exchange) {
        return registerFeatureObservation(clientState, token, exchange, FeatureType.ATTRIBUTES);
    }

    /**
     * 功能：保存或创建RPC。
     * 参数：
     * - `clientState`：客户端对象。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean registerRpcObservation(TbCoapClientState clientState, String token, CoapExchange exchange) {
        return registerFeatureObservation(clientState, token, exchange, FeatureType.RPC);
    }

    /**
     * 功能：获取通知。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：处理结果。
     */
    @Override
    public AtomicInteger getNotificationCounterByToken(String token) {
        TbCoapClientState state = clientsByToken.get(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return null;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            return state.getAttrs().getObserveCounter();
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            return state.getRpc().getObserveCounter();
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
        return null;
    }

    /**
     * 功能：保存或创建关系。
     * 参数：
     * - `token`：`token` 参数。
     * - `relation`：`relation` 参数。
     * 返回：无。
     */
    @Override
    public void registerObserveRelation(String token, ObserveRelation relation) {
        TbCoapClientState state = clientsByToken.get(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            state.getAttrs().setObserveRelation(relation);
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            state.getRpc().setObserveRelation(relation);
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
    }

    /**
     * 功能：执行 `deregisterObserveRelation` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：无。
     */
    @Override
    public void deregisterObserveRelation(String token) {
        TbCoapClientState state = clientsByToken.remove(token);
        if (state == null) {
            log.trace("Failed to find state using token: {}", token);
            return;
        }
        if (state.getAttrs() != null && state.getAttrs().getToken().equals(token)) {
            cancelAttributeSubscription(state);
        } else {
            log.trace("Failed to find attr subscription using token: {}", token);
        }
        if (state.getRpc() != null && state.getRpc().getToken().equals(token)) {
            cancelRpcSubscription(state);
        } else {
            log.trace("Failed to find rpc subscription using token: {}", token);
        }
    }

    /**
     * 功能：上报`Activity`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void reportActivity() {
        for (TbCoapClientState state : clients.values()) {
            if (state.getSession() != null) {
                transportService.recordActivity(state.getSession());
            }
        }
    }

    /**
     * 功能：处理`on Uplink`。
     * 参数：
     * - `client`：客户端对象。
     * - `notifyOtherServers`：`notifyOtherServers` 参数。
     * - `uplinkTs`：时间戳。
     * 返回：无。
     */
    private void onUplink(TbCoapClientState client, boolean notifyOtherServers, long uplinkTs) {
        PowerMode powerMode = client.getPowerMode();
        PowerSavingConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            if (clientProfile.isPresent()) {
                profileSettings = clientProfile.get().getClientSettings();
                if (profileSettings != null) {
                    powerMode = profileSettings.getPowerMode();
                }
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode)) {
            client.updateLastUplinkTime(uplinkTs);
            return;
        }
        client.lock();
        try {
            long uplinkTime = client.updateLastUplinkTime(uplinkTs);
            long timeout = getTimeout(client, powerMode, profileSettings);
            Future<Void> sleepTask = client.getSleepTask();
            if (sleepTask != null) {
                sleepTask.cancel(false);
            }
            Future<Void> task = transportContext.getScheduler().schedule(() -> {
                if (uplinkTime == client.getLastUplinkTime()) {
                    asleep(client);
                }
                return null;
            }, timeout, TimeUnit.MILLISECONDS);
            client.setSleepTask(task);
            if (notifyOtherServers && partitionService.countTransportsByType(DataConstants.COAP_TRANSPORT_NAME) > 1) {
                transportService.notifyAboutUplink(getNewSyncSession(client), TransportProtos.UplinkNotificationMsg.newBuilder().setUplinkTs(uplinkTime).build(), TransportServiceCallback.EMPTY);
            }
        } finally {
            client.unlock();
        }
    }

    /**
     * 功能：获取超时时间。
     * 参数：
     * - `client`：客户端对象。
     * - `powerMode`：`powerMode` 参数。
     * - `profileSettings`：配置对象。
     * 返回：数值结果。
     */
    private long getTimeout(TbCoapClientState client, PowerMode powerMode, PowerSavingConfiguration profileSettings) {
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
        return timeout;
    }

    /**
     * 功能：保存或创建功能项。
     * 参数：
     * - `state`：`state` 参数。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * - `featureType`：类型。
     * 返回：判断结果。
     */
    private boolean registerFeatureObservation(TbCoapClientState state, String token, CoapExchange exchange, FeatureType featureType) {
        state.lock();
        try {
            boolean newObservation;
            if (FeatureType.ATTRIBUTES.equals(featureType)) {
                if (state.getAttrs() == null) {
                    newObservation = true;
                    state.setAttrs(new TbCoapObservationState(exchange, token));
                } else {
                    newObservation = !state.getAttrs().getToken().equals(token);
                    if (newObservation) {
                        TbCoapObservationState old = state.getAttrs();
                        state.setAttrs(new TbCoapObservationState(exchange, token));
                        old.getExchange().respond(CoAP.ResponseCode.DELETED);
                    }
                }
            } else {
                if (state.getRpc() == null) {
                    newObservation = true;
                    state.setRpc(new TbCoapObservationState(exchange, token));
                } else {
                    newObservation = !state.getRpc().getToken().equals(token);
                    if (newObservation) {
                        TbCoapObservationState old = state.getRpc();
                        state.setRpc(new TbCoapObservationState(exchange, token));
                        old.getExchange().respond(CoAP.ResponseCode.DELETED);
                    }
                }
            }
            if (newObservation) {
                clientsByToken.put(token, state);
                if (state.getSession() == null) {
                    TransportProtos.SessionInfoProto session = SessionInfoCreator.create(state.getCredentials(), transportContext, UUID.randomUUID());
                    state.setSession(session);
                    CoapSessionListener listener = new CoapSessionListener(state);
                    state.setListener(listener);
                    transportService.registerAsyncSession(session, state.getListener());
                    transportService.process(session, getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
                }
                if (FeatureType.ATTRIBUTES.equals(featureType)) {
                    transportService.process(state.getSession(),
                            TransportProtos.SubscribeToAttributeUpdatesMsg.getDefaultInstance(), new CoapNoOpCallback(exchange));
                    transportService.process(state.getSession(),
                            TransportProtos.GetAttributeRequestMsg.newBuilder().setOnlyShared(true).build(),
                            new CoapNoOpCallback(exchange));
                } else {
                    transportService.process(state.getSession(),
                            TransportProtos.SubscribeToRPCMsg.getDefaultInstance(),
                            new CoapOkCallback(exchange, CoAP.ResponseCode.VALID, CoAP.ResponseCode.INTERNAL_SERVER_ERROR)
                    );
                }
            }
            return newObservation;
        } finally {
            state.unlock();
        }
    }

    /**
     * 功能：执行 `deregisterAttributeObservation` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    public void deregisterAttributeObservation(TbCoapClientState state, String token, CoapExchange exchange) {
        state.lock();
        try {
            clientsByToken.remove(token);
            if (state.getSession() == null) {
                log.trace("[{}] Failed to delete attribute observation: {}. Session is not present.", state.getDeviceId(), token);
                return;
            }
            if (state.getAttrs() == null) {
                log.trace("[{}] Failed to delete attribute observation: {}. It is not registered.", state.getDeviceId(), token);
                return;
            }
            if (!state.getAttrs().getToken().equals(token)) {
                log.trace("[{}] Failed to delete attribute observation: {}. Token mismatch.", state.getDeviceId(), token);
                return;
            }
            cancelAttributeSubscription(state);
        } finally {
            state.unlock();
        }
    }

    /**
     * 功能：执行 `deregisterRpcObservation` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * - `token`：`token` 参数。
     * - `exchange`：`exchange` 参数。
     * 返回：无。
     */
    @Override
    public void deregisterRpcObservation(TbCoapClientState state, String token, CoapExchange exchange) {
        state.lock();
        try {
            clientsByToken.remove(token);
            if (state.getSession() == null) {
                log.trace("[{}] Failed to delete rpc observation: {}. Session is not present.", state.getDeviceId(), token);
                return;
            }
            if (state.getRpc() == null) {
                log.trace("[{}] Failed to delete rpc observation: {}. It is not registered.", state.getDeviceId(), token);
                return;
            }
            if (!state.getRpc().getToken().equals(token)) {
                log.trace("[{}] Failed to delete rpc observation: {}. Token mismatch.", state.getDeviceId(), token);
                return;
            }
            cancelRpcSubscription(state);
        } finally {
            state.unlock();
        }
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `type`：类型。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public TbCoapClientState getOrCreateClient(CoapSessionMsgType type, ValidateDeviceCredentialsResponse deviceCredentials, DeviceProfile deviceProfile) throws AdaptorException {
        DeviceId deviceId = deviceCredentials.getDeviceInfo().getDeviceId();
        TbCoapClientState state = getClientState(deviceId);
        state.lock();
        try {
            if (state.getConfiguration() == null || state.getAdaptor() == null) {
                initStateAdaptor(deviceProfile, state);
            }
            if (state.getCredentials() == null) {
                state.init(deviceCredentials);
            }
        } finally {
            state.unlock();
        }
        return state;
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：处理结果。
     */
    @Override
    public TransportProtos.SessionInfoProto getNewSyncSession(TbCoapClientState state) {
        return SessionInfoCreator.create(state.getCredentials(), transportContext, UUID.randomUUID());
    }

    /**
     * 功能：获取状态。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    private TbCoapClientState getClientState(DeviceId deviceId) {
        return clients.computeIfAbsent(deviceId, TbCoapClientState::new);
    }

    /**
     * 功能：获取会话。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：处理结果。
     */
    private static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private TransportConfigurationContainer getTransportConfigurationContainer(DeviceProfile deviceProfile) throws AdaptorException {
        DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof DefaultDeviceProfileTransportConfiguration) {
            return new TransportConfigurationContainer(true);
        } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
            CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration =
                    (CoapDeviceProfileTransportConfiguration) transportConfiguration;
            CoapDeviceTypeConfiguration coapDeviceTypeConfiguration =
                    coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
            if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration =
                        (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                TransportPayloadTypeConfiguration transportPayloadTypeConfiguration =
                        defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                if (transportPayloadTypeConfiguration instanceof JsonTransportPayloadConfiguration) {
                    return new TransportConfigurationContainer(true);
                } else {
                    ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                            (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                    String deviceTelemetryProtoSchema = protoTransportPayloadConfiguration.getDeviceTelemetryProtoSchema();
                    String deviceAttributesProtoSchema = protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema();
                    String deviceRpcRequestProtoSchema = protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema();
                    String deviceRpcResponseProtoSchema = protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema();
                    return new TransportConfigurationContainer(false,
                            protoTransportPayloadConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema),
                            protoTransportPayloadConfiguration.getAttributesDynamicMessageDescriptor(deviceAttributesProtoSchema),
                            protoTransportPayloadConfiguration.getRpcResponseDynamicMessageDescriptor(deviceRpcResponseProtoSchema),
                            protoTransportPayloadConfiguration.getRpcRequestDynamicMessageBuilder(deviceRpcRequestProtoSchema)
                    );
                }
            } else {
                throw new AdaptorException("Invalid CoapDeviceTypeConfiguration type: " + coapDeviceTypeConfiguration.getClass().getSimpleName() + "!");
            }
        } else {
            throw new AdaptorException("Invalid DeviceProfileTransportConfiguration type" + transportConfiguration.getClass().getSimpleName() + "!");
        }
    }

    /**
     * 功能：初始化或启动状态。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    private void initStateAdaptor(DeviceProfile deviceProfile, TbCoapClientState state) throws AdaptorException {
        state.setConfiguration(getTransportConfigurationContainer(deviceProfile));
        state.setAdaptor(getCoapTransportAdaptor(state.getConfiguration().isJsonPayload()));
        state.setContentFormat(state.getAdaptor().getContentFormat());
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `jsonPayloadType`：类型。
     * 返回：处理结果。
     */
    private CoapTransportAdaptor getCoapTransportAdaptor(boolean jsonPayloadType) {
        return jsonPayloadType ? transportContext.getJsonCoapAdaptor() : transportContext.getProtoCoapAdaptor();
    }

    /**
     * 中文说明：
     * 1. `CoapSessionListener` 是 ThingsBoard Common Transport 中处理会话的处理器。
     * 2. 它把单一处理步骤封装为可调用、可替换的组件。
     * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
     * 4. 直接依赖的类型边界包括 `SessionMsgListener`。
     * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
     * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
     */
    @RequiredArgsConstructor
    public class CoapSessionListener implements SessionMsgListener {

        /**
         * 状态，表示当前对象所处状态。
         */
        private final TbCoapClientState state;

        /**
         * 功能：处理响应。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onGetAttributesResponse(TransportProtos.GetAttributeResponseMsg msg) {
            TbCoapObservationState attrs = state.getAttrs();
            if (attrs != null) {
                try {
                    Response response = state.getAdaptor().convertToPublish(msg);
                    respond(attrs.getExchange(), response, state.getContentFormat());
                } catch (AdaptorException e) {
                    log.trace("Failed to reply due to error", e);
                    cancelObserveRelation(attrs);
                    cancelAttributeSubscription(state);
                }
            } else {
                log.debug("[{}] Get Attrs exchange is empty", state.getDeviceId());
            }
        }

        /**
         * 功能：处理属性。
         * 参数：
         * - `sessionId`：会话ID。
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg msg) {
            if (!isDownlinkAllowed(state)) {
                log.trace("[{}] ignore downlink request cause client is sleeping.", state.getDeviceId());
                state.lock();
                try {
                    state.addQueuedNotification(msg);
                } finally {
                    state.unlock();
                }
                return;
            }
            log.trace("[{}] Received attributes update notification to device", sessionId);
            TbCoapObservationState attrs = state.getAttrs();
            if (attrs != null) {
                try {
                    boolean conRequest = AbstractSyncSessionCallback.isConRequest(state.getAttrs());
                    int requestId = getNextMsgId();
                    Response response = state.getAdaptor().convertToPublish(msg);
                    response.setConfirmable(conRequest);
                    response.setMID(requestId);
                    if (conRequest) {
                        response.addMessageObserver(new TbCoapMessageObserver(requestId, id -> awake(state), id -> asleep(state)));
                    }
                    respond(attrs.getExchange(), response, state.getContentFormat());
                } catch (AdaptorException e) {
                    log.trace("[{}] Failed to reply due to error", state.getDeviceId(), e);
                    cancelObserveRelation(attrs);
                    cancelAttributeSubscription(state);
                }
            } else {
                log.debug("[{}] Get Attrs exchange is empty", state.getDeviceId());
            }
        }

        /**
         * 功能：处理设备配置。
         * 参数：
         * - `newSessionInfo`：会话对象。
         * - `deviceProfile`：设备信息或设备标识。
         * 返回：无。
         */
        @Override
        public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto newSessionInfo, DeviceProfile deviceProfile) {
            try {
                initStateAdaptor(deviceProfile, state);
            } catch (AdaptorException e) {
                log.warn("[{}] Failed to update device profile: ", deviceProfile.getId(), e);
            }
        }

        /**
         * 功能：处理设备。
         * 参数：
         * - `sessionInfo`：会话对象。
         * - `device`：设备信息或设备标识。
         * - `deviceProfileOpt`：设备信息或设备标识。
         * 返回：无。
         */
        @Override
        public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
            if (deviceProfileOpt.isPresent()) {
                try {
                    initStateAdaptor(deviceProfileOpt.get(), state);
                } catch (AdaptorException e) {
                    log.warn("[{}] Failed to update device: ", device.getId(), e);
                }
            }
            state.onDeviceUpdate(device);
        }

        /**
         * 功能：处理设备。
         * 参数：
         * - `deviceId`：设备IDID。
         * 返回：无。
         */
        @Override
        public void onDeviceDeleted(DeviceId deviceId) {
            cancelRpcSubscription(state);
            cancelAttributeSubscription(state);
        }

        /**
         * 功能：处理会话。
         * 参数：
         * - `sessionId`：会话ID。
         * - `sessionCloseNotification`：会话对象。
         * 返回：无。
         */
        @Override
        public void onRemoteSessionCloseCommand(UUID sessionId, TransportProtos.SessionCloseNotificationProto sessionCloseNotification) {
            log.trace("[{}] Received the remote command to close the session: {}", sessionId, sessionCloseNotification.getMessage());
            cancelRpcSubscription(state);
            cancelAttributeSubscription(state);
        }

        /**
         * 功能：处理设备。
         * 参数：
         * - `sessionId`：会话ID。
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg msg) {
            DeviceId deviceId = state.getDeviceId();
            log.trace("[{}][{}] Received RPC command to device: {}", deviceId, sessionId, msg);
            if (!isDownlinkAllowed(state)) {
                log.trace("[{}][{}] ignore downlink request cause client is sleeping.", deviceId, sessionId);
                return;
            }
            boolean sent = false;
            String error = null;
            boolean conRequest = AbstractSyncSessionCallback.isConRequest(state.getRpc());
            int requestId = getNextMsgId();
            try {
                Response response = state.getAdaptor().convertToPublish(msg, state.getConfiguration().getRpcRequestDynamicMessageBuilder());
                response.setConfirmable(conRequest);
                response.setMID(requestId);
                if (conRequest) {
                    PowerMode powerMode = state.getPowerMode();
                    PowerSavingConfiguration profileSettings = null;
                    if (powerMode == null) {
                        var clientProfile = getProfile(state.getProfileId());
                        if (clientProfile.isPresent()) {
                            profileSettings = clientProfile.get().getClientSettings();
                            if (profileSettings != null) {
                                powerMode = profileSettings.getPowerMode();
                            }
                        }
                    }

                    transportContext.getRpcAwaitingAck().put(requestId, msg);
                    transportContext.getScheduler().schedule(() -> {
                        TransportProtos.ToDeviceRpcRequestMsg rpcRequestMsg = transportContext.getRpcAwaitingAck().remove(requestId);
                        if (rpcRequestMsg != null) {
                            log.trace("[{}][{}][{}] Going to send to device actor RPC request TIMEOUT status update due to server timeout ...", deviceId, sessionId, requestId);
                            transportService.process(state.getSession(), msg, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
                        }
                    }, Math.min(getTimeout(state, powerMode, profileSettings), msg.getExpirationTime() - System.currentTimeMillis()), TimeUnit.MILLISECONDS);

                    response.addMessageObserver(new TbCoapMessageObserver(requestId, id -> {
                        TransportProtos.ToDeviceRpcRequestMsg rpcRequestMsg = transportContext.getRpcAwaitingAck().remove(id);
                        if (rpcRequestMsg != null) {
                            log.trace("[{}][{}][{}] Going to send to device actor RPC request DELIVERED status update ...", deviceId, sessionId, requestId);
                            transportService.process(state.getSession(), rpcRequestMsg, RpcStatus.DELIVERED, true, TransportServiceCallback.EMPTY);
                        }
                    }, id -> {
                        TransportProtos.ToDeviceRpcRequestMsg rpcRequestMsg = transportContext.getRpcAwaitingAck().remove(id);
                        if (rpcRequestMsg != null) {
                            log.trace("[{}][{}][{}] Going to send to device actor RPC request TIMEOUT status update ...", deviceId, sessionId, requestId);
                            transportService.process(state.getSession(), msg, RpcStatus.TIMEOUT, TransportServiceCallback.EMPTY);
                        }
                    }));
                }
                if (conRequest) {
                    response.addMessageObserver(new TbCoapMessageObserver(requestId, id -> awake(state), id -> asleep(state)));
                }
                respond(state.getRpc().getExchange(), response, state.getContentFormat());
                sent = true;
            } catch (AdaptorException e) {
                log.trace("Failed to reply due to error", e);
                cancelObserveRelation(state.getRpc());
                cancelRpcSubscription(state);
                error = "Failed to convert device RPC command to CoAP msg";
            } catch (Exception e) {
                error = "Internal error: " + e.getMessage();
            } finally {
                if (StringUtils.isNotEmpty(error)) {
                    transportService.process(state.getSession(),
                            TransportProtos.ToDeviceRpcResponseMsg.newBuilder()
                                    .setRequestId(msg.getRequestId()).setError(error).build(), TransportServiceCallback.EMPTY);
                } else if (sent) {
                    if (!conRequest) {
                        log.trace("[{}][{}][{}] Going to send to device actor non-confirmable RPC request DELIVERED status update ...", deviceId, sessionId, requestId);
                        transportService.process(state.getSession(), msg, RpcStatus.DELIVERED, TransportServiceCallback.EMPTY);
                    } else if (msg.getPersisted()) {
                        log.trace("[{}][{}][{}] Going to send to device actor RPC request SENT status update ...", deviceId, sessionId, requestId);
                        transportService.process(state.getSession(), msg, RpcStatus.SENT, TransportServiceCallback.EMPTY);
                    }
                }
            }
        }

        /**
         * 功能：处理RPC。
         * 参数：
         * - `msg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg msg) {
            log.trace("[{}] Received server rpc response in the wrong session.", state.getSession());
        }

        /**
         * 功能：处理通知。
         * 参数：
         * - `notificationMsg`：待处理消息。
         * 返回：无。
         */
        @Override
        public void onUplinkNotification(TransportProtos.UplinkNotificationMsg notificationMsg) {
            awake(state, false, notificationMsg.getUplinkTs());
        }

        /**
         * 功能：执行 `cancelObserveRelation` 对应的处理。
         * 参数：
         * - `attrs`：`attrs` 参数。
         * 返回：无。
         */
        private void cancelObserveRelation(TbCoapObservationState attrs) {
            if (attrs.getObserveRelation() != null) {
                attrs.getObserveRelation().cancel();
            }
        }
    }

    /**
     * 功能：执行 `asleep` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    private boolean asleep(TbCoapClientState client) {
        boolean changed = compareAndSetSleepFlag(client, true);
        if (changed) {
            log.debug("[{}] client is sleeping", client.getDeviceId());
            transportService.log(client.getSession(), "Info: Client is sleeping!");
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
    public boolean awake(TbCoapClientState client) {
        return awake(client, true, System.currentTimeMillis());
    }

    /**
     * 功能：执行 `awake` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `notifyOtherServers`：`notifyOtherServers` 参数。
     * - `uplinkTs`：时间戳。
     * 返回：判断结果。
     */
    private boolean awake(TbCoapClientState client, boolean notifyOtherServers, long uplinkTs) {
        onUplink(client, notifyOtherServers, uplinkTs);
        boolean changed = compareAndSetSleepFlag(client, false);
        if (changed) {
            log.debug("[{}] client is awake", client.getDeviceId());
            transportService.log(client.getSession(), "Info: Client is awake!");
            sendMsgsAfterSleeping(client);
        }
        return changed;
    }

    /**
     * 功能：发送或提交`Msgs After Sleeping`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    private void sendMsgsAfterSleeping(TbCoapClientState client) {
        if (client.getRpc() != null) {
            TransportProtos.TransportToDeviceActorMsg persistentRpcRequestMsg = TransportProtos.TransportToDeviceActorMsg
                    .newBuilder()
                    .setSessionInfo(client.getSession())
                    .setSendPendingRPC(TransportProtos.SendPendingRPCMsg.newBuilder().build())
                    .build();
            transportService.process(persistentRpcRequestMsg, TransportServiceCallback.EMPTY);
        }
        if (client.getAttrs() != null && client.getMissedAttributeUpdates() != null) {
            client.getListener().onAttributeUpdate(new UUID(client.getSession().getSessionIdMSB(), client.getSession().getSessionIdLSB()), client.getAndClearMissedUpdates());
        }
    }

    /**
     * 功能：执行 `compareAndSetSleepFlag` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `sleeping`：`sleeping` 参数。
     * 返回：判断结果。
     */
    private boolean compareAndSetSleepFlag(TbCoapClientState client, boolean sleeping) {
        if (sleeping == client.isAsleep()) {
            log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getDeviceId(), client.isAsleep(), sleeping);
            return false;
        }
        client.lock();
        try {
            if (sleeping == client.isAsleep()) {
                log.trace("[{}] Client is already at sleeping: {}, ignoring event: {}", client.getDeviceId(), client.isAsleep(), sleeping);
                return false;
            } else {
                PowerMode powerMode = getPowerMode(client);
                if (PowerMode.PSM.equals(powerMode) || PowerMode.E_DRX.equals(powerMode)) {
                    log.trace("[{}] Switch sleeping from: {} to: {}", client.getDeviceId(), client.isAsleep(), sleeping);
                    client.setAsleep(sleeping);
                    // TODO: persist changes.
                    // update(client);
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
     * 功能：判断`Downlink Allowed`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    private boolean isDownlinkAllowed(TbCoapClientState client) {
        PowerMode powerMode = client.getPowerMode();
        PowerSavingConfiguration profileSettings = null;
        if (powerMode == null && client.getProfileId() != null) {
            var clientProfile = getProfile(client.getProfileId());
            if (clientProfile.isPresent()) {
                profileSettings = clientProfile.get().getClientSettings();
                if (profileSettings != null) {
                    powerMode = profileSettings.getPowerMode();
                }
            }
        }
        if (powerMode == null || PowerMode.DRX.equals(powerMode)) {
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
     * 功能：获取`Power Mode`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：处理结果。
     */
    private PowerMode getPowerMode(TbCoapClientState client) {
        PowerMode powerMode = client.getPowerMode();
        if (powerMode == null) {
            powerMode = PowerMode.PSM;
            if (client.getProfileId() != null) {
                Optional<CoapDeviceProfileTransportConfiguration> deviceProfile = getProfile(client.getProfileId());
                if (deviceProfile.isPresent()) {
                    powerMode = deviceProfile.get().getClientSettings().getPowerMode();
                }
            }
        }
        return powerMode;
    }

    /**
     * 功能：获取配置。
     * 参数：
     * - `profileId`：配置ID。
     * 返回：可能存在的结果。
     */
    public Optional<CoapDeviceProfileTransportConfiguration> getProfile(DeviceProfileId profileId) {
        DeviceProfile deviceProfile = profileCache.get(profileId);
        if (deviceProfile.getTransportType().equals(DeviceTransportType.COAP)) {
            return Optional.of((CoapDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration());
        } else if (deviceProfile.getTransportType().equals(DeviceTransportType.DEFAULT)) {
            return Optional.empty();
        } else {
            log.warn("[{}] Invalid device profile type: {}", profileId, deviceProfile.getTransportType());
            throw new IllegalArgumentException("Invalid device profile type: " + deviceProfile.getTransportType());
        }
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：数值结果。
     */
    protected int getNextMsgId() {
        return ThreadLocalRandom.current().nextInt(NONE, MAX_MID + 1);
    }

    /**
     * 功能：执行 `cancelRpcSubscription` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    private void cancelRpcSubscription(TbCoapClientState state) {
        if (state.getRpc() != null) {
            clientsByToken.remove(state.getRpc().getToken());
            CoapExchange exchange = state.getRpc().getExchange();
            state.setRpc(null);
            transportService.process(state.getSession(),
                    TransportProtos.SubscribeToRPCMsg.newBuilder().setUnsubscribe(true).build(),
                    new CoapOkCallback(exchange, CoAP.ResponseCode.DELETED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
            if (state.getAttrs() == null) {
                closeAndCleanup(state);
            }
        }
    }

    /**
     * 功能：执行 `cancelAttributeSubscription` 对应的处理。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    private void cancelAttributeSubscription(TbCoapClientState state) {
        if (state.getAttrs() != null) {
            clientsByToken.remove(state.getAttrs().getToken());
            CoapExchange exchange = state.getAttrs().getExchange();
            state.setAttrs(null);
            transportService.process(state.getSession(),
                    TransportProtos.SubscribeToAttributeUpdatesMsg.newBuilder().setUnsubscribe(true).build(),
                    new CoapOkCallback(exchange, CoAP.ResponseCode.DELETED, CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
            if (state.getRpc() == null) {
                closeAndCleanup(state);
            }
        }
    }

    /**
     * 功能：停止或关闭`And Cleanup`。
     * 参数：
     * - `state`：`state` 参数。
     * 返回：无。
     */
    private void closeAndCleanup(TbCoapClientState state) {
        transportService.process(state.getSession(), getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
        transportService.deregisterSession(state.getSession());
        state.setSession(null);
        state.setConfiguration(null);
        state.setCredentials(null);
        state.setAdaptor(null);
        //TODO: add optimistic lock check that the client was already deleted and cleanup "clients" map.
    }

    /**
     * 功能：执行 `respond` 对应的处理。
     * 参数：
     * - `exchange`：`exchange` 参数。
     * - `response`：响应对象。
     * - `defContentFormat`：`defContentFormat` 参数。
     * 返回：无。
     */
    private void respond(CoapExchange exchange, Response response, int defContentFormat) {
        response.getOptions().setContentFormat(TbCoapContentFormatUtil.getContentFormat(exchange.getRequestOptions().getContentFormat(), defContentFormat));
        exchange.respond(response);
    }
}
