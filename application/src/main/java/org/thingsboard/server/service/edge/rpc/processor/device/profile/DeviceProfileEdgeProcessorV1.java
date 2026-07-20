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
package org.thingsboard.server.service.edge.rpc.processor.device.profile;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `DeviceProfileEdgeProcessorV1` 是 ThingsBoard Application 中处理设备配置的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `DeviceProfileEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@TbCoreComponent
public class DeviceProfileEdgeProcessorV1 extends DeviceProfileEdgeProcessor {

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 功能：执行 `constructDeviceProfileFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：处理结果。
     */
    @Override
    protected DeviceProfile constructDeviceProfileFromUpdateMsg(TenantId tenantId, DeviceProfileId deviceProfileId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setCreatedTime(Uuids.unixTimestamp(deviceProfileId.getId()));
        deviceProfile.setName(deviceProfileUpdateMsg.getName());
        deviceProfile.setDefault(deviceProfileUpdateMsg.getDefault());
        deviceProfile.setDescription(deviceProfileUpdateMsg.hasDescription() ? deviceProfileUpdateMsg.getDescription() : null);
        deviceProfile.setType(DeviceProfileType.valueOf(deviceProfileUpdateMsg.getType()));
        deviceProfile.setTransportType(deviceProfileUpdateMsg.hasTransportType()
                ? DeviceTransportType.valueOf(deviceProfileUpdateMsg.getTransportType()) : DeviceTransportType.DEFAULT);
        deviceProfile.setImage(deviceProfileUpdateMsg.hasImage()
                ? new String(deviceProfileUpdateMsg.getImage().toByteArray(), StandardCharsets.UTF_8) : null);
        deviceProfile.setProvisionType(deviceProfileUpdateMsg.hasProvisionType()
                ? DeviceProfileProvisionType.valueOf(deviceProfileUpdateMsg.getProvisionType()) : DeviceProfileProvisionType.DISABLED);
        deviceProfile.setProvisionDeviceKey(deviceProfileUpdateMsg.hasProvisionDeviceKey()
                ? deviceProfileUpdateMsg.getProvisionDeviceKey() : null);
        deviceProfile.setDefaultQueueName(deviceProfileUpdateMsg.getDefaultQueueName());

        Optional<DeviceProfileData> profileDataOpt =
                dataDecodingEncodingService.decode(deviceProfileUpdateMsg.getProfileDataBytes().toByteArray());
        deviceProfile.setProfileData(profileDataOpt.orElse(null));

        String defaultQueueName = StringUtils.isNotBlank(deviceProfileUpdateMsg.getDefaultQueueName())
                ? deviceProfileUpdateMsg.getDefaultQueueName() : null;
        deviceProfile.setDefaultQueueName(defaultQueueName);

        UUID firmwareUUID = safeGetUUID(deviceProfileUpdateMsg.getFirmwareIdMSB(), deviceProfileUpdateMsg.getFirmwareIdLSB());
        deviceProfile.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);

        UUID softwareUUID = safeGetUUID(deviceProfileUpdateMsg.getSoftwareIdMSB(), deviceProfileUpdateMsg.getSoftwareIdLSB());
        deviceProfile.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);
        return deviceProfile;
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    @Override
    protected void setDefaultRuleChainId(TenantId tenantId, DeviceProfile deviceProfile, RuleChainId ruleChainId) {
        deviceProfile.setDefaultRuleChainId(ruleChainId);
    }

    /**
     * 功能：更新规则链。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * - `ruleChainId`：规则链ID。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    protected void setDefaultEdgeRuleChainId(DeviceProfile deviceProfile, RuleChainId ruleChainId, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultEdgeRuleChainUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultRuleChainIdMSB(), deviceProfileUpdateMsg.getDefaultRuleChainIdLSB());
        deviceProfile.setDefaultEdgeRuleChainId(defaultEdgeRuleChainUUID != null ? new RuleChainId(defaultEdgeRuleChainUUID) : ruleChainId);
    }

    /**
     * 功能：更新仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `deviceProfile`：设备信息或设备标识。
     * - `deviceProfileUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    protected void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, DeviceProfile deviceProfile, DeviceProfileUpdateMsg deviceProfileUpdateMsg) {
        UUID defaultDashboardUUID = safeGetUUID(deviceProfileUpdateMsg.getDefaultDashboardIdMSB(), deviceProfileUpdateMsg.getDefaultDashboardIdLSB());
        deviceProfile.setDefaultDashboardId(defaultDashboardUUID != null ? new DashboardId(defaultDashboardUUID) : dashboardId);
    }
}
