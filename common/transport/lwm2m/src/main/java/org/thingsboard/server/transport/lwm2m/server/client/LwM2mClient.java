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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.codec.LwM2mValueConverter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.transport.lwm2m.config.TbLwM2mVersion;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_PATH;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertMultiResourceValuesFromRpcBody;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.convertObjectIdToVersionedId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.equalsResourceTypeGetSimpleName;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.fromVersionedIdToObjectId;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.getVerFromPathIdVerOrId;

/**
 * 中文说明：
 * 1. `LwM2mClient` 是 ThingsBoard Common Transport 中访问 LwM2M 的客户端封装。
 * 2. 它把连接建立、请求发送、认证信息和响应解析集中到统一入口。
 * 3. 公开方法以平台数据模型作为输入输出，隐藏底层通信细节。
 * 4. 它直接协作于网络客户端、认证模型和请求响应对象。
 * 5. 独立客户端可以保持调用 API 稳定，并避免使用方重复处理连接与序列化。
 * 6. 阅读时重点关注连接配置、认证状态、请求构造和资源释放。
 */
@Slf4j
@EqualsAndHashCode(of = {"endpoint"})
@ToString(of = "endpoint")
public class LwM2mClient {

    /**
     * 节点实例ID，用于定位对应业务对象。
     */
    @Getter
    private final String nodeId;
    /**
     * `endpoint` 字段，保存当前对象的对应属性。
     */
    @Getter
    private final String endpoint;

    /**
     * 锁，用于保护并发读写的共享状态。
     */
    private final Lock lock;

    /**
     * `resources`映射关系，用于按键查找对应值。
     */
    @Getter
    private final Map<String, ResourceValue> resources;
    /**
     * `sharedAttributes`映射关系，用于按键查找对应值。
     */
    @Getter
    private final Map<String, TsKvProto> sharedAttributes;
    /**
     * 时间戳映射关系，用于按键查找对应值。
     */
    @Getter
    private final ConcurrentMap<String, AtomicLong> keyTsLatestMap;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    private TenantId tenantId;
    /**
     * 配置ID，用于定位对应业务对象。
     */
    @Getter
    private UUID profileId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    @Getter
    private UUID deviceId;
    /**
     * 状态，表示当前对象所处状态。
     */
    @Getter
    @Setter
    private LwM2MClientState state;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Getter
    private SessionInfoProto session;
    /**
     * `powerMode` 字段，保存当前对象的对应属性。
     */
    @Getter
    private PowerMode powerMode;
    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    @Getter
    private Long psmActivityTimer;
    /**
     * `pagingTransmissionWindow` 字段，保存当前对象的对应属性。
     */
    @Getter
    private Long pagingTransmissionWindow;
    /**
     * `edrxCycle` 字段，保存当前对象的对应属性。
     */
    @Getter
    private Long edrxCycle;
    /**
     * `registration` 字段，保存当前对象的对应属性。
     */
    @Getter
    private Registration registration;
    /**
     * 是否满足`asleep`条件。
     */
    @Getter
    @Setter
    private boolean asleep;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    @Getter
    private long lastUplinkTime;
    /**
     * `sleepTask` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    private Future<Void> sleepTask;

    /**
     * 是否满足`firstEdrxDownlink`条件。
     */
    private boolean firstEdrxDownlink = true;

    /**
     * 客户端集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private Set<ContentFormat> clientSupportContentFormats;
    /**
     * 内容格式，表示当前对象的对应属性。
     */
    @Getter
    private ContentFormat defaultContentFormat;
    /**
     * 重试次数，表示当前对象的对应属性。
     */
    @Getter
    private final AtomicInteger retryAttempts;

