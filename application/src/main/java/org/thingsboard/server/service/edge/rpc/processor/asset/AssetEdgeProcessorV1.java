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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `AssetEdgeProcessorV1` 是 ThingsBoard Application 中处理资产的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AssetEdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@TbCoreComponent
public class AssetEdgeProcessorV1 extends AssetEdgeProcessor {

    /**
     * 功能：执行 `constructAssetFromUpdateMsg` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `assetId`：资产IDID。
     * - `assetUpdateMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    protected Asset constructAssetFromUpdateMsg(TenantId tenantId, AssetId assetId, AssetUpdateMsg assetUpdateMsg) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetUpdateMsg.getName());
        asset.setCreatedTime(Uuids.unixTimestamp(assetId.getId()));
        asset.setType(assetUpdateMsg.getType());
        asset.setLabel(assetUpdateMsg.hasLabel() ? assetUpdateMsg.getLabel() : null);
        asset.setAdditionalInfo(assetUpdateMsg.hasAdditionalInfo()
                ? JacksonUtil.toJsonNode(assetUpdateMsg.getAdditionalInfo()) : null);

        UUID assetProfileUUID = safeGetUUID(assetUpdateMsg.getAssetProfileIdMSB(), assetUpdateMsg.getAssetProfileIdLSB());
        asset.setAssetProfileId(assetProfileUUID != null ? new AssetProfileId(assetProfileUUID) : null);

        CustomerId customerId = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB());
        asset.setCustomerId(customerId);
        return asset;
    }

    /**
     * 功能：更新客户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `asset`：`asset` 参数。
     * - `assetUpdateMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Asset asset, AssetUpdateMsg assetUpdateMsg) {
        CustomerId customerUUID = safeGetCustomerId(assetUpdateMsg.getCustomerIdMSB(), assetUpdateMsg.getCustomerIdLSB());
        asset.setCustomerId(customerUUID != null ? customerUUID : customerId);
    }
}
