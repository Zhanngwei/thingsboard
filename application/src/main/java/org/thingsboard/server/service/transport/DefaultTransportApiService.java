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
package org.thingsboard.server.service.transport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.ProvisionDeviceCredentialsData;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.device.profile.ProvisionDeviceProfileCredentials;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.exception.EntitiesLimitException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetEntityProfileResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetResourceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GetSnmpDevicesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceTokenRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceX509CertRequestMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.resource.TbResourceService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.transport.BasicCredentialsValidationResult.PASSWORD_MISMATCH;
import static org.thingsboard.server.service.transport.BasicCredentialsValidationResult.VALID;

/**
 * Created by ashvayka on 05.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`DefaultTransportApiService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTransportApiService implements TransportApiService {

    private static final Pattern X509_CERTIFICATE_TRIM_CHAIN_PATTERN = Pattern.compile("-----BEGIN CERTIFICATE-----\\s*.*?\\s*-----END CERTIFICATE-----");

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    private final TbDeviceProfileCache deviceProfileCache;
    private final TbTenantProfileCache tenantProfileCache;
    /**
     * 状态，提供当前类调用的业务操作。
     */
    private final TbApiUsageStateService apiUsageStateService;
    private final DeviceService deviceService;
    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    private final DeviceProfileService deviceProfileService;
    private final RelationService relationService;
    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    private final DeviceCredentialsService deviceCredentialsService;
    private final DbCallbackExecutorService dbCallbackExecutorService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService tbClusterService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceProvisionService deviceProvisionService;
    private final ResourceService resourceService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final OtaPackageService otaPackageService;
    private final OtaPackageDataCache otaPackageDataCache;
    /**
     * 队列，提供当前类调用的业务操作。
     */
    private final QueueService queueService;

    private final ConcurrentMap<String, ReentrantLock> deviceCreationLocks = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Value("${queue.transport_api.max_core_handler_threads:16}")
    private int maxCoreHandlerThreads;

    /**
     * 处理器列表，用于保存一组待处理对象。
     */
    ListeningExecutorService handlerExecutor;

    /**
     * 功能：校验凭据。
     * 参数：
     * - `credentials`：`credentials` 参数。
     * 返回：判断结果。
     */
    private static boolean checkIsMqttCredentials(DeviceCredentials credentials) {
        return credentials != null && DeviceCredentialsType.MQTT_BASIC.equals(credentials.getCredentialsType());
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        handlerExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(maxCoreHandlerThreads, "transport-api-service-core-handler"));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (handlerExecutor != null) {
            handlerExecutor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `tbProtoQueueMsg`：队列名称或队列对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<TbProtoQueueMsg<TransportApiResponseMsg>> handle(TbProtoQueueMsg<TransportApiRequestMsg> tbProtoQueueMsg) {
        TransportApiRequestMsg transportApiRequestMsg = tbProtoQueueMsg.getValue();
        ListenableFuture<TransportApiResponseMsg> result = null;

        if (transportApiRequestMsg.hasValidateTokenRequestMsg()) {
            ValidateDeviceTokenRequestMsg msg = transportApiRequestMsg.getValidateTokenRequestMsg();
            final String token = msg.getToken();
            result = handlerExecutor.submit(() -> validateCredentials(token, DeviceCredentialsType.ACCESS_TOKEN));
        } else if (transportApiRequestMsg.hasValidateBasicMqttCredRequestMsg()) {
            TransportProtos.ValidateBasicMqttCredRequestMsg msg = transportApiRequestMsg.getValidateBasicMqttCredRequestMsg();
            result = handlerExecutor.submit(() -> validateCredentials(msg));
        } else if (transportApiRequestMsg.hasValidateX509CertRequestMsg()) {
            ValidateDeviceX509CertRequestMsg msg = transportApiRequestMsg.getValidateX509CertRequestMsg();
            final String hash = msg.getHash();
            result = handlerExecutor.submit(() -> validateCredentials(hash, DeviceCredentialsType.X509_CERTIFICATE));
        } else if (transportApiRequestMsg.hasValidateOrCreateX509CertRequestMsg()) {
            TransportProtos.ValidateOrCreateDeviceX509CertRequestMsg msg = transportApiRequestMsg.getValidateOrCreateX509CertRequestMsg();
            final String certChain = msg.getCertificateChain();
            result = handlerExecutor.submit(() -> validateOrCreateDeviceX509Certificate(certChain));
        } else if (transportApiRequestMsg.hasGetOrCreateDeviceRequestMsg()) {
            result = handlerExecutor.submit(() -> handle(transportApiRequestMsg.getGetOrCreateDeviceRequestMsg()));
        } else if (transportApiRequestMsg.hasEntityProfileRequestMsg()) {
            result = handle(transportApiRequestMsg.getEntityProfileRequestMsg());
        } else if (transportApiRequestMsg.hasLwM2MRequestMsg()) {
            result = handle(transportApiRequestMsg.getLwM2MRequestMsg());
        } else if (transportApiRequestMsg.hasValidateDeviceLwM2MCredentialsRequestMsg()) {
            ValidateDeviceLwM2MCredentialsRequestMsg msg = transportApiRequestMsg.getValidateDeviceLwM2MCredentialsRequestMsg();
            final String credentialsId = msg.getCredentialsId();
            result = handlerExecutor.submit(() -> validateCredentials(credentialsId, DeviceCredentialsType.LWM2M_CREDENTIALS));
        } else if (transportApiRequestMsg.hasProvisionDeviceRequestMsg()) {
            result = handle(transportApiRequestMsg.getProvisionDeviceRequestMsg());
        } else if (transportApiRequestMsg.hasResourceRequestMsg()) {
            result = handle(transportApiRequestMsg.getResourceRequestMsg());
        } else if (transportApiRequestMsg.hasSnmpDevicesRequestMsg()) {
            result = handle(transportApiRequestMsg.getSnmpDevicesRequestMsg());
        } else if (transportApiRequestMsg.hasDeviceRequestMsg()) {
            result = handle(transportApiRequestMsg.getDeviceRequestMsg());
        } else if (transportApiRequestMsg.hasDeviceCredentialsRequestMsg()) {
            result = handle(transportApiRequestMsg.getDeviceCredentialsRequestMsg());
        } else if (transportApiRequestMsg.hasOtaPackageRequestMsg()) {
            result = handle(transportApiRequestMsg.getOtaPackageRequestMsg());
        } else if (transportApiRequestMsg.hasGetAllQueueRoutingInfoRequestMsg()) {
            return Futures.transform(handle(transportApiRequestMsg.getGetAllQueueRoutingInfoRequestMsg()), value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()), MoreExecutors.directExecutor());
        }

        return Futures.transform(Optional.ofNullable(result).orElseGet(this::getEmptyTransportApiResponseFuture),
                value -> new TbProtoQueueMsg<>(tbProtoQueueMsg.getKey(), value, tbProtoQueueMsg.getHeaders()),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：校验凭据。
     * 参数：
     * - `credentialsId`：凭据ID。
     * - `credentialsType`：类型。
     * 返回：判断结果。
     */
    private TransportApiResponseMsg validateCredentials(String credentialsId, DeviceCredentialsType credentialsType) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(credentialsId);
        if (credentials != null && credentials.getCredentialsType() == credentialsType) {
            return getDeviceInfo(credentials);
        } else {
            return getEmptyTransportApiResponse();
        }
    }

    /**
     * 功能：校验凭据。
     * 参数：
     * - `mqtt`：`mqtt` 参数。
     * 返回：判断结果。
     */
    private TransportApiResponseMsg validateCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg mqtt) {
        DeviceCredentials credentials;
        if (StringUtils.isEmpty(mqtt.getUserName())) {
            credentials = checkMqttCredentials(mqtt, EncryptionUtil.getSha3Hash(mqtt.getClientId()));
            if (credentials != null) {
                return getDeviceInfo(credentials);
            } else {
                return getEmptyTransportApiResponse();
            }
        } else {
            credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(
                    EncryptionUtil.getSha3Hash("|", mqtt.getClientId(), mqtt.getUserName()));
            if (checkIsMqttCredentials(credentials)) {
                var validationResult = validateMqttCredentials(mqtt, credentials);
                if (VALID.equals(validationResult)) {
                    return getDeviceInfo(credentials);
                } else if (PASSWORD_MISMATCH.equals(validationResult)) {
                    return getEmptyTransportApiResponse();
                } else {
                    return validateUserNameCredentials(mqtt);
                }
            } else {
                return validateUserNameCredentials(mqtt);
            }
        }
    }

    /**
     * 功能：校验证书。
     * 参数：
     * - `certificateChain`：`certificateChain` 参数。
     * 返回：判断结果。
     */
    protected TransportApiResponseMsg validateOrCreateDeviceX509Certificate(String certificateChain) {
        List<String> chain = X509_CERTIFICATE_TRIM_CHAIN_PATTERN.matcher(certificateChain).results().map(match ->
                EncryptionUtil.certTrimNewLines(match.group())).collect(Collectors.toList());
        for (String certificateValue : chain) {
            String certificateHash = EncryptionUtil.getSha3Hash(certificateValue);
            DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(certificateHash);
            if (credentials != null && DeviceCredentialsType.X509_CERTIFICATE.equals(credentials.getCredentialsType())) {
                return getDeviceInfo(credentials);
            }
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByProvisionDeviceKey(certificateHash);
            if (deviceProfile != null && DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(deviceProfile.getProvisionType())) {
                String updatedDeviceProvisionSecret = chain.get(0);
                ProvisionRequest provisionRequest = createProvisionRequest(updatedDeviceProvisionSecret);
                try {
                    ProvisionResponse provisionResponse = deviceProvisionService.provisionDeviceViaX509Chain(deviceProfile, provisionRequest);
                    if (ProvisionResponseStatus.SUCCESS.equals(provisionResponse.getResponseStatus())) {
                        return getDeviceInfo(provisionResponse.getDeviceCredentials());
                    }
                } catch (ProvisionFailedException e) {
                    log.debug("[{}][{}] Failed to provision device with cert chain: {}", deviceProfile.getTenantId(), deviceProfile.getId(), provisionRequest, e);
                    return getEmptyTransportApiResponse();
                }
            } else if (deviceProfile != null) {
                log.warn("[{}][{}] Device Profile provision configuration mismatched: expected {}, actual {}", deviceProfile.getTenantId(), deviceProfile.getId(), DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN, deviceProfile.getProvisionType());
            }
        }
        return getEmptyTransportApiResponse();
    }

    /**
     * 功能：校验用户。
     * 参数：
     * - `mqtt`：`mqtt` 参数。
     * 返回：判断结果。
     */
    private TransportApiResponseMsg validateUserNameCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg mqtt) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(mqtt.getUserName());
        if (credentials != null) {
            switch (credentials.getCredentialsType()) {
                case ACCESS_TOKEN:
                    return getDeviceInfo(credentials);
                case MQTT_BASIC:
                    if (VALID.equals(validateMqttCredentials(mqtt, credentials))) {
                        return getDeviceInfo(credentials);
                    } else {
                        return getEmptyTransportApiResponse();
                    }
            }
        }
        return getEmptyTransportApiResponse();
    }

    /**
     * 功能：校验凭据。
     * 参数：
     * - `clientCred`：客户端对象。
     * - `credId`：`credId`ID。
     * 返回：判断结果。
     */
    private DeviceCredentials checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, String credId) {
        return checkMqttCredentials(clientCred, deviceCredentialsService.findDeviceCredentialsByCredentialsId(credId));
    }

    /**
     * 功能：校验凭据。
     * 参数：
     * - `clientCred`：客户端对象。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：判断结果。
     */
    private DeviceCredentials checkMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, DeviceCredentials deviceCredentials) {
        if (deviceCredentials != null && deviceCredentials.getCredentialsType() == DeviceCredentialsType.MQTT_BASIC) {
            if (VALID.equals(validateMqttCredentials(clientCred, deviceCredentials))) {
                return deviceCredentials;
            }
        }
        return null;
    }

    /**
     * 功能：校验凭据。
     * 参数：
     * - `clientCred`：客户端对象。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：判断结果。
     */
    private BasicCredentialsValidationResult validateMqttCredentials(TransportProtos.ValidateBasicMqttCredRequestMsg clientCred, DeviceCredentials deviceCredentials) {
        BasicMqttCredentials dbCred = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
        if (!StringUtils.isEmpty(dbCred.getClientId()) && !dbCred.getClientId().equals(clientCred.getClientId())) {
            return BasicCredentialsValidationResult.HASH_MISMATCH;
        }
        if (!StringUtils.isEmpty(dbCred.getUserName()) && !dbCred.getUserName().equals(clientCred.getUserName())) {
            return BasicCredentialsValidationResult.HASH_MISMATCH;
        }
        if (!StringUtils.isEmpty(dbCred.getPassword())) {
            if (StringUtils.isEmpty(clientCred.getPassword())) {
                return BasicCredentialsValidationResult.PASSWORD_MISMATCH;
            } else {
                return dbCred.getPassword().equals(clientCred.getPassword()) ? VALID : BasicCredentialsValidationResult.PASSWORD_MISMATCH;
            }
        }
        return VALID;
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：处理结果。
     */
    private TransportApiResponseMsg handle(GetOrCreateDeviceFromGatewayRequestMsg requestMsg) {
        DeviceId gatewayId = new DeviceId(new UUID(requestMsg.getGatewayIdMSB(), requestMsg.getGatewayIdLSB()));
        Device gateway = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, gatewayId);
        Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(requestMsg.getDeviceName(), id -> new ReentrantLock());
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceByTenantIdAndName(gateway.getTenantId(), requestMsg.getDeviceName());
            if (device == null) {
                TenantId tenantId = gateway.getTenantId();
                device = new Device();
                device.setTenantId(tenantId);
                device.setName(requestMsg.getDeviceName());
                device.setType(requestMsg.getDeviceType());
                device.setCustomerId(gateway.getCustomerId());
                DeviceProfile deviceProfile = deviceProfileCache.findOrCreateDeviceProfile(gateway.getTenantId(), requestMsg.getDeviceType());

                device.setDeviceProfileId(deviceProfile.getId());
                ObjectNode additionalInfo = JacksonUtil.newObjectNode();
                additionalInfo.put(DataConstants.LAST_CONNECTED_GATEWAY, gatewayId.toString());
                device.setAdditionalInfo(additionalInfo);
                Device savedDevice = deviceService.saveDevice(device);
                tbClusterService.onDeviceUpdated(savedDevice, null);
                device = savedDevice;

                relationService.saveRelation(TenantId.SYS_TENANT_ID, new EntityRelation(gateway.getId(), device.getId(), "Created"));

                TbMsgMetaData metaData = new TbMsgMetaData();
                CustomerId customerId = gateway.getCustomerId();
                if (customerId != null && !customerId.isNullUid()) {
                    metaData.putValue("customerId", customerId.toString());
                }
                metaData.putValue("gatewayId", gatewayId.toString());

                DeviceId deviceId = device.getId();
                JsonNode entityNode = JacksonUtil.valueToTree(device);
                TbMsg tbMsg = TbMsg.newMsg(TbMsgType.ENTITY_CREATED, deviceId, customerId, metaData, TbMsgDataType.JSON, JacksonUtil.toString(entityNode));
                tbClusterService.pushMsgToRuleEngine(tenantId, deviceId, tbMsg, null);
            } else {
                JsonNode deviceAdditionalInfo = device.getAdditionalInfo();
                if (deviceAdditionalInfo == null) {
                    deviceAdditionalInfo = JacksonUtil.newObjectNode();
                }
                if (deviceAdditionalInfo.isObject() &&
                        (!deviceAdditionalInfo.has(DataConstants.LAST_CONNECTED_GATEWAY)
                                || !gatewayId.toString().equals(deviceAdditionalInfo.get(DataConstants.LAST_CONNECTED_GATEWAY).asText()))) {
                    ObjectNode newDeviceAdditionalInfo = (ObjectNode) deviceAdditionalInfo;
                    newDeviceAdditionalInfo.put(DataConstants.LAST_CONNECTED_GATEWAY, gatewayId.toString());
                    Device savedDevice = deviceService.saveDevice(device);
                    tbClusterService.onDeviceUpdated(savedDevice, device);
                }
            }
            GetOrCreateDeviceFromGatewayResponseMsg.Builder builder = GetOrCreateDeviceFromGatewayResponseMsg.newBuilder()
                    .setDeviceInfo(getDeviceInfoProto(device));
            DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
            if (deviceProfile != null) {
                builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
            } else {
                log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
            }
            return TransportApiResponseMsg.newBuilder()
                    .setGetOrCreateDeviceResponseMsg(builder.build())
                    .build();
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to lookup device by gateway id and name: [{}]", gatewayId, requestMsg.getDeviceName(), e);
            throw new RuntimeException(e);
        } catch (EntitiesLimitException e) {
            log.warn("[{}][{}] API limit exception: [{}]", e.getTenantId(), gatewayId, e.getMessage());
            return TransportApiResponseMsg.newBuilder()
                    .setGetOrCreateDeviceResponseMsg(
                            GetOrCreateDeviceFromGatewayResponseMsg.newBuilder()
                                    .setError(TransportProtos.TransportApiRequestErrorCode.ENTITY_LIMIT))
                    .build();
        } finally {
            deviceCreationLock.unlock();
        }
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(ProvisionDeviceRequestMsg requestMsg) {
        ListenableFuture<ProvisionResponse> provisionResponseFuture = null;
        try {
            provisionResponseFuture = Futures.immediateFuture(deviceProvisionService.provisionDevice(
                    new ProvisionRequest(
                            requestMsg.getDeviceName(),
                            requestMsg.getCredentialsType() != null ? DeviceCredentialsType.valueOf(requestMsg.getCredentialsType().name()) : null,
                            new ProvisionDeviceCredentialsData(requestMsg.getCredentialsDataProto().getValidateDeviceTokenRequestMsg().getToken(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getClientId(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getUserName(),
                                    requestMsg.getCredentialsDataProto().getValidateBasicMqttCredRequestMsg().getPassword(),
                                    requestMsg.getCredentialsDataProto().getValidateDeviceX509CertRequestMsg().getHash()),
                            new ProvisionDeviceProfileCredentials(
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceKey(),
                                    requestMsg.getProvisionDeviceCredentialsMsg().getProvisionDeviceSecret()))));
        } catch (ProvisionFailedException e) {
            return Futures.immediateFuture(getTransportApiResponseMsg(
                    new DeviceCredentials(),
                    TransportProtos.ResponseStatus.valueOf(e.getMessage())));
        }
        return Futures.transform(provisionResponseFuture, provisionResponse -> getTransportApiResponseMsg(provisionResponse.getDeviceCredentials(), TransportProtos.ResponseStatus.SUCCESS),
                dbCallbackExecutorService);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * - `status`：`status` 参数。
     * 返回：处理结果。
     */
    private TransportApiResponseMsg getTransportApiResponseMsg(DeviceCredentials
                                                                       deviceCredentials, TransportProtos.ResponseStatus status) {
        if (!status.equals(TransportProtos.ResponseStatus.SUCCESS)) {
            return TransportApiResponseMsg.newBuilder().setProvisionDeviceResponseMsg(TransportProtos.ProvisionDeviceResponseMsg.newBuilder().setStatus(status).build()).build();
        }
        TransportProtos.ProvisionDeviceResponseMsg.Builder provisionResponse = TransportProtos.ProvisionDeviceResponseMsg.newBuilder()
                .setCredentialsType(TransportProtos.CredentialsType.valueOf(deviceCredentials.getCredentialsType().name()))
                .setStatus(status);
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
            case X509_CERTIFICATE:
            case LWM2M_CREDENTIALS:
                provisionResponse.setCredentialsValue(deviceCredentials.getCredentialsValue());
                break;
        }

        return TransportApiResponseMsg.newBuilder()
                .setProvisionDeviceResponseMsg(provisionResponse.build())
                .build();
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(GetEntityProfileRequestMsg requestMsg) {
        EntityType entityType = EntityType.valueOf(requestMsg.getEntityType());
        UUID entityUuid = new UUID(requestMsg.getEntityIdMSB(), requestMsg.getEntityIdLSB());
        GetEntityProfileResponseMsg.Builder builder = GetEntityProfileResponseMsg.newBuilder();
        if (entityType.equals(EntityType.DEVICE_PROFILE)) {
            DeviceProfileId deviceProfileId = new DeviceProfileId(entityUuid);
            DeviceProfile deviceProfile = deviceProfileCache.find(deviceProfileId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
        } else if (entityType.equals(EntityType.TENANT)) {
            TenantId tenantId = TenantId.fromUUID(entityUuid);
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            ApiUsageState state = apiUsageStateService.getApiUsageState(tenantId);
            builder.setData(ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile)));
            builder.setApiState(ByteString.copyFrom(dataDecodingEncodingService.encode(state)));
        } else {
            throw new RuntimeException("Invalid entity profile request: " + entityType);
        }
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setEntityProfileResponseMsg(builder).build());
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(GetDeviceRequestMsg requestMsg) {
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, deviceId);

        TransportApiResponseMsg responseMsg;
        if (device != null) {
            UUID deviceProfileId = device.getDeviceProfileId().getId();
            responseMsg = TransportApiResponseMsg.newBuilder()
                    .setDeviceResponseMsg(TransportProtos.GetDeviceResponseMsg.newBuilder()
                            .setDeviceProfileIdMSB(deviceProfileId.getMostSignificantBits())
                            .setDeviceProfileIdLSB(deviceProfileId.getLeastSignificantBits())
                            .setDeviceTransportConfiguration(ByteString.copyFrom(
                                    dataDecodingEncodingService.encode(device.getDeviceData().getTransportConfiguration())
                            )))
                    .build();
        } else {
            responseMsg = TransportApiResponseMsg.getDefaultInstance();
        }

        return Futures.immediateFuture(responseMsg);
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(GetDeviceCredentialsRequestMsg requestMsg) {
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(TenantId.SYS_TENANT_ID, deviceId);

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .setDeviceCredentialsResponseMsg(TransportProtos.GetDeviceCredentialsResponseMsg.newBuilder()
                        .setDeviceCredentialsData(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceCredentials))))
                .build());
    }


    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(GetResourceRequestMsg requestMsg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        ResourceType resourceType = ResourceType.valueOf(requestMsg.getResourceType());
        String resourceKey = requestMsg.getResourceKey();
        TransportProtos.GetResourceResponseMsg.Builder builder = TransportProtos.GetResourceResponseMsg.newBuilder();
        TbResource resource = resourceService.findResourceByTenantIdAndKey(tenantId, resourceType, resourceKey);

        if (resource == null && !tenantId.equals(TenantId.SYS_TENANT_ID)) {
            resource = resourceService.findResourceByTenantIdAndKey(TenantId.SYS_TENANT_ID, resourceType, resourceKey);
        }

        if (resource != null) {
            builder.setResource(ByteString.copyFrom(dataDecodingEncodingService.encode(resource)));
        }

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setResourceResponseMsg(builder).build());
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(GetSnmpDevicesRequestMsg requestMsg) {
        PageLink pageLink = new PageLink(requestMsg.getPageSize(), requestMsg.getPage());
        PageData<UUID> result = deviceService.findDevicesIdsByDeviceProfileTransportType(DeviceTransportType.SNMP, pageLink);

        GetSnmpDevicesResponseMsg responseMsg = GetSnmpDevicesResponseMsg.newBuilder()
                .addAllIds(result.getData().stream()
                        .map(UUID::toString)
                        .collect(Collectors.toList()))
                .setHasNextPage(result.hasNext())
                .build();

        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .setSnmpDevicesResponseMsg(responseMsg)
                .build());
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `credentials`：`credentials` 参数。
     * 返回：处理结果。
     */
    TransportApiResponseMsg getDeviceInfo(DeviceCredentials credentials) {
        Device device = deviceService.findDeviceById(TenantId.SYS_TENANT_ID, credentials.getDeviceId());
        if (device == null) {
            log.trace("[{}] Failed to lookup device by id", credentials.getDeviceId());
            return getEmptyTransportApiResponse();
        }
        try {
            ValidateDeviceCredentialsResponseMsg.Builder builder = ValidateDeviceCredentialsResponseMsg.newBuilder();
            builder.setDeviceInfo(getDeviceInfoProto(device));
            DeviceProfile deviceProfile = deviceProfileCache.get(device.getTenantId(), device.getDeviceProfileId());
            if (deviceProfile != null) {
                builder.setProfileBody(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile)));
            } else {
                log.warn("[{}] Failed to find device profile [{}] for device. ", device.getId(), device.getDeviceProfileId());
            }
            if (!StringUtils.isEmpty(credentials.getCredentialsValue())) {
                builder.setCredentialsBody(credentials.getCredentialsValue());
            }
            return TransportApiResponseMsg.newBuilder()
                    .setValidateCredResponseMsg(builder.build()).build();
        } catch (JsonProcessingException e) {
            log.warn("[{}] Failed to lookup device by id", credentials.getDeviceId(), e);
            return getEmptyTransportApiResponse();
        }
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private DeviceInfoProto getDeviceInfoProto(Device device) throws JsonProcessingException {
        DeviceInfoProto.Builder builder = DeviceInfoProto.newBuilder()
                .setTenantIdMSB(device.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(device.getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(Optional.ofNullable(device.getCustomerId()).map(customerId -> customerId.getId().getMostSignificantBits()).orElse(0L))
                .setCustomerIdLSB(Optional.ofNullable(device.getCustomerId()).map(customerId -> customerId.getId().getLeastSignificantBits()).orElse(0L))
                .setDeviceIdMSB(device.getId().getId().getMostSignificantBits())
                .setDeviceIdLSB(device.getId().getId().getLeastSignificantBits())
                .setDeviceName(device.getName())
                .setDeviceType(device.getType())
                .setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits())
                .setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));

        PowerSavingConfiguration psmConfiguration = null;
        switch (device.getDeviceData().getTransportConfiguration().getType()) {
            case LWM2M:
                psmConfiguration = (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
            case COAP:
                psmConfiguration = (CoapDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
                break;
        }

        if (psmConfiguration != null) {
            PowerMode powerMode = psmConfiguration.getPowerMode();
            if (powerMode != null) {
                builder.setPowerMode(powerMode.name());
                if (powerMode.equals(PowerMode.PSM)) {
                    builder.setPsmActivityTimer(checkLong(psmConfiguration.getPsmActivityTimer()));
                } else if (powerMode.equals(PowerMode.E_DRX)) {
                    builder.setEdrxCycle(checkLong(psmConfiguration.getEdrxCycle()));
                    builder.setPagingTransmissionWindow(checkLong(psmConfiguration.getPagingTransmissionWindow()));
                }
            }
        }
        return builder.build();
    }

    /**
     * 功能：获取响应。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> getEmptyTransportApiResponseFuture() {
        return Futures.immediateFuture(getEmptyTransportApiResponse());
    }

    /**
     * 功能：获取响应。
     * 参数：无。
     * 返回：处理结果。
     */
    private TransportApiResponseMsg getEmptyTransportApiResponse() {
        return TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(ValidateDeviceCredentialsResponseMsg.getDefaultInstance()).build();
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.LwM2MRequestMsg requestMsg) {
        if (requestMsg.hasRegistrationMsg()) {
            return handleRegistration(requestMsg.getRegistrationMsg());
        } else {
            return Futures.immediateFailedFuture(new RuntimeException("Not supported!"));
        }
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.GetOtaPackageRequestMsg requestMsg) {
        TenantId tenantId = TenantId.fromUUID(new UUID(requestMsg.getTenantIdMSB(), requestMsg.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(requestMsg.getDeviceIdMSB(), requestMsg.getDeviceIdLSB()));
        OtaPackageType otaPackageType = OtaPackageType.valueOf(requestMsg.getType());
        Device device = deviceService.findDeviceById(tenantId, deviceId);

        if (device == null) {
            return getEmptyTransportApiResponseFuture();
        }

        OtaPackageId otaPackageId = OtaPackageUtil.getOtaPackageId(device, otaPackageType);
        if (otaPackageId == null) {
            DeviceProfile deviceProfile = deviceProfileCache.find(device.getDeviceProfileId());
            otaPackageId = OtaPackageUtil.getOtaPackageId(deviceProfile, otaPackageType);
        }

        TransportProtos.GetOtaPackageResponseMsg.Builder builder = TransportProtos.GetOtaPackageResponseMsg.newBuilder();

        if (otaPackageId == null) {
            builder.setResponseStatus(TransportProtos.ResponseStatus.NOT_FOUND);
        } else {
            OtaPackageInfo otaPackageInfo = otaPackageService.findOtaPackageInfoById(tenantId, otaPackageId);

            if (otaPackageInfo == null) {
                builder.setResponseStatus(TransportProtos.ResponseStatus.NOT_FOUND);
            } else if (otaPackageInfo.hasUrl()) {
                builder.setResponseStatus(TransportProtos.ResponseStatus.FAILURE);
                log.trace("[{}] Can`t send OtaPackage with URL data!", otaPackageInfo.getId());
            } else {
                builder.setResponseStatus(TransportProtos.ResponseStatus.SUCCESS);
                builder.setOtaPackageIdMSB(otaPackageId.getId().getMostSignificantBits());
                builder.setOtaPackageIdLSB(otaPackageId.getId().getLeastSignificantBits());
                builder.setType(otaPackageInfo.getType().name());
                builder.setTitle(otaPackageInfo.getTitle());
                builder.setVersion(otaPackageInfo.getVersion());
                builder.setFileName(otaPackageInfo.getFileName());
                builder.setContentType(otaPackageInfo.getContentType());
                if (!otaPackageDataCache.has(otaPackageId.toString())) {
                    OtaPackage otaPackage = otaPackageService.findOtaPackageById(tenantId, otaPackageId);
                    otaPackageDataCache.put(otaPackageId.toString(), otaPackage.getData().array());
                }
            }
        }

        return Futures.immediateFuture(
                TransportApiResponseMsg.newBuilder()
                        .setOtaPackageResponseMsg(builder.build())
                        .build());
    }

    private ListenableFuture<TransportApiResponseMsg> handleRegistration
            (TransportProtos.LwM2MRegistrationRequestMsg msg) {
        TenantId tenantId = TenantId.fromUUID(UUID.fromString(msg.getTenantId()));
        String deviceName = msg.getEndpoint();
        Lock deviceCreationLock = deviceCreationLocks.computeIfAbsent(deviceName, id -> new ReentrantLock());
        deviceCreationLock.lock();
        try {
            Device device = deviceService.findDeviceByTenantIdAndName(tenantId, deviceName);
            if (device == null) {
                device = new Device();
                device.setTenantId(tenantId);
                device.setName(deviceName);
                device.setType("LwM2M");
                device = deviceService.saveDevice(device);
                tbClusterService.onDeviceUpdated(device, null);
            }
            TransportProtos.LwM2MRegistrationResponseMsg registrationResponseMsg =
                    TransportProtos.LwM2MRegistrationResponseMsg.newBuilder()
                            .setDeviceInfo(getDeviceInfoProto(device)).build();
            TransportProtos.LwM2MResponseMsg responseMsg = TransportProtos.LwM2MResponseMsg.newBuilder().setRegistrationMsg(registrationResponseMsg).build();
            return Futures.immediateFuture(TransportApiResponseMsg.newBuilder().setLwM2MResponseMsg(responseMsg).build());
        } catch (JsonProcessingException e) {
            log.warn("[{}][{}] Failed to lookup device by gateway id and name", tenantId, deviceName, e);
            throw new RuntimeException(e);
        } finally {
            deviceCreationLock.unlock();
        }
    }

    /**
     * 功能：执行 `handle` 对应的处理。
     * 参数：
     * - `requestMsg`：请求对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> handle(TransportProtos.GetAllQueueRoutingInfoRequestMsg requestMsg) {
        return queuesToTransportApiResponseMsg(queueService.findAllQueues());
    }

    /**
     * 功能：执行 `queuesToTransportApiResponseMsg` 对应的处理。
     * 参数：
     * - `queues`：队列名称或队列对象。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<TransportApiResponseMsg> queuesToTransportApiResponseMsg(List<Queue> queues) {
        return Futures.immediateFuture(TransportApiResponseMsg.newBuilder()
                .addAllGetQueueRoutingInfoResponseMsgs(queues.stream()
                        .map(queue -> TransportProtos.GetQueueRoutingInfoResponseMsg.newBuilder()
                                .setTenantIdMSB(queue.getTenantId().getId().getMostSignificantBits())
                                .setTenantIdLSB(queue.getTenantId().getId().getLeastSignificantBits())
                                .setQueueIdMSB(queue.getId().getId().getMostSignificantBits())
                                .setQueueIdLSB(queue.getId().getId().getLeastSignificantBits())
                                .setQueueName(queue.getName())
                                .setQueueTopic(queue.getTopic())
                                .setPartitions(queue.getPartitions())
                                .build()).collect(Collectors.toList())).build());
    }


    /**
     * 功能：校验`Long`。
     * 参数：
     * - `l`：`l` 参数。
     * 返回：判断结果。
     */
    private Long checkLong(Long l) {
        return l != null ? l : 0;
    }

    /**
     * 功能：保存或创建请求。
     * 参数：
     * - `certificateValue`：值。
     * 返回：处理结果。
     */
    private ProvisionRequest createProvisionRequest(String certificateValue) {
        return new ProvisionRequest(null, DeviceCredentialsType.X509_CERTIFICATE,
                new ProvisionDeviceCredentialsData(null, null, null, null, certificateValue),
                null);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTransportApiService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
