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

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `TenantMsgConstructorV2` 是 ThingsBoard Application 中创建或提供租户对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TenantMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class TenantMsgConstructorV2 implements TenantMsgConstructor {

    /**
     * 功能：执行 `constructTenantUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `tenant`：租户信息或租户标识。
     * 返回：处理结果。
     */
    @Override
    public TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant) {
        return TenantUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenant)).build();
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
        tenantProfile.getProfileData().setConfiguration(configuration);

        return TenantProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenantProfile)).build();
    }

}
