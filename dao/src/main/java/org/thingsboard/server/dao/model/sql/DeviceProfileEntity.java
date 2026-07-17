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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonBinaryType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DeviceProfileEntity` 是 ThingsBoard DAO 中表示设备配置持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name = ModelConstants.DEVICE_PROFILE_TABLE_NAME)
public final class DeviceProfileEntity extends BaseSqlEntity<DeviceProfile> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_NAME_PROPERTY)
    private String name;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TYPE_PROPERTY)
    private DeviceProfileType type;

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_IMAGE_PROPERTY)
    private String image;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY)
    private DeviceTransportType transportType;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.DEVICE_PROFILE_PROVISION_TYPE_PROPERTY)
    private DeviceProfileProvisionType provisionType;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_DESCRIPTION_PROPERTY)
    private String description;

    /**
     * 当前对象是否为默认项。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_IS_DEFAULT_PROPERTY)
    private boolean isDefault;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultRuleChainId;

    /**
     * 仪表盘ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY)
    private UUID defaultDashboardId;

    /**
     * 队列名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY)
    private String defaultQueueName;

    /**
     * 配置，保存当前步骤读取或计算得到的内容。
     */
    @Type(type = "jsonb")
    @Column(name = ModelConstants.DEVICE_PROFILE_PROFILE_DATA_PROPERTY, columnDefinition = "jsonb")
    private JsonNode profileData;

    /**
     * 设备，用于定位映射、配置或数据项。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_PROVISION_DEVICE_KEY)
    private String provisionDeviceKey;

    /**
     * `firmwareId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_FIRMWARE_ID_PROPERTY)
    private UUID firmwareId;

    /**
     * `softwareId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_SOFTWARE_ID_PROPERTY)
    private UUID softwareId;

    /**
     * 规则链ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.DEVICE_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID defaultEdgeRuleChainId;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `DeviceProfileEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DeviceProfileEntity() {
        super();
    }

    /**
     * 功能：创建 `DeviceProfileEntity` 实例，并初始化必要字段。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DeviceProfileEntity(DeviceProfile deviceProfile) {
        if (deviceProfile.getId() != null) {
            this.setUuid(deviceProfile.getId().getId());
        }
        if (deviceProfile.getTenantId() != null) {
            this.tenantId = deviceProfile.getTenantId().getId();
        }
        this.setCreatedTime(deviceProfile.getCreatedTime());
        this.name = deviceProfile.getName();
        this.type = deviceProfile.getType();
        this.image = deviceProfile.getImage();
        this.transportType = deviceProfile.getTransportType();
        this.provisionType = deviceProfile.getProvisionType();
        this.description = deviceProfile.getDescription();
        this.isDefault = deviceProfile.isDefault();
        this.profileData = JacksonUtil.convertValue(deviceProfile.getProfileData(), ObjectNode.class);
        if (deviceProfile.getDefaultRuleChainId() != null) {
            this.defaultRuleChainId = deviceProfile.getDefaultRuleChainId().getId();
        }
        if (deviceProfile.getDefaultDashboardId() != null) {
            this.defaultDashboardId = deviceProfile.getDefaultDashboardId().getId();
        }
        this.defaultQueueName = deviceProfile.getDefaultQueueName();
        this.provisionDeviceKey = deviceProfile.getProvisionDeviceKey();
        if (deviceProfile.getFirmwareId() != null) {
            this.firmwareId = deviceProfile.getFirmwareId().getId();
        }
        if (deviceProfile.getSoftwareId() != null) {
            this.softwareId = deviceProfile.getSoftwareId().getId();
        }
        if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
            this.defaultEdgeRuleChainId = deviceProfile.getDefaultEdgeRuleChainId().getId();
        }
        if (deviceProfile.getExternalId() != null) {
            this.externalId = deviceProfile.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfile toData() {
        DeviceProfile deviceProfile = new DeviceProfile(new DeviceProfileId(this.getUuid()));
        deviceProfile.setCreatedTime(createdTime);
        if (tenantId != null) {
            deviceProfile.setTenantId(TenantId.fromUUID(tenantId));
        }
        deviceProfile.setName(name);
        deviceProfile.setType(type);
        deviceProfile.setImage(image);
        deviceProfile.setTransportType(transportType);
        deviceProfile.setProvisionType(provisionType);
        deviceProfile.setDescription(description);
        deviceProfile.setDefault(isDefault);
        deviceProfile.setDefaultQueueName(defaultQueueName);
        deviceProfile.setProfileData(JacksonUtil.convertValue(profileData, DeviceProfileData.class));
        if (defaultRuleChainId != null) {
            deviceProfile.setDefaultRuleChainId(new RuleChainId(defaultRuleChainId));
        }
        if (defaultDashboardId != null) {
            deviceProfile.setDefaultDashboardId(new DashboardId(defaultDashboardId));
        }
        deviceProfile.setProvisionDeviceKey(provisionDeviceKey);

        if (firmwareId != null) {
            deviceProfile.setFirmwareId(new OtaPackageId(firmwareId));
        }
        if (softwareId != null) {
            deviceProfile.setSoftwareId(new OtaPackageId(softwareId));
        }
        if (defaultEdgeRuleChainId != null) {
            deviceProfile.setDefaultEdgeRuleChainId(new RuleChainId(defaultEdgeRuleChainId));
        }
        if (externalId != null) {
            deviceProfile.setExternalId(new DeviceProfileId(externalId));
        }

        return deviceProfile;
    }
}
