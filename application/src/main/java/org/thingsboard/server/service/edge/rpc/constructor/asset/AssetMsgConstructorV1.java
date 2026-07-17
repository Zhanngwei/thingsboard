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
package org.thingsboard.server.service.edge.rpc.constructor.asset;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.nio.charset.StandardCharsets;

/**
 * 中文说明：
 * 1. `AssetMsgConstructorV1` 是 ThingsBoard Application 中创建或提供资产对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseAssetMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class AssetMsgConstructorV1 extends BaseAssetMsgConstructor {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：执行 `constructAssetUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset) {
        AssetUpdateMsg.Builder builder = AssetUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(asset.getUuidId().getMostSignificantBits())
                .setIdLSB(asset.getUuidId().getLeastSignificantBits())
                .setName(asset.getName())
                .setType(asset.getType());
        if (asset.getLabel() != null) {
            builder.setLabel(asset.getLabel());
        }
        if (asset.getCustomerId() != null) {
            builder.setCustomerIdMSB(asset.getCustomerId().getId().getMostSignificantBits());
            builder.setCustomerIdLSB(asset.getCustomerId().getId().getLeastSignificantBits());
        }
        if (asset.getAssetProfileId() != null) {
            builder.setAssetProfileIdMSB(asset.getAssetProfileId().getId().getMostSignificantBits());
            builder.setAssetProfileIdLSB(asset.getAssetProfileId().getId().getLeastSignificantBits());
        }
        if (asset.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(asset.getAdditionalInfo()));
        }
        return builder.build();
    }

    /**
     * 功能：执行 `constructAssetProfileUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, AssetProfile assetProfile) {
        assetProfile = JacksonUtil.clone(assetProfile);
        imageService.inlineImageForEdge(assetProfile);
        AssetProfileUpdateMsg.Builder builder = AssetProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(assetProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(assetProfile.getId().getId().getLeastSignificantBits())
                .setName(assetProfile.getName())
                .setDefault(assetProfile.isDefault());
        if (assetProfile.getDefaultDashboardId() != null) {
            builder.setDefaultDashboardIdMSB(assetProfile.getDefaultDashboardId().getId().getMostSignificantBits())
                    .setDefaultDashboardIdLSB(assetProfile.getDefaultDashboardId().getId().getLeastSignificantBits());
        }
        if (assetProfile.getDefaultQueueName() != null) {
            builder.setDefaultQueueName(assetProfile.getDefaultQueueName());
        }
        if (assetProfile.getDescription() != null) {
            builder.setDescription(assetProfile.getDescription());
        }
        if (assetProfile.getImage() != null) {
            builder.setImage(ByteString.copyFrom(assetProfile.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (assetProfile.getDefaultEdgeRuleChainId() != null) {
            builder.setDefaultRuleChainIdMSB(assetProfile.getDefaultEdgeRuleChainId().getId().getMostSignificantBits())
                    .setDefaultRuleChainIdLSB(assetProfile.getDefaultEdgeRuleChainId().getId().getLeastSignificantBits());
        }
        return builder.build();
    }

}
