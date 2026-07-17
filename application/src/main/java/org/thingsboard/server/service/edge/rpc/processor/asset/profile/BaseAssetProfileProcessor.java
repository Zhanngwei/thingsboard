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
package org.thingsboard.server.service.edge.rpc.processor.asset.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

/**
 * 中文说明：
 * 1. `BaseAssetProfileProcessor` 是 ThingsBoard Application 中处理资产配置的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class BaseAssetProfileProcessor extends BaseEdgeProcessor {

    /**
     * 功能：保存或创建资产配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `assetProfileUpdateMsg`：待处理消息。
     * 返回：处理结果。
     */
    protected Pair<Boolean, Boolean> saveOrUpdateAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        boolean created = false;
        boolean assetProfileNameUpdated = false;
        assetCreationLock.lock();
        try {
            AssetProfile assetProfile = constructAssetProfileFromUpdateMsg(tenantId, assetProfileId, assetProfileUpdateMsg);
            if (assetProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] assetProfileUpdateMsg {" + assetProfileUpdateMsg + "} cannot be converted to asset profile");
            }
            AssetProfile assetProfileById = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
            if (assetProfileById == null) {
                created = true;
                assetProfile.setId(null);
                assetProfile.setDefault(false);
            } else {
                assetProfile.setId(assetProfileId);
                assetProfile.setDefault(assetProfileById.isDefault());
            }
            String assetProfileName = assetProfile.getName();
            AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfileName);
            if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                assetProfileName = assetProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Asset profile with name {} already exists. Renaming asset profile name to {}",
                        tenantId, assetProfile.getName(), assetProfileName);
                assetProfileNameUpdated = true;
            }
            assetProfile.setName(assetProfileName);

            RuleChainId ruleChainId = assetProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, assetProfile, created ? null : assetProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(assetProfile, ruleChainId, assetProfileUpdateMsg);
            setDefaultDashboardId(tenantId, created ? null : assetProfileById.getDefaultDashboardId(), assetProfile, assetProfileUpdateMsg);

            assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
            if (created) {
                assetProfile.setId(assetProfileId);
            }
            assetProfileService.saveAssetProfile(assetProfile, false);
        } catch (Exception e) {
            log.error("[{}] Failed to process asset profile update msg [{}]", tenantId, assetProfileUpdateMsg, e);
            throw e;
        } finally {
            assetCreationLock.unlock();
        }
        return Pair.of(created, assetProfileNameUpdated);
    }

    /**
     * 功能：执行 `constructAssetProfileFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfileId`：资产配置ID。
     * - `assetProfileUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    protected abstract AssetProfile constructAssetProfileFromUpdateMsg(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetProfile`：`assetProfile` 参数。
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    protected abstract void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId);

    /**
     * 功能：更新规则链。
     * 参数：
     * - `assetProfile`：`assetProfile` 参数。
     * - `ruleChainId`：规则链ID。
     * - `assetProfileUpdateMsg`：待处理消息。
     * 返回：无。
     */
    protected abstract void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg);

    /**
     * 功能：更新仪表盘ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `dashboardId`：仪表盘IDID。
     * - `assetProfile`：`assetProfile` 参数。
     * - `assetProfileUpdateMsg`：待处理消息。
     * 返回：无。
     */
    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg);
}
