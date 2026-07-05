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
package org.thingsboard.server.service.ota;

import com.google.common.util.concurrent.FutureCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.common.msg.rule.engine.DeviceAttributesEventNotificationMsg;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.ota.OtaPackageUpdateStatus;
import org.thingsboard.server.common.data.ota.OtaPackageUtil;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbCoreQueueFactory;
import org.thingsboard.server.queue.provider.TbRuleEngineQueueFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.thingsboard.server.common.data.ota.OtaPackageKey.CHECKSUM;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.CHECKSUM_ALGORITHM;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.SIZE;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.STATE;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.TAG;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.TITLE;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.TS;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.URL;
import static org.thingsboard.server.common.data.ota.OtaPackageKey.VERSION;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getAttributeKey;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getTargetTelemetryKey;
import static org.thingsboard.server.common.data.ota.OtaPackageUtil.getTelemetryKey;

/**
 * 中文说明：
 * 1. 类目的：`DefaultOtaPackageStateService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
@Service
public class DefaultOtaPackageStateService implements OtaPackageStateService {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbClusterService tbClusterService;
    private final OtaPackageService otaPackageService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    private final DeviceService deviceService;
    private final DeviceProfileService deviceProfileService;
    /**
     * 遥测，提供当前类调用的业务操作。
     */
    private final RuleEngineTelemetryService telemetryService;
    private final TbQueueProducer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> otaPackageStateMsgProducer;

    /**
     * 功能：创建 `DefaultOtaPackageStateService` 实例，并初始化必要字段。
     * 参数：
     * - `tbClusterService`：服务对象。
     * - `otaPackageService`：服务对象。
     * - `deviceService`：设备信息或设备标识。
     * - `deviceProfileService`：设备信息或设备标识。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public DefaultOtaPackageStateService(@Lazy TbClusterService tbClusterService,
                                         OtaPackageService otaPackageService,
                                         DeviceService deviceService,
                                         DeviceProfileService deviceProfileService,
                                         @Lazy RuleEngineTelemetryService telemetryService,
                                         Optional<TbCoreQueueFactory> coreQueueFactory,
                                         Optional<TbRuleEngineQueueFactory> reQueueFactory) {
        this.tbClusterService = tbClusterService;
        this.otaPackageService = otaPackageService;
        this.deviceService = deviceService;
        this.deviceProfileService = deviceProfileService;
        this.telemetryService = telemetryService;
        if (coreQueueFactory.isPresent()) {
            this.otaPackageStateMsgProducer = coreQueueFactory.get().createToOtaPackageStateServiceMsgProducer();
        } else {
            this.otaPackageStateMsgProducer = reQueueFactory.get().createToOtaPackageStateServiceMsgProducer();
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void update(Device device, Device oldDevice) {
        updateFirmware(device, oldDevice);
        updateSoftware(device, oldDevice);
    }

    /**
     * 功能：更新`Firmware`。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * 返回：无。
     */
    private void updateFirmware(Device device, Device oldDevice) {
        OtaPackageId newFirmwareId = device.getFirmwareId();
        if (newFirmwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newFirmwareId = newDeviceProfile.getFirmwareId();
        }
        if (oldDevice != null) {
            OtaPackageId oldFirmwareId = oldDevice.getFirmwareId();
            if (oldFirmwareId == null) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                oldFirmwareId = oldDeviceProfile.getFirmwareId();
            }
            if (newFirmwareId != null) {
                if (!newFirmwareId.equals(oldFirmwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), FIRMWARE);
                }
            } else if (oldFirmwareId != null){
                // Device was updated and new firmware is not set.
                remove(device, FIRMWARE);
            }
        } else if (newFirmwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newFirmwareId, System.currentTimeMillis(), FIRMWARE);
        }
    }

    /**
     * 功能：更新`Software`。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `oldDevice`：设备信息或设备标识。
     * 返回：无。
     */
    private void updateSoftware(Device device, Device oldDevice) {
        OtaPackageId newSoftwareId = device.getSoftwareId();
        if (newSoftwareId == null) {
            DeviceProfile newDeviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
            newSoftwareId = newDeviceProfile.getSoftwareId();
        }
        if (oldDevice != null) {
            OtaPackageId oldSoftwareId = oldDevice.getSoftwareId();
            if (oldSoftwareId == null) {
                DeviceProfile oldDeviceProfile = deviceProfileService.findDeviceProfileById(oldDevice.getTenantId(), oldDevice.getDeviceProfileId());
                oldSoftwareId = oldDeviceProfile.getSoftwareId();
            }
            if (newSoftwareId != null) {
                if (!newSoftwareId.equals(oldSoftwareId)) {
                    // Device was updated and new firmware is different from previous firmware.
                    send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), SOFTWARE);
                }
            } else if (oldSoftwareId != null){
                // Device was updated and new firmware is not set.
                remove(device, SOFTWARE);
            }
        } else if (newSoftwareId != null) {
            // Device was created and firmware is defined.
            send(device.getTenantId(), device.getId(), newSoftwareId, System.currentTimeMillis(), SOFTWARE);
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `isFirmwareChanged`：`isFirmwareChanged` 参数。
     * - `isSoftwareChanged`：`isSoftwareChanged` 参数。
     * 返回：无。
     */
    @Override
    public void update(DeviceProfile deviceProfile, boolean isFirmwareChanged, boolean isSoftwareChanged) {
        TenantId tenantId = deviceProfile.getTenantId();

        if (isFirmwareChanged) {
            update(tenantId, deviceProfile, FIRMWARE);
        }
        if (isSoftwareChanged) {
            update(tenantId, deviceProfile, SOFTWARE);
        }
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `otaPackageType`：类型。
     * 返回：无。
     */
    private void update(TenantId tenantId, DeviceProfile deviceProfile, OtaPackageType otaPackageType) {
        Consumer<Device> updateConsumer;
        OtaPackageId packageId = OtaPackageUtil.getOtaPackageId(deviceProfile, otaPackageType);

        if (packageId != null) {
            long ts = System.currentTimeMillis();
            updateConsumer = d -> send(d.getTenantId(), d.getId(), packageId, ts, otaPackageType);
        } else {
            updateConsumer = d -> remove(d, otaPackageType);
        }

        PageLink pageLink = new PageLink(100);
        PageData<Device> pageData;
        do {
            pageData = deviceService.findDevicesByTenantIdAndTypeAndEmptyOtaPackage(tenantId, deviceProfile.getId(), otaPackageType, pageLink);
            pageData.getData().forEach(updateConsumer);

            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    public boolean process(ToOtaPackageStateServiceMsg msg) {
        boolean isSuccess = false;
        OtaPackageId targetOtaPackageId = new OtaPackageId(new UUID(msg.getOtaPackageIdMSB(), msg.getOtaPackageIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(msg.getDeviceIdMSB(), msg.getDeviceIdLSB()));
        TenantId tenantId = TenantId.fromUUID(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        OtaPackageType firmwareType = OtaPackageType.valueOf(msg.getType());
        long ts = msg.getTs();

        Device device = deviceService.findDeviceById(tenantId, deviceId);
        if (device == null) {
            log.warn("[{}] [{}] Device was removed during firmware update msg was queued!", tenantId, deviceId);
        } else {
            OtaPackageId currentOtaPackageId = OtaPackageUtil.getOtaPackageId(device, firmwareType);
            if (currentOtaPackageId == null) {
                DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, device.getDeviceProfileId());
                currentOtaPackageId = OtaPackageUtil.getOtaPackageId(deviceProfile, firmwareType);
            }

            if (targetOtaPackageId.equals(currentOtaPackageId)) {
                update(device, otaPackageService.findOtaPackageInfoById(device.getTenantId(), targetOtaPackageId), ts);
                isSuccess = true;
            } else {
                log.warn("[{}] [{}] Can`t update firmware for the device, target firmwareId: [{}], current firmwareId: [{}]!", tenantId, deviceId, targetOtaPackageId, currentOtaPackageId);
            }
        }
        return isSuccess;
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `firmwareId`：`firmwareId`ID。
     * - `ts`：时间戳。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void send(TenantId tenantId, DeviceId deviceId, OtaPackageId firmwareId, long ts, OtaPackageType firmwareType) {
        ToOtaPackageStateServiceMsg msg = ToOtaPackageStateServiceMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setOtaPackageIdMSB(firmwareId.getId().getMostSignificantBits())
                .setOtaPackageIdLSB(firmwareId.getId().getLeastSignificantBits())
                .setType(firmwareType.name())
                .setTs(ts)
                .build();

        OtaPackageInfo firmware = otaPackageService.findOtaPackageInfoById(tenantId, firmwareId);
        if (firmware == null) {
            log.warn("[{}] Failed to send firmware update because firmware was already deleted", firmwareId);
            return;
        }

        TopicPartitionInfo tpi = new TopicPartitionInfo(otaPackageStateMsgProducer.getDefaultTopic(), null, null, false);
        otaPackageStateMsgProducer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);

        List<TsKvEntry> telemetry = new ArrayList<>();
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), TITLE), firmware.getTitle())));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), VERSION), firmware.getVersion())));

        if (StringUtils.isNotEmpty(firmware.getTag())) {
            telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTargetTelemetryKey(firmware.getType(), TAG), firmware.getTag())));
        }

        telemetry.add(new BasicTsKvEntry(ts, new LongDataEntry(getTargetTelemetryKey(firmware.getType(), TS), ts)));
        telemetry.add(new BasicTsKvEntry(ts, new StringDataEntry(getTelemetryKey(firmware.getType(), STATE), OtaPackageUpdateStatus.QUEUED.name())));

        telemetryService.saveAndNotify(tenantId, deviceId, telemetry, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save firmware status!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save firmware status!", deviceId, t);
            }
        });
    }


    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `otaPackage`：`otaPackage` 参数。
     * - `ts`：时间戳。
     * 返回：无。
     */
    private void update(Device device, OtaPackageInfo otaPackage, long ts) {
        TenantId tenantId = device.getTenantId();
        DeviceId deviceId = device.getId();
        OtaPackageType otaPackageType = otaPackage.getType();

        BasicTsKvEntry status = new BasicTsKvEntry(System.currentTimeMillis(), new StringDataEntry(getTelemetryKey(otaPackageType, STATE), OtaPackageUpdateStatus.INITIATED.name()));

        telemetryService.saveAndNotify(tenantId, deviceId, Collections.singletonList(status), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save telemetry with target {} for device!", deviceId, otaPackage);
                updateAttributes(device, otaPackage, ts, tenantId, deviceId, otaPackageType);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save telemetry with target {} for device!", deviceId, otaPackage, t);
                updateAttributes(device, otaPackage, ts, tenantId, deviceId, otaPackageType);
            }
        });
    }

    /**
     * 功能：更新`Attributes`。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `otaPackage`：`otaPackage` 参数。
     * - `ts`：时间戳。
     * - `tenantId`：租户IDID。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    private void updateAttributes(Device device, OtaPackageInfo otaPackage, long ts, TenantId tenantId, DeviceId deviceId, OtaPackageType otaPackageType) {
        List<AttributeKvEntry> attributes = new ArrayList<>();
        List<String> attrToRemove = new ArrayList<>();
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, TITLE), otaPackage.getTitle())));
        attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, VERSION), otaPackage.getVersion())));
        if (StringUtils.isNotEmpty(otaPackage.getTag())) {
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, TAG), otaPackage.getTag())));
        } else {
            attrToRemove.add(getAttributeKey(otaPackageType, TAG));
        }
        if (otaPackage.hasUrl()) {
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, URL), otaPackage.getUrl())));

            if (otaPackage.getDataSize() == null) {
                attrToRemove.add(getAttributeKey(otaPackageType, SIZE));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(getAttributeKey(otaPackageType, SIZE), otaPackage.getDataSize())));
            }

            if (otaPackage.getChecksumAlgorithm() != null) {
                attrToRemove.add(getAttributeKey(otaPackageType, CHECKSUM_ALGORITHM));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, CHECKSUM_ALGORITHM), otaPackage.getChecksumAlgorithm().name())));
            }

            if (StringUtils.isEmpty(otaPackage.getChecksum())) {
                attrToRemove.add(getAttributeKey(otaPackageType, CHECKSUM));
            } else {
                attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, CHECKSUM), otaPackage.getChecksum())));
            }
        } else {
            attributes.add(new BaseAttributeKvEntry(ts, new LongDataEntry(getAttributeKey(otaPackageType, SIZE), otaPackage.getDataSize())));
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, CHECKSUM_ALGORITHM), otaPackage.getChecksumAlgorithm().name())));
            attributes.add(new BaseAttributeKvEntry(ts, new StringDataEntry(getAttributeKey(otaPackageType, CHECKSUM), otaPackage.getChecksum())));
            attrToRemove.add(getAttributeKey(otaPackageType, URL));
        }

        remove(device, otaPackageType, attrToRemove);

        telemetryService.saveAndNotify(tenantId, deviceId, DataConstants.SHARED_SCOPE, attributes, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable Void tmp) {
                log.trace("[{}] Success save attributes with target firmware!", deviceId);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to save attributes with target firmware!", deviceId, t);
            }
        });
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `otaPackageType`：类型。
     * 返回：无。
     */
    private void remove(Device device, OtaPackageType otaPackageType) {
        remove(device, otaPackageType, OtaPackageUtil.getAttributeKeys(otaPackageType));
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `otaPackageType`：类型。
     * - `attributesKeys`：键。
     * 返回：无。
     */
    private void remove(Device device, OtaPackageType otaPackageType, List<String> attributesKeys) {
        telemetryService.deleteAndNotify(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, attributesKeys,
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        log.trace("[{}] Success remove target {} attributes!", device.getId(), otaPackageType);
                        tbClusterService.pushMsgToCore(DeviceAttributesEventNotificationMsg.onDelete(device.getTenantId(), device.getId(), DataConstants.SHARED_SCOPE, attributesKeys), null);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.error("[{}] Failed to remove target {} attributes!", device.getId(), otaPackageType, t);
                    }
                });
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultOtaPackageStateService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
