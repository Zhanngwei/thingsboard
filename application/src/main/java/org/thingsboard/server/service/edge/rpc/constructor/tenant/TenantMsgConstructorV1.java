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
package org.thingsboard.server.service.edge.rpc.constructor.tenant;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

/**
 * 中文说明：
 * 1. `TenantMsgConstructorV1` 是 ThingsBoard Application 中创建或提供租户对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TenantMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class TenantMsgConstructorV1 implements TenantMsgConstructor {

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 功能：执行 `constructTenantUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @Override
    public TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant) {
        TenantUpdateMsg.Builder builder = TenantUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenant.getId().getId().getMostSignificantBits())
                .setIdLSB(tenant.getId().getId().getLeastSignificantBits())
                .setTitle(tenant.getTitle())
                .setProfileIdMSB(tenant.getTenantProfileId().getId().getMostSignificantBits())
                .setProfileIdLSB(tenant.getTenantProfileId().getId().getLeastSignificantBits())
                .setRegion(tenant.getRegion());
        if (tenant.getCountry() != null) {
            builder.setCountry(tenant.getCountry());
        }
        if (tenant.getState() != null) {
            builder.setState(tenant.getState());
        }
        if (tenant.getCity() != null) {
            builder.setCity(tenant.getCity());
        }
        if (tenant.getAddress() != null) {
            builder.setAddress(tenant.getAddress());
        }
        if (tenant.getAddress2() != null) {
            builder.setAddress2(tenant.getAddress2());
        }
        if (tenant.getZip() != null) {
            builder.setZip(tenant.getZip());
        }
        if (tenant.getPhone() != null) {
            builder.setPhone(tenant.getPhone());
        }
        if (tenant.getEmail() != null) {
            builder.setEmail(tenant.getEmail());
        }
        if (tenant.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(tenant.getAdditionalInfo()));
        }
        return builder.build();
    }

    /**
     * 功能：执行 `constructTenantProfileUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `tenantProfile`：租户信息或租户标识。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public TenantProfileUpdateMsg constructTenantProfileUpdateMsg(UpdateMsgType msgType, TenantProfile tenantProfile, EdgeVersion edgeVersion) {
        tenantProfile = JacksonUtil.clone(tenantProfile);
        // clear all config
        var tenantProfileData = tenantProfile.getProfileData();
        var configuration = tenantProfile.getDefaultProfileConfiguration();
        configuration.setRpcTtlDays(0);
        configuration.setMaxJSExecutions(0);
        configuration.setMaxREExecutions(0);
        configuration.setMaxDPStorageDays(0);
        configuration.setMaxTbelExecutions(0);
        configuration.setQueueStatsTtlDays(0);
        configuration.setMaxTransportMessages(0);
        configuration.setDefaultStorageTtlDays(0);
        configuration.setMaxTransportDataPoints(0);
        configuration.setRuleEngineExceptionsTtlDays(0);
        configuration.setMaxRuleNodeExecutionsPerMessage(0);
        tenantProfileData.setConfiguration(configuration);
        tenantProfile.setProfileData(tenantProfileData);

        ByteString profileData = EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_6_2) ?
                ByteString.empty() : ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile.getProfileData()));
        TenantProfileUpdateMsg.Builder builder = TenantProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenantProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(tenantProfile.getId().getId().getLeastSignificantBits())
                .setName(tenantProfile.getName())
                .setDefault(tenantProfile.isDefault())
                .setIsolatedRuleChain(tenantProfile.isIsolatedTbRuleEngine())
                .setProfileDataBytes(profileData);
        if (tenantProfile.getDescription() != null) {
            builder.setDescription(tenantProfile.getDescription());
        }
        return builder.build();
    }

}