    /**
     * RPCID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private UUID lastSentRpcId;

    /**
     * 功能：执行 `clone` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * 功能：创建 `LwM2mClient` 实例，并初始化必要字段。
     * 参数：
     * - `nodeId`：节点实例ID。
     * - `endpoint`：`endpoint` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2mClient(String nodeId, String endpoint) {
        this.nodeId = nodeId;
        this.endpoint = endpoint;
        this.sharedAttributes = new ConcurrentHashMap<>();
        this.resources = new ConcurrentHashMap<>();
        this.keyTsLatestMap = new ConcurrentHashMap<>();
        this.state = LwM2MClientState.CREATED;
        this.lock = new ReentrantLock();
        this.retryAttempts = new AtomicInteger(0);
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `credentials`：`credentials` 参数。
     * - `sessionId`：会话ID。
     * 返回：无。
     */
    public void init(ValidateDeviceCredentialsResponse credentials, UUID sessionId) {
        this.session = createSession(nodeId, sessionId, credentials);
        this.tenantId = TenantId.fromUUID(new UUID(session.getTenantIdMSB(), session.getTenantIdLSB()));
        this.deviceId = new UUID(session.getDeviceIdMSB(), session.getDeviceIdLSB());
        this.profileId = new UUID(session.getDeviceProfileIdMSB(), session.getDeviceProfileIdLSB());
        this.powerMode = credentials.getDeviceInfo().getPowerMode();
        this.edrxCycle = credentials.getDeviceInfo().getEdrxCycle();
        this.psmActivityTimer = credentials.getDeviceInfo().getPsmActivityTimer();
        this.pagingTransmissionWindow = credentials.getDeviceInfo().getPagingTransmissionWindow();
    }

    /**
     * 功能：更新`Registration`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    public void setRegistration(Registration registration) {
        this.registration = registration;
        this.clientSupportContentFormats = clientSupportContentFormat(registration);
        this.defaultContentFormat = calculateDefaultContentFormat(registration);
    }

    /**
     * 功能：执行 `lock` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void lock() {
        lock.lock();
    }

    /**
     * 功能：执行 `unlock` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void unlock() {
        lock.unlock();
    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceProfileOpt`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceUpdate(Device device, Optional<DeviceProfile> deviceProfileOpt) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        this.deviceId = device.getUuidId();
        builder.setDeviceIdMSB(deviceId.getMostSignificantBits());
        builder.setDeviceIdLSB(deviceId.getLeastSignificantBits());
        builder.setDeviceName(device.getName());
        deviceProfileOpt.ifPresent(deviceProfile -> updateSession(deviceProfile, builder));
        this.session = builder.build();
        Lwm2mDeviceTransportConfiguration transportConfiguration = (Lwm2mDeviceTransportConfiguration) device.getDeviceData().getTransportConfiguration();
        this.powerMode = transportConfiguration.getPowerMode();
        this.edrxCycle = transportConfiguration.getEdrxCycle();
        this.psmActivityTimer = transportConfiguration.getPsmActivityTimer();
        this.pagingTransmissionWindow = transportConfiguration.getPagingTransmissionWindow();
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    public void onDeviceProfileUpdate(DeviceProfile deviceProfile) {
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        updateSession(deviceProfile, builder);
        this.session = builder.build();
    }

    /**
     * 功能：更新会话。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `builder`：`builder` 参数。
     * 返回：无。
     */
    private void updateSession(DeviceProfile deviceProfile, SessionInfoProto.Builder builder) {
        this.profileId = deviceProfile.getUuidId();
        builder.setDeviceProfileIdMSB(profileId.getMostSignificantBits());
        builder.setDeviceProfileIdLSB(profileId.getLeastSignificantBits());
        builder.setDeviceType(deviceProfile.getName());
    }

    /**
     * 功能：更新会话。
     * 参数：
     * - `nodeId`：节点实例ID。
     * 返回：无。
     */
    public void refreshSessionId(String nodeId) {
        UUID newId = UUID.randomUUID();
        SessionInfoProto.Builder builder = SessionInfoProto.newBuilder().mergeFrom(session);
        builder.setNodeId(nodeId);
        builder.setSessionIdMSB(newId.getMostSignificantBits());
        builder.setSessionIdLSB(newId.getLeastSignificantBits());
        this.session = builder.build();
    }

