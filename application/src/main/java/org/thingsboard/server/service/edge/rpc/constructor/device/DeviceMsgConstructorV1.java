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
package org.thingsboard.server.service.edge.rpc.constructor.device;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

/**
 * 中文说明：
 * 1. `DeviceMsgConstructorV1` 是 ThingsBoard Application 中创建或提供设备对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseDeviceMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class DeviceMsgConstructorV1 extends BaseDeviceMsgConstructor {

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：执行 `constructDeviceUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device) {
        DeviceUpdateMsg.Builder builder = DeviceUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits())
                .setName(device.getName())
                .setType(device.getType());
        if (device.getLabel() != null) {
            builder.setLabel(device.getLabel());
        }
        if (device.getCustomerId() != null) {
            builder.setCustomerIdMSB(device.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(device.getCustomerId().getId().getLeastSignificantBits());
        }
        if (device.getDeviceProfileId() != null) {
            builder.setDeviceProfileIdMSB(device.getDeviceProfileId().getId().getMostSignificantBits());
            builder.setDeviceProfileIdLSB(device.getDeviceProfileId().getId().getLeastSignificantBits());
        }
        if (device.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(device.getAdditionalInfo()));
        }
        if (device.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(device.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(device.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (device.getSoftwareId() != null) {
            builder.setSoftwareIdMSB(device.getSoftwareId().getId().getMostSignificantBits())
                    .setSoftwareIdLSB(device.getSoftwareId().getId().getLeastSignificantBits());
        }
        if (device.getDeviceData() != null) {
            builder.setDeviceDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(device.getDeviceData())));
        }
        return builder.build();
    }

    /**
     * 功能：执行 `constructDeviceCredentialsUpdatedMsg` 对应的处理。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        DeviceCredentialsUpdateMsg.Builder builder = DeviceCredentialsUpdateMsg.newBuilder()
                .setDeviceIdMSB(deviceCredentials.getDeviceId().getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceCredentials.getDeviceId().getId().getLeastSignificantBits());
        if (deviceCredentials.getCredentialsType() != null) {
            builder.setCredentialsType(deviceCredentials.getCredentialsType().name())
                    .setCredentialsId(deviceCredentials.getCredentialsId());
        }
        if (deviceCredentials.getCredentialsValue() != null) {
            builder.setCredentialsValue(deviceCredentials.getCredentialsValue());
        }
        return builder.build();
    }

    /**
     * 功能：执行 `constructDeviceProfileUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        deviceProfile = JacksonUtil.clone(deviceProfile);
        imageService.inlineImageForEdge(deviceProfile);
        DeviceProfileUpdateMsg.Builder builder = DeviceProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits())
                .setName(deviceProfile.getName())
                .setDefault(deviceProfile.isDefault())
                .setType(deviceProfile.getType().name())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(deviceProfile.getProfileData())));
        if (deviceProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(deviceProfile.getDefaultQueueName());
        }
        if (deviceProfile.getDescription() != null) {
            builder.setDescription(deviceProfile.getDescription());
        }
        if (deviceProfile.getTransportType() != null) {
            builder.setTransportType(deviceProfile.getTransportType().name());
        }
        if (deviceProfile.getProvisionType() != null) {
            builder.setProvisionType(deviceProfile.getProvisionType().name());
        }
        if (deviceProfile.getProvisionDeviceKey() != null) {
            builder.setProvisionDeviceKey(deviceProfile.getProvisionDeviceKey());
        }
        if (deviceProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(deviceProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (deviceProfile.getFirmwareId() != null) {
            builder.setFirmwareIdMSB(deviceProfile.getFirmwareId().getId().getMostSignificantBits())
                    .setFirmwareIdLSB(deviceProfile.getFirmwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getSoftwareId() != null) {
            builder.setSoftwareIdMSB(deviceProfile.getSoftwareId().getId().getMostSignificantBits())
                    .setSoftwareIdLSB(deviceProfile.getSoftwareId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultEdgeRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(deviceProfile.getDefaultEdgeRuleChainId().getId().getLeastSignificantBits());
        }
        if (deviceProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(deviceProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(deviceProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

}
