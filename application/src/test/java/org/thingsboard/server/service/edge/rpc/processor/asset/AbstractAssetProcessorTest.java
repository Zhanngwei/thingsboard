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
package org.thingsboard.server.service.edge.rpc.processor.asset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessorTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.willReturn;

/**
 * 中文说明：
 * 1. `AbstractAssetProcessorTest` 是 ThingsBoard Application 中验证 `AbstractAssetProcessor` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessorTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public abstract class AbstractAssetProcessorTest extends BaseEdgeProcessorTest {


    /**
     * 资产ID，用于定位对应业务对象。
     */
    protected AssetId assetId;
    protected AssetProfileId assetProfileId;
    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    protected AssetProfile assetProfile;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        edgeId = new EdgeId(UUID.randomUUID());
        tenantId = new TenantId(UUID.randomUUID());
        assetId = new AssetId(UUID.randomUUID());
        assetProfileId = new AssetProfileId(UUID.randomUUID());

        assetProfile = new AssetProfile();
        assetProfile.setId(assetProfileId);
        assetProfile.setName("AssetProfile");
        assetProfile.setDefault(true);

        Asset asset = new Asset();
        asset.setAssetProfileId(assetProfileId);
        asset.setId(assetId);
        asset.setName("Asset");
        asset.setType(assetProfile.getName());

        edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(EdgeEventActionType.ADDED);


        willReturn(asset).given(assetService).findAssetById(tenantId, assetId);
        willReturn(assetProfile).given(assetProfileService).findAssetProfileById(tenantId, assetProfileId);
    }

    /**
     * 功能：更新资产配置。
     * 参数：
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - `expectedRuleChainIdLSB`：`expectedRuleChainIdLSB` 参数。
     * 返回：无。
     */
    protected void updateAssetProfileDefaultFields(long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                                                    long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        DashboardId dashboardId = getDashboardId(expectedDashboardIdMSB, expectedDashboardIdLSB);
        RuleChainId ruleChainId = getRuleChainId(expectedRuleChainIdMSB, expectedRuleChainIdLSB);

        assetProfile.setDefaultDashboardId(dashboardId);
        assetProfile.setDefaultEdgeRuleChainId(ruleChainId);

    }

    /**
     * 功能：执行 `verify` 对应的处理。
     * 参数：
     * - `downlinkMsg`：待处理消息。
     * - `expectedDashboardIdMSB`：`expectedDashboardIdMSB` 参数。
     * - `expectedDashboardIdLSB`：`expectedDashboardIdLSB` 参数。
     * - `expectedRuleChainIdMSB`：`expectedRuleChainIdMSB` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    protected void verify(DownlinkMsg downlinkMsg, long expectedDashboardIdMSB, long expectedDashboardIdLSB,
                          long expectedRuleChainIdMSB, long expectedRuleChainIdLSB) {
        AssetProfileUpdateMsg assetProfileUpdateMsg = downlinkMsg.getAssetProfileUpdateMsgList().get(0);
        assertNotNull(assetProfileUpdateMsg);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdMSB()).isEqualTo(expectedDashboardIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultDashboardIdLSB()).isEqualTo(expectedDashboardIdLSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdMSB()).isEqualTo(expectedRuleChainIdMSB);
        Assertions.assertThat(assetProfileUpdateMsg.getDefaultRuleChainIdLSB()).isEqualTo(expectedRuleChainIdLSB);
    }
}