    /**
     * 功能：保存或创建会话。
     * 参数：
     * - `nodeId`：节点实例ID。
     * - `sessionId`：会话ID。
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    private SessionInfoProto createSession(String nodeId, UUID sessionId, ValidateDeviceCredentialsResponse msg) {
        return SessionInfoProto.newBuilder()
                .setNodeId(nodeId)
                .setSessionIdMSB(sessionId.getMostSignificantBits())
                .setSessionIdLSB(sessionId.getLeastSignificantBits())
                .setDeviceIdMSB(msg.getDeviceInfo().getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(msg.getDeviceInfo().getDeviceId().getId().getLeastSignificantBits())
                .setTenantIdMSB(msg.getDeviceInfo().getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(msg.getDeviceInfo().getTenantId().getId().getLeastSignificantBits())
                .setCustomerIdMSB(msg.getDeviceInfo().getCustomerId().getId().getMostSignificantBits())
                .setCustomerIdLSB(msg.getDeviceInfo().getCustomerId().getId().getLeastSignificantBits())
                .setDeviceName(msg.getDeviceInfo().getDeviceName())
                .setDeviceType(msg.getDeviceInfo().getDeviceType())
                .setDeviceProfileIdMSB(msg.getDeviceInfo().getDeviceProfileId().getId().getMostSignificantBits())
                .setDeviceProfileIdLSB(msg.getDeviceInfo().getDeviceProfileId().getId().getLeastSignificantBits())
                .build();
    }

    /**
     * 功能：保存或创建值。
     * 参数：
     * - `pathRezIdVer`：文件或资源路径。
     * - `resource`：`resource` 参数。
     * - `modelProvider`：`modelProvider` 参数。
     * - `mode`：`mode` 参数。
     * 返回：判断结果。
     */
    public boolean saveResourceValue(String pathRezIdVer, LwM2mResource resource, LwM2mModelProvider modelProvider, Mode mode) {
        if (this.resources.get(pathRezIdVer) != null && this.resources.get(pathRezIdVer).getResourceModel() != null) {
            this.resources.get(pathRezIdVer).updateLwM2mResource(resource, mode);
            return true;
        } else {
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.put(pathRezIdVer, new ResourceValue(resource, resourceModel));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `pathRezIdVer`：文件或资源路径。
     * - `pathRezId`：路径ID。
     * 返回：处理结果。
     */
    public Object getResourceValue(String pathRezIdVer, String pathRezId) {
        String pathRez = pathRezIdVer == null ? convertObjectIdToVersionedId(pathRezId, this.registration) : pathRezIdVer;
        if (this.resources.get(pathRez) != null) {
            return this.resources.get(pathRez).getLwM2mResource().getValue();
        }
        return null;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `pathRezIdVer`：文件或资源路径。
     * - `pathRezId`：路径ID。
     * 返回：处理结果。
     */
    public Object getResourceNameByRezId(String pathRezIdVer, String pathRezId) {
        String pathRez = pathRezIdVer == null ? convertObjectIdToVersionedId(pathRezId, this.registration) : pathRezIdVer;
        if (this.resources.get(pathRez) != null) {
            return this.resources.get(pathRez).getResourceModel().name;
        }
        return null;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `resourceName`：名称。
     * - `pathObjectInstanceIdVer`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：文本结果。
     */
    public String getRezIdByResourceNameAndObjectInstanceId(String resourceName, String pathObjectInstanceIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathObjectInstanceIdVer));
        if (pathIds.isObjectInstance()) {
            Set<Integer> rezIds = modelProvider.getObjectModel(registration)
                    .getObjectModel(pathIds.getObjectId()).resources.entrySet()
                    .stream()
                    .filter(map -> resourceName.equals(map.getValue().name))
                    .map(map -> map.getKey())
                    .collect(Collectors.toSet());
            return rezIds.size() > 0 ? String.valueOf(rezIds.stream().findFirst().get()) : null;
        }
        return null;
    }

    /**
     * 功能：获取`Resource Model`。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：处理结果。
     */
    public ResourceModel getResourceModel(String pathIdVer, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        String verRez = getVerFromPathIdVerOrId(pathIdVer);
        return verRez != null && verRez.equals(verSupportedObject) ? modelProvider.getObjectModel(registration)
                .getResourceModel(pathIds.getObjectId(), pathIds.getResourceId()) : null;
    }

    /**
     * 功能：判断`Resource Multi Instances`。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：判断结果。
     */
    public boolean isResourceMultiInstances(String pathIdVer, LwM2mModelProvider modelProvider) {
        var resourceModel = getResourceModel(pathIdVer, modelProvider);
        if (resourceModel != null && resourceModel.multiple != null) {
            return resourceModel.multiple;
        } else {
            return false;
        }
    }

    /**
     * 功能：获取`Object Model`。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：处理结果。
     */
    public ObjectModel getObjectModel(String pathIdVer, LwM2mModelProvider modelProvider) {
        try {
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathIdVer));
            String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
            String verRez = getVerFromPathIdVerOrId(pathIdVer);
            return verRez != null && verRez.equals(verSupportedObject) ? modelProvider.getObjectModel(registration)
                    .getObjectModel(pathIds.getObjectId()) : null;
        } catch (Exception e) {
            if (registration == null) {
                log.error("[{}] Failed Registration is null, GetObjectModelRegistration. ", this.endpoint, e);
            } else if (registration.getSupportedObject() == null) {
                log.error("[{}] Failed SupportedObject in Registration, GetObjectModelRegistration.", this.endpoint, e);
            } else {
                log.error("[{}] Failed ModelProvider.getObjectModel [{}] in Registration. ", this.endpoint, registration.getSupportedObject(), e);
            }
            return null;
        }
    }


