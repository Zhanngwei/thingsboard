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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AbstractDeviceEntity` 是 ThingsBoard DAO 中表示设备持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `Device`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonStringType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
@MappedSuperclass
public abstract class AbstractDeviceEntity<T extends Device> extends BaseSqlEntity<T> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;

    /**
     * 客户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = ModelConstants.DEVICE_TYPE_PROPERTY)
    private String type;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.DEVICE_NAME_PROPERTY)
    private String name;

    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.DEVICE_LABEL_PROPERTY)
    private String label;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.DEVICE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_DEVICE_PROFILE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID deviceProfileId;

    /**
     * `firmwareId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_FIRMWARE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID firmwareId;

    /**
     * `softwareId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_SOFTWARE_ID_PROPERTY, columnDefinition = "uuid")
    private UUID softwareId;

    /**
     * 设备，保存当前步骤读取或计算得到的内容。
     */
    @Type(type = "jsonb")
    @Column(name = ModelConstants.DEVICE_DEVICE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode deviceData;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY, columnDefinition = "uuid")
    private UUID externalId;

    /**
     * 功能：创建 `AbstractDeviceEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractDeviceEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractDeviceEntity` 实例，并初始化必要字段。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public AbstractDeviceEntity(Device device) {
        if (device.getId() != null) {
            this.setUuid(device.getUuidId());
        }
        this.setCreatedTime(device.getCreatedTime());
        if (device.getTenantId() != null) {
            this.tenantId = device.getTenantId().getId();
        }
        if (device.getCustomerId() != null) {
            this.customerId = device.getCustomerId().getId();
        }
        if (device.getDeviceProfileId() != null) {
            this.deviceProfileId = device.getDeviceProfileId().getId();
        }
        if (device.getFirmwareId() != null) {
            this.firmwareId = device.getFirmwareId().getId();
        }
        if (device.getSoftwareId() != null) {
            this.softwareId = device.getSoftwareId().getId();
        }
        this.deviceData = JacksonUtil.convertValue(device.getDeviceData(), ObjectNode.class);
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.additionalInfo = device.getAdditionalInfo();
        if (device.getExternalId() != null) {
            this.externalId = device.getExternalId().getId();
        }
    }

    /**
     * 功能：创建 `AbstractDeviceEntity` 实例，并初始化必要字段。
     * 参数：
     * - `deviceEntity`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public AbstractDeviceEntity(DeviceEntity deviceEntity) {
        this.setId(deviceEntity.getId());
        this.setCreatedTime(deviceEntity.getCreatedTime());
        this.tenantId = deviceEntity.getTenantId();
        this.customerId = deviceEntity.getCustomerId();
        this.deviceProfileId = deviceEntity.getDeviceProfileId();
        this.deviceData = deviceEntity.getDeviceData();
        this.type = deviceEntity.getType();
        this.name = deviceEntity.getName();
        this.label = deviceEntity.getLabel();
        this.additionalInfo = deviceEntity.getAdditionalInfo();
        this.firmwareId = deviceEntity.getFirmwareId();
        this.softwareId = deviceEntity.getSoftwareId();
        this.externalId = deviceEntity.getExternalId();
    }

    /**
     * 功能：执行 `toDevice` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Device toDevice() {
        Device device = new Device(new DeviceId(getUuid()));
        device.setCreatedTime(createdTime);
        if (tenantId != null) {
            device.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            device.setCustomerId(new CustomerId(customerId));
        }
        if (deviceProfileId != null) {
            device.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        if (firmwareId != null) {
            device.setFirmwareId(new OtaPackageId(firmwareId));
        }
        if (softwareId != null) {
            device.setSoftwareId(new OtaPackageId(softwareId));
        }
        device.setDeviceData(JacksonUtil.convertValue(deviceData, DeviceData.class));
        device.setName(name);
        device.setType(type);
        device.setLabel(label);
        device.setAdditionalInfo(additionalInfo);
        if (externalId != null) {
            device.setExternalId(new DeviceId(externalId));
        }
        return device;
    }

}
