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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`Device` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class Device extends BaseDataWithAdditionalInfo<DeviceId> implements HasLabel, HasTenantId, HasCustomerId, HasOtaPackage, ExportableEntity<DeviceId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 2807343040519543363L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private CustomerId customerId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    private String name;
    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @Length(fieldName = "type")
    private String type;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @NoXss
    @Length(fieldName = "label")
    private String label;
    private DeviceProfileId deviceProfileId;
    /**
     * 设备，保存当前步骤读取或计算得到的内容。
     */
    private transient DeviceData deviceData;
    /**
     * 设备列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] deviceDataBytes;

    /**
     * `firmwareId`ID，用于定位对应业务对象。
     */
    private OtaPackageId firmwareId;
    private OtaPackageId softwareId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter @Setter
    private DeviceId externalId;

    /**
     * 功能：创建 `Device` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Device() {
        super();
    }

    /**
     * 功能：创建 `Device` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public Device(DeviceId id) {
        super(id);
    }

    /**
     * 功能：创建 `Device` 实例，并初始化必要字段。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public Device(Device device) {
        super(device);
        this.tenantId = device.getTenantId();
        this.customerId = device.getCustomerId();
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.deviceProfileId = device.getDeviceProfileId();
        this.setDeviceData(device.getDeviceData());
        this.firmwareId = device.getFirmwareId();
        this.softwareId = device.getSoftwareId();
        this.externalId = device.getExternalId();
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    public Device updateDevice(Device device) {
        this.tenantId = device.getTenantId();
        this.customerId = device.getCustomerId();
        this.name = device.getName();
        this.type = device.getType();
        this.label = device.getLabel();
        this.deviceProfileId = device.getDeviceProfileId();
        this.setDeviceData(device.getDeviceData());
        this.setFirmwareId(device.getFirmwareId());
        this.setSoftwareId(device.getSoftwareId());
        Optional.ofNullable(device.getAdditionalInfo()).ifPresent(this::setAdditionalInfo);
        this.setExternalId(device.getExternalId());
        return this;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Device Id. " +
            "Specify this field to update the Device. " +
            "Referencing non-existing Device Id will cause error. " +
            "Omit this field to create new Device." )
    @Override
    public DeviceId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the device creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Use 'assignDeviceToTenant' to change the Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * 功能：更新租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * 功能：获取客户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Customer Id. Use 'assignDeviceToCustomer' to change the Customer Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    public CustomerId getCustomerId() {
        return customerId;
    }

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 5, required = true, value = "Unique Device Name in scope of Tenant", example = "A4B72CCDFF33")
    @Override
    public String getName() {
        return name;
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `name`：名称。
     * 返回：无。
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Device Profile Name", example = "Temperature Sensor")
    public String getType() {
        return type;
    }

    /**
     * 功能：更新类型。
     * 参数：
     * - `type`：类型。
     * 返回：无。
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * 功能：获取显示标签。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 7, value = "Label that may be used in widgets", example = "Room 234 Sensor")
    public String getLabel() {
        return label;
    }

    /**
     * 功能：更新显示标签。
     * 参数：
     * - `label`：`label` 参数。
     * 返回：无。
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * 功能：获取设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 8, required = true, value = "JSON object with Device Profile Id.")
    public DeviceProfileId getDeviceProfileId() {
        return deviceProfileId;
    }

    /**
     * 功能：更新设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * 返回：无。
     */
    public void setDeviceProfileId(DeviceProfileId deviceProfileId) {
        this.deviceProfileId = deviceProfileId;
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 9, value = "JSON object with content specific to type of transport in the device profile.")
    public DeviceData getDeviceData() {
        if (deviceData != null) {
            return deviceData;
        } else {
            if (deviceDataBytes != null) {
                try {
                    deviceData = mapper.readValue(new ByteArrayInputStream(deviceDataBytes), DeviceData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize device data: ", e);
                    return null;
                }
                return deviceData;
            } else {
                return null;
            }
        }
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    public void setDeviceData(DeviceData data) {
        this.deviceData = data;
        try {
            this.deviceDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize device data: ", e);
        }
    }

    /**
     * 功能：获取`Firmware Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 10, value = "JSON object with Ota Package Id.")
    public OtaPackageId getFirmwareId() {
        return firmwareId;
    }

    /**
     * 功能：更新`Firmware Id`。
     * 参数：
     * - `firmwareId`：`firmwareId`ID。
     * 返回：无。
     */
    public void setFirmwareId(OtaPackageId firmwareId) {
        this.firmwareId = firmwareId;
    }

    /**
     * 功能：获取`Software Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 11, value = "JSON object with Ota Package Id.")
    public OtaPackageId getSoftwareId() {
        return softwareId;
    }

    /**
     * 功能：更新`Software Id`。
     * 参数：
     * - `softwareId`：`softwareId`ID。
     * 返回：无。
     */
    public void setSoftwareId(OtaPackageId softwareId) {
        this.softwareId = softwareId;
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 12, value = "Additional parameters of the device", dataType = "com.fasterxml.jackson.databind.JsonNode")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Device [tenantId=");
        builder.append(tenantId);
        builder.append(", customerId=");
        builder.append(customerId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", label=");
        builder.append(label);
        builder.append(", deviceProfileId=");
        builder.append(deviceProfileId);
        builder.append(", deviceData=");
        builder.append(firmwareId);
        builder.append(", firmwareId=");
        builder.append(deviceData);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`Device` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