    /**
     * 功能：获取`New Resource For Instance`。
     * 参数：
     * - `pathRezIdVer`：文件或资源路径。
     * - `params`：`params` 参数。
     * - `modelProvider`：`modelProvider` 参数。
     * - `converter`：`converter` 参数。
     * 返回：匹配的数据集合。
     */
    public Collection<LwM2mResource> getNewResourceForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                               LwM2mValueConverter converter) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        resourceModels.forEach((resId, resourceModel) -> {
            if (resId.equals(pathIds.getResourceId())) {
                resources.add(LwM2mSingleResource.newResource(resId, converter.convertValue(params,
                        equalsResourceTypeGetSimpleName(params), resourceModel.type, pathIds), resourceModel.type));

            }
        });
        return resources;
    }

    /**
     * The instance must have all the resources that have the property
     * <Mandatory>Mandatory</Mandatory>
     */
    /**
     * 功能：获取`New Resources For Instance`。
     * 参数：
     * - `pathRezIdVer`：文件或资源路径。
     * - `params`：`params` 参数。
     * - `modelProvider`：`modelProvider` 参数。
     * - `converter`：`converter` 参数。
     * 返回：匹配的数据集合。
     */
    public Collection<LwM2mResource> getNewResourcesForInstance(String pathRezIdVer, Object params, LwM2mModelProvider modelProvider,
                                                                LwM2mValueConverter converter) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRezIdVer));
        Collection<LwM2mResource> resources = ConcurrentHashMap.newKeySet();
        Map<Integer, ResourceModel> resourceModels = modelProvider.getObjectModel(registration)
                .getObjectModel(pathIds.getObjectId()).resources;
        if (params instanceof Map && ((Map) params).size() > 0) {
            Map paramsMap = (Map) params;
            resourceModels.forEach((resourceId, resourceModel) -> {
                if (paramsMap.containsKey(String.valueOf(resourceId))) {
                    Object value = paramsMap.get((String.valueOf(resourceId)));
                    LwM2mResource resource;
                    if (resourceModel.multiple) {
                        try {
                            Map<Integer, Object> values = convertMultiResourceValuesFromRpcBody(value, resourceModel.type, pathRezIdVer);
                            resource = LwM2mMultipleResource.newResource(resourceId, values, resourceModel.type);
                        } catch (Exception e) {
                            throw new IllegalArgumentException("Resource id=" + resourceId + ", class = " +
                                    value.getClass().getSimpleName() + ", value = " + value + " is bad. Value of Multi-Instance Resource must be in Json format!");
                        }
                    } else {
                        Object valueRez = value instanceof Integer ? ((Integer) value).longValue() : value;
                        resource = LwM2mSingleResource.newResource(resourceId,
                                converter.convertValue(valueRez, equalsResourceTypeGetSimpleName(value), resourceModel.type, pathIds), resourceModel.type);
                    }
                    if (resource != null) {
                        resources.add(resource);
                    } else if (resourceModel.operations.isWritable() && resourceModel.mandatory) {
                        throw new IllegalArgumentException("Resource id=" + resourceId + " is mandatory. The value of this resource must not be null.");
                    }
                } else if (resourceModel.operations.isWritable() && resourceModel.mandatory) {
                    throw new IllegalArgumentException("Resource id=" + resourceId + " is mandatory. The value of this resource must not be null.");
                }
            });
        } else if (params == null) {
            throw new IllegalArgumentException("The value of this resource must not be null.");
        } else {
            throw new IllegalArgumentException("The value of this resource must be in Map format and size > 0");
        }
        return resources;
    }

    /**
     * 功能：判断版本号。
     * 参数：
     * - `path`：文件或资源路径。
     * 返回：判断结果。
     */
    public String isValidObjectVersion(String path) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(path));
        String verSupportedObject = registration.getSupportedObject().get(pathIds.getObjectId());
        if (verSupportedObject == null) {
            return String.format("Specified object id %s absent in the list supported objects of the client or is security object!", pathIds.getObjectId());
        } else {
            String verRez = getVerFromPathIdVerOrId(path);
            if (verRez == null || !verRez.equals(verSupportedObject)) {
                return String.format("Specified resource id %s is not valid version! Must be version: %s", path, verSupportedObject);
            }
        }
        return "";
    }

    /**
     * @param pathIdVer     == "3_1.0"
     * @param modelProvider -
     */
    /**
     * 功能：删除或清理`Resources`。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：无。
     */
    public void deleteResources(String pathIdVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(pathIdVer);
        key.forEach(pathRez -> {
            LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRez));
            ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
            if (resourceModel != null) {
                this.resources.get(pathRez).setResourceModel(resourceModel);
            } else {
                this.resources.remove(pathRez);
            }
        });
    }

    /**
     * @param idVer         -
     * @param modelProvider -
     */
    /**
     * 功能：更新`Resource Model`。
     * 参数：
     * - `idVer`：`idVer` 参数。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：无。
     */
    public void updateResourceModel(String idVer, LwM2mModelProvider modelProvider) {
        Set<String> key = getKeysEqualsIdVer(idVer);
        key.forEach(k -> this.saveResourceModel(k, modelProvider));
    }

    /**
     * 功能：保存或创建`Resource Model`。
     * 参数：
     * - `pathRez`：文件或资源路径。
     * - `modelProvider`：`modelProvider` 参数。
     * 返回：无。
     */
    private void saveResourceModel(String pathRez, LwM2mModelProvider modelProvider) {
        LwM2mPath pathIds = new LwM2mPath(fromVersionedIdToObjectId(pathRez));
        ResourceModel resourceModel = modelProvider.getObjectModel(registration).getResourceModel(pathIds.getObjectId(), pathIds.getResourceId());
        this.resources.get(pathRez).setResourceModel(resourceModel);
    }

    /**
     * 功能：获取`Keys Equals Id Ver`。
     * 参数：
     * - `idVer`：`idVer` 参数。
     * 返回：匹配的数据集合。
     */
    private Set<String> getKeysEqualsIdVer(String idVer) {
        return this.resources.keySet()
                .stream()
                .filter(e -> idVer.equals(e.split(LWM2M_SEPARATOR_PATH)[1]))
                .collect(Collectors.toSet());
    }

    /**
     * 功能：执行 `calculateDefaultContentFormat` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private ContentFormat calculateDefaultContentFormat(Registration registration) {
        if (registration == null) {
            return ContentFormat.DEFAULT;
        } else {
            return TbLwM2mVersion.fromVersion(registration.getLwM2mVersion()).getContentFormat();
        }
    }

    /**
     * 功能：执行 `clientSupportContentFormat` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：匹配的数据集合。
     */
    private static Set<ContentFormat> clientSupportContentFormat(Registration registration) {
        Set<ContentFormat> contentFormats = new HashSet<>();
        contentFormats.add(ContentFormat.DEFAULT);
        LinkParamValue ct = Arrays.stream(registration.getObjectLinks())
                .filter(link -> link.getUriReference().equals("/"))
                .findFirst()
                .map(link -> link.getLinkParams().get("ct")).orElse(null);
        if (ct != null) {
            Set<ContentFormat> codes = Stream.of(ct.getUnquoted().replaceAll("\"", "").split(" ", -1))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .map(ContentFormat::fromCode)
                    .collect(Collectors.toSet());
            contentFormats.addAll(codes);
        }
        return contentFormats;
    }

    /**
     * 功能：更新时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long updateLastUplinkTime() {
        this.lastUplinkTime = System.currentTimeMillis();
        this.firstEdrxDownlink = true;
        return lastUplinkTime;
    }

    /**
     * 功能：校验`First Downlink`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean checkFirstDownlink() {
        boolean result = firstEdrxDownlink;
        firstEdrxDownlink = false;
        return result;
    }

}
